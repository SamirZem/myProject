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
    /**
     * 0..10, tenths allowed since the bar fills continuously between pips. There is no
     * equivalent for the opponent: Clash Royale's normal 1v1 UI never displays the
     * opponent's elixir count, so it isn't something screen-reading can observe.
     */
    val myElixir: Double,
    /** King tower first, then remaining princess towers. Empty entry once destroyed. */
    val myTowerHpFractions: List<Double>,
    val oppTowerHpFractions: List<Double>,
    /** Card name recognized in each of the 4 visible hand slots, null if unrecognized/empty. */
    val handCards: List<String?>,
    /**
     * 0..1 fraction of sampled points in the opponent's board zone that changed noticeably since
     * the previous frame (frame-differencing motion signal). This doesn't identify *what* the
     * opponent deployed, only *that* something moved/appeared on their side of the board — enough
     * to flag "opponent is pushing" without pretending to recognize their cards. Defaults to 0.0
     * so existing call sites that don't care about this signal are unaffected.
     */
    val oppBoardActivity: Double = 0.0,
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

    /**
     * Detected via [oppBoardActivity] spiking above its rolling baseline: the opponent deployed
     * something (possibly several things at once). `intensity` is the peak activity fraction
     * during the spike, useful for ranking "big push" vs. a single small troop.
     */
    data class OpponentPush(
        override val timestampMs: Long,
        val intensity: Double,
    ) : GameEvent()
}

enum class Side { ME, OPPONENT }
