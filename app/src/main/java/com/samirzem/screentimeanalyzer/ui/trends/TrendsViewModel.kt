package com.samirzem.screentimeanalyzer.ui.trends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samirzem.screentimeanalyzer.data.ScreenTimeRepository
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

data class TrendsUiState(
    val isLoading: Boolean = true,
    val weeklySeries: List<Long> = emptyList(),
    val weeklyLabels: List<String> = emptyList(),
    val hourlyMs: LongArray = LongArray(24),
    val weekdayAverages: List<WeekdayAverageUi> = emptyList(),
    val totalUnlocksWeek: Int = 0,
    val averageUnlocksPerDay: Double = 0.0,
)

class TrendsViewModel(private val repository: ScreenTimeRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(TrendsUiState())
    val uiState: StateFlow<TrendsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val today = TimeUtils.todayEpochDay()
            val buckets = repository.getDayBuckets(today - (ANALYSIS_DAYS - 1), today)

            val hourly = LongArray(24)
            val byWeekday = mutableMapOf<DayOfWeek, MutableList<Long>>()
            for (bucket in buckets) {
                for (h in 0 until 24) hourly[h] += bucket.hourlyMs[h]
                val weekday = LocalDate.ofEpochDay(bucket.epochDay).dayOfWeek
                byWeekday.getOrPut(weekday) { mutableListOf() }.add(bucket.totalScreenTimeMs)
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

            _uiState.value = TrendsUiState(
                isLoading = false,
                weeklySeries = lastWeek.map { it.totalScreenTimeMs },
                weeklyLabels = lastWeek.map { dayLabel(it.epochDay) },
                hourlyMs = hourly,
                weekdayAverages = weekdayAverages,
                totalUnlocksWeek = lastWeek.sumOf { it.unlockCount },
                averageUnlocksPerDay = if (lastWeek.isNotEmpty()) {
                    lastWeek.sumOf { it.unlockCount }.toDouble() / lastWeek.size
                } else {
                    0.0
                },
            )
        }
    }

    private fun dayLabel(epochDay: Long): String {
        val date = LocalDate.ofEpochDay(epochDay)
        return date.format(DateTimeFormatter.ofPattern("EEE", Locale.FRENCH)).replaceFirstChar { it.uppercase() }
    }

    private companion object {
        const val ANALYSIS_DAYS = 28L
    }
}
