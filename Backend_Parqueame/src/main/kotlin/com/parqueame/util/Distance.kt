package com.parqueame.util

import kotlin.math.*
import java.time.*

/** Distancia Haversine en metros (lat/lon en grados). */
fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val R = 6371000.0 // radio de la Tierra en metros
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val φ1 = Math.toRadians(lat1)
    val φ2 = Math.toRadians(lat2)

    val a = sin(dLat / 2).pow(2.0) + cos(φ1) * cos(φ2) * sin(dLon / 2).pow(2.0)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return R * c
}

/** Texto amigable para distancias. Ej: 850 m, 1.2 km, 12 km */
fun distanceText(meters: Double?): String? {
    meters ?: return null
    val m = meters.coerceAtLeast(0.0)
    return when {
        m >= 10_000 -> "${(m / 1000.0).roundToInt()} km"        // >= 10 km → sin decimales
        m >= 1_000  -> String.format("%.1f km", m / 1000.0)     // 1.0–9.9 km → 1 decimal
        else        -> "${m.roundToInt()} m"                    // < 1 km → metros
    }
}

/* ============================================================
 *  Tiempo: helpers en 24h y compatibilidad con 12h
 * ============================================================ */

/** Convierte 24h local → epoch MINUTES (UTC) para una fecha y zona dadas. */
fun toEpochMinutes24(
    hour24: Int,
    minute: Int,
    localDate: LocalDate,
    tz: ZoneId
): Long {
    val h = hour24.coerceIn(0, 23)
    val m = minute.coerceIn(0, 59)
    val ldt = LocalDateTime.of(localDate, LocalTime.of(h, m))
    return ldt.atZone(tz).toInstant().epochSecond / 60
}

/** Convierte 24h local → epoch SECONDS (UTC). */
fun toEpochSeconds24(
    hour24: Int,
    minute: Int,
    localDate: LocalDate,
    tz: ZoneId
): Long {
    val h = hour24.coerceIn(0, 23)
    val m = minute.coerceIn(0, 59)
    val ldt = LocalDateTime.of(localDate, LocalTime.of(h, m))
    return ldt.atZone(tz).toInstant().epochSecond
}

/** Convierte 24h local → epoch MILLIS (UTC). */
fun toEpochMillis24(
    hour24: Int,
    minute: Int,
    localDate: LocalDate,
    tz: ZoneId
): Long {
    val h = hour24.coerceIn(0, 23)
    val m = minute.coerceIn(0, 59)
    val ldt = LocalDateTime.of(localDate, LocalTime.of(h, m))
    return ldt.atZone(tz).toInstant().toEpochMilli()
}

/** Convierte epoch MINUTES (UTC) → LocalDateTime en la zona dada. */
fun fromEpochMinutesToLocalDateTime(epochMinutesUtc: Long, tz: ZoneId): LocalDateTime {
    val instant = Instant.ofEpochSecond(epochMinutesUtc * 60)
    return LocalDateTime.ofInstant(instant, tz)
}

/** Diferencia en minutos entre dos epoch MINUTES (UTC), resultado ≥ 0. */
fun diffMinutes(startEpochMinUtc: Long, endEpochMinUtc: Long): Long =
    (endEpochMinUtc - startEpochMinUtc).coerceAtLeast(0)

/** Redondea minutos a horas facturables (ceil). */
fun ceilHours(minutes: Long): Int = ((minutes + 59) / 60).toInt()

/** Overload práctico: ceil de horas entre dos instantes (epoch MINUTES UTC). */
fun ceilHours(startEpochMinUtc: Long, endEpochMinUtc: Long): Int =
    ceilHours(diffMinutes(startEpochMinUtc, endEpochMinUtc))

/* ============================================================
 *  Compatibilidad 12h (obsoleto)
 * ============================================================ */

/** Convierte 12h local → epoch MINUTES (UTC). Obsoleto; usa toEpochMinutes24. */
@Deprecated(
    message = "Usa toEpochMinutes24(hour24, minute, localDate, tz) para evitar AM/PM.",
    replaceWith = ReplaceWith("toEpochMinutes24(hour12To24(hour12, period), minute, localDate, tz)")
)
fun toEpochMinutes(
    hour12: Int,
    minute: Int,
    period: String, // "AM"/"PM"
    localDate: LocalDate,
    tz: ZoneId
): Long {
    val h24 = hour12To24(hour12, period)
    return toEpochMinutes24(h24, minute, localDate, tz)
}

/** Helper interno: 12h + AM/PM → 24h. */
fun hour12To24(hour12: Int, period: String): Int {
    val base = (hour12 % 12).let { if (it < 0) 0 else it } // 0..11, 12→0
    val pm = period.equals("PM", ignoreCase = true)
    val h = if (pm) base + 12 else base
    return when {
        h < 0   -> 0
        h > 23  -> 23
        else    -> h
    }
}