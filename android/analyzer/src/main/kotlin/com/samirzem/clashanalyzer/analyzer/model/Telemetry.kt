package com.samirzem.clashanalyzer.analyzer.model

/**
 * One sampled readout of the on-screen game state, produced by the on-device
 * computer-vision pipeline (elixir bar pip count, tower HP bar length, hand-slot
 * card recognition). Samples are taken every ~500ms while the capture service runs.
 *
 * This is intentionally CV-derived and approximate: elixir counts and HP fractions
 * are readings of UI pixels, not ground truth from the game engine.
 */
data class TelemetrySample(
    val timestampMs: Long,
    /** 0..10, tenths allowed since the bar fills continuously between pips. */
    val myElixir: Double,
    val oppElixir: Double,
    /** King tower first, then remaining princess towers. Empty entry once destroyed. */
    val myTowerHpFractions: List<Double>,
    val oppTowerHpFractions: List<Double>,
    /** Card name recognized in each of the 4 visible hand slots, null if unrecognized/empty. */
    val handCards: List<String?>,
)

sealed class GameEvent {
    abstract val timestampMs: Long

    data class CardPlayed(
        override val timestampMs: Long,
        val cardName: String,
        val elixirCost: Int,
        val side: Side,
    ) : GameEvent()

    data class TowerDamaged(
        override val timestampMs: Long,
        val side: Side,
        val towerIndex: Int,
        val hpFractionLost: Double,
    ) : GameEvent()

    data class TowerDestroyed(
        override val timestampMs: Long,
        val side: Side,
        val towerIndex: Int,
    ) : GameEvent()
}

enum class Side { ME, OPPONENT }
