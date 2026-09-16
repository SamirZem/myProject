package com.samirzem.screentimeanalyzer.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samirzem.screentimeanalyzer.data.AppInfoResolver
import com.samirzem.screentimeanalyzer.data.ResolvedAppInfo
import com.samirzem.screentimeanalyzer.data.ScreenTimeRepository
import com.samirzem.screentimeanalyzer.util.TimeUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AppRowUi(val info: ResolvedAppInfo, val totalTimeMs: Long)

data class DashboardUiState(
    val isLoading: Boolean = true,
    val selectedEpochDay: Long = TimeUtils.todayEpochDay(),
    val isToday: Boolean = true,
    val dayTotalMs: Long = 0,
    val averagePreviousDaysMs: Long = 0,
    val unlockCountDay: Int = 0,
    val firstUnlockAtMs: Long? = null,
    val lastUsedAtMs: Long? = null,
    val topApps: List<AppRowUi> = emptyList(),
    val topAppsMaxMs: Long = 1,
    val appCountDay: Int = 0,
    /** This day's foreground milliseconds per hour-of-day, all apps combined. */
    val dayHourlyMs: LongArray = LongArray(24),
    val hourlyTopApps: Map<Int, List<AppRowUi>> = emptyMap(),
)

class DashboardViewModel(
    private val repository: ScreenTimeRepository,
    private val appInfoResolver: AppInfoResolver,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        load(TimeUtils.todayEpochDay())
    }

    fun refresh() = load(_uiState.value.selectedEpochDay)

    fun selectDay(epochDay: Long) = load(epochDay.coerceAtMost(TimeUtils.todayEpochDay()))

    fun goToPreviousDay() = load(_uiState.value.selectedEpochDay - 1)

    fun goToNextDay() = load((_uiState.value.selectedEpochDay + 1).coerceAtMost(TimeUtils.todayEpochDay()))

    fun goToToday() = load(TimeUtils.todayEpochDay())

    private fun load(epochDay: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, selectedEpochDay = epochDay)

            val today = TimeUtils.todayEpochDay()
            val buckets = repository.getDayBuckets(epochDay - LOOKBACK_DAYS, epochDay)
            val dayBucket = buckets.last()
            val previousDays = buckets.dropLast(1)
            val averagePrevious = if (previousDays.isNotEmpty()) {
                previousDays.sumOf { it.totalScreenTimeMs } / previousDays.size
            } else {
                0L
            }

            val topApps = dayBucket.perApp.take(TOP_APPS_COUNT).map { stat ->
                AppRowUi(appInfoResolver.resolve(stat.packageName), stat.totalTimeMs)
            }

            val hourlyTopApps = (0 until 24).associateWith { hour ->
                dayBucket.perApp
                    .filter { it.hourlyMs[hour] > 0 }
                    .sortedByDescending { it.hourlyMs[hour] }
                    .take(TOP_APPS_COUNT)
                    .map { AppRowUi(appInfoResolver.resolve(it.packageName), it.hourlyMs[hour]) }
            }

            _uiState.value = DashboardUiState(
                isLoading = false,
                selectedEpochDay = epochDay,
                isToday = epochDay == today,
                dayTotalMs = dayBucket.totalScreenTimeMs,
                averagePreviousDaysMs = averagePrevious,
                unlockCountDay = dayBucket.unlockCount,
                firstUnlockAtMs = dayBucket.firstUnlockAtMs,
                lastUsedAtMs = dayBucket.perApp.maxOfOrNull { it.lastUsedAtMs },
                topApps = topApps,
                topAppsMaxMs = topApps.maxOfOrNull { it.totalTimeMs } ?: 1L,
                appCountDay = dayBucket.appCount,
                dayHourlyMs = dayBucket.hourlyMs,
                hourlyTopApps = hourlyTopApps,
            )
        }
    }

    private companion object {
        const val LOOKBACK_DAYS = 7L
        const val TOP_APPS_COUNT = 6
    }
}
