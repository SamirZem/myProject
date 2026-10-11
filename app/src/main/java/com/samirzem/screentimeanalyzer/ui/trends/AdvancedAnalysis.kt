package com.samirzem.screentimeanalyzer.ui.trends

import com.samirzem.screentimeanalyzer.data.AppInfoResolver
import com.samirzem.screentimeanalyzer.data.DayBucket
import com.samirzem.screentimeanalyzer.data.ResolvedAppInfo
import com.samirzem.screentimeanalyzer.util.Formatters
import com.samirzem.screentimeanalyzer.util.TimeUtils
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class CompulsiveAppUi(val info: ResolvedAppInfo, val opensPerDay: Double, val avgSessionMs: Long)

/** One night's sleep estimate: gap between the last unlock and the next morning's first, minus the fall-asleep buffer. */
data class SleepNightUi(val dateLabel: String, val durationMs: Long, val bedtimeLabel: String, val wakeLabel: String)

data class NightUsageUi(
    val nightsWithUsage: Int,
    val totalNights: Int,
    val averageNightMs: Long,
    val topApp: ResolvedAppInfo?,
    val averageSleepMs: Long?,
    val sleepNights: List<SleepNightUi>,
)

data class AnomalyDayUi(
    val dateLabel: String,
    val totalMs: Long,
    val deltaPercent: Int,
    val topContributor: ResolvedAppInfo?,
)

data class TrendMoverUi(val label: String, val changePercent: Int, val isRising: Boolean)

/**
 * Rule-based, entirely local "deduced" insights that go beyond raw totals: apps
 * checked compulsively rather than genuinely used, night-time usage as a rough
 * sleep-hygiene proxy, days that break from the usual pattern, and which
 * apps/categories are trending up or down over the period. All derived from data
 * the engine already collects - no new tracking beyond the app-switch heatmap.
 */
object AdvancedAnalysis {

    /** Hours treated as "night" for the sleep proxy: late evening through early morning. */
    private val NIGHT_HOURS = listOf(23, 0, 1, 2, 3, 4)

    private val dateFormatter = DateTimeFormatter.ofPattern("EEE d MMM", Locale.FRENCH)

    fun compulsiveApps(buckets: List<DayBucket>, appInfoResolver: AppInfoResolver): List<CompulsiveAppUi> {
        if (buckets.isEmpty()) return emptyList()

        val sessionTotals = mutableMapOf<String, Int>()
        val timeTotals = mutableMapOf<String, Long>()
        for (bucket in buckets) {
            for (stat in bucket.perApp) {
                sessionTotals[stat.packageName] = (sessionTotals[stat.packageName] ?: 0) + stat.sessionCount
                timeTotals[stat.packageName] = (timeTotals[stat.packageName] ?: 0L) + stat.totalTimeMs
            }
        }

        val days = buckets.size
        return sessionTotals.entries.mapNotNull { (pkg, sessions) ->
            if (sessions <= 0) return@mapNotNull null
            val opensPerDay = sessions.toDouble() / days
            val avgSessionMs = (timeTotals[pkg] ?: 0L) / sessions
            // Opened often, but each visit barely lasts - "checked by reflex" rather
            // than genuinely used, which the raw duration ranking hides completely.
            if (opensPerDay >= MIN_OPENS_PER_DAY && avgSessionMs in 1..MAX_AVG_SESSION_MS) {
                CompulsiveAppUi(appInfoResolver.resolve(pkg), opensPerDay, avgSessionMs)
            } else {
                null
            }
        }.sortedByDescending { it.opensPerDay }.take(MAX_RESULTS)
    }

    fun nightUsage(buckets: List<DayBucket>, appInfoResolver: AppInfoResolver): NightUsageUi? {
        if (buckets.isEmpty()) return null

        var nightsWithUsage = 0
        var totalNightMs = 0L
        val appTotals = mutableMapOf<String, Long>()
        for (bucket in buckets) {
            val nightMs = NIGHT_HOURS.sumOf { bucket.hourlyMs[it] }
            if (nightMs >= NIGHT_USAGE_THRESHOLD_MS) nightsWithUsage++
            totalNightMs += nightMs
            for (stat in bucket.perApp) {
                val appNightMs = NIGHT_HOURS.sumOf { stat.hourlyMs[it] }
                if (appNightMs > 0) appTotals[stat.packageName] = (appTotals[stat.packageName] ?: 0L) + appNightMs
            }
        }

        val topPackage = appTotals.entries.maxByOrNull { it.value }?.key

        val sleepNights = estimateSleepNights(buckets)
        val averageSleepMs = if (sleepNights.isNotEmpty()) sleepNights.sumOf { it.durationMs } / sleepNights.size else null

        return NightUsageUi(
            nightsWithUsage = nightsWithUsage,
            totalNights = buckets.size,
            averageNightMs = totalNightMs / buckets.size,
            topApp = topPackage?.let { appInfoResolver.resolve(it) },
            averageSleepMs = averageSleepMs,
            sleepNights = sleepNights,
        )
    }

    /**
     * Estimates sleep duration per night from unlock timestamps alone: the gap between
     * the last unlock of one day and the first unlock of the next, minus a fixed buffer
     * for the time between putting the phone down and actually falling asleep. Only
     * counts nights where that last unlock falls in the evening and the next first
     * unlock falls in the morning - otherwise the gap isn't a reliable bedtime/wake pair
     * (e.g. the phone simply wasn't touched for a stretch during the day).
     */
    private fun estimateSleepNights(buckets: List<DayBucket>): List<SleepNightUi> {
        val sorted = buckets.sortedBy { it.epochDay }
        val nights = mutableListOf<SleepNightUi>()
        for (i in 0 until sorted.size - 1) {
            val day = sorted[i]
            val nextDay = sorted[i + 1]
            if (nextDay.epochDay != day.epochDay + 1) continue

            val bedtime = day.lastUnlockAtMs ?: continue
            val wake = nextDay.firstUnlockAtMs ?: continue
            if (hourOf(bedtime) < BEDTIME_MIN_HOUR) continue
            if (hourOf(wake) >= WAKE_MAX_HOUR) continue

            val durationMs = (wake - bedtime - SLEEP_BUFFER_MS).coerceAtLeast(0)
            if (durationMs <= 0) continue

            nights += SleepNightUi(
                dateLabel = LocalDate.ofEpochDay(nextDay.epochDay).format(dateFormatter)
                    .replaceFirstChar { it.uppercase() },
                durationMs = durationMs,
                bedtimeLabel = Formatters.clockTime(bedtime),
                wakeLabel = Formatters.clockTime(wake),
            )
        }
        return nights
    }

    private fun hourOf(epochMs: Long): Int = Instant.ofEpochMilli(epochMs).atZone(TimeUtils.zoneId).hour

    fun anomalies(buckets: List<DayBucket>, appInfoResolver: AppInfoResolver): List<AnomalyDayUi> {
        val withData = buckets.filter { it.totalScreenTimeMs > 0 }
        if (withData.size < MIN_DAYS_FOR_ANOMALY) return emptyList()

        val values = withData.map { it.totalScreenTimeMs.toDouble() }
        val mean = values.average()
        val variance = values.sumOf { (it - mean) * (it - mean) } / values.size
        val stdDev = sqrt(variance)
        if (stdDev <= 0) return emptyList()

        return withData.mapNotNull { bucket ->
            val z = (bucket.totalScreenTimeMs - mean) / stdDev
            if (z < ANOMALY_Z_THRESHOLD) return@mapNotNull null

            // Which app explains the spike: the one furthest above its own average
            // on every OTHER day, not just the biggest app that day.
            val otherDays = withData.filter { it.epochDay != bucket.epochDay }
            val otherAppTotals = mutableMapOf<String, Long>()
            for (other in otherDays) {
                for (stat in other.perApp) {
                    otherAppTotals[stat.packageName] = (otherAppTotals[stat.packageName] ?: 0L) + stat.totalTimeMs
                }
            }
            val otherDaysCount = otherDays.size.coerceAtLeast(1)
            val topContributor = bucket.perApp.maxByOrNull { stat ->
                stat.totalTimeMs - (otherAppTotals[stat.packageName] ?: 0L) / otherDaysCount
            }

            AnomalyDayUi(
                dateLabel = LocalDate.ofEpochDay(bucket.epochDay).format(dateFormatter)
                    .replaceFirstChar { it.uppercase() },
                totalMs = bucket.totalScreenTimeMs,
                deltaPercent = (((bucket.totalScreenTimeMs - mean) / mean) * 100).roundToInt(),
                topContributor = topContributor?.let { appInfoResolver.resolve(it.packageName) },
            )
        }.sortedByDescending { it.totalMs }.take(MAX_RESULTS)
    }

    /** Apps/categories whose usage clearly rose or fell between the first and second half of [buckets]. */
    fun trendMovers(buckets: List<DayBucket>, appInfoResolver: AppInfoResolver): List<TrendMoverUi> {
        if (buckets.size < MIN_DAYS_FOR_TREND) return emptyList()

        val midpoint = buckets.size / 2
        val earlier = buckets.subList(0, midpoint)
        val later = buckets.subList(midpoint, buckets.size)

        fun totalsByApp(days: List<DayBucket>): Map<String, Long> {
            val totals = mutableMapOf<String, Long>()
            for (day in days) {
                for (stat in day.perApp) {
                    totals[stat.packageName] = (totals[stat.packageName] ?: 0L) + stat.totalTimeMs
                }
            }
            return totals
        }

        fun totalsByCategory(days: List<DayBucket>): Map<String, Long> {
            val totals = mutableMapOf<String, Long>()
            for (day in days) {
                for (stat in day.perApp) {
                    val category = appInfoResolver.resolve(stat.packageName).category
                    totals[category] = (totals[category] ?: 0L) + stat.totalTimeMs
                }
            }
            return totals
        }

        fun movers(
            earlierTotals: Map<String, Long>,
            laterTotals: Map<String, Long>,
            labelOf: (String) -> String,
        ): List<TrendMoverUi> {
            val keys = earlierTotals.keys intersect laterTotals.keys
            return keys.mapNotNull { key ->
                val earlierAvg = (earlierTotals[key] ?: 0L).toDouble() / earlier.size
                val laterAvg = (laterTotals[key] ?: 0L).toDouble() / later.size
                // Needs a real baseline, or a barely-used app swinging from 10s to 30s
                // would register as a meaningless "+200%".
                if (earlierAvg < MIN_EARLIER_AVG_MS_FOR_TREND) return@mapNotNull null
                val changePercent = (((laterAvg - earlierAvg) / earlierAvg) * 100).roundToInt()
                if (abs(changePercent) < MIN_TREND_CHANGE_PERCENT) return@mapNotNull null
                TrendMoverUi(labelOf(key), changePercent, changePercent > 0)
            }
        }

        val appMovers = movers(totalsByApp(earlier), totalsByApp(later)) { pkg -> appInfoResolver.resolve(pkg).label }
        val categoryMovers = movers(totalsByCategory(earlier), totalsByCategory(later)) { it }

        return (appMovers + categoryMovers)
            .sortedByDescending { abs(it.changePercent) }
            .take(MAX_RESULTS + 1)
    }

    private const val MAX_RESULTS = 5
    private const val MIN_OPENS_PER_DAY = 6.0
    private const val MAX_AVG_SESSION_MS = 90_000L
    private const val NIGHT_USAGE_THRESHOLD_MS = 5 * 60_000L
    private const val SLEEP_BUFFER_MS = 20 * 60_000L
    private const val BEDTIME_MIN_HOUR = 18
    private const val WAKE_MAX_HOUR = 12
    private const val MIN_DAYS_FOR_ANOMALY = 5
    private const val ANOMALY_Z_THRESHOLD = 1.3
    private const val MIN_DAYS_FOR_TREND = 14
    private const val MIN_EARLIER_AVG_MS_FOR_TREND = 60_000L
    private const val MIN_TREND_CHANGE_PERCENT = 15
}
