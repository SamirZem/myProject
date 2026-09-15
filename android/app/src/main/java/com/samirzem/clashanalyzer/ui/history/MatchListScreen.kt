package com.samirzem.clashanalyzer.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.samirzem.clashanalyzer.data.local.MatchAnalysisEntity
import com.samirzem.clashanalyzer.di.ServiceLocator
import java.text.DateFormat
import java.util.Date

@Composable
fun MatchListScreen(onOpenDetail: (Long) -> Unit) {
    val matches by ServiceLocator.matchRepository.observeMatches().collectAsState(initial = emptyList())

    Scaffold(topBar = { TopAppBar(title = { Text("Historique des parties") }) }) { padding ->
        if (matches.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
                Text("Aucune partie analysée pour le moment.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(matches, key = { it.id }) { entity -> MatchRow(entity, onClick = { onOpenDetail(entity.id) }) }
            }
        }
    }
}

@Composable
private fun MatchRow(entity: MatchAnalysisEntity, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(entity.opponentName ?: "Adversaire inconnu", style = MaterialTheme.typography.titleMedium)
                Text("${entity.score}/100", style = MaterialTheme.typography.titleMedium)
            }
            Text(
                DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(entity.createdAtMs)) +
                    " · " + if (entity.source == "LIVE_CAPTURE") "Capture live" else "API officielle",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(entity.summary, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}
