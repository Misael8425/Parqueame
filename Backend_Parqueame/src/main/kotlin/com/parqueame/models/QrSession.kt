package com.parqueame.models

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId
import java.time.Instant

@Serializable
data class QrSession(
    @BsonId @Contextual
    val _id: ObjectId = ObjectId(),

    val token: String,
    val reservationId: String,
    val parkingId: String,
    val userId: String,

    val createdAt: Long = Instant.now().toEpochMilli(),
    val expiresAt: Long,
    val validatedAt: Long? = null
)