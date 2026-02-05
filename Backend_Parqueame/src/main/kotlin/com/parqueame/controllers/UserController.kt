package com.parqueame.controllers

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.parqueame.DatabaseFactory
import com.parqueame.models.*
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.SerializationException
import org.mindrot.jbcrypt.BCrypt

fun Route.usuarioController() {

    route("/usuarios") {

        // 🔑 Login de usuario
        post("/login") {
            try {
                val request = call.receive<LoginRequest>() // { email, password }

                // Colección tipada a Usuario (driver oficial): usa find(...).first()
                val usuario: Usuario? = DatabaseFactory.usuariosCollection
                    .find(Filters.eq("correo", request.email))
                    .first()

                val credencialesOk = usuario != null && BCrypt.checkpw(request.password, usuario.contrasena)
                if (!credencialesOk) {
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

                // ✅ Aquí ya no usamos !! — usuario no es nulo gracias al return temprano
                val u: Usuario = usuario

                val response = LoginSuccessResponse(
                    success = true,
                    message = "Login exitoso",
                    user = LoginUserData(
                        id = u._id.toHexString(),
                        correo = u.correo,
                        nombre = u.nombre,
                        tipo = u.tipo.name,                   // CONDUCTOR / EMPRESA
                        tipoDocumento = u.tipoDocumento.name, // CEDULA / RNC
                        documento = u.documento,
                        imagenesURL = u.imagenesURL,
                        estado = u.estado
                    )
                )

                call.respond(HttpStatusCode.OK, response)

            } catch (e: Exception) {
                // ✅ usamos 'e' para evitar el warning "Parameter e is never used"
                e.printStackTrace()
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf(
                        "success" to false,
                        "message" to "Error interno del servidor",
                        "details" to (e.message ?: "desconocido")
                    )
                )
            }
        }

        // 📌 Obtener todos los usuarios
        get {
            try {
                val usuarios: List<Usuario> = DatabaseFactory.usuariosCollection.find().toList()

                if (usuarios.isEmpty()) {
                    call.respond(
                        HttpStatusCode.NoContent,
                        mapOf("message" to "No hay usuarios registrados")
                    )
                    return@get
                }

                call.respond(HttpStatusCode.OK, usuarios)
            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Error interno del servidor", "details" to (e.message ?: "desconocido"))
                )
            }
        }

        // 📌 Crear nuevo usuario
        post {
            try {
                val usuario = try {
                    call.receive<Usuario>()
                } catch (e: SerializationException) {
                    // ✅ usamos e.message
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Error al deserializar el cuerpo de la solicitud", "details" to (e.message ?: ""))
                    )
                    return@post
                }

                if (usuario.nombre.isBlank() || usuario.correo.isBlank() || usuario.documento.isBlank()) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Nombre, correo y documento son requeridos")
                    )
                    return@post
                }

                if (DatabaseFactory.isCorreoExistente(usuario.correo)) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "El correo electrónico ya está registrado")
                    )
                    return@post
                }

                if (DatabaseFactory.isDocumentoExistente(usuario.documento)) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "El número de documento ya está registrado")
                    )
                    return@post
                }

                val encryptedPassword = encryptPassword(usuario.contrasena)
                val nuevoUsuario = usuario.copy(contrasena = encryptedPassword)

                val result = DatabaseFactory.usuariosCollection.insertOne(nuevoUsuario)
                val id = result.insertedId?.asObjectId()?.value?.toHexString()

                if (result.wasAcknowledged()) {
                    call.respond(
                        HttpStatusCode.Created,
                        mapOf("message" to "Usuario creado correctamente", "id" to id)
                    )
                } else {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf("error" to "Error al crear el usuario")
                    )
                }

            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Error interno del servidor", "details" to (e.message ?: "desconocido"))
                )
            }
        }

        // 📌 Obtener un usuario por correo (para perfil)
        get("/me") {
            try {
                val correo = call.request.queryParameters["correo"]

                if (correo.isNullOrBlank()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Correo requerido"))
                    return@get
                }

                val usuario: Usuario? = DatabaseFactory.usuariosCollection
                    .find(Filters.eq("correo", correo))
                    .first()

                if (usuario == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Usuario no encontrado"))
                    return@get
                }

                val response = mapOf(
                    "nombre" to usuario.nombre,
                    "rol" to usuario.tipo.name,
                    "fotoUrl" to usuario.imagenesURL
                )

                call.respond(HttpStatusCode.OK, response)
            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Error interno del servidor", "details" to (e.message ?: "desconocido"))
                )
            }
        }

        // 📌 Actualizar nombre y/o foto de perfil
        put("/actualizar") {
            try {
                val request = call.receive<ActualizarPerfilRequest>()

                if (request.correo.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Correo requerido"))
                    return@put
                }

                val updates = mutableListOf<org.bson.conversions.Bson>()

                request.nuevoNombre?.takeIf { it.isNotBlank() }?.let {
                    updates.add(Updates.set("nombre", it))
                }

                // aunque sea null, actualizamos explícitamente el campo imagenesURL
                updates.add(Updates.set("imagenesURL", request.nuevaFotoUrl))

                if (updates.isEmpty()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Nada que actualizar"))
                    return@put
                }

                val result = DatabaseFactory.usuariosCollection.updateOne(
                    Filters.eq("correo", request.correo),
                    Updates.combine(updates)
                )

                if (result.matchedCount == 0L) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Usuario no encontrado"))
                    return@put
                }

                call.respond(HttpStatusCode.OK, mapOf("message" to "Perfil actualizado correctamente"))
            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Error actualizando perfil", "details" to (e.message ?: "desconocido"))
                )
            }
        }
    }
}

// 🔐 Función para encriptar contraseña
fun encryptPassword(password: String): String = BCrypt.hashpw(password, BCrypt.gensalt())
