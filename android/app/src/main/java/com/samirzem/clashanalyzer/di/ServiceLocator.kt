package com.samirzem.clashanalyzer.di

import android.content.Context
import com.samirzem.clashanalyzer.capture.CalibrationStore
import com.samirzem.clashanalyzer.capture.CardTemplateStore
import com.samirzem.clashanalyzer.capture.MatchSessionController
import com.samirzem.clashanalyzer.data.DeckImportRepository
import com.samirzem.clashanalyzer.data.MatchRepository
import com.samirzem.clashanalyzer.data.SettingsDataStore
import com.samirzem.clashanalyzer.data.local.AppDatabase
import com.samirzem.clashanalyzer.data.remote.BackendApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType.Companion.toMediaType
import java.util.concurrent.TimeUnit

/**
 * Small hand-rolled dependency container. No DI framework: the object graph here is tiny
 * (one repository, one database, one retrofit client built lazily against a user-configurable
 * backend URL) and doesn't earn the extra build complexity a framework like Hilt would add.
 */
object ServiceLocator {

    private lateinit var appContext: Context
    private var cachedBaseUrl: String? = null
    private var cachedApi: BackendApi? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    val settings: SettingsDataStore by lazy { SettingsDataStore(appContext) }
    val calibrationStore: CalibrationStore by lazy { CalibrationStore(appContext) }
    val cardTemplateStore: CardTemplateStore by lazy { CardTemplateStore(appContext) }

    private val database: AppDatabase by lazy { AppDatabase.getInstance(appContext) }
    private val appScope: CoroutineScope by lazy { CoroutineScope(SupervisorJob() + Dispatchers.Default) }

    val matchRepository: MatchRepository by lazy {
        MatchRepository(database.matchAnalysisDao(), settings) { backendApi() }
    }

    val deckImportRepository: DeckImportRepository by lazy {
        DeckImportRepository { backendApi() }
    }

    val matchSessionController: MatchSessionController by lazy {
        MatchSessionController(calibrationStore, cardTemplateStore, matchRepository, settings, appScope)
    }

    private val httpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    /** Rebuilds the Retrofit client only when the configured backend URL actually changes. */
    private suspend fun backendApi(): BackendApi {
        val baseUrl = settings.backendBaseUrl.first()
            ?: error("Configure d'abord l'URL de ton serveur relais dans les paramètres.")
        cachedApi?.let { if (cachedBaseUrl == baseUrl) return it }

        val json = Json { ignoreUnknownKeys = true }
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(httpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        return retrofit.create(BackendApi::class.java).also {
            cachedApi = it
            cachedBaseUrl = baseUrl
        }
    }
}
