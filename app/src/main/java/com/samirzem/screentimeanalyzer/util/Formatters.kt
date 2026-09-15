package com.samirzem.screentimeanalyzer.util

import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

object Formatters {
    private val hourMinuteFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.FRANCE)

    /** e.g. "3 h 42" or "48 min". */
    fun duration(ms: Long): String {
        val totalMinutes = (ms / 60_000L).toInt()
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours > 0 && minutes > 0 -> "${hours} h ${minutes}"
            hours > 0 -> "${hours} h"
            else -> "${minutes} min"
        }
    }

    /** Short variant for compact chart labels, e.g. "3h42". */
    fun durationShort(ms: Long): String {
        val totalMinutes = (ms / 60_000L).toInt()
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours > 0 -> "${hours}h${minutes.toString().padStart(2, '0')}"
            else -> "${minutes}min"
        }
    }

    fun clockTime(epochMs: Long): String =
        hourMinuteFormatter.format(Instant.ofEpochMilli(epochMs).atZone(TimeUtils.zoneId))

    fun percent(fraction: Double): String = "${(fraction * 100).roundToInt()} %"
}
