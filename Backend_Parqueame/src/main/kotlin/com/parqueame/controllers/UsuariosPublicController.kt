package com.parqueame.controllers

import com.parqueame.DatabaseFactory
import com.parqueame.models.Usuario
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId

@Serializable
data class UsuarioPublicoDto(
    val nombre: String,
    val rol: String,
    val fotoUrl: String?
)

fun Route.usuarioPublicController() {

    // GET /usuarios/{id}/public → perfil público (nombre, rol, foto)
    get("/usuarios/{id}/public") {
        val idHex = call.parameters["id"]?.trim().orEmpty()
        if (idHex.isEmpty() || !ObjectId.isValid(idHex)) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "id inválido"))
            return@get
        }

        try {
            // La colección de usuarios usa _id: ObjectId
            val user: Usuario? = DatabaseFactory.usuariosCollection.findOneById(ObjectId(idHex))
            if (user == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Usuario no encontrado"))
                return@get
            }

            val dto = UsuarioPublicoDto(
                nombre  = user.nombre,
                rol     = user.tipo.name,                   // enum → String
                fotoUrl = user.imagenesURL?.ifBlank { null } // nullable y puede venir vacío
            )
            call.respond(HttpStatusCode.OK, dto)
        } catch (t: Throwable) {
            t.printStackTrace()
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("error" to (t.message ?: "Error interno"))
            )
        }
    }
}