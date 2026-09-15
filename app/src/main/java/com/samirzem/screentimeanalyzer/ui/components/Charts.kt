package com.samirzem.screentimeanalyzer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** A minimal bottom-aligned bar chart, one bar per entry in [values]. */
@Composable
fun DailyBarChart(
    values: List<Long>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 120.dp,
    barColor: Color = MaterialTheme.colorScheme.primary,
    highlightIndex: Int? = null,
    highlightColor: Color = MaterialTheme.colorScheme.secondary,
) {
    val maxValue = (values.maxOrNull() ?: 0L).coerceAtLeast(1L)
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom,
    ) {
        values.forEachIndexed { index, value ->
            val fraction = (value.toFloat() / maxValue).coerceIn(0f, 1f)
            val barHeight = (chartHeight.value * fraction.coerceAtLeast(if (value > 0) 0.03f else 0f)).dp
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(barHeight)
                        .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                        .background(if (index == highlightIndex) highlightColor else barColor)
                )
                androidx.compose.foundation.layout.Spacer(Modifier.height(4.dp))
                Text(
                    text = labels.getOrElse(index) { "" },
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                )
            }
        }
    }
}

/** 24-cell hour-of-day intensity grid, darker/brighter = more foreground time in that hour. */
@Composable
fun HourHeatmap(hourlyMs: LongArray, modifier: Modifier = Modifier) {
    val maxValue = (hourlyMs.maxOrNull() ?: 0L).coerceAtLeast(1L)
    val baseColor = MaterialTheme.colorScheme.primary
    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            for (hour in 0 until 24) {
                val intensity = (hourlyMs[hour].toFloat() / maxValue).coerceIn(0f, 1f)
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(0.55f)
                        .padding(1.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(baseColor.copy(alpha = 0.06f + intensity * 0.9f)),
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
    }
}
