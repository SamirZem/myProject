package com.samirzem.screentimeanalyzer.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samirzem.screentimeanalyzer.ui.LambdaViewModelFactory
import com.samirzem.screentimeanalyzer.ui.components.AppUsageRow
import com.samirzem.screentimeanalyzer.ui.rememberApp
import com.samirzem.screentimeanalyzer.util.Formatters

@Composable
fun DashboardScreen(onAppClick: (String) -> Unit, onSeeAllClick: () -> Unit) {
    val app = rememberApp()
    val viewModel: DashboardViewModel = viewModel(
        factory = LambdaViewModelFactory { DashboardViewModel(app.repository, app.appInfoResolver) },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Column {
                Text("Aujourd'hui", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = Formatters.duration(state.todayTotalMs),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                )
                if (state.averagePreviousDaysMs > 0) {
                    val diff = state.todayTotalMs - state.averagePreviousDaysMs
                    val diffPercent = (diff.toDouble() / state.averagePreviousDaysMs) * 100
                    val trendText = if (diff >= 0) {
                        "+${Formatters.duration(diff)} vs moyenne des 7 derniers jours (%+.0f %%)".format(diffPercent)
                    } else {
                        "-${Formatters.duration(-diff)} vs moyenne des 7 derniers jours (%+.0f %%)".format(diffPercent)
                    }
                    Text(trendText, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StatCard(
                    title = "Déverrouillages",
                    value = state.unlockCountToday.toString(),
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    title = "Apps utilisées",
                    value = state.appCountToday.toString(),
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    title = "Dernière activité",
                    value = state.lastUsedAtMs?.let(Formatters::clockTime) ?: "—",
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Top applications", style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = onSeeAllClick) { Text("Tout voir") }
            }
        }

        if (state.topApps.isEmpty() && !state.isLoading) {
            item { Text("Pas encore de données pour aujourd'hui.") }
        }

        items(state.topApps) { row ->
            AppUsageRow(
                info = row.info,
                totalTimeMs = row.totalTimeMs,
                fraction = row.totalTimeMs.toFloat() / state.topAppsMaxMs,
                onClick = { onAppClick(row.info.packageName) },
            )
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.labelSmall)
        }
    }
}
