package com.samirzem.clashanalyzer.ui.calibration

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.samirzem.clashanalyzer.capture.CalibrationProfile
import com.samirzem.clashanalyzer.capture.CaptureForegroundService
import com.samirzem.clashanalyzer.capture.FrameAnalyzer
import com.samirzem.clashanalyzer.capture.NormalizedRect
import com.samirzem.clashanalyzer.di.ServiceLocator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private data class Region(val label: String, val get: (CalibrationProfile) -> NormalizedRect, val set: (CalibrationProfile, NormalizedRect) -> CalibrationProfile)

private fun regions(): List<Region> = listOf(
    Region("Élixir (moi)", { it.myElixirBarRect }, { p, r -> p.copy(myElixirBarRect = r) }),
    // No "Élixir (adversaire)" region: Clash Royale's normal 1v1 UI never shows the opponent's
    // elixir count, so there is nothing on screen to calibrate for it.
    Region("Carte main 1", { it.handSlotRects[0] }, { p, r -> p.copy(handSlotRects = p.handSlotRects.toMutableList().also { it[0] = r }) }),
    Region("Carte main 2", { it.handSlotRects[1] }, { p, r -> p.copy(handSlotRects = p.handSlotRects.toMutableList().also { it[1] = r }) }),
    Region("Carte main 3", { it.handSlotRects[2] }, { p, r -> p.copy(handSlotRects = p.handSlotRects.toMutableList().also { it[2] = r }) }),
    Region("Carte main 4", { it.handSlotRects[3] }, { p, r -> p.copy(handSlotRects = p.handSlotRects.toMutableList().also { it[3] = r }) }),
    Region("Tour Roi (moi)", { it.myTowerRects[0] }, { p, r -> p.copy(myTowerRects = p.myTowerRects.toMutableList().also { it[0] = r }) }),
    Region("Tour Princesse G (moi)", { it.myTowerRects[1] }, { p, r -> p.copy(myTowerRects = p.myTowerRects.toMutableList().also { it[1] = r }) }),
    Region("Tour Princesse D (moi)", { it.myTowerRects[2] }, { p, r -> p.copy(myTowerRects = p.myTowerRects.toMutableList().also { it[2] = r }) }),
    Region("Tour Roi (adversaire)", { it.oppTowerRects[0] }, { p, r -> p.copy(oppTowerRects = p.oppTowerRects.toMutableList().also { it[0] = r }) }),
    Region("Tour Princesse G (adv.)", { it.oppTowerRects[1] }, { p, r -> p.copy(oppTowerRects = p.oppTowerRects.toMutableList().also { it[1] = r }) }),
    Region("Tour Princesse D (adv.)", { it.oppTowerRects[2] }, { p, r -> p.copy(oppTowerRects = p.oppTowerRects.toMutableList().also { it[2] = r }) }),
)

@Composable
fun CalibrationScreen() {
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf(CalibrationProfile.default()) }
    var selectedRegion by remember { mutableIntStateOf(0) }
    var cardNameForTemplate by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<String?>(null) }
    val frame by CaptureForegroundService.latestFrame.collectAsState()
    val regionList = remember { regions() }

    LaunchedEffect(Unit) {
        profile = ServiceLocator.calibrationStore.profile.first()
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Calibration") }) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Lance d'abord une capture (retour à l'accueil > Démarrer la capture), ouvre Clash Royale, " +
                    "puis reviens ici : l'aperçu ci-dessous montre l'écran capturé en direct pour ajuster chaque zone.",
                style = MaterialTheme.typography.bodySmall,
            )

            Box(modifier = Modifier.fillMaxWidth().aspectRatio(9f / 20f).background(Color.DarkGray)) {
                frame?.let { bmp ->
                    androidx.compose.foundation.Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                    )
                } ?: Text("Aperçu indisponible (démarre la capture)", modifier = Modifier.padding(16.dp))

                val selected = regionList[selectedRegion].get(profile)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val topLeft = Offset(selected.left * size.width, selected.top * size.height)
                    val boxSize = Size((selected.right - selected.left) * size.width, (selected.bottom - selected.top) * size.height)
                    drawRect(color = Color.Yellow, topLeft = topLeft, size = boxSize, style = Stroke(width = 3f))
                }
            }

            Text("Zone à ajuster", style = MaterialTheme.typography.titleMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(regionList.size) { index ->
                    FilterChip(
                        selected = index == selectedRegion,
                        onClick = { selectedRegion = index },
                        label = { Text(regionList[index].label) },
                    )
                }
            }

            val current = regionList[selectedRegion].get(profile)
            RectSliders(current) { updated ->
                profile = regionList[selectedRegion].set(profile, FrameAnalyzer.sanitize(updated))
            }

            Text("Couleurs de référence", style = MaterialTheme.typography.titleMedium)
            Text("Vise le centre de la zone sélectionnée ci-dessus avant d'appuyer (élixir plein pour la barre d'élixir, tour en pleine santé pour les tours).", style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    frame?.let { profile = profile.copy(elixirFilledColor = sampleCenter(it, current)) }
                }) { Text("Élixir plein") }
                Button(onClick = {
                    frame?.let { profile = profile.copy(elixirEmptyColor = sampleCenter(it, current)) }
                }) { Text("Élixir vide") }
            }
            Text(
                "Les barres de PV sont bleues côté allié et rouges/roses côté adversaire : vise une " +
                    "de tes tours pour la première, une tour adverse pour la seconde.",
                style = MaterialTheme.typography.bodySmall,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    frame?.let { profile = profile.copy(myTowerHealthyColor = sampleCenter(it, current)) }
                }) { Text("Tour pleine vie (moi)") }
                Button(onClick = {
                    frame?.let { profile = profile.copy(oppTowerHealthyColor = sampleCenter(it, current)) }
                }) { Text("Tour pleine vie (adv.)") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    frame?.let { profile = profile.copy(towerBackgroundColor = sampleCenter(it, current)) }
                }) { Text("Fond barre de tour") }
            }

            Text("Empreintes de cartes (pour reconnaître ta main)", style = MaterialTheme.typography.titleMedium)
            Text(
                "Sélectionne une des 4 zones \"Carte main\" ci-dessus, joue la carte correspondante en jeu pour " +
                    "qu'elle soit visible dans ce slot, puis capture son empreinte.",
                style = MaterialTheme.typography.bodySmall,
            )
            OutlinedTextField(value = cardNameForTemplate, onValueChange = { cardNameForTemplate = it }, label = { Text("Nom exact de la carte (ex: Hog Rider)") })
            Button(onClick = {
                val f = frame
                if (f != null && cardNameForTemplate.isNotBlank() && selectedRegion in 1..4) {
                    val rect = regionList[selectedRegion].get(profile)
                    val crop = FrameAnalyzer.cropHandSlot(f, rect)
                    ServiceLocator.cardTemplateStore.saveTemplate(cardNameForTemplate.trim(), crop)
                    status = "Empreinte enregistrée pour ${cardNameForTemplate.trim()}."
                } else {
                    status = "Sélectionne une zone \"Carte main\" et lance la capture d'abord."
                }
            }) { Text("Capturer l'empreinte de cette carte") }

            Button(onClick = {
                scope.launch {
                    ServiceLocator.calibrationStore.save(profile)
                    status = "Calibration enregistrée."
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Sauvegarder la calibration") }

            status?.let { Text(it) }
        }
    }
}

private fun sampleCenter(bitmap: android.graphics.Bitmap, rect: NormalizedRect): Int =
    FrameAnalyzer.sampleColorAt(bitmap, (rect.left + rect.right) / 2f, (rect.top + rect.bottom) / 2f)

@Composable
private fun RectSliders(rect: NormalizedRect, onChange: (NormalizedRect) -> Unit) {
    Column {
        LabeledSlider("Gauche", rect.left) { onChange(rect.copy(left = it)) }
        LabeledSlider("Haut", rect.top) { onChange(rect.copy(top = it)) }
        LabeledSlider("Droite", rect.right) { onChange(rect.copy(right = it)) }
        LabeledSlider("Bas", rect.bottom) { onChange(rect.copy(bottom = it)) }
    }
}

@Composable
private fun LabeledSlider(label: String, value: Float, onChange: (Float) -> Unit) {
    Column {
        Text("$label : ${"%.3f".format(value)}", style = MaterialTheme.typography.bodySmall)
        Slider(value = value, onValueChange = onChange, valueRange = 0f..1f)
    }
}
