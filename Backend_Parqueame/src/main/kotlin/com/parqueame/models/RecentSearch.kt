package com.parqueame.models

import kotlinx.serialization.Serializable
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId
import java.time.Instant

@Serializable
data class RecentSearch(
    @BsonId val id: String = ObjectId().toHexString(),
    val userId: String,                 // del JWT
    val name: String,
    val address: String,
    val lat: Double,
    val lng: Double,
    val originLat: Double? = null,
    val originLng: Double? = null,
    val distanceMeters: Double? = null, // guardado para no recalcular siempre
    val createdAt: Long = Instant.now().toEpochMilli()
)