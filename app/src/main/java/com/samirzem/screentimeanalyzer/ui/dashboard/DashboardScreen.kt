package com.samirzem.screentimeanalyzer.ui.dashboard

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samirzem.screentimeanalyzer.ui.LambdaViewModelFactory
import com.samirzem.screentimeanalyzer.ui.components.AppUsageRow
import com.samirzem.screentimeanalyzer.ui.rememberApp
import com.samirzem.screentimeanalyzer.ui.theme.TrendDownContainer
import com.samirzem.screentimeanalyzer.ui.theme.TrendDownOnContainer
import com.samirzem.screentimeanalyzer.ui.theme.TrendUpContainer
import com.samirzem.screentimeanalyzer.ui.theme.TrendUpOnContainer
import com.samirzem.screentimeanalyzer.util.Formatters
import com.samirzem.screentimeanalyzer.util.TimeUtils
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val MS_PER_DAY = 86_400_000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(onAppClick: (String) -> Unit, onSeeAllClick: () -> Unit) {
    val app = rememberApp()
    val viewModel: DashboardViewModel = viewModel(
        factory = LambdaViewModelFactory { DashboardViewModel(app.repository, app.appInfoResolver) },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        DayPickerDialog(
            initialEpochDay = state.selectedEpochDay,
            onDismiss = { showDatePicker = false },
            onConfirm = { epochDay ->
                viewModel.selectDay(epochDay)
                showDatePicker = false
            },
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = viewModel::goToPreviousDay) {
                    Icon(Icons.Filled.ChevronLeft, contentDescription = "Jour précédent")
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = { showDatePicker = true })
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Icon(
                        Icons.Filled.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Text(
                        text = dayHeaderLabel(state.selectedEpochDay, state.isToday),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                IconButton(onClick = viewModel::goToNextDay, enabled = !state.isToday) {
                    Icon(
                        Icons.Filled.ChevronRight,
                        contentDescription = "Jour suivant",
                        tint = if (state.isToday) {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                }
            }
        }

        item {
            Column {
                Text(
                    text = Formatters.duration(state.dayTotalMs),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (state.averagePreviousDaysMs > 0) {
                    Spacer(Modifier.height(8.dp))
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
                    title = "Déverrouillages",
                    value = state.unlockCountDay.toString(),
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    title = "Apps utilisées",
                    value = state.appCountDay.toString(),
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
            item { Text("Aucune donnée pour ce jour.") }
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
private fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayPickerDialog(
    initialEpochDay: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    val maxMillis = TimeUtils.todayEpochDay() * MS_PER_DAY
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initialEpochDay * MS_PER_DAY,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= maxMillis
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { millis -> onConfirm(millis / MS_PER_DAY) }
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    ) {
        DatePicker(state = state)
    }
}

private fun dayHeaderLabel(epochDay: Long, isToday: Boolean): String {
    if (isToday) return "Aujourd'hui"
    if (epochDay == TimeUtils.todayEpochDay() - 1) return "Hier"
    return LocalDate.ofEpochDay(epochDay)
        .format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH))
        .replaceFirstChar { it.uppercase() }
}
