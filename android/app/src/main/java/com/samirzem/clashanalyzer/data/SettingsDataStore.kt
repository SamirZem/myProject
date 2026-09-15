package com.samirzem.clashanalyzer.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val PLAYER_TAG = stringPreferencesKey("player_tag")
        val BACKEND_BASE_URL = stringPreferencesKey("backend_base_url")
        val CURRENT_DECK = stringPreferencesKey("current_deck") // comma-separated card names, in hand-slot order
    }

    val playerTag: Flow<String?> = context.dataStore.data.map { it[Keys.PLAYER_TAG] }
    val backendBaseUrl: Flow<String?> = context.dataStore.data.map { it[Keys.BACKEND_BASE_URL] }
    val currentDeck: Flow<List<String>> = context.dataStore.data.map {
        it[Keys.CURRENT_DECK]?.split(",")?.filter { name -> name.isNotBlank() } ?: emptyList()
    }

    suspend fun setPlayerTag(tag: String) {
        context.dataStore.edit { it[Keys.PLAYER_TAG] = tag.trim().uppercase() }
    }

    suspend fun setBackendBaseUrl(url: String) {
        context.dataStore.edit { it[Keys.BACKEND_BASE_URL] = url.trim().trimEnd('/') + "/" }
    }

    suspend fun setCurrentDeck(cardNames: List<String>) {
        context.dataStore.edit { it[Keys.CURRENT_DECK] = cardNames.joinToString(",") }
    }
}
