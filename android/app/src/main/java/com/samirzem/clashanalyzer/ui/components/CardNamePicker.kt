package com.samirzem.clashanalyzer.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.samirzem.clashanalyzer.analyzer.CardDatabase

private val allCardNames: List<String> by lazy { CardDatabase.all.map { it.name }.sorted() }
private const val MAX_SUGGESTIONS = 8

/**
 * A text field with a filterable dropdown of known card names (from [CardDatabase]), so the
 * saved name always matches exactly what the analyzer recognizes — no typos, no guessing the
 * exact spelling/punctuation (e.g. "P.E.K.K.A"). Still accepts free text for a card that isn't
 * in the database yet (a very recent release), it just won't show suggestions for it.
 *
 * Uses a plain anchored [DropdownMenu] rather than Material3's ExposedDropdownMenuBox: the
 * latter's API has changed across recent Material3 versions (menuAnchor signature, the
 * ExposedDropdownMenu composable itself), and this project can't compile-check against the
 * exact version in use here, so the simpler, long-stable DropdownMenu is the safer bet.
 */
@Composable
fun CardNamePicker(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val suggestions = remember(value) {
        val matches = if (value.isBlank()) allCardNames else allCardNames.filter { it.contains(value, ignoreCase = true) }
        matches.take(MAX_SUGGESTIONS)
    }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                expanded = true
            },
            label = { Text(label) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        DropdownMenu(
            expanded = expanded && suggestions.isNotEmpty(),
            onDismissRequest = { expanded = false },
            properties = androidx.compose.ui.window.PopupProperties(focusable = false),
        ) {
            suggestions.forEach { name ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        onValueChange(name)
                        expanded = false
                    },
                )
            }
        }
    }
}
