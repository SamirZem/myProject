package com.samirzem.clashanalyzer.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.samirzem.clashanalyzer.di.ServiceLocator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    onRequestCapture: () -> Unit,
    onStopCapture: () -> Unit,
    onOpenCalibration: () -> Unit,
    onOpenHistory: () -> Unit,
) {
    val controller = ServiceLocator.matchSessionController
    val isRunning by controller.isRunning.collectAsState()
    val sampleCount by controller.sampleCount.collectAsState()
    val scope = rememberCoroutineScope()

    var playerTag by remember { mutableStateOf("") }
    var backendUrl by remember { mutableStateOf("") }
    var deckInput by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        playerTag = ServiceLocator.settings.playerTag.first().orEmpty()
        backendUrl = ServiceLocator.settings.backendBaseUrl.first().orEmpty()
        deckInput = ServiceLocator.settings.currentDeck.first().joinToString(", ")
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Clash Analyzer") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Capture en direct", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                    Text(
                        "Lance la capture AVANT d'ouvrir Clash Royale. L'écran est lu en local pour l'élixir, " +
                            "les PV des tours et tes cartes jouées ; rien n'est enregistré sur le disque, l'analyse " +
                            "est calculée à la volée puis les images sont jetées.",
                    )
                    if (isRunning) {
                        Spacer(Modifier.height(4.dp))
                        androidx.compose.foundation.layout.Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Capture en cours… $sampleCount échantillons")
                        }
                        Button(onClick = {
                            scope.launch {
                                val id = controller.stopAndSave()
                                statusMessage = if (id != null) "Partie analysée (id=$id)." else "Pas assez de données capturées."
                                onStopCapture()
                            }
                        }) { Text("Arrêter et analyser") }
                    } else {
                        Button(onClick = onRequestCapture) { Text("Démarrer la capture") }
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Paramètres", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = backendUrl,
                        onValueChange = { backendUrl = it },
                        label = { Text("URL du serveur relais (ex: https://xxx.onrender.com/)") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = playerTag,
                        onValueChange = { playerTag = it },
                        label = { Text("Ton tag joueur (#XXXXXXX)") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = deckInput,
                        onValueChange = { deckInput = it },
                        label = { Text("Ton deck actuel (8 noms séparés par des virgules)") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(onClick = {
                        scope.launch {
                            if (backendUrl.isNotBlank()) ServiceLocator.settings.setBackendBaseUrl(backendUrl)
                            if (playerTag.isNotBlank()) ServiceLocator.settings.setPlayerTag(playerTag)
                            val deck = deckInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                            ServiceLocator.settings.setCurrentDeck(deck)
                            statusMessage = "Paramètres enregistrés."
                        }
                    }) { Text("Enregistrer") }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Confirmer le résultat via l'API officielle", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                    Text("Récupère ta dernière partie via l'API Clash Royale pour recouper le score de l'analyse live.")
                    Button(onClick = {
                        scope.launch {
                            val result = ServiceLocator.matchRepository.fetchAndSaveLatestFromApi()
                            statusMessage = result.fold(
                                onSuccess = { "Analyse API enregistrée (id=$it)." },
                                onFailure = { "Erreur : ${it.message}" },
                            )
                        }
                    }) { Text("Analyser ma dernière partie (API)") }
                }
            }

            OutlinedButton(onClick = onOpenCalibration, modifier = Modifier.fillMaxWidth()) { Text("Calibration de la capture") }
            OutlinedButton(onClick = onOpenHistory, modifier = Modifier.fillMaxWidth()) { Text("Historique des parties") }

            statusMessage?.let { Text(it) }
        }
    }
}
