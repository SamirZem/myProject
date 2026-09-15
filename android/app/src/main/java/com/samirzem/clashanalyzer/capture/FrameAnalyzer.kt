package com.samirzem.clashanalyzer.capture

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import kotlin.math.max
import kotlin.math.min

/**
 * Pure pixel-reading functions: given a captured frame and a [CalibrationProfile], extract the
 * game-state readouts the analyzer needs. No Android UI/service dependencies here on purpose —
 * this is the part worth unit-testing with synthetic bitmaps once real capture is wired up.
 */
object FrameAnalyzer {

    private const val COLOR_TOLERANCE = 60

    fun toPixelRect(bitmap: Bitmap, rect: NormalizedRect): Rect {
        val left = (rect.left * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
        val top = (rect.top * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
        val right = (rect.right * bitmap.width).toInt().coerceIn(left + 1, bitmap.width)
        val bottom = (rect.bottom * bitmap.height).toInt().coerceIn(top + 1, bitmap.height)
        return Rect(left, top, right, bottom)
    }

    /**
     * Reads a horizontal fill bar (elixir or tower HP): scans left-to-right along the rect's
     * vertical center and returns how far the "filled" color extends before giving way to the
     * "empty" background color, as a 0..1 fraction of the rect's width.
     */
    fun readFillFraction(bitmap: Bitmap, rect: NormalizedRect, filledColor: Int, emptyColor: Int): Double {
        val px = toPixelRect(bitmap, rect)
        val y = ((px.top + px.bottom) / 2).coerceIn(0, bitmap.height - 1)
        val width = px.width()
        if (width <= 0) return 0.0

        var lastFilledIndex = -1
        for (x in px.left until px.right) {
            val color = bitmap.getPixel(x.coerceIn(0, bitmap.width - 1), y)
            if (isCloser(color, filledColor, emptyColor)) {
                lastFilledIndex = x - px.left
            }
        }
        return ((lastFilledIndex + 1).toDouble() / width).coerceIn(0.0, 1.0)
    }

    fun readElixir(bitmap: Bitmap, rect: NormalizedRect, profile: CalibrationProfile): Double =
        readFillFraction(bitmap, rect, profile.elixirFilledColor, profile.elixirEmptyColor) * 10.0

    /** Ally and enemy tower bars render in different colors (blue vs red/pink), so the caller picks which. */
    fun readTowerHpFraction(bitmap: Bitmap, rect: NormalizedRect, healthyColor: Int, backgroundColor: Int): Double =
        readFillFraction(bitmap, rect, healthyColor, backgroundColor)

    fun cropHandSlot(bitmap: Bitmap, rect: NormalizedRect): Bitmap {
        val px = toPixelRect(bitmap, rect)
        return Bitmap.createBitmap(bitmap, px.left, px.top, px.width(), px.height())
    }

    /** True if [color] is nearer to [target] than to [other], within a plausibility tolerance. */
    private fun isCloser(color: Int, target: Int, other: Int): Boolean {
        val toTarget = colorDistance(color, target)
        val toOther = colorDistance(color, other)
        return toTarget < toOther && toTarget < COLOR_TOLERANCE * 3
    }

    private fun colorDistance(a: Int, b: Int): Int {
        val dr = Color.red(a) - Color.red(b)
        val dg = Color.green(a) - Color.green(b)
        val db = Color.blue(a) - Color.blue(b)
        return dr * dr + dg * dg + db * db
    }

    fun sampleColorAt(bitmap: Bitmap, xFraction: Float, yFraction: Float): Int {
        val x = (xFraction * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
        val y = (yFraction * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
        return bitmap.getPixel(x, y)
    }

    /** Clamps a rect so it always has at least a 1px extent, used when the user drags handles past each other. */
    fun sanitize(rect: NormalizedRect): NormalizedRect = NormalizedRect(
        left = min(rect.left, rect.right - 0.005f).coerceIn(0f, 0.995f),
        top = min(rect.top, rect.bottom - 0.005f).coerceIn(0f, 0.995f),
        right = max(rect.right, rect.left + 0.005f).coerceIn(0.005f, 1f),
        bottom = max(rect.bottom, rect.top + 0.005f).coerceIn(0.005f, 1f),
    )
}
