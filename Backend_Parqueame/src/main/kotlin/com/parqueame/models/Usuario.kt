package com.parqueame.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

@Serializable
enum class TipoUsuario {
    CONDUCTOR, ADMINISTRADOR, PROPIETARIO, EMPRESA
}

@Serializable
enum class TipoDocumento {
    CEDULA, RNC
}

@Serializable
data class Usuario(
    @BsonId
    @Transient // evita errores de serialización al responder al cliente
    val _id: ObjectId = ObjectId(), // lo maneja MongoDB internamente

    val nombre: String,
    val correo: String,
    val contrasena: String,
    val tipo: TipoUsuario,
    val tipoDocumento: TipoDocumento,
    val documento: String,
    val imagenesURL: String? = null,
    val estado: Boolean = true
)

@Serializable
data class ActualizarPerfilRequest(
    val correo: String,
    val nuevoNombre: String? = null,
    val nuevaFotoUrl: String? = null
)
