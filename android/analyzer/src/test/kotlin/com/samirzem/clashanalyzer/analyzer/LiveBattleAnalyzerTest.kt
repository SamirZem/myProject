package com.samirzem.clashanalyzer.analyzer

import com.samirzem.clashanalyzer.analyzer.model.GameEvent
import com.samirzem.clashanalyzer.analyzer.model.TelemetrySample
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LiveBattleAnalyzerTest {

    private fun timeline(stepMs: Long = 500L, untilMs: Long): List<Long> =
        (0..untilMs step stepMs).toList()

    @Test
    fun `event extractor detects a card leaving the hand as a play`() {
        val samples = timeline(untilMs = 6000).map { t ->
            TelemetrySample(
                timestampMs = t,
                myElixir = 5.0,
                oppElixir = 5.0,
                myTowerHpFractions = listOf(1.0),
                oppTowerHpFractions = listOf(1.0),
                handCards = listOf(if (t < 3000) "Hog Rider" else "Ice Golem", "Musketeer", "Cannon", "Fireball"),
            )
        }

        val events = GameEventExtractor.extract(samples)
        val plays = events.filterIsInstance<GameEvent.CardPlayed>()

        assertEquals(1, plays.size)
        assertEquals("Hog Rider", plays.first().cardName)
        assertEquals(4, plays.first().elixirCost)
        assertEquals(3000L, plays.first().timestampMs)
    }

    @Test
    fun `overcommitting a big push that gets punished is flagged as a mistake`() {
        val samples = timeline(untilMs = 10000).map { t ->
            TelemetrySample(
                timestampMs = t,
                myElixir = if (t < 6000) 10.0 else 1.0,
                oppElixir = 10.0,
                myTowerHpFractions = listOf(if (t < 9000) 1.0 else 0.85),
                oppTowerHpFractions = listOf(1.0),
                handCards = listOf(
                    if (t < 5000) "Giant" else "Skeletons",
                    if (t < 5500) "Wizard" else "Ice Spirit",
                    "Minions",
                    "Zap",
                ),
            )
        }

        val result = LiveBattleAnalyzer.analyze(samples, doubleElixirStartMs = 120_000L)

        assertTrue(result.mistakes.any { it.title.contains("Sur-investissement") })
    }

    @Test
    fun `a cheap push that damages the enemy tower without retaliation is a good move`() {
        val samples = timeline(untilMs = 8000).map { t ->
            TelemetrySample(
                timestampMs = t,
                myElixir = 5.0,
                oppElixir = 5.0,
                myTowerHpFractions = listOf(1.0),
                oppTowerHpFractions = listOf(if (t < 7000) 1.0 else 0.85),
                handCards = listOf(if (t < 3000) "Hog Rider" else "Ice Golem", "Musketeer", "Cannon", "Fireball"),
            )
        }

        val result = LiveBattleAnalyzer.analyze(samples, doubleElixirStartMs = 120_000L)

        assertTrue(result.goodMoves.any { it.title.contains("Bonne poussée") })
    }

    @Test
    fun `tower damage taken with elixir available and no card played is a missed defense`() {
        val samples = timeline(untilMs = 6000).map { t ->
            TelemetrySample(
                timestampMs = t,
                myElixir = 8.0,
                oppElixir = 5.0,
                myTowerHpFractions = listOf(if (t < 4000) 1.0 else 0.8),
                oppTowerHpFractions = listOf(1.0),
                handCards = listOf("Musketeer", "Cannon", "Fireball", "Zap"),
            )
        }

        val result = LiveBattleAnalyzer.analyze(samples, doubleElixirStartMs = 120_000L)

        assertTrue(result.mistakes.any { it.title.contains("Défense manquée") })
    }

    @Test
    fun `elixir sitting at the cap for a long stretch is flagged as waste`() {
        val samples = timeline(untilMs = 5000).map { t ->
            TelemetrySample(
                timestampMs = t,
                myElixir = 10.0,
                oppElixir = 5.0,
                myTowerHpFractions = listOf(1.0),
                oppTowerHpFractions = listOf(1.0),
                handCards = listOf("Musketeer", "Cannon", "Fireball", "Zap"),
            )
        }

        val result = LiveBattleAnalyzer.analyze(samples, doubleElixirStartMs = 0L)

        assertTrue(result.mistakes.any { it.title.contains("plafond") })
    }

    @Test
    fun `too few samples returns a neutral result instead of crashing`() {
        val result = LiveBattleAnalyzer.analyze(listOf(), doubleElixirStartMs = 120_000L)
        assertEquals(50, result.score)
        assertTrue(result.fromLiveCapture)
    }

    @Test
    fun `estimateElixirSpent accounts for natural regeneration`() {
        // 2.8s at single-elixir regen (1 per 2.8s) means ~1 elixir regenerated; going from 5 to 5 means 1 was spent.
        val spent = LiveBattleAnalyzer.estimateElixirSpent(previousElixir = 5.0, currentElixir = 5.0, dtMs = 2800L, isDoubleElixir = false)
        assertEquals(1.0, spent, 0.01)
    }
}
