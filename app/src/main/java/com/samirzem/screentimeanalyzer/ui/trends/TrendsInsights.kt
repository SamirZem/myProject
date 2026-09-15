package com.samirzem.screentimeanalyzer.ui.trends

import com.samirzem.screentimeanalyzer.data.AppInfoResolver
import com.samirzem.screentimeanalyzer.data.DayBucket
import com.samirzem.screentimeanalyzer.ui.common.AnalysisPeriod
import com.samirzem.screentimeanalyzer.util.Formatters
import kotlin.math.roundToInt

/**
 * Turns the numbers already computed for the Trends screen into a short list of
 * plain-French sentences - entirely rule-based from local data, no network call.
 */
object TrendsInsights {

    fun build(
        period: AnalysisPeriod,
        buckets: List<DayBucket>,
        previousBuckets: List<DayBucket>,
        unlockHourly: LongArray,
        timeOfDaySegments: List<TimeOfDaySegmentUi>,
        weekdayAverages: List<WeekdayAverageUi>,
        appInfoResolver: AppInfoResolver,
    ): List<String> {
        if (buckets.isEmpty()) return emptyList()

        val insights = mutableListOf<String>()

        val totalMs = buckets.sumOf { it.totalScreenTimeMs }
        val avgPerDayMs = totalMs / buckets.size
        val previousAvgPerDayMs = if (previousBuckets.isNotEmpty()) {
            previousBuckets.sumOf { it.totalScreenTimeMs } / previousBuckets.size
        } else {
            0L
        }

        insights += trendSentence(period, avgPerDayMs, previousAvgPerDayMs)

        topAppSentence(buckets, totalMs, appInfoResolver)?.let { insights += it }
        busiestMomentSentence(timeOfDaySegments)?.let { insights += it }
        unlockSentence(buckets, unlockHourly)?.let { insights += it }
        weekdayContrastSentence(weekdayAverages)?.let { insights += it }

        return insights
    }

    private fun trendSentence(period: AnalysisPeriod, avgPerDayMs: Long, previousAvgPerDayMs: Long): String {
        if (avgPerDayMs <= 0) return "Aucune activité enregistrée sur ${period.label.lowercase()}."
        if (previousAvgPerDayMs <= 0) {
            return "En moyenne ${Formatters.duration(avgPerDayMs)} par jour sur ${period.label.lowercase()}."
        }
        val diffPercent = ((avgPerDayMs - previousAvgPerDayMs).toDouble() / previousAvgPerDayMs * 100).roundToInt()
        val trend = when {
            diffPercent > 5 -> "en hausse de $diffPercent %"
            diffPercent < -5 -> "en baisse de ${-diffPercent} %"
            else -> "stable"
        }
        return "En moyenne ${Formatters.duration(avgPerDayMs)} par jour sur ${period.label.lowercase()}, " +
            "$trend par rapport à la période précédente (${Formatters.duration(previousAvgPerDayMs)}/jour)."
    }

    private fun topAppSentence(buckets: List<DayBucket>, totalMs: Long, appInfoResolver: AppInfoResolver): String? {
        if (totalMs <= 0) return null
        val appTotals = mutableMapOf<String, Long>()
        for (bucket in buckets) {
            for (stat in bucket.perApp) {
                appTotals[stat.packageName] = (appTotals[stat.packageName] ?: 0L) + stat.totalTimeMs
            }
        }
        val top = appTotals.entries.maxByOrNull { it.value } ?: return null
        val info = appInfoResolver.resolve(top.key)
        val percent = (top.value.toDouble() / totalMs * 100).roundToInt()
        return "${info.label} est ton app la plus utilisée : ${Formatters.duration(top.value)} au total, soit $percent % de ton temps d'écran."
    }

    private fun busiestMomentSentence(segments: List<TimeOfDaySegmentUi>): String? {
        val busiest = segments.maxByOrNull { it.totalMs } ?: return null
        if (busiest.totalMs <= 0) return null
        val moment = when (busiest.label) {
            "Nuit" -> "la nuit"
            "Matin" -> "le matin"
            "Après-midi" -> "l'après-midi"
            "Soir" -> "le soir"
            else -> busiest.label.lowercase()
        }
        return "Tu es le plus actif $moment (${busiest.hourRangeLabel}), avec ${Formatters.duration(busiest.totalMs)} cumulées sur la période."
    }

    private fun unlockSentence(buckets: List<DayBucket>, unlockHourly: LongArray): String? {
        val totalUnlocks = buckets.sumOf { it.unlockCount }
        if (totalUnlocks <= 0) return null
        val avgUnlocks = totalUnlocks.toDouble() / buckets.size
        val peakHour = (0 until 24).maxByOrNull { unlockHourly[it] }
        val peakText = if (peakHour != null && unlockHourly[peakHour] > 0) ", surtout autour de ${peakHour}h" else ""
        return "Tu déverrouilles ton téléphone ${"%.0f".format(avgUnlocks)} fois par jour en moyenne$peakText."
    }

    private fun weekdayContrastSentence(weekdayAverages: List<WeekdayAverageUi>): String? {
        val withData = weekdayAverages.filter { it.averageMs > 0 }
        if (withData.size < 2) return null
        val busiest = withData.maxByOrNull { it.averageMs }!!
        val quietest = withData.minByOrNull { it.averageMs }!!
        if (busiest.label == quietest.label) return null
        return "${busiest.label} est ton jour le plus chargé (${Formatters.duration(busiest.averageMs)} en moyenne), " +
            "${quietest.label} le plus calme (${Formatters.duration(quietest.averageMs)})."
    }
}
