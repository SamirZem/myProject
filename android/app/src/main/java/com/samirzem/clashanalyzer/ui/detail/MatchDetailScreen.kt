package com.samirzem.clashanalyzer.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.samirzem.clashanalyzer.analyzer.model.AnalysisResult
import com.samirzem.clashanalyzer.analyzer.model.Insight
import com.samirzem.clashanalyzer.analyzer.model.Severity
import com.samirzem.clashanalyzer.data.local.MatchAnalysisMapper
import com.samirzem.clashanalyzer.di.ServiceLocator
import com.samirzem.clashanalyzer.ui.theme.GoodColor
import com.samirzem.clashanalyzer.ui.theme.MajorColor
import com.samirzem.clashanalyzer.ui.theme.MinorColor

@Composable
fun MatchDetailScreen(matchId: Long) {
    var result by remember { mutableStateOf<AnalysisResult?>(null) }

    LaunchedEffect(matchId) {
        result = ServiceLocator.matchRepository.getMatch(matchId)?.let { MatchAnalysisMapper.toAnalysisResult(it) }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Détail de la partie") }) }) { padding ->
        val current = result
        if (current == null) {
            Column(Modifier.fillMaxSize().padding(padding).padding(24.dp)) { Text("Chargement…") }
            return@Scaffold
        }
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Score de performance", style = MaterialTheme.typography.labelLarge)
                    Text("${current.score}/100", style = MaterialTheme.typography.displaySmall)
                    Text(current.summary)
                }
            }

            InsightSection("Erreurs commises", current.mistakes)
            InsightSection("Bons mouvements", current.goodMoves)
            InsightSection("Conseils", current.tips)
        }
    }
}

@Composable
private fun InsightSection(title: String, insights: List<Insight>) {
    if (insights.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        insights.forEach { InsightCard(it) }
    }
}

@Composable
private fun InsightCard(insight: Insight) {
    val accent = when (insight.severity) {
        Severity.MAJOR -> MajorColor
        Severity.MINOR -> MinorColor
        Severity.GOOD -> GoodColor
        Severity.INFO -> Color.Gray
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(insight.title, style = MaterialTheme.typography.titleSmall, color = accent)
            Text(insight.detail, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
