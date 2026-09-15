package com.samirzem.clashanalyzer.capture

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.calibrationDataStore by preferencesDataStore(name = "calibration")

class CalibrationStore(private val context: Context) {

    private val key = stringPreferencesKey("profile_json")
    private val json = Json { ignoreUnknownKeys = true }

    val profile: Flow<CalibrationProfile> = context.calibrationDataStore.data.map { prefs ->
        prefs[key]?.let { raw -> runCatching { json.decodeFromString(CalibrationProfile.serializer(), raw) }.getOrNull() }
            ?: CalibrationProfile.default()
    }

    suspend fun save(profile: CalibrationProfile) {
        context.calibrationDataStore.edit { it[key] = json.encodeToString(CalibrationProfile.serializer(), profile) }
    }
}
