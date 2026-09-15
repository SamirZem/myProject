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
 * for the elixir/HP bars. The default values are a best-effort guess at Clash Royale's portrait
 * layout proportions — they were not measured against a real device (this project was built
 * without access to a Clash Royale screenshot or an Android emulator) and will very likely need
 * adjustment on the Calibration screen before the live analysis is usable.
 */
@Serializable
data class CalibrationProfile(
    val myElixirBarRect: NormalizedRect,
    val oppElixirBarRect: NormalizedRect,
    /** Left-to-right, the 4 currently visible hand card slots. */
    val handSlotRects: List<NormalizedRect>,
    /** [king, left princess, right princess] health-bar rectangles, my side. */
    val myTowerRects: List<NormalizedRect>,
    val oppTowerRects: List<NormalizedRect>,
    /** Color sampled from a full elixir bar segment. */
    val elixirFilledColor: Int,
    /** Color sampled from an empty/background elixir bar segment. */
    val elixirEmptyColor: Int,
    /** Color sampled from a healthy (full HP) tower bar. */
    val towerHealthyColor: Int,
    /** Color sampled from the tower bar's empty background (visible once damaged). */
    val towerBackgroundColor: Int,
) {
    companion object {
        fun default(): CalibrationProfile = CalibrationProfile(
            myElixirBarRect = NormalizedRect(0.06f, 0.955f, 0.62f, 0.975f),
            oppElixirBarRect = NormalizedRect(0.06f, 0.045f, 0.40f, 0.06f),
            handSlotRects = listOf(
                NormalizedRect(0.30f, 0.865f, 0.42f, 0.945f),
                NormalizedRect(0.44f, 0.865f, 0.56f, 0.945f),
                NormalizedRect(0.58f, 0.865f, 0.70f, 0.945f),
                NormalizedRect(0.72f, 0.865f, 0.84f, 0.945f),
            ),
            myTowerRects = listOf(
                NormalizedRect(0.44f, 0.78f, 0.56f, 0.79f), // king
                NormalizedRect(0.18f, 0.70f, 0.30f, 0.71f), // left princess
                NormalizedRect(0.70f, 0.70f, 0.82f, 0.71f), // right princess
            ),
            oppTowerRects = listOf(
                NormalizedRect(0.44f, 0.14f, 0.56f, 0.15f), // king
                NormalizedRect(0.18f, 0.22f, 0.30f, 0.23f), // left princess
                NormalizedRect(0.70f, 0.22f, 0.82f, 0.23f), // right princess
            ),
            elixirFilledColor = 0xFFB03DF0.toInt(),
            elixirEmptyColor = 0xFF2B2140.toInt(),
            towerHealthyColor = 0xFF4CD964.toInt(),
            towerBackgroundColor = 0xFF1A1A1A.toInt(),
        )
    }
}
