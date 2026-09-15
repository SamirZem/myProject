package com.samirzem.screentimeanalyzer.ui.appdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samirzem.screentimeanalyzer.data.AppInfoResolver
import com.samirzem.screentimeanalyzer.data.ResolvedAppInfo
import com.samirzem.screentimeanalyzer.data.ScreenTimeRepository
import com.samirzem.screentimeanalyzer.util.TimeUtils
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AppDetailUiState(
    val isLoading: Boolean = true,
    val info: ResolvedAppInfo? = null,
    val totalTimeMs: Long = 0,
    val averagePerDayMs: Long = 0,
    val totalSessions: Int = 0,
    val longestSessionMs: Long = 0,
    val averageSessionMs: Long = 0,
    val dailySeries: List<Long> = emptyList(),
    val dailyLabels: List<String> = emptyList(),
)

class AppDetailViewModel(
    private val packageName: String,
    private val repository: ScreenTimeRepository,
    private val appInfoResolver: AppInfoResolver,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppDetailUiState())
    val uiState: StateFlow<AppDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val today = TimeUtils.todayEpochDay()
            val buckets = repository.getDayBuckets(today - (SERIES_DAYS - 1), today)

            val perDay = buckets.map { it.usageFor(packageName)?.totalTimeMs ?: 0L }
            val allStats = buckets.mapNotNull { it.usageFor(packageName) }

            val totalTime = perDay.sum()
            val totalSessions = allStats.sumOf { it.sessionCount }
            val longestSession = allStats.maxOfOrNull { it.longestSessionMs } ?: 0L
            val averageSession = if (totalSessions > 0) totalTime / totalSessions else 0L
            val activeDays = perDay.count { it > 0 }.coerceAtLeast(1)

            _uiState.value = AppDetailUiState(
                isLoading = false,
                info = appInfoResolver.resolve(packageName),
                totalTimeMs = totalTime,
                averagePerDayMs = totalTime / activeDays,
                totalSessions = totalSessions,
                longestSessionMs = longestSession,
                averageSessionMs = averageSession,
                dailySeries = perDay,
                dailyLabels = buckets.map { dayLabel(it.epochDay) },
            )
        }
    }

    private fun dayLabel(epochDay: Long): String {
        val date = LocalDate.ofEpochDay(epochDay)
        return date.format(DateTimeFormatter.ofPattern("EEE", Locale.FRENCH)).replaceFirstChar { it.uppercase() }
    }

    private companion object {
        const val SERIES_DAYS = 14L
    }
}
