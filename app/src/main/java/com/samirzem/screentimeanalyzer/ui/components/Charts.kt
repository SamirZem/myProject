package com.samirzem.screentimeanalyzer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** A minimal bottom-aligned bar chart, one bar per entry in [values]. Tap a bar via [onBarClick]. */
@Composable
fun DailyBarChart(
    values: List<Long>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 120.dp,
    barColor: Color = MaterialTheme.colorScheme.primary,
    highlightIndex: Int? = null,
    highlightColor: Color = MaterialTheme.colorScheme.secondary,
    onBarClick: ((Int) -> Unit)? = null,
) {
    val maxValue = (values.maxOrNull() ?: 0L).coerceAtLeast(1L)
    // Past ~16 bars, a label under every single one just overlaps into noise; tap-to-reveal
    // (wired up by callers via onBarClick) is how the exact day is found instead.
    val showLabels = values.size <= 16
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom,
    ) {
        values.forEachIndexed { index, value ->
            val fraction = (value.toFloat() / maxValue).coerceIn(0f, 1f)
            val barHeight = (chartHeight.value * fraction.coerceAtLeast(if (value > 0) 0.03f else 0f)).dp
            Column(
                modifier = Modifier
                    .weight(1f)
                    .let { m -> if (onBarClick != null) m.clickable { onBarClick(index) } else m },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(barHeight)
                        .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                        .background(if (index == highlightIndex) highlightColor else barColor)
                )
                if (showLabels) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = labels.getOrElse(index) { "" },
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/**
 * 24-cell hour-of-day intensity grid with a gradient legend. Tap a cell via [onHourClick];
 * [selectedHour] outlines the currently expanded one.
 */
@Composable
fun HourHeatmap(
    values: LongArray,
    modifier: Modifier = Modifier,
    selectedHour: Int? = null,
    onHourClick: ((Int) -> Unit)? = null,
    valueFormatter: (Long) -> String = { it.toString() },
) {
    val maxValue = (values.maxOrNull() ?: 0L).coerceAtLeast(1L)
    val baseColor = MaterialTheme.colorScheme.primary
    val selectionColor = MaterialTheme.colorScheme.secondary
    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            for (hour in 0 until 24) {
                val intensity = (values[hour].toFloat() / maxValue).coerceIn(0f, 1f)
                val isSelected = hour == selectedHour
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(0.55f)
                        .padding(1.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(baseColor.copy(alpha = 0.08f + intensity * 0.85f))
                        .let { m ->
                            if (isSelected) {
                                m.border(2.dp, selectionColor, RoundedCornerShape(3.dp))
                            } else {
                                m
                            }
                        }
                        .let { m -> if (onHourClick != null) m.clickable { onHourClick(hour) } else m },
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            listOf("0h", "6h", "12h", "18h", "24h").forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Faible", style = MaterialTheme.typography.labelSmall)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(baseColor.copy(alpha = 0.08f), baseColor),
                        ),
                    ),
            )
            Text("Élevé (${valueFormatter(maxValue)})", style = MaterialTheme.typography.labelSmall)
        }
    }
}
