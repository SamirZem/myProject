package com.samirzem.clashanalyzer.ui.deckimport

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.samirzem.clashanalyzer.capture.DeckScreenLayout
import com.samirzem.clashanalyzer.capture.FrameAnalyzer
import com.samirzem.clashanalyzer.capture.NormalizedRect
import com.samirzem.clashanalyzer.di.ServiceLocator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Imports a deck without any backend/API key: pick a screenshot of Clash Royale's deck-selection
 * menu from the gallery, line up 8 rectangles over the card slots, type each card's name once.
 * Saves both the deck (Settings) and a recognition template per card (for live hand-slot
 * detection during capture) — entirely on-device, nothing ever leaves the phone.
 */
@Composable
fun DeckImportScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var layout by remember { mutableStateOf(DeckScreenLayout.default()) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedSlot by remember { mutableIntStateOf(0) }
    val cardNames = remember { mutableStateListOf("", "", "", "", "", "", "", "") }
    var status by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        layout = ServiceLocator.deckScreenLayoutStore.layout.first()
    }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) {
            bitmap = decodeBitmap(uri, context)
            status = null
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Importer le deck depuis une image") }) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Prends une capture d'écran de ton menu Deck en jeu (8 cartes visibles), importe-la ici, " +
                    "ajuste les 8 cases si besoin, puis indique le nom de chaque carte. Tout reste sur ton téléphone.",
                style = MaterialTheme.typography.bodySmall,
            )

            Button(onClick = { pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                Text("Choisir une capture d'écran du deck")
            }

            Box(modifier = Modifier.fillMaxWidth().aspectRatio(9f / 20f).background(Color.DarkGray)) {
                val bmp = bitmap
                if (bmp != null) {
                    Image(bitmap = bmp.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize())

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        layout.cardRects.forEachIndexed { index, rect ->
                            val topLeft = Offset(rect.left * size.width, rect.top * size.height)
                            val boxSize = Size((rect.right - rect.left) * size.width, (rect.bottom - rect.top) * size.height)
                            val color = if (index == selectedSlot) Color.Yellow else Color(0x99FFFFFF)
                            drawRect(color = color, topLeft = topLeft, size = boxSize, style = Stroke(width = if (index == selectedSlot) 4f else 2f))
                        }
                    }
                } else {
                    Text("Aucune image choisie", modifier = Modifier.padding(16.dp))
                }
            }

            Text("Case à ajuster", style = MaterialTheme.typography.titleMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(8) { index ->
                    FilterChip(
                        selected = index == selectedSlot,
                        onClick = { selectedSlot = index },
                        label = { Text("Carte ${index + 1}") },
                    )
                }
            }

            val selectedRect = layout.cardRects[selectedSlot]
            RectSliders(selectedRect) { updated ->
                val sanitized = FrameAnalyzer.sanitize(updated)
                layout = layout.copy(cardRects = layout.cardRects.toMutableList().also { it[selectedSlot] = sanitized })
            }

            OutlinedTextField(
                value = cardNames[selectedSlot],
                onValueChange = { cardNames[selectedSlot] = it },
                label = { Text("Nom exact de la carte ${selectedSlot + 1} (ex: Hog Rider)") },
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val bmp = bitmap
                    val names = cardNames.map { it.trim() }
                    when {
                        bmp == null -> status = "Choisis d'abord une image."
                        names.any { it.isEmpty() } -> status = "Renseigne le nom des 8 cartes avant d'importer."
                        else -> {
                            scope.launch {
                                layout.cardRects.forEachIndexed { index, rect ->
                                    val crop = FrameAnalyzer.cropHandSlot(bmp, rect)
                                    ServiceLocator.cardTemplateStore.saveTemplate(names[index], crop)
                                }
                                ServiceLocator.settings.setCurrentDeck(names)
                                ServiceLocator.deckScreenLayoutStore.save(layout)
                                status = "Deck importé et empreintes enregistrées : ${names.joinToString(", ")}"
                            }
                        }
                    }
                },
            ) { Text("Importer le deck") }

            status?.let { Text(it) }
        }
    }
}

private fun decodeBitmap(uri: Uri, context: Context): Bitmap {
    val source = ImageDecoder.createSource(context.contentResolver, uri)
    return ImageDecoder.decodeBitmap(source)
}

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
