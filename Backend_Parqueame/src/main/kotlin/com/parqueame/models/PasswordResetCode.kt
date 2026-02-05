package com.parqueame.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.Contextual
import org.litote.kmongo.Id
import org.litote.kmongo.newId
//import org.litote.kmongo.id.serialization.IdKSerializer
import java.util.Date

@Serializable
data class PasswordResetCode(
    //val _id: Id<PasswordResetCode> = newId(),
    val email: String,
    val code: String,
    val expiresAt: Long,     // millis para lógica
    // Date para TTL: marcar como contextual para que KMongo la trate como BSON Date
    @Contextual
    val expiresAtDate: Date  // Date para TTL de Mongo
)
