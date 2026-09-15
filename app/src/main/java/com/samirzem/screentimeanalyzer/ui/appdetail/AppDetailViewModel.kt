package com.samirzem.screentimeanalyzer.ui.appdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samirzem.screentimeanalyzer.data.AppInfoResolver
import com.samirzem.screentimeanalyzer.data.ResolvedAppInfo
import com.samirzem.screentimeanalyzer.data.ScreenTimeRepository
import com.samirzem.screentimeanalyzer.ui.common.AnalysisPeriod
import com.samirzem.screentimeanalyzer.ui.trends.WeekdayAverageUi
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

data class AppDetailUiState(
    val isLoading: Boolean = true,
    val period: AnalysisPeriod = AnalysisPeriod.WEEK,
    val info: ResolvedAppInfo? = null,
    val totalTimeMs: Long = 0,
    val averagePerDayMs: Long = 0,
    val totalSessions: Int = 0,
    val longestSessionMs: Long = 0,
    val averageSessionMs: Long = 0,
    val dailySeries: List<Long> = emptyList(),
    val dailyLabels: List<String> = emptyList(),
    /** This app's foreground milliseconds per hour-of-day, summed over the selected period. */
    val hourlyMs: LongArray = LongArray(24),
    val weekdayAverages: List<WeekdayAverageUi> = emptyList(),
)

class AppDetailViewModel(
    private val packageName: String,
    private val repository: ScreenTimeRepository,
    private val appInfoResolver: AppInfoResolver,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppDetailUiState())
    val uiState: StateFlow<AppDetailUiState> = _uiState.asStateFlow()

    init {
        load(AnalysisPeriod.WEEK)
    }

    fun selectPeriod(period: AnalysisPeriod) = load(period)

    private fun load(period: AnalysisPeriod) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, period = period)

            val today = TimeUtils.todayEpochDay()
            val buckets = repository.getDayBuckets(today - (period.days - 1), today)

            val perDay = buckets.map { it.usageFor(packageName)?.totalTimeMs ?: 0L }
            val allStats = buckets.mapNotNull { it.usageFor(packageName) }

            val totalTime = perDay.sum()
            val totalSessions = allStats.sumOf { it.sessionCount }
            val longestSession = allStats.maxOfOrNull { it.longestSessionMs } ?: 0L
            val averageSession = if (totalSessions > 0) totalTime / totalSessions else 0L
            val activeDays = perDay.count { it > 0 }.coerceAtLeast(1)

            val hourly = LongArray(24)
            for (stat in allStats) {
                for (h in 0 until 24) hourly[h] += stat.hourlyMs[h]
            }

            val byWeekday = mutableMapOf<DayOfWeek, MutableList<Long>>()
            for (bucket in buckets) {
                val weekday = LocalDate.ofEpochDay(bucket.epochDay).dayOfWeek
                byWeekday.getOrPut(weekday) { mutableListOf() }.add(bucket.usageFor(packageName)?.totalTimeMs ?: 0L)
            }
            val weekdayAverages = DayOfWeek.values().map { day ->
                val values = byWeekday[day].orEmpty()
                val avg = if (values.isNotEmpty()) values.sum() / values.size else 0L
                WeekdayAverageUi(
                    label = day.getDisplayName(TextStyle.SHORT, Locale.FRENCH).replaceFirstChar { it.uppercase() },
                    averageMs = avg,
                )
            }

            _uiState.value = AppDetailUiState(
                isLoading = false,
                period = period,
                info = appInfoResolver.resolve(packageName),
                totalTimeMs = totalTime,
                averagePerDayMs = totalTime / activeDays,
                totalSessions = totalSessions,
                longestSessionMs = longestSession,
                averageSessionMs = averageSession,
                dailySeries = perDay,
                dailyLabels = buckets.map { dayLabel(it.epochDay) },
                hourlyMs = hourly,
                weekdayAverages = weekdayAverages,
            )
        }
    }

    private fun dayLabel(epochDay: Long): String {
        val date = LocalDate.ofEpochDay(epochDay)
        // Includes the day-of-month so taps stay unambiguous once the period spans
        // more than one week (otherwise every Monday would just read "Lun").
        return date.format(DateTimeFormatter.ofPattern("EEE d", Locale.FRENCH)).replaceFirstChar { it.uppercase() }
    }
}
