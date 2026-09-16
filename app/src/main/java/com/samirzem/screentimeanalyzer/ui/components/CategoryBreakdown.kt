package com.samirzem.screentimeanalyzer.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.samirzem.screentimeanalyzer.data.ResolvedAppInfo
import com.samirzem.screentimeanalyzer.ui.theme.SeriesColors
import com.samirzem.screentimeanalyzer.util.Formatters

data class CategoryTopApp(val info: ResolvedAppInfo, val totalTimeMs: Long)

data class CategoryUsageUi(
    val label: String,
    val totalMs: Long,
    val topApps: List<CategoryTopApp> = emptyList(),
)

/**
 * Stacked color bar + legend for a category breakdown. Tap a category row to expand
 * its top apps below it; tap an app there to open it via [onAppClick].
 */
@Composable
fun CategoryBreakdown(categories: List<CategoryUsageUi>, onAppClick: ((String) -> Unit)? = null) {
    var expandedIndex by remember { mutableStateOf<Int?>(null) }
    val total = categories.sumOf { it.totalMs }.coerceAtLeast(1L)

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp)),
        ) {
            categories.forEachIndexed { index, category ->
                val weight = (category.totalMs.toFloat() / total).coerceAtLeast(0.01f)
                Box(
                    modifier = Modifier
                        .weight(weight)
                        .fillMaxHeight()
                        .background(SeriesColors[index % SeriesColors.size]),
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        categories.forEachIndexed { index, category ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { expandedIndex = if (expandedIndex == index) null else index }
                    .padding(vertical = 4.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(SeriesColors[index % SeriesColors.size]),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = category.label,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "${(category.totalMs * 100 / total)} %",
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = Formatters.duration(category.totalMs),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                AnimatedVisibility(visible = expandedIndex == index) {
                    Column(Modifier.padding(top = 8.dp, start = 20.dp)) {
                        if (category.topApps.isEmpty()) {
                            Text(
                                "Aucun détail disponible pour cette catégorie.",
                                style = MaterialTheme.typography.labelSmall,
                            )
                        } else {
                            val maxMs = category.topApps.maxOf { it.totalTimeMs }
                            category.topApps.forEachIndexed { appIndex, app ->
                                AppUsageRow(
                                    info = app.info,
                                    totalTimeMs = app.totalTimeMs,
                                    fraction = app.totalTimeMs.toFloat() / maxMs,
                                    accentColor = SeriesColors[appIndex % SeriesColors.size],
                                    onClick = if (onAppClick != null) {
                                        { onAppClick(app.info.packageName) }
                                    } else {
                                        null
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
