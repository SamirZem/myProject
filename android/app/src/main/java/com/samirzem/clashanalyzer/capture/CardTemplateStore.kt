package com.samirzem.clashanalyzer.capture

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import java.io.File
import java.io.FileOutputStream

/**
 * Recognizes which of the player's own 8 deck cards is showing in a hand slot, by comparing
 * against small reference crops captured from the player's own screen during a one-time
 * enrollment step (see CalibrationScreen). No Supercell artwork ships in this app or its
 * source — every template is derived at runtime from the user's own device and never leaves it.
 */
class CardTemplateStore(context: Context) {

    private val dir = File(context.filesDir, "card_templates").apply { mkdirs() }
    private val gridSize = 12

    /** Downsamples [bitmap] to a small fixed grid so comparisons are cheap and roughly rotation/scale tolerant. */
    private fun fingerprint(bitmap: Bitmap): IntArray {
        val scaled = Bitmap.createScaledBitmap(bitmap, gridSize, gridSize, true)
        val out = IntArray(gridSize * gridSize * 3)
        var i = 0
        for (y in 0 until gridSize) {
            for (x in 0 until gridSize) {
                val c = scaled.getPixel(x, y)
                out[i++] = Color.red(c)
                out[i++] = Color.green(c)
                out[i++] = Color.blue(c)
            }
        }
        return out
    }

    fun saveTemplate(cardName: String, crop: Bitmap) {
        val fp = fingerprint(crop)
        val file = File(dir, "${sanitizeName(cardName)}.fp")
        // Each fingerprint value is a 0..255 RGB channel reading, so one byte per value is
        // lossless; the array length is always gridSize*gridSize*3, so no header is needed.
        val bytes = ByteArray(fp.size) { fp[it].toByte() }
        FileOutputStream(file).use { it.write(bytes) }
    }

    fun hasTemplate(cardName: String): Boolean = File(dir, "${sanitizeName(cardName)}.fp").exists()

    fun clearAll() {
        dir.listFiles()?.forEach { it.delete() }
    }

    /** Returns the best-matching card name among [candidateNames] for this crop, or null if nothing is close enough. */
    fun match(crop: Bitmap, candidateNames: List<String>): String? {
        val target = fingerprint(crop)
        var bestName: String? = null
        var bestDistance = Int.MAX_VALUE
        for (name in candidateNames) {
            val templateFp = loadFingerprint(name) ?: continue
            val distance = squaredDistance(target, templateFp)
            if (distance < bestDistance) {
                bestDistance = distance
                bestName = name
            }
        }
        // Empirical-ish threshold: a genuine match on a 12x12x3 grid (0..255 per channel) is
        // typically well under this; tune from real captures if recognition feels off.
        val maxPlausibleDistance = 12 * 12 * 3 * 40 * 40
        return if (bestDistance <= maxPlausibleDistance) bestName else null
    }

    private fun loadFingerprint(cardName: String): IntArray? {
        val file = File(dir, "${sanitizeName(cardName)}.fp")
        if (!file.exists()) return null
        val bytes = file.readBytes()
        if (bytes.isEmpty()) return null
        return IntArray(bytes.size) { bytes[it].toInt() and 0xFF }
    }

    private fun squaredDistance(a: IntArray, b: IntArray): Int {
        var sum = 0
        val n = min(a.size, b.size)
        for (i in 0 until n) {
            val d = a[i] - b[i]
            sum += d * d
        }
        return sum
    }

    private fun min(a: Int, b: Int) = if (a < b) a else b

    private fun sanitizeName(name: String): String = name.replace(Regex("[^A-Za-z0-9]"), "_")
}
