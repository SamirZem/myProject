package com.samirzem.screentimeanalyzer.ui.appdetail

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samirzem.screentimeanalyzer.ui.LambdaViewModelFactory
import com.samirzem.screentimeanalyzer.ui.components.AppIcon
import com.samirzem.screentimeanalyzer.ui.components.DailyBarChart
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StatTile("Total (14j)", Formatters.duration(state.totalTimeMs), Modifier.weight(1f))
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
                Text("Derniers 14 jours", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))
                DailyBarChart(
                    values = state.dailySeries,
                    labels = state.dailyLabels,
                    highlightIndex = state.dailySeries.lastIndex,
                )
            }
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
