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
    val weekdayAverages: List<WeekdayAverageUi> = emptyList(),
    /** Single day whose hour-by-hour breakdown is shown - independent of [period]. */
    val hourlyDayEpochDay: Long = TimeUtils.todayEpochDay(),
    val hourlyDayIsToday: Boolean = true,
    val dayHourlyMs: LongArray = LongArray(24),
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
        loadHourlyDay(TimeUtils.todayEpochDay())
    }

    fun selectPeriod(period: AnalysisPeriod) = load(period)

    fun selectHourlyDay(epochDay: Long) = loadHourlyDay(epochDay.coerceAtMost(TimeUtils.todayEpochDay()))

    fun goToPreviousHourlyDay() = loadHourlyDay(_uiState.value.hourlyDayEpochDay - 1)

    fun goToNextHourlyDay() =
        loadHourlyDay((_uiState.value.hourlyDayEpochDay + 1).coerceAtMost(TimeUtils.todayEpochDay()))

    private fun loadHourlyDay(epochDay: Long) {
        viewModelScope.launch {
            val bucket = repository.getDayBucket(epochDay)
            val hourly = bucket.usageFor(packageName)?.hourlyMs ?: LongArray(24)
            _uiState.value = _uiState.value.copy(
                hourlyDayEpochDay = epochDay,
                hourlyDayIsToday = epochDay == TimeUtils.todayEpochDay(),
                dayHourlyMs = hourly,
            )
        }
    }

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

            // .copy() rather than a full replacement so the independently-loaded
            // hourlyDay* fields above survive a period change.
            _uiState.value = _uiState.value.copy(
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
