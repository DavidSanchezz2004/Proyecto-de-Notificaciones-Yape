package com.yape.yapenotificaciones.util

import java.time.*
import java.time.format.DateTimeFormatter
import java.util.TimeZone

fun localZone(): ZoneId = ZoneId.of("America/Lima")

fun startEndOfDay(date: LocalDate, zone: ZoneId = localZone()): Pair<Long, Long> {
    val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
    val end = date.plusDays(1).atStartOfDay(zone).minusNanos(1).toInstant().toEpochMilli()
    return start to end
}

fun startEndOfMonth(ym: YearMonth, zone: ZoneId = localZone()): Pair<Long, Long> {
    val start = ym.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
    val end = ym.plusMonths(1).atDay(1).atStartOfDay(zone).minusNanos(1).toInstant().toEpochMilli()
    return start to end
}

fun formatLocalDateTime(epochMillis: Long, zone: ZoneId = localZone()): String {
    return Instant.ofEpochMilli(epochMillis).atZone(zone)
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
}
