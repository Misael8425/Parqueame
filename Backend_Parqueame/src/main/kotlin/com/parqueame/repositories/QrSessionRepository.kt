package com.parqueame.repositories

import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import com.parqueame.models.QrSession
import org.bson.types.ObjectId
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.eq
import org.litote.kmongo.setValue
import java.time.Instant

// ✅ Se usa CoroutineCollection<QrSession> de KMongo
class QrSessionRepository(private val col: CoroutineCollection<QrSession>) {

    suspend fun ensureIndexes() {
        col.createIndex(Indexes.ascending("token"), IndexOptions().unique(true).background(true))
        col.createIndex(Indexes.ascending("reservationId"), IndexOptions().background(true))
        col.createIndex(Indexes.ascending("expiresAt"), IndexOptions().background(true))
    }

    suspend fun insert(s: QrSession) {
        col.insertOne(s)
    }

    suspend fun findByToken(token: String): QrSession? {
        // ✅ Consulta type-safe con KMongo
        return col.findOne(QrSession::token eq token)
    }

    suspend fun markValidated(token: String): Boolean {
        val now = Instant.now().toEpochMilli()
        val result = col.updateOne(
            // ✅ Búsqueda type-safe
            QrSession::token eq token,
            // ✅ Actualización type-safe, el compilador sabe que validatedAt es un Long?
            setValue(QrSession::validatedAt, now)
        )
        return result.modifiedCount > 0
    }

    suspend fun deleteById(id: ObjectId) {
        col.deleteOneById(id)
    }
}