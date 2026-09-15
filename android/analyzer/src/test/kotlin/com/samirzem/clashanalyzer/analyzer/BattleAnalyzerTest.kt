package com.samirzem.clashanalyzer.analyzer

import com.samirzem.clashanalyzer.analyzer.model.BattleRecord
import com.samirzem.clashanalyzer.analyzer.model.CardUsed
import com.samirzem.clashanalyzer.analyzer.model.PlayerBattleInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BattleAnalyzerTest {

    private fun deckOf(vararg names: String, level: Int = 11) = names.map { CardUsed(it, level) }

    @Test
    fun `dominant win with towers untouched scores high and reports good defense`() {
        val battle = BattleRecord(
            battleTime = "20260101T120000.000Z",
            gameMode = "Ladder",
            isLadder = true,
            trophyChange = 30,
            me = PlayerBattleInfo(
                tag = "#ME", name = "Me", startingTrophies = 5000, crowns = 3,
                kingTowerHpFraction = 1.0, princessTowerHpFractions = listOf(1.0, 1.0),
                deck = deckOf("Hog Rider", "The Log", "Ice Golem", "Musketeer", "Cannon", "Fireball", "Skeletons", "Ice Spirit"),
            ),
            opponent = PlayerBattleInfo(
                tag = "#OPP", name = "Opp", startingTrophies = 5010, crowns = 0,
                kingTowerHpFraction = 0.5, princessTowerHpFractions = emptyList(),
                deck = deckOf("Golem", "Night Witch", "Baby Dragon", "Lightning", "Tornado", "Mega Minion", "Elixir Collector", "Barbarian Barrel"),
            ),
        )

        val result = BattleAnalyzer.analyze(battle)

        assertTrue(result.score >= 80, "expected a high score for a dominant 3-0 win, got ${result.score}")
        assertTrue(result.goodMoves.any { it.title.contains("Défense parfaite") })
        assertEquals(false, result.fromLiveCapture)
    }

    @Test
    fun `loss without an answer to swarm flags the missing small spell`() {
        val battle = BattleRecord(
            battleTime = "20260101T120000.000Z",
            gameMode = "Ladder",
            isLadder = true,
            trophyChange = -28,
            me = PlayerBattleInfo(
                tag = "#ME", name = "Me", startingTrophies = 5000, crowns = 0,
                kingTowerHpFraction = 0.8, princessTowerHpFractions = listOf(0.6),
                deck = deckOf("Golem", "Night Witch", "Lightning", "Baby Dragon", "Elixir Collector", "Mega Minion", "P.E.K.K.A", "Witch"),
            ),
            opponent = PlayerBattleInfo(
                tag = "#OPP", name = "Opp", startingTrophies = 5015, crowns = 3,
                kingTowerHpFraction = 1.0, princessTowerHpFractions = listOf(1.0, 1.0),
                deck = deckOf("Skeleton Army", "Goblin Gang", "Minion Horde", "Hog Rider", "Fireball", "Ice Spirit", "Cannon", "The Log"),
            ),
        )

        val result = BattleAnalyzer.analyze(battle)

        assertTrue(result.mistakes.any { it.title.contains("swarm") })
        assertTrue(result.score < 50, "expected a below-average score for a clean 0-3 loss, got ${result.score}")
    }

    @Test
    fun `winning despite a card level disadvantage is recognized as a good move`() {
        val battle = BattleRecord(
            battleTime = "20260101T120000.000Z",
            gameMode = "Ladder",
            isLadder = true,
            trophyChange = 32,
            me = PlayerBattleInfo(
                tag = "#ME", name = "Me", startingTrophies = 5000, crowns = 2,
                kingTowerHpFraction = 1.0, princessTowerHpFractions = listOf(1.0, 0.7),
                deck = deckOf("Hog Rider", "The Log", "Ice Golem", "Musketeer", "Cannon", "Fireball", "Skeletons", "Ice Spirit", level = 9),
            ),
            opponent = PlayerBattleInfo(
                tag = "#OPP", name = "Opp", startingTrophies = 5010, crowns = 1,
                kingTowerHpFraction = 0.9, princessTowerHpFractions = listOf(0.3),
                deck = deckOf("Giant", "Wizard", "Minions", "Zap", "Arrows", "Knight", "Mini P.E.K.K.A", "Archers", level = 12),
            ),
        )

        val result = BattleAnalyzer.analyze(battle)

        assertTrue(result.goodMoves.any { it.title.contains("désavantage de niveau") })
    }
}
