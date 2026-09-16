package com.samirzem.screentimeanalyzer.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samirzem.screentimeanalyzer.ui.LambdaViewModelFactory
import com.samirzem.screentimeanalyzer.ui.components.AppUsageRow
import com.samirzem.screentimeanalyzer.ui.components.DayNavigator
import com.samirzem.screentimeanalyzer.ui.components.HourHeatmap
import com.samirzem.screentimeanalyzer.ui.rememberApp
import com.samirzem.screentimeanalyzer.ui.theme.Amber
import com.samirzem.screentimeanalyzer.ui.theme.Coral
import com.samirzem.screentimeanalyzer.ui.theme.SeriesColors
import com.samirzem.screentimeanalyzer.ui.theme.Teal
import com.samirzem.screentimeanalyzer.ui.theme.TrendDownContainer
import com.samirzem.screentimeanalyzer.ui.theme.TrendDownOnContainer
import com.samirzem.screentimeanalyzer.ui.theme.TrendUpContainer
import com.samirzem.screentimeanalyzer.ui.theme.TrendUpOnContainer
import com.samirzem.screentimeanalyzer.util.Formatters

@Composable
fun DashboardScreen(onAppClick: (String) -> Unit, onSeeAllClick: () -> Unit) {
    val app = rememberApp()
    val viewModel: DashboardViewModel = viewModel(
        factory = LambdaViewModelFactory { DashboardViewModel(app.repository, app.appInfoResolver) },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedHour by remember { mutableStateOf<Int?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            DayNavigator(
                selectedEpochDay = state.selectedEpochDay,
                isToday = state.isToday,
                onPreviousDay = viewModel::goToPreviousDay,
                onNextDay = viewModel::goToNextDay,
                onDaySelected = viewModel::selectDay,
            )
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .background(
                        Brush.linearGradient(
                            listOf(Teal.copy(alpha = 0.22f), Teal.copy(alpha = 0.04f)),
                        ),
                    )
                    .padding(20.dp),
            ) {
                Text(
                    text = Formatters.duration(state.dayTotalMs),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (state.averagePreviousDaysMs > 0) {
                    Spacer(Modifier.height(10.dp))
                    TrendPill(dayTotalMs = state.dayTotalMs, averagePreviousDaysMs = state.averagePreviousDaysMs)
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StatCard(
                    icon = Icons.Filled.LockOpen,
                    accentColor = Coral,
                    title = "Déverrouillages",
                    value = state.unlockCountDay.toString(),
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    icon = Icons.Filled.Apps,
                    accentColor = Teal,
                    title = "Apps utilisées",
                    value = state.appCountDay.toString(),
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    icon = Icons.Filled.Schedule,
                    accentColor = Amber,
                    title = "Dernière activité",
                    value = state.lastUsedAtMs?.let(Formatters::clockTime) ?: "—",
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            SectionCard(title = "Répartition par heure") {
                HourHeatmap(
                    values = state.dayHourlyMs,
                    selectedHour = selectedHour,
                    onHourClick = { hour -> selectedHour = if (selectedHour == hour) null else hour },
                    valueFormatter = Formatters::duration,
                )
                AnimatedVisibility(visible = selectedHour != null) {
                    val hour = selectedHour ?: 0
                    val topApps = state.hourlyTopApps[hour].orEmpty()
                    Column {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "${hour}h-${hour + 1}h : ${Formatters.duration(state.dayHourlyMs.getOrElse(hour) { 0 })}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (topApps.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            val maxMs = topApps.maxOf { it.totalTimeMs }
                            topApps.forEachIndexed { index, row ->
                                AppUsageRow(
                                    info = row.info,
                                    totalTimeMs = row.totalTimeMs,
                                    fraction = row.totalTimeMs.toFloat() / maxMs,
                                    accentColor = SeriesColors[index % SeriesColors.size],
                                )
                            }
                        }
                    }
                }
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
            item { Text("Aucune donnée pour ce jour.") }
        }

        itemsIndexed(state.topApps) { index, row ->
            AppUsageRow(
                info = row.info,
                totalTimeMs = row.totalTimeMs,
                fraction = row.totalTimeMs.toFloat() / state.topAppsMaxMs,
                accentColor = SeriesColors[index % SeriesColors.size],
                onClick = { onAppClick(row.info.packageName) },
            )
        }
    }
}

@Composable
private fun TrendPill(dayTotalMs: Long, averagePreviousDaysMs: Long) {
    val diff = dayTotalMs - averagePreviousDaysMs
    val diffPercent = (diff.toDouble() / averagePreviousDaysMs) * 100
    val isUp = diff >= 0
    val containerColor = if (isUp) TrendUpContainer else TrendDownContainer
    val onContainerColor = if (isUp) TrendUpOnContainer else TrendDownOnContainer
    val sign = if (isUp) "+" else "-"
    val text = "$sign${Formatters.duration(kotlin.math.abs(diff))} vs moyenne 7j (%+.0f %%)".format(diffPercent)

    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
        color = onContainerColor,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(containerColor)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
private fun StatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.labelSmall)
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
