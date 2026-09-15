package com.samirzem.clashanalyzer.analyzer

import com.samirzem.clashanalyzer.analyzer.model.GameEvent
import com.samirzem.clashanalyzer.analyzer.model.Side
import com.samirzem.clashanalyzer.analyzer.model.TelemetrySample

/**
 * Turns raw, noisy CV samples into discrete game events (a card was played, a tower took
 * damage, a tower was destroyed). Applies simple debouncing since individual samples come
 * from pixel reads and can misfire for a frame or two.
 */
object GameEventExtractor {

    /** A HP fraction drop smaller than this between samples is treated as CV noise, not real damage. */
    private const val HP_NOISE_THRESHOLD = 0.02

    /** A hand-slot reading must repeat this many consecutive samples before it's trusted. */
    private const val STABILITY_RUN = 2

    fun extract(samples: List<TelemetrySample>): List<GameEvent> {
        if (samples.isEmpty()) return emptyList()
        val events = mutableListOf<GameEvent>()
        events += extractCardPlays(samples)
        events += extractTowerEvents(samples, Side.ME) { it.myTowerHpFractions }
        events += extractTowerEvents(samples, Side.OPPONENT) { it.oppTowerHpFractions }
        return events.sortedBy { it.timestampMs }
    }

    private fun extractCardPlays(samples: List<TelemetrySample>): List<GameEvent.CardPlayed> {
        val slotCount = samples.maxOf { it.handCards.size }
        val events = mutableListOf<GameEvent.CardPlayed>()
        for (slot in 0 until slotCount) {
            val stabilized = stabilize(samples.map { it.handCards.getOrNull(slot) }, samples.map { it.timestampMs })
            var previous: String? = null
            for ((name, timestampMs) in stabilized) {
                if (previous != null && name != previous && CardDatabase.contains(previous)) {
                    val card = CardDatabase[previous]
                    events += GameEvent.CardPlayed(timestampMs, previous, card.elixirCost, Side.ME)
                }
                previous = name
            }
        }
        return events
    }

    /** Collapses a noisy value stream into (value, firstSeenAt) pairs, only accepting a value once it repeats [STABILITY_RUN] times. */
    private fun stabilize(values: List<String?>, timestamps: List<Long>): List<Pair<String?, Long>> {
        val out = mutableListOf<Pair<String?, Long>>()
        var runValue: String? = null
        var runStart = 0
        var runLength = 0
        var accepted: String? = null
        for (i in values.indices) {
            val v = values[i]
            if (v == runValue) {
                runLength++
            } else {
                runValue = v
                runStart = i
                runLength = 1
            }
            if (runLength == STABILITY_RUN && v != accepted) {
                accepted = v
                out += accepted to timestamps[runStart]
            }
        }
        return out
    }

    private fun extractTowerEvents(
        samples: List<TelemetrySample>,
        side: Side,
        selector: (TelemetrySample) -> List<Double>,
    ): List<GameEvent> {
        val events = mutableListOf<GameEvent>()
        val towerCount = samples.maxOf { selector(it).size }
        for (towerIndex in 0 until towerCount) {
            var previousHp: Double? = null
            for (sample in samples) {
                val hp = selector(sample).getOrNull(towerIndex) ?: continue
                val prev = previousHp
                if (prev != null) {
                    val drop = prev - hp
                    if (drop >= HP_NOISE_THRESHOLD) {
                        events += GameEvent.TowerDamaged(sample.timestampMs, side, towerIndex, drop)
                    }
                    if (prev > 0.0 && hp <= 0.0) {
                        events += GameEvent.TowerDestroyed(sample.timestampMs, side, towerIndex)
                    }
                }
                previousHp = hp
            }
        }
        return events
    }
}
