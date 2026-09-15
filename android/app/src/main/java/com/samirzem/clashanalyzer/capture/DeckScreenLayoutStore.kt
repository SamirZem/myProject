package com.samirzem.clashanalyzer.capture

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.deckScreenLayoutDataStore by preferencesDataStore(name = "deck_screen_layout")

class DeckScreenLayoutStore(private val context: Context) {

    private val key = stringPreferencesKey("layout_json")
    private val json = Json { ignoreUnknownKeys = true }

    val layout: Flow<DeckScreenLayout> = context.deckScreenLayoutDataStore.data.map { prefs ->
        prefs[key]?.let { raw -> runCatching { json.decodeFromString(DeckScreenLayout.serializer(), raw) }.getOrNull() }
            ?: DeckScreenLayout.default()
    }

    suspend fun save(layout: DeckScreenLayout) {
        context.deckScreenLayoutDataStore.edit { it[key] = json.encodeToString(DeckScreenLayout.serializer(), layout) }
    }
}
