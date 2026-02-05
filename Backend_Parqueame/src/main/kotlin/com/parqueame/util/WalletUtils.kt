//package com.parqueame.util
//
//import java.time.*
//import java.time.format.DateTimeFormatter
//
//private val DO_TZ: ZoneId = ZoneId.of("America/Santo_Domingo")
//private val OUT_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yy")
//
//fun maskAccountNumber(raw: String): String {
//    if (raw.length <= 4) return raw
//    return "*".repeat(raw.length - 4) + raw.takeLast(4)
//}
//
//fun instantToDoShort(instant: Instant): String {
//    val zdt = instant.atZone(DO_TZ)
//    return zdt.format(OUT_FMT)
//}
//
//// Parse local ISO date boundaries to Instants in DO timezone
//fun parseDateRange(startDate: String?, endDate: String?): Pair<Instant?, Instant?> {
//    val start = startDate?.let {
//        LocalDate.parse(it).atStartOfDay(DO_TZ).toInstant()
//    }
//    val end = endDate?.let {
//        // fin del día inclusive
//        LocalDate.parse(it).plusDays(1).atStartOfDay(DO_TZ).minusNanos(1).toInstant()
//    }
//    return start to end
//}
//
//// Helpers para mes/año en DO
//fun monthYearToRange(month: Int, year: Int): Pair<Instant, Instant> {
//    val first = LocalDate.of(year, month, 1)
//        .atStartOfDay(DO_TZ).toInstant()
//    val last = first.atZone(DO_TZ).toLocalDate()
//        .plusMonths(1).atStartOfDay(DO_TZ).minusNanos(1).toInstant()
//    return first to last
//}