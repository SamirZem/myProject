package com.samirzem.screentimeanalyzer.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
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
import com.samirzem.screentimeanalyzer.util.TimeUtils
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val MS_PER_DAY = 86_400_000L

/**
 * Previous/next arrows around a tappable date label - tapping it opens a full
 * date picker. Shared by any screen that lets you browse a single day at a time.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayNavigator(
    selectedEpochDay: Long,
    isToday: Boolean,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onDaySelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        DayPickerDialog(
            initialEpochDay = selectedEpochDay,
            onDismiss = { showDatePicker = false },
            onConfirm = { epochDay ->
                onDaySelected(epochDay)
                showDatePicker = false
            },
        )
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onPreviousDay) {
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
                text = dayHeaderLabel(selectedEpochDay, isToday),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        IconButton(onClick = onNextDay, enabled = !isToday) {
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = "Jour suivant",
                tint = if (isToday) {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
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

fun dayHeaderLabel(epochDay: Long, isToday: Boolean): String {
    if (isToday) return "Aujourd'hui"
    if (epochDay == TimeUtils.todayEpochDay() - 1) return "Hier"
    return LocalDate.ofEpochDay(epochDay)
        .format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH))
        .replaceFirstChar { it.uppercase() }
}
