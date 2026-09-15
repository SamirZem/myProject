package com.samirzem.clashanalyzer.capture

import android.graphics.Bitmap
import com.samirzem.clashanalyzer.analyzer.model.TelemetrySample
import com.samirzem.clashanalyzer.data.MatchRepository
import com.samirzem.clashanalyzer.data.SettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Drives a live capture session: samples [CaptureForegroundService.latestFrame] every 500ms,
 * turns each sampled frame into a [TelemetrySample] via [FrameAnalyzer]/[CardTemplateStore], and
 * keeps only that small numeric list in memory — frames themselves are never retained or written
 * to disk. On stop, the telemetry is handed to [MatchRepository] for analysis and the buffer is
 * cleared either way (saved or not).
 */
class MatchSessionController(
    private val calibrationStore: CalibrationStore,
    private val templateStore: CardTemplateStore,
    private val repository: MatchRepository,
    private val settings: SettingsDataStore,
    private val scope: CoroutineScope,
) {
    private val sampleIntervalMs = 500L
    private var samplingJob: Job? = null
    private val samples = mutableListOf<TelemetrySample>()
    private var startTimeMs = 0L
    private var currentDeck: List<String> = emptyList()

    private val _isRunning = MutableStateFlow(false)
    val isRunning = _isRunning.asStateFlow()

    private val _sampleCount = MutableStateFlow(0)
    val sampleCount = _sampleCount.asStateFlow()

    fun start() {
        if (_isRunning.value) return
        samples.clear()
        _sampleCount.value = 0
        startTimeMs = System.currentTimeMillis()
        _isRunning.value = true

        samplingJob = scope.launch {
            val profile = calibrationStore.profile.first()
            currentDeck = settings.currentDeck.first()
            while (isActive && _isRunning.value) {
                CaptureForegroundService.latestFrame.value?.let { frame ->
                    samples += buildSample(frame, profile, currentDeck)
                    _sampleCount.value = samples.size
                }
                delay(sampleIntervalMs)
            }
        }
    }

    private fun buildSample(frame: Bitmap, profile: CalibrationProfile, deck: List<String>): TelemetrySample {
        val handCards = profile.handSlotRects.map { rect ->
            val crop = FrameAnalyzer.cropHandSlot(frame, rect)
            templateStore.match(crop, deck)
        }
        return TelemetrySample(
            timestampMs = System.currentTimeMillis() - startTimeMs,
            myElixir = FrameAnalyzer.readElixir(frame, profile.myElixirBarRect, profile),
            myTowerHpFractions = profile.myTowerRects.map {
                FrameAnalyzer.readTowerHpFraction(frame, it, profile.myTowerHealthyColor, profile.towerBackgroundColor)
            },
            oppTowerHpFractions = profile.oppTowerRects.map {
                FrameAnalyzer.readTowerHpFraction(frame, it, profile.oppTowerHealthyColor, profile.towerBackgroundColor)
            },
            handCards = handCards,
        )
    }

    /** Stops sampling, analyzes whatever was collected, saves it, and always clears the in-memory buffer. */
    suspend fun stopAndSave(opponentName: String? = null): Long? {
        _isRunning.value = false
        samplingJob?.cancelAndJoin()
        samplingJob = null
        val result = if (samples.size >= 2) repository.saveLiveCaptureResult(samples.toList(), opponentName, currentDeck) else null
        samples.clear()
        return result
    }

    /** Aborts the session without saving anything (e.g. the user backed out of the capture permission dialog). */
    fun cancel() {
        _isRunning.value = false
        samplingJob?.cancel()
        samplingJob = null
        samples.clear()
        _sampleCount.value = 0
    }
}
