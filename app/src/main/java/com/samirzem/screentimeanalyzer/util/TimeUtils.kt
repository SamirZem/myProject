package com.samirzem.screentimeanalyzer.util

import java.time.LocalDate
import java.time.ZoneId

object TimeUtils {
    val zoneId: ZoneId = ZoneId.systemDefault()

    fun startOfDayMs(epochDay: Long): Long =
        LocalDate.ofEpochDay(epochDay).atStartOfDay(zoneId).toInstant().toEpochMilli()

    fun endOfDayMs(epochDay: Long): Long = startOfDayMs(epochDay + 1)

    fun todayEpochDay(): Long = LocalDate.now(zoneId).toEpochDay()

    fun nowMs(): Long = System.currentTimeMillis()
}
