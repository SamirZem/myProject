package com.samirzem.screentimeanalyzer.ui.trends

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samirzem.screentimeanalyzer.ui.LambdaViewModelFactory
import com.samirzem.screentimeanalyzer.ui.common.AnalysisPeriod
import com.samirzem.screentimeanalyzer.ui.components.AppUsageRow
import com.samirzem.screentimeanalyzer.ui.components.DailyBarChart
import com.samirzem.screentimeanalyzer.ui.components.HourHeatmap
import com.samirzem.screentimeanalyzer.ui.components.PeriodSelector
import com.samirzem.screentimeanalyzer.ui.rememberApp
import com.samirzem.screentimeanalyzer.ui.theme.SeriesColors
import com.samirzem.screentimeanalyzer.util.Formatters

@Composable
fun TrendsScreen() {
    val app = rememberApp()
    val viewModel: TrendsViewModel = viewModel(
        factory = LambdaViewModelFactory { TrendsViewModel(app.repository, app.appInfoResolver) },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var selectedWeekDay by remember { mutableStateOf<Int?>(null) }
    var selectedHour by remember { mutableStateOf<Int?>(null) }
    var selectedUnlockHour by remember { mutableStateOf<Int?>(null) }
    var selectedWeekday by remember { mutableStateOf<Int?>(null) }
    var expandedSegment by remember { mutableStateOf<Int?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Tendances", style = MaterialTheme.typography.headlineMedium)
                PeriodSelector(
                    options = AnalysisPeriod.entries,
                    selected = state.period,
                    labelOf = { it.label },
                    onSelect = viewModel::selectPeriod,
                )
            }
        }

        if (state.insights.isNotEmpty()) {
            item {
                SectionCard(title = "Résumé") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        state.insights.forEachIndexed { index, sentence ->
                            Row {
                                Text(
                                    "●  ",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SeriesColors[index % SeriesColors.size],
                                )
                                Text(sentence, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }

        item {
            SectionCard(title = "7 derniers jours") {
                DailyBarChart(
                    values = state.weeklySeries,
                    labels = state.weeklyLabels,
                    highlightIndex = state.weeklySeries.lastIndex,
                    onBarClick = { index -> selectedWeekDay = if (selectedWeekDay == index) null else index },
                )
                SelectionDetail(
                    text = selectedWeekDay?.let { i ->
                        "${state.weeklyLabels.getOrNull(i).orEmpty()} : ${Formatters.duration(state.weeklySeries.getOrElse(i) { 0 })}"
                    },
                )
            }
        }

        item {
            SectionCard(title = "Répartition par heure (${state.period.label} cumulés)") {
                Text(
                    "Touche une heure pour voir ce que tu utilisais à ce moment.",
                    style = MaterialTheme.typography.labelSmall,
                )
                Spacer(Modifier.height(8.dp))
                HourHeatmap(
                    values = state.hourlyMs,
                    selectedHour = selectedHour,
                    onHourClick = { hour -> selectedHour = if (selectedHour == hour) null else hour },
                    valueFormatter = Formatters::duration,
                )
                HourDetail(
                    hour = selectedHour,
                    totalMs = selectedHour?.let { state.hourlyMs.getOrElse(it) { 0 } },
                    topApps = selectedHour?.let { state.hourlyTopApps[it] }.orEmpty(),
                )
            }
        }

        item {
            SectionCard(title = "Par moment de la journée") {
                state.timeOfDaySegments.forEachIndexed { index, segment ->
                    TimeOfDaySegmentRow(
                        segment = segment,
                        expanded = expandedSegment == index,
                        onClick = { expandedSegment = if (expandedSegment == index) null else index },
                    )
                    if (index != state.timeOfDaySegments.lastIndex) Spacer(Modifier.height(4.dp))
                }
            }
        }

        item {
            SectionCard(title = "Moyenne par jour de la semaine") {
                DailyBarChart(
                    values = state.weekdayAverages.map { it.averageMs },
                    labels = state.weekdayAverages.map { it.label },
                    onBarClick = { index -> selectedWeekday = if (selectedWeekday == index) null else index },
                )
                SelectionDetail(
                    text = selectedWeekday?.let { i ->
                        val day = state.weekdayAverages.getOrNull(i)
                        day?.let { "${it.label} : ${Formatters.duration(it.averageMs)} en moyenne" }
                    },
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
                            state.totalUnlocks.toString(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text("sur ${state.period.label.lowercase()}", style = MaterialTheme.typography.bodyMedium)
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
                Spacer(Modifier.height(16.dp))
                Text(
                    "Répartition par heure (${state.period.label} cumulés)",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(8.dp))
                HourHeatmap(
                    values = state.unlockHourly,
                    selectedHour = selectedUnlockHour,
                    onHourClick = { hour -> selectedUnlockHour = if (selectedUnlockHour == hour) null else hour },
                    valueFormatter = { "$it déverrouillages" },
                )
                SelectionDetail(
                    text = selectedUnlockHour?.let { hour ->
                        val count = state.unlockHourly.getOrElse(hour) { 0 }
                        "${hour}h-${hour + 1}h : $count déverrouillage${if (count > 1) "s" else ""}"
                    },
                )
            }
        }
    }
}

@Composable
private fun SelectionDetail(text: String?) {
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
private fun HourDetail(hour: Int?, totalMs: Long?, topApps: List<TopAppUsage>) {
    AnimatedVisibility(visible = hour != null) {
        Column {
            Spacer(Modifier.height(12.dp))
            Text(
                text = "${hour}h-${(hour ?: 0) + 1}h : ${Formatters.duration(totalMs ?: 0)} au total",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            if (topApps.isEmpty()) {
                Text("Aucun usage sur cette heure.", style = MaterialTheme.typography.labelSmall)
            } else {
                val maxMs = topApps.maxOf { it.totalTimeMs }
                Column {
                    topApps.forEachIndexed { index, app ->
                        AppUsageRow(
                            info = app.info,
                            totalTimeMs = app.totalTimeMs,
                            fraction = app.totalTimeMs.toFloat() / maxMs,
                            accentColor = SeriesColors[index % SeriesColors.size],
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeOfDaySegmentRow(
    segment: TimeOfDaySegmentUi,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(segment.label, style = MaterialTheme.typography.titleMedium)
                Text(segment.hourRangeLabel, style = MaterialTheme.typography.labelSmall)
            }
            Text(
                Formatters.duration(segment.totalMs),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(Modifier.padding(top = 8.dp)) {
                if (segment.topApps.isEmpty()) {
                    Text("Aucun usage sur cette période.", style = MaterialTheme.typography.labelSmall)
                } else {
                    val maxMs = segment.topApps.maxOf { it.totalTimeMs }
                    segment.topApps.forEachIndexed { index, app ->
                        AppUsageRow(
                            info = app.info,
                            totalTimeMs = app.totalTimeMs,
                            fraction = app.totalTimeMs.toFloat() / maxMs,
                            accentColor = SeriesColors[index % SeriesColors.size],
                        )
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
