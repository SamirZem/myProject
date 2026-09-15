package com.samirzem.screentimeanalyzer.ui.appdetail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samirzem.screentimeanalyzer.ui.LambdaViewModelFactory
import com.samirzem.screentimeanalyzer.ui.common.AnalysisPeriod
import com.samirzem.screentimeanalyzer.ui.components.AppIcon
import com.samirzem.screentimeanalyzer.ui.components.DailyBarChart
import com.samirzem.screentimeanalyzer.ui.components.HourHeatmap
import com.samirzem.screentimeanalyzer.ui.components.PeriodSelector
import com.samirzem.screentimeanalyzer.ui.rememberApp
import com.samirzem.screentimeanalyzer.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailScreen(packageName: String, onBack: () -> Unit) {
    val app = rememberApp()
    val viewModel: AppDetailViewModel = viewModel(
        factory = LambdaViewModelFactory {
            AppDetailViewModel(packageName, app.repository, app.appInfoResolver)
        },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var selectedDay by remember { mutableStateOf<Int?>(null) }
    var selectedHour by remember { mutableStateOf<Int?>(null) }
    var selectedWeekday by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.info?.label ?: packageName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                state.info?.let { info ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppIcon(info, modifier = Modifier.size(56.dp))
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(info.label, style = MaterialTheme.typography.titleLarge)
                            Text(info.category, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            item {
                PeriodSelector(
                    options = AnalysisPeriod.entries,
                    selected = state.period,
                    labelOf = { it.label },
                    onSelect = viewModel::selectPeriod,
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StatTile("Total (${state.period.label})", Formatters.duration(state.totalTimeMs), Modifier.weight(1f))
                    StatTile("Moyenne / jour actif", Formatters.duration(state.averagePerDayMs), Modifier.weight(1f))
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StatTile("Sessions", state.totalSessions.toString(), Modifier.weight(1f))
                    StatTile("Session la plus longue", Formatters.duration(state.longestSessionMs), Modifier.weight(1f))
                    StatTile("Session moyenne", Formatters.duration(state.averageSessionMs), Modifier.weight(1f))
                }
            }

            item {
                SectionCard(title = "Par jour (${state.period.label})") {
                    DailyBarChart(
                        values = state.dailySeries,
                        labels = state.dailyLabels,
                        highlightIndex = state.dailySeries.lastIndex,
                        onBarClick = { index -> selectedDay = if (selectedDay == index) null else index },
                    )
                    DetailLine(
                        text = selectedDay?.let { i ->
                            "${state.dailyLabels.getOrNull(i).orEmpty()} : ${Formatters.duration(state.dailySeries.getOrElse(i) { 0 })}"
                        },
                    )
                }
            }

            item {
                SectionCard(title = "Répartition sur la journée (${state.period.label} cumulés)") {
                    Text(
                        "Touche une heure pour voir le temps passé sur cette app à ce moment.",
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Spacer(Modifier.height(8.dp))
                    HourHeatmap(
                        values = state.hourlyMs,
                        selectedHour = selectedHour,
                        onHourClick = { hour -> selectedHour = if (selectedHour == hour) null else hour },
                        valueFormatter = Formatters::duration,
                    )
                    DetailLine(
                        text = selectedHour?.let { hour ->
                            "${hour}h-${hour + 1}h : ${Formatters.duration(state.hourlyMs.getOrElse(hour) { 0 })}"
                        },
                    )
                }
            }

            item {
                SectionCard(title = "Moyenne par jour de la semaine") {
                    DailyBarChart(
                        values = state.weekdayAverages.map { it.averageMs },
                        labels = state.weekdayAverages.map { it.label },
                        onBarClick = { index -> selectedWeekday = if (selectedWeekday == index) null else index },
                    )
                    DetailLine(
                        text = selectedWeekday?.let { i ->
                            val day = state.weekdayAverages.getOrNull(i)
                            day?.let { "${it.label} : ${Formatters.duration(it.averageMs)} en moyenne" }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailLine(text: String?) {
    AnimatedVisibility(visible = text != null) {
        Column {
            Spacer(Modifier.height(8.dp))
            Text(
                text = text.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
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

@Composable
private fun StatTile(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.labelSmall)
        }
    }
}
