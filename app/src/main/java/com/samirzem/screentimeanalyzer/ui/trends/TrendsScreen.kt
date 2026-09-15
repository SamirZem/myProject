package com.samirzem.screentimeanalyzer.ui.trends

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samirzem.screentimeanalyzer.ui.LambdaViewModelFactory
import com.samirzem.screentimeanalyzer.ui.components.DailyBarChart
import com.samirzem.screentimeanalyzer.ui.components.HourHeatmap
import com.samirzem.screentimeanalyzer.ui.rememberApp

@Composable
fun TrendsScreen() {
    val app = rememberApp()
    val viewModel: TrendsViewModel = viewModel(
        factory = LambdaViewModelFactory { TrendsViewModel(app.repository) },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        item { Text("Tendances", style = MaterialTheme.typography.headlineMedium) }

        item {
            SectionCard(title = "7 derniers jours") {
                DailyBarChart(
                    values = state.weeklySeries,
                    labels = state.weeklyLabels,
                    highlightIndex = state.weeklySeries.lastIndex,
                )
            }
        }

        item {
            SectionCard(title = "Répartition par heure (28 derniers jours cumulés)") {
                HourHeatmap(hourlyMs = state.hourlyMs)
            }
        }

        item {
            SectionCard(title = "Moyenne par jour de la semaine") {
                DailyBarChart(
                    values = state.weekdayAverages.map { it.averageMs },
                    labels = state.weekdayAverages.map { it.label },
                )
            }
        }

        item {
            SectionCard(title = "Déverrouillages") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    Column {
                        Text(
                            state.totalUnlocksWeek.toString(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text("cette semaine", style = MaterialTheme.typography.bodyMedium)
                    }
                    Column {
                        Text(
                            "%.0f".format(state.averageUnlocksPerDay),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text("par jour en moyenne", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}
