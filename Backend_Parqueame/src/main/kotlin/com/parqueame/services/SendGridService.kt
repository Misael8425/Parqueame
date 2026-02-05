package com.parqueame.services

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class SendGridEmail(
    val personalizations: List<Personalization>,
    val from: EmailAddress,
    val reply_to: EmailAddress? = null, // Agregado campo reply_to
    val subject: String,
    val content: List<Content>
)

@Serializable
data class Personalization(
    val to: List<EmailAddress>
)

@Serializable
data class EmailAddress(
    val email: String,
    val name: String? = null
)

@Serializable
data class Content(
    val type: String,
    val value: String
)

object SendGridService {

    // Usar la dirección verificada de parqueame.do
    private val apiKey = System.getenv("SENDGRID_API_KEY")
    private val fromEmail = System.getenv("FROM_EMAIL") ?: "parqueame.do@gmail.com"
    private val replyToEmail = System.getenv("REPLY_TO_EMAIL") ?: "parqueame.do@gmail.com"

    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = false
            })
        }
    }

    suspend fun sendResetCode(toEmail: String, code: String): Boolean {
        println("Verificando configuración SendGrid...")
        require(!apiKey.isNullOrBlank()) { "SENDGRID_API_KEY no configurado" }

        println("Preparando email para: $toEmail")

        val emailData = SendGridEmail(
            personalizations = listOf(
                Personalization(
                    to = listOf(EmailAddress(toEmail))
                )
            ),
            from = EmailAddress(fromEmail, "Parquéame"),
            reply_to = EmailAddress(replyToEmail, "Parquéame Soporte"), // Reply-to configurado
            subject = "Tu código de recuperación - Parquéame",
            content = listOf(
                Content(
                    type = "text/plain",
                    value = """
                        Hola,

                        Tu código de recuperación para Parquéame es: $code

                        Este código expira en 5 minutos.

                        Si no solicitaste este cambio, ignora este mensaje.

                        — Equipo Parquéame
                    """.trimIndent()
                ),
                Content(
                    type = "text/html",
                    value = """
                        <div style="font-family:system-ui,-apple-system,Segoe UI,Roboto,sans-serif;line-height:1.6;max-width:600px;margin:0 auto;padding:20px;">
                          <div style="text-align:center;margin-bottom:30px;">
                            <h1 style="color:#005BC1;font-size:28px;margin:0;">Parquéame</h1>
                          </div>
                          
                          <div style="background:#f8f9fa;padding:30px;border-radius:8px;border-left:4px solid #005BC1;">
                            <h2 style="color:#333;margin:0 0 20px;font-size:22px;">Código de recuperación</h2>
                            <p style="color:#666;margin:0 0 20px;font-size:16px;">
                              Usa este código para reestablecer tu contraseña:
                            </p>
                            <div style="text-align:center;margin:25px 0;">
                              <span style="display:inline-block;background:#005BC1;color:white;padding:15px 25px;border-radius:8px;font-size:24px;font-weight:700;letter-spacing:3px;font-family:monospace;">$code</span>
                            </div>
                            <p style="color:#666;font-size:14px;margin:20px 0 0;">
                              ⏰ Este código expira en <strong>5 minutos</strong>.
                            </p>
                          </div>
                          
                          <div style="margin-top:30px;padding-top:20px;border-top:1px solid #eee;text-align:center;">
                            <p style="color:#999;font-size:13px;margin:0;">
                              Si no solicitaste este cambio, puedes ignorar este mensaje de forma segura.
                            </p>
                            <p style="color:#999;font-size:12px;margin:10px 0 0;">
                              Para soporte, responde a este correo.
                            </p>
                          </div>
                        </div>
                    """.trimIndent()
                )
            )
        )

        return try {
            println("Enviando email via SendGrid...")
            val response = httpClient.post("https://api.sendgrid.com/v3/mail/send") {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $apiKey")
                setBody(emailData)
            }

            when {
                response.status.isSuccess() -> {
                    println("✅ Email enviado exitosamente a $toEmail")
                    true
                }
                response.status == HttpStatusCode.Unauthorized -> {
                    println("❌ Error SendGrid: API key inválida")
                    false
                }
                response.status == HttpStatusCode.Forbidden -> {
                    val errorBody = response.bodyAsText()
                    println("❌ Error SendGrid 403 (Sender Identity): $errorBody")
                    println("⚠️  Verifica que '$fromEmail' esté verificado en SendGrid Dashboard")
                    false
                }
                else -> {
                    val errorBody = response.bodyAsText()
                    println("❌ Error SendGrid: ${response.status} - $errorBody")
                    false
                }
            }
        } catch (e: Exception) {
            println("❌ Error de conexión SendGrid: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    suspend fun testConfiguration(): String {
        val status = StringBuilder()
        status.append("📧 Configuración SendGrid:\n")
        status.append("- API_KEY: ${if (!apiKey.isNullOrBlank()) "✅ Configurado (${apiKey.take(6)}...)" else "❌ No configurado"}\n")
        status.append("- FROM_EMAIL: $fromEmail\n")
        status.append("- REPLY_TO_EMAIL: $replyToEmail\n")

        // Test de conectividad
        if (!apiKey.isNullOrBlank()) {
            try {
                val response = httpClient.get("https://api.sendgrid.com/v3/user/profile") {
                    header("Authorization", "Bearer $apiKey")
                }
                status.append("- Conectividad: ${if (response.status.isSuccess()) "✅ OK" else "❌ Error: ${response.status}"}\n")

                // Test de sender verification
                try {
                    val senderResponse = httpClient.get("https://api.sendgrid.com/v3/verified_senders") {
                        header("Authorization", "Bearer $apiKey")
                    }
                    if (senderResponse.status.isSuccess()) {
                        val body = senderResponse.bodyAsText()
                        val isVerified = body.contains(fromEmail)
                        status.append("- Sender '$fromEmail': ${if (isVerified) "✅ Verificado" else "❌ No verificado"}\n")
                        if (!isVerified) {
                            status.append("  → Ve a SendGrid Dashboard → Settings → Sender Authentication\n")
                        }
                    }
                } catch (e: Exception) {
                    status.append("- Sender verification check: ❌ Error: ${e.message}\n")
                }

            } catch (e: Exception) {
                status.append("- Conectividad: ❌ Error: ${e.message}\n")
            }
        }

        return status.toString()
    }

    // Función para verificar si un sender está verificado
    suspend fun checkSenderVerification(email: String = fromEmail): Boolean {
        if (apiKey.isNullOrBlank()) return false

        return try {
            val response = httpClient.get("https://api.sendgrid.com/v3/verified_senders") {
                header("Authorization", "Bearer $apiKey")
            }
            if (response.status.isSuccess()) {
                val body = response.bodyAsText()
                body.contains(email)
            } else false
        } catch (e: Exception) {
            println("Error verificando sender: ${e.message}")
            false
        }
    }
}