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
    val todayTotalMs: Long = 0,
    val averagePreviousDaysMs: Long = 0,
    val unlockCountToday: Int = 0,
    val firstUnlockAtMs: Long? = null,
    val lastUsedAtMs: Long? = null,
    val topApps: List<AppRowUi> = emptyList(),
    val topAppsMaxMs: Long = 1,
    val appCountToday: Int = 0,
)

class DashboardViewModel(
    private val repository: ScreenTimeRepository,
    private val appInfoResolver: AppInfoResolver,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val today = TimeUtils.todayEpochDay()
            val buckets = repository.getDayBuckets(today - LOOKBACK_DAYS, today)
            val todayBucket = buckets.last()
            val previousDays = buckets.dropLast(1)
            val averagePrevious = if (previousDays.isNotEmpty()) {
                previousDays.sumOf { it.totalScreenTimeMs } / previousDays.size
            } else {
                0L
            }

            val topApps = todayBucket.perApp.take(TOP_APPS_COUNT).map { stat ->
                AppRowUi(appInfoResolver.resolve(stat.packageName), stat.totalTimeMs)
            }

            _uiState.value = DashboardUiState(
                isLoading = false,
                todayTotalMs = todayBucket.totalScreenTimeMs,
                averagePreviousDaysMs = averagePrevious,
                unlockCountToday = todayBucket.unlockCount,
                firstUnlockAtMs = todayBucket.firstUnlockAtMs,
                lastUsedAtMs = todayBucket.perApp.maxOfOrNull { it.lastUsedAtMs },
                topApps = topApps,
                topAppsMaxMs = topApps.maxOfOrNull { it.totalTimeMs } ?: 1L,
                appCountToday = todayBucket.appCount,
            )
        }
    }

    private companion object {
        const val LOOKBACK_DAYS = 7L
        const val TOP_APPS_COUNT = 6
    }
}
