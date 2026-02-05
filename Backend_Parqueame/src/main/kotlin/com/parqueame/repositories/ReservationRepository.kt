package com.parqueame.repositories

import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.`in`
import com.mongodb.client.model.Filters.gte
import com.mongodb.client.model.Filters.lte
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes.ascending
import com.mongodb.client.model.Updates.set
import com.parqueame.models.Reservation
import com.parqueame.util.ReservationStatusUtil
import org.bson.types.ObjectId

class ReservationRepository(
    private val col: MongoCollection<Reservation>
) {
    // Para queries Mongo por "status" (string)
    private val activeStatusNames: Set<String> = ReservationStatusUtil.activeStatusNames()

    // -------------------- CREATE --------------------
    fun insert(reservation: Reservation): Reservation {
        col.insertOne(reservation)
        return reservation
    }

    // -------------------- READ --------------------
    fun findById(id: String): Reservation? {
        val oid = runCatching { ObjectId(id) }.getOrNull() ?: return null
        return col.find(eq("_id", oid)).firstOrNull()
    }

    fun listByUser(userId: String): List<Reservation> =
        col.find(eq("userId", userId)).toList()

    fun listByParking(parkingId: String): List<Reservation> =
        col.find(eq("parkingId", parkingId)).toList()

    /**
     * Cuenta reservas activas que solapan [startMin, endMin) en un parqueo dado.
     * Regla de solape: startA < endB && endA > startB
     */
    fun countOverlaps(parkingId: String, startMin: Long, endMin: Long): Int {
        val filter = and(
            eq("parkingId", parkingId),
            `in`("status", activeStatusNames),
            lte("startEpochMin", endMin - 1), // start < endB
            gte("endEpochMin", startMin + 1)  // end   > startB
        )
        return col.countDocuments(filter).toInt()
    }

    // -------------------- UPDATE --------------------
    fun cancel(id: String): Boolean {
        val oid = runCatching { ObjectId(id) }.getOrNull() ?: return false
        val canceledName = ReservationStatusUtil.cancelStatusName()
        val result = col.updateOne(eq("_id", oid), set("status", canceledName))
        return result.modifiedCount > 0
    }

    // -------------------- INDEXES --------------------
    fun ensureIndexes() {
        // Por usuario + estado
        col.createIndex(ascending("userId", "status"), IndexOptions().background(true))
        // Por parqueo + estado
        col.createIndex(ascending("parkingId", "status"), IndexOptions().background(true))
        // Para availability/overlaps: parqueo + ventana de tiempo
        col.createIndex(ascending("parkingId", "startEpochMin", "endEpochMin"), IndexOptions().background(true))
        // Opcionales
        col.createIndex(ascending("status"), IndexOptions().background(true))
        col.createIndex(ascending("createdAt"), IndexOptions().background(true))
    }
}