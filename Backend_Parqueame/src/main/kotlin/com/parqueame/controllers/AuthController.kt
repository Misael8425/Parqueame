package com.parqueame.controllers

import com.parqueame.services.AuthService
import com.parqueame.services.*
import com.parqueame.models.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import org.mindrot.jbcrypt.BCrypt

fun Route.authController() {

    val authService = AuthService()

    route("/auth") {

        // ───────────────── TEST: verificar configuración SendGrid
        get("/test-sendgrid") {
            try {
                val config = SendGridService.testConfiguration()
                val isVerified = SendGridService.checkSenderVerification()

                val fullReport = buildString {
                    append(config)
                    append("\n🔍 Estado detallado:\n")
                    append("- Sender verificado: ${if (isVerified) "✅ Listo para enviar" else "❌ Requiere verificación"}\n")
                    if (!isVerified) {
                        append("\n📋 Pasos para solucionar:\n")
                        append("1. Ve a SendGrid Dashboard\n")
                        append("2. Settings → Sender Authentication → Single Sender Verification\n")
                        append("3. Verifica 'parqueame.do@gmail.com'\n")
                        append("4. Revisa el Gmail y confirma\n")
                    }
                }

                call.respond(HttpStatusCode.OK, mapOf("config" to fullReport))
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Error verificando SendGrid: ${e.message}")
                )
            }
        }

        // ───────────────── RESET: solicitar código (responder primero, email en background)
        post("/request-reset") {
            try {
                val body = call.receive<EmailRequest>()
                val email = body.email.trim().lowercase()

                // Validar formato de email básico
                if (!email.contains("@") || email.length < 5) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse(false, "Formato de correo inválido")
                    )
                    return@post
                }

                val exists = try {
                    withTimeout(5_000) { authService.userExists(email) }
                } catch (_: TimeoutCancellationException) {
                    call.respond(
                        HttpStatusCode.ServiceUnavailable,
                        ApiResponse(false, "Servicio de base de datos lento, inténtalo de nuevo")
                    )
                    return@post
                }

                if (!exists) {
                    call.respond(
                        HttpStatusCode.NotFound,
                        ApiResponse(false, "Correo no registrado")
                    )
                    return@post
                }

                val code = try {
                    withTimeout(5_000) { authService.generateResetCode(email) }
                } catch (_: TimeoutCancellationException) {
                    call.respond(
                        HttpStatusCode.ServiceUnavailable,
                        ApiResponse(false, "Servicio de base de datos lento, inténtalo de nuevo")
                    )
                    return@post
                }

                // Responder de inmediato para evitar timeouts en el cliente
                call.respond(HttpStatusCode.OK, ApiResponse(true, "Código generado y enviado al correo"))

                // Enviar el correo en background (no bloquea la respuesta)
                call.application.launch {
                    try {
                        // Verificar primero si SendGrid está configurado correctamente
                        val isVerified = SendGridService.checkSenderVerification()
                        if (!isVerified) {
                            call.application.log.error("SendGrid sender no verificado. Ve a Dashboard para verificar 'parqueame.do@gmail.com'")
                            return@launch
                        }

                        val success = SendGridService.sendResetCode(email, code)
                        if (success) {
                            call.application.log.info("✅ Código enviado exitosamente a $email")
                        } else {
                            call.application.log.error("❌ Error enviando código a $email")
                        }
                    } catch (e: Exception) {
                        call.application.log.error("❌ Error enviando reset a $email: ${e.message}", e)
                    }
                }

            } catch (e: Exception) {
                call.application.log.error("Error en request-reset", e)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse(false, "Error interno del servidor: ${e.message}")
                )
            }
        }

        // ───────────────── RESET: verificar código
        post("/verify-code") {
            try {
                val body = call.receive<CodeVerificationRequest>()
                val email = body.email.trim().lowercase()
                val code = body.code.trim()

                if (email.isBlank() || code.isBlank()) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse(false, "Email y código son requeridos")
                    )
                    return@post
                }

                val isValid = try {
                    withTimeout(5_000) { authService.validateCode(email, code) }
                } catch (_: TimeoutCancellationException) {
                    call.respond(
                        HttpStatusCode.ServiceUnavailable,
                        ApiResponse(false, "Servicio de base de datos lento, inténtalo de nuevo")
                    )
                    return@post
                }

                if (!isValid) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse(false, "Código inválido o expirado")
                    )
                    return@post
                }

                call.respond(HttpStatusCode.OK, ApiResponse(true, "Código válido"))
            } catch (e: Exception) {
                call.application.log.error("Error en verify-code", e)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse(false, "Error interno del servidor: ${e.message}")
                )
            }
        }

        // En AuthController - endpoint reset-password corregido
        post("/reset-password") {
            try {
                val body = call.receive<ResetPasswordRequest>()
                val email = body.email.trim().lowercase()
                val code = body.code.trim()
                val newPassword = body.newPassword.trim()

                if (email.isBlank() || code.isBlank() || newPassword.isBlank()) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse(false, "Todos los campos son requeridos")
                    )
                    return@post
                }

                if (newPassword.length < 6) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse(false, "La contraseña debe tener al menos 6 caracteres")
                    )
                    return@post
                }

                call.application.log.info("Reset password request for $email with code $code")

                // VERIFICAR PRIMERO SI EL CÓDIGO EXISTE Y ES VÁLIDO
                val isValidCode = try {
                    withTimeout(5_000) { authService.validateCode(email, code) }
                } catch (_: TimeoutCancellationException) {
                    call.respond(
                        HttpStatusCode.ServiceUnavailable,
                        ApiResponse(false, "Servicio de base de datos lento, inténtalo de nuevo")
                    )
                    return@post
                }

                if (!isValidCode) {
                    call.application.log.warn("Invalid or expired code for $email")
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse(false, "Código inválido o expirado")
                    )
                    return@post
                }

                // VERIFICAR SI EL USUARIO EXISTE
                val userExists = try {
                    withTimeout(5_000) { authService.userExists(email) }
                } catch (_: TimeoutCancellationException) {
                    call.respond(
                        HttpStatusCode.ServiceUnavailable,
                        ApiResponse(false, "Servicio de base de datos lento, inténtalo de nuevo")
                    )
                    return@post
                }

                if (!userExists) {
                    call.application.log.warn("User not found for email: $email")
                    call.respond(
                        HttpStatusCode.NotFound,
                        ApiResponse(false, "Usuario no encontrado")
                    )
                    return@post
                }

                // INTENTAR ACTUALIZAR LA CONTRASEÑA
                val success = try {
                    withTimeout(10_000) { authService.resetPassword(email, code, newPassword) }
                } catch (_: TimeoutCancellationException) {
                    call.respond(
                        HttpStatusCode.ServiceUnavailable,
                        ApiResponse(false, "Servicio de base de datos lento, inténtalo de nuevo")
                    )
                    return@post
                }

                if (!success) {
                    call.application.log.error("Failed to reset password for $email")
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse(false, "Error al actualizar la contraseña")
                    )
                    return@post
                }

                call.application.log.info("Password successfully reset for $email")
                call.respond(HttpStatusCode.OK, ApiResponse(true, "Contraseña actualizada exitosamente"))

            } catch (e: Exception) {
                call.application.log.error("Error en reset-password", e)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse(false, "Error interno del servidor: ${e.message}")
                )
            }
        }

        // ───────────────── LOGIN
        post("/login") {
            try {
                val body = call.receive<LoginRequest>()
                val email = body.email.trim()
                val password = body.password

                if (email.isBlank() || password.isBlank()) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse(false, "Correo y contraseña son requeridos")
                    )
                    return@post
                }

                val user = authService.getUserByEmail(email)

                if (user == null || !BCrypt.checkpw(password, user.contrasena)) {
                    call.respond(
                        HttpStatusCode.Unauthorized,
                        LoginSuccessResponse(
                            success = false,
                            message = "Credenciales inválidas",
                            user = LoginUserData(
                                id = "",
                                correo = "",
                                nombre = "",
                                tipo = "",
                                tipoDocumento = "",
                                documento = "",
                                imagenesURL = null,
                                estado = false
                            )
                        )
                    )
                    return@post
                }

                val response = LoginSuccessResponse(
                    success = true,
                    message = "Inicio de sesión exitoso",
                    user = LoginUserData(
                        id = user._id.toHexString(),
                        correo = user.correo,
                        nombre = user.nombre,
                        tipo = user.tipo.name,
                        tipoDocumento = user.tipoDocumento.name,
                        documento = user.documento,
                        imagenesURL = user.imagenesURL,
                        estado = user.estado
                    )
                )

                call.respond(HttpStatusCode.OK, response)

            } catch (e: Exception) {
                call.application.log.error("Error en login", e)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse(false, "Error interno del servidor: ${e.message}")
                )
            }
        }
    }
}