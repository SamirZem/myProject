package com.samirzem.clashanalyzer.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import com.samirzem.clashanalyzer.analyzer.CardDatabase
import com.samirzem.clashanalyzer.analyzer.model.CardInfo

private val allCards: List<CardInfo> by lazy { CardDatabase.all.sortedBy { it.frenchName } }
private const val MAX_SUGGESTIONS = 8

/** French display name for the given canonical (English/API) card name, falling back to the name itself. */
private fun frenchNameOf(canonicalName: String): String =
    if (canonicalName.isBlank()) "" else CardDatabase[canonicalName].frenchName

/**
 * A text field with a filterable dropdown of known card names, shown in French (matching the
 * French Clash Royale client) while the value read/written through [onValueChange] stays the
 * canonical English name the rest of the app (storage, deck matching, template lookup) expects —
 * that canonical name is also what the official API always returns, regardless of game language.
 *
 * Shows the full list as soon as the field is focused (not just once typing starts), and keeps
 * filtering it as the user types. Still accepts free text for a card that isn't in the database
 * yet (a very recent release), it just won't show suggestions for it.
 *
 * Uses a plain anchored [DropdownMenu] rather than Material3's ExposedDropdownMenuBox: the
 * latter's API has changed across recent Material3 versions (menuAnchor signature, the
 * ExposedDropdownMenu composable itself), and this project can't compile-check against the
 * exact version in use here, so the simpler, long-stable DropdownMenu is the safer bet.
 */
@Composable
fun CardNamePicker(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    var query by remember(value) { mutableStateOf(frenchNameOf(value).ifBlank { value }) }
    val suggestions = remember(query) {
        val matches = if (query.isBlank()) {
            allCards
        } else {
            allCards.filter { it.frenchName.contains(query, ignoreCase = true) || it.name.contains(query, ignoreCase = true) }
        }
        matches.take(MAX_SUGGESTIONS)
    }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                onValueChange(it)
                expanded = true
            },
            label = { Text(label) },
            singleLine = true,
            trailingIcon = {
                val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "cardPickerArrowRotation")
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Ouvrir/fermer la liste des cartes",
                        modifier = Modifier.graphicsLayer { rotationZ = rotation },
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { if (it.isFocused) expanded = true },
        )
        DropdownMenu(
            expanded = expanded && suggestions.isNotEmpty(),
            onDismissRequest = { expanded = false },
            properties = androidx.compose.ui.window.PopupProperties(focusable = false),
        ) {
            suggestions.forEach { card ->
                DropdownMenuItem(
                    text = { Text(card.frenchName) },
                    onClick = {
                        query = card.frenchName
                        onValueChange(card.name)
                        expanded = false
                    },
                )
            }
        }
    }
}
