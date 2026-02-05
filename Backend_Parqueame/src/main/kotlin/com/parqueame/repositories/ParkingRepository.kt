package com.parqueame.repositories

import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Filters
import com.parqueame.models.ParkingLot
import com.parqueame.models.ScheduleRange
import com.parqueame.models.WeekDay
import org.bson.types.ObjectId
import java.time.*

class ParkingRepository(private val col: MongoCollection<ParkingLot>) {

    fun getById(id: String): ParkingLot? =
        runCatching { ObjectId(id) }.getOrNull()
            ?.let { col.find(Filters.eq("_id", it)).firstOrNull() }

    /**
     * Verifica si el rango [startEpochMin, endEpochMin) está ABIERTO (tz-aware),
     * soportando franjas diurnas y franjas nocturnas (que cruzan medianoche).
     */
    fun isOpenInRange(
        parking: ParkingLot,
        startEpochMin: Long,
        endEpochMin: Long,
        tz: ZoneId
    ): Boolean {
        require(endEpochMin > startEpochMin) { "endEpochMin debe ser mayor que startEpochMin" }
        if (parking.daysOfWeek.isEmpty() || parking.schedules.isEmpty()) return false

        val startZdt = epochMinToZdt(startEpochMin, tz)
        val endZdt = epochMinToZdt(endEpochMin, tz)

        // Caso: mismo día
        if (startZdt.toLocalDate().isEqual(endZdt.toLocalDate())) {
            val dayOk = dayEnabled(parking, startZdt.dayOfWeek)
            if (!dayOk) return false
            return isWindowInsideAnyScheduleSameDay(
                sLocal = startZdt.toLocalTime(),
                eLocal = endZdt.toLocalTime(),
                schedules = parking.schedules
            )
        }

        // Caso: cruza de día
        val startDayEnabled = dayEnabled(parking, startZdt.dayOfWeek)
        val endDayEnabled = dayEnabled(parking, endZdt.dayOfWeek)
        if (!startDayEnabled && !endDayEnabled) return false

        val sLocal = startZdt.toLocalTime()
        val eLocal = endZdt.toLocalTime()

        val startDayAllows = isStartCoveredOnStartDay(sLocal, parking.schedules) && startDayEnabled
        val endDayAllows = isEndCoveredOnEndDay(eLocal, parking.schedules) && endDayEnabled

        return startDayAllows && endDayAllows
    }

    // ---------------- Helpers ----------------

    private fun isWindowInsideAnyScheduleSameDay(
        sLocal: LocalTime,
        eLocal: LocalTime,
        schedules: List<ScheduleRange>
    ): Boolean {
        return schedules.any { sr ->
            val (oH, oM) = parseHHmm(sr.open)
            val (cH, cM) = parseHHmm(sr.close)
            val o = LocalTime.of(oH, oM)
            val c = LocalTime.of(cH, cM)

            if (o <= c) {
                // Diurna: 08:00–18:00
                !sLocal.isBefore(o) && !eLocal.isAfter(c)
            } else {
                // Nocturna: 22:00–02:00 (cruza medianoche)
                !sLocal.isBefore(o)
            }
        }
    }

    private fun isStartCoveredOnStartDay(
        sLocal: LocalTime,
        schedules: List<ScheduleRange>
    ): Boolean {
        return schedules.any { sr ->
            val (oH, oM) = parseHHmm(sr.open)
            val (cH, cM) = parseHHmm(sr.close)
            val o = LocalTime.of(oH, oM)
            val c = LocalTime.of(cH, cM)

            if (o <= c) {
                !sLocal.isBefore(o) && !sLocal.isAfter(c)
            } else {
                // Nocturna que cruza: si s >= o, estás dentro del tramo que sigue tras medianoche
                !sLocal.isBefore(o)
            }
        }
    }

    private fun isEndCoveredOnEndDay(
        eLocal: LocalTime,
        schedules: List<ScheduleRange>
    ): Boolean {
        return schedules.any { sr ->
            val (oH, oM) = parseHHmm(sr.open)
            val (cH, cM) = parseHHmm(sr.close)
            val c = LocalTime.of(cH, cM)

            // En día siguiente (cruza medianoche), basta con que e <= hora de cierre
            !eLocal.isAfter(c)
        }
    }

    private fun epochMinToZdt(epochMin: Long, tz: ZoneId): ZonedDateTime =
        Instant.ofEpochSecond(epochMin * 60L).atZone(tz)

    private fun parseHHmm(s: String): Pair<Int, Int> {
        val parts = s.trim().split(":")
        val h = (parts.getOrNull(0)?.toIntOrNull() ?: 0).coerceIn(0, 23)
        val m = (parts.getOrNull(1)?.toIntOrNull() ?: 0).coerceIn(0, 59)
        return h to m
    }

    private fun dayEnabled(parking: ParkingLot, dow: DayOfWeek): Boolean {
        val asWeekDay = when (dow) {
            DayOfWeek.MONDAY -> WeekDay.MON
            DayOfWeek.TUESDAY -> WeekDay.TUE
            DayOfWeek.WEDNESDAY -> WeekDay.WED
            DayOfWeek.THURSDAY -> WeekDay.THU
            DayOfWeek.FRIDAY -> WeekDay.FRI
            DayOfWeek.SATURDAY -> WeekDay.SAT
            DayOfWeek.SUNDAY -> WeekDay.SUN
        }
        return parking.daysOfWeek.contains(asWeekDay)
    }
}