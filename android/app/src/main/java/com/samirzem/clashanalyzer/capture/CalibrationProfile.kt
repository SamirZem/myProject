package com.samirzem.clashanalyzer.capture

import kotlinx.serialization.Serializable

/** A rectangle in screen-fraction coordinates (0..1), independent of actual screen resolution. */
@Serializable
data class NormalizedRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

/**
 * Where on screen to read each piece of game state, and what colors mean "filled" vs "empty"
 * for the elixir/HP bars.
 *
 * The default values were measured from a real 1008x2244 Clash Royale screenshot (not the Pixel
 * 8 Pro's own resolution, but fractions are resolution-independent), so they should already be
 * close. Two things they can't get from a single screenshot: exact HP-bar positions for king
 * towers, which only appear once a king tower has taken damage — this match's screenshot showed
 * neither king damaged, so those two rects are estimated from tower geometry instead of measured
 * directly; and the "empty" track colors for the elixir and tower bars, since every bar in that
 * screenshot happened to be full. Expect to still need the Calibration screen, just less than before.
 */
@Serializable
data class CalibrationProfile(
    val myElixirBarRect: NormalizedRect,
    /** Left-to-right, the 4 currently visible hand card slots. */
    val handSlotRects: List<NormalizedRect>,
    /** [king, left princess, right princess] health-bar rectangles, my side. */
    val myTowerRects: List<NormalizedRect>,
    val oppTowerRects: List<NormalizedRect>,
    /** Color sampled from a full elixir bar segment. */
    val elixirFilledColor: Int,
    /** Color sampled from an empty/background elixir bar segment. */
    val elixirEmptyColor: Int,
    /** Color sampled from one of MY (ally) tower bars at full health — these render blue. */
    val myTowerHealthyColor: Int,
    /** Color sampled from one of the OPPONENT's tower bars at full health — these render red/pink. */
    val oppTowerHealthyColor: Int,
    /** Color sampled from a tower bar's empty background (visible once damaged), shared by both sides. */
    val towerBackgroundColor: Int,
) {
    companion object {
        fun default(): CalibrationProfile = CalibrationProfile(
            myElixirBarRect = NormalizedRect(0.21f, 0.965f, 0.96f, 0.988f),
            handSlotRects = listOf(
                NormalizedRect(0.214f, 0.855f, 0.363f, 0.959f),
                NormalizedRect(0.368f, 0.855f, 0.517f, 0.959f),
                NormalizedRect(0.522f, 0.855f, 0.671f, 0.959f),
                NormalizedRect(0.676f, 0.855f, 0.824f, 0.959f),
            ),
            myTowerRects = listOf(
                NormalizedRect(0.40f, 0.705f, 0.60f, 0.720f), // king — estimated, not damaged in the reference screenshot
                NormalizedRect(0.164f, 0.608f, 0.286f, 0.623f), // left princess — measured
                NormalizedRect(0.754f, 0.608f, 0.876f, 0.623f), // right princess — measured
            ),
            oppTowerRects = listOf(
                NormalizedRect(0.40f, 0.170f, 0.60f, 0.185f), // king — estimated, not damaged in the reference screenshot
                NormalizedRect(0.162f, 0.186f, 0.286f, 0.199f), // left princess — measured
                NormalizedRect(0.752f, 0.186f, 0.876f, 0.199f), // right princess — measured
            ),
            elixirFilledColor = 0xFFE37FDE.toInt(),
            elixirEmptyColor = 0xFF0E367E.toInt(),
            myTowerHealthyColor = 0xFF84A4C0.toInt(),
            oppTowerHealthyColor = 0xFFD74B74.toInt(),
            towerBackgroundColor = 0xFF1E1E2E.toInt(),
        )
    }
}
