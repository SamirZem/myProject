package com.samirzem.screentimeanalyzer.ui.applist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samirzem.screentimeanalyzer.ui.LambdaViewModelFactory
import com.samirzem.screentimeanalyzer.ui.components.AppUsageRow
import com.samirzem.screentimeanalyzer.ui.components.PeriodSelector
import com.samirzem.screentimeanalyzer.ui.rememberApp

@Composable
fun AppListScreen(onAppClick: (String) -> Unit) {
    val app = rememberApp()
    val viewModel: AppListViewModel = viewModel(
        factory = LambdaViewModelFactory { AppListViewModel(app.repository, app.appInfoResolver) },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val maxTotal = state.rows.maxOfOrNull { it.totalTimeMs } ?: 1L

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Applications", style = MaterialTheme.typography.headlineMedium)
                PeriodSelector(
                    options = Period.entries,
                    selected = state.period,
                    labelOf = { it.label },
                    onSelect = viewModel::selectPeriod,
                )
            }
        }

        if (state.rows.isEmpty() && !state.isLoading) {
            item { Text("Aucune donnée pour cette période.") }
        }

        items(state.rows, key = { it.info.packageName }) { row ->
            AppUsageRow(
                info = row.info,
                totalTimeMs = row.totalTimeMs,
                fraction = row.totalTimeMs.toFloat() / maxTotal,
                subtitle = "${row.sessionCount} session" + if (row.sessionCount > 1) "s" else "",
                onClick = { onAppClick(row.info.packageName) },
            )
        }
    }
}
