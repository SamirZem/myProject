package com.samirzem.screentimeanalyzer.ui.applist

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

enum class Period(val label: String, val days: Long) {
    TODAY("Aujourd'hui", 1),
    WEEK("7 jours", 7),
    MONTH("30 jours", 30),
}

data class AppListRowUi(
    val info: ResolvedAppInfo,
    val totalTimeMs: Long,
    val sessionCount: Int,
)

data class AppListUiState(
    val isLoading: Boolean = true,
    val period: Period = Period.TODAY,
    val rows: List<AppListRowUi> = emptyList(),
)

class AppListViewModel(
    private val repository: ScreenTimeRepository,
    private val appInfoResolver: AppInfoResolver,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppListUiState())
    val uiState: StateFlow<AppListUiState> = _uiState.asStateFlow()

    init {
        load(Period.TODAY)
    }

    fun selectPeriod(period: Period) = load(period)

    private fun load(period: Period) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, period = period)

            val today = TimeUtils.todayEpochDay()
            val buckets = repository.getDayBuckets(today - (period.days - 1), today)

            val totals = mutableMapOf<String, Long>()
            val sessions = mutableMapOf<String, Int>()
            for (bucket in buckets) {
                for (stat in bucket.perApp) {
                    totals[stat.packageName] = (totals[stat.packageName] ?: 0) + stat.totalTimeMs
                    sessions[stat.packageName] = (sessions[stat.packageName] ?: 0) + stat.sessionCount
                }
            }

            val rows = totals.entries
                .sortedByDescending { it.value }
                .map { (pkg, total) ->
                    AppListRowUi(
                        info = appInfoResolver.resolve(pkg),
                        totalTimeMs = total,
                        sessionCount = sessions[pkg] ?: 0,
                    )
                }

            _uiState.value = AppListUiState(isLoading = false, period = period, rows = rows)
        }
    }
}
