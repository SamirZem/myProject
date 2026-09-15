package com.samirzem.screentimeanalyzer.ui.trends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samirzem.screentimeanalyzer.data.AppInfoResolver
import com.samirzem.screentimeanalyzer.data.ResolvedAppInfo
import com.samirzem.screentimeanalyzer.data.ScreenTimeRepository
import com.samirzem.screentimeanalyzer.ui.common.AnalysisPeriod
import com.samirzem.screentimeanalyzer.util.TimeUtils
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class WeekdayAverageUi(val label: String, val averageMs: Long)

data class TopAppUsage(val info: ResolvedAppInfo, val totalTimeMs: Long)

data class TimeOfDaySegmentUi(
    val label: String,
    val hourRangeLabel: String,
    val totalMs: Long,
    val topApps: List<TopAppUsage>,
)

data class TrendsUiState(
    val isLoading: Boolean = true,
    val period: AnalysisPeriod = AnalysisPeriod.WEEK,
    val weeklySeries: List<Long> = emptyList(),
    val weeklyLabels: List<String> = emptyList(),
    val hourlyMs: LongArray = LongArray(24),
    val hourlyTopApps: Map<Int, List<TopAppUsage>> = emptyMap(),
    val weekdayAverages: List<WeekdayAverageUi> = emptyList(),
    val totalUnlocks: Int = 0,
    val averageUnlocksPerDay: Double = 0.0,
    val unlockHourly: LongArray = LongArray(24),
    val timeOfDaySegments: List<TimeOfDaySegmentUi> = emptyList(),
)

class TrendsViewModel(
    private val repository: ScreenTimeRepository,
    private val appInfoResolver: AppInfoResolver,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrendsUiState())
    val uiState: StateFlow<TrendsUiState> = _uiState.asStateFlow()

    init {
        load(AnalysisPeriod.WEEK)
    }

    fun selectPeriod(period: AnalysisPeriod) = load(period)

    private fun load(period: AnalysisPeriod) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, period = period)

            val today = TimeUtils.todayEpochDay()
            val buckets = repository.getDayBuckets(today - (period.days - 1), today)

            val hourly = LongArray(24)
            val unlockHourly = LongArray(24)
            val hourlyAppTotals = Array(24) { mutableMapOf<String, Long>() }
            val byWeekday = mutableMapOf<DayOfWeek, MutableList<Long>>()

            for (bucket in buckets) {
                for (h in 0 until 24) {
                    hourly[h] += bucket.hourlyMs[h]
                    unlockHourly[h] += bucket.unlockHourly[h]
                }
                for (stat in bucket.perApp) {
                    for (h in 0 until 24) {
                        val ms = stat.hourlyMs[h]
                        if (ms > 0) {
                            hourlyAppTotals[h][stat.packageName] = (hourlyAppTotals[h][stat.packageName] ?: 0L) + ms
                        }
                    }
                }
                val weekday = LocalDate.ofEpochDay(bucket.epochDay).dayOfWeek
                byWeekday.getOrPut(weekday) { mutableListOf() }.add(bucket.totalScreenTimeMs)
            }

            val hourlyTopApps = (0 until 24).associateWith { hour ->
                hourlyAppTotals[hour].entries
                    .sortedByDescending { it.value }
                    .take(TOP_APPS_PER_BREAKDOWN)
                    .map { (pkg, ms) -> TopAppUsage(appInfoResolver.resolve(pkg), ms) }
            }

            val weekdayAverages = DayOfWeek.values().map { day ->
                val values = byWeekday[day].orEmpty()
                val avg = if (values.isNotEmpty()) values.sum() / values.size else 0L
                WeekdayAverageUi(
                    label = day.getDisplayName(TextStyle.SHORT, Locale.FRENCH).replaceFirstChar { it.uppercase() },
                    averageMs = avg,
                )
            }

            val lastWeek = buckets.takeLast(7)

            val timeOfDaySegments = SEGMENTS.map { segment ->
                val segmentTotals = mutableMapOf<String, Long>()
                for (h in segment.hours) {
                    hourlyAppTotals[h].forEach { (pkg, ms) ->
                        segmentTotals[pkg] = (segmentTotals[pkg] ?: 0L) + ms
                    }
                }
                TimeOfDaySegmentUi(
                    label = segment.label,
                    hourRangeLabel = segment.rangeLabel,
                    totalMs = segment.hours.sumOf { hourly[it] },
                    topApps = segmentTotals.entries
                        .sortedByDescending { it.value }
                        .take(TOP_APPS_PER_BREAKDOWN)
                        .map { (pkg, ms) -> TopAppUsage(appInfoResolver.resolve(pkg), ms) },
                )
            }

            _uiState.value = TrendsUiState(
                isLoading = false,
                period = period,
                weeklySeries = lastWeek.map { it.totalScreenTimeMs },
                weeklyLabels = lastWeek.map { dayLabel(it.epochDay) },
                hourlyMs = hourly,
                hourlyTopApps = hourlyTopApps,
                weekdayAverages = weekdayAverages,
                totalUnlocks = buckets.sumOf { it.unlockCount },
                averageUnlocksPerDay = if (buckets.isNotEmpty()) {
                    buckets.sumOf { it.unlockCount }.toDouble() / buckets.size
                } else {
                    0.0
                },
                unlockHourly = unlockHourly,
                timeOfDaySegments = timeOfDaySegments,
            )
        }
    }

    private fun dayLabel(epochDay: Long): String {
        val date = LocalDate.ofEpochDay(epochDay)
        return date.format(DateTimeFormatter.ofPattern("EEE", Locale.FRENCH)).replaceFirstChar { it.uppercase() }
    }

    private class Segment(val label: String, val rangeLabel: String, val hours: IntRange)

    private companion object {
        const val TOP_APPS_PER_BREAKDOWN = 5

        val SEGMENTS = listOf(
            Segment("Nuit", "0h-6h", 0..5),
            Segment("Matin", "6h-12h", 6..11),
            Segment("Après-midi", "12h-18h", 12..17),
            Segment("Soir", "18h-24h", 18..23),
        )
    }
}
