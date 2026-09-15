package com.samirzem.clashanalyzer.capture

import kotlinx.serialization.Serializable

/**
 * Where the 8 card slots sit on Clash Royale's deck-selection menu screen — a different screen
 * (and different layout) than the in-match capture [CalibrationProfile] covers. Used to import a
 * deck from a gallery screenshot instead of typing card names by hand, entirely on-device (no
 * backend/API key needed).
 *
 * Default values measured from a real 1008x2244 deck-menu screenshot; fractions are
 * resolution-independent, but expect to nudge them via the import screen's sliders if your
 * device's screenshot doesn't line up exactly.
 */
@Serializable
data class DeckScreenLayout(
    /** Reading order: row 1 left-to-right, then row 2 left-to-right. Always size 8. */
    val cardRects: List<NormalizedRect>,
) {
    companion object {
        fun default(): DeckScreenLayout = DeckScreenLayout(
            cardRects = listOf(
                NormalizedRect(0.015f, 0.226f, 0.253f, 0.407f),
                NormalizedRect(0.258f, 0.226f, 0.496f, 0.407f),
                NormalizedRect(0.501f, 0.226f, 0.739f, 0.407f),
                NormalizedRect(0.744f, 0.238f, 0.977f, 0.396f),
                NormalizedRect(0.015f, 0.422f, 0.253f, 0.565f),
                NormalizedRect(0.258f, 0.422f, 0.496f, 0.565f),
                NormalizedRect(0.501f, 0.422f, 0.739f, 0.565f),
                NormalizedRect(0.744f, 0.422f, 0.977f, 0.565f),
            ),
        )
    }
}
