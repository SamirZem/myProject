package com.samirzem.clashanalyzer.analyzer

import com.samirzem.clashanalyzer.analyzer.model.AnalysisResult
import com.samirzem.clashanalyzer.analyzer.model.BattleRecord
import com.samirzem.clashanalyzer.analyzer.model.CardTag
import com.samirzem.clashanalyzer.analyzer.model.Insight
import com.samirzem.clashanalyzer.analyzer.model.PlayerBattleInfo
import com.samirzem.clashanalyzer.analyzer.model.Severity
import kotlin.math.roundToInt

/**
 * Deck/result-level analysis built purely from the official post-match API data.
 *
 * This cannot see individual plays, timing or troop placement (the API doesn't expose
 * that), so it reasons about deck composition, card-level parity, tower damage and the
 * final outcome — the "match report" level, not a move-by-move coach.
 */
object BattleAnalyzer {

    fun analyze(battle: BattleRecord): AnalysisResult {
        val mistakes = mutableListOf<Insight>()
        val goodMoves = mutableListOf<Insight>()
        val tips = mutableListOf<Insight>()

        val me = battle.me
        val opp = battle.opponent
        val won = me.crowns > opp.crowns
        val drew = me.crowns == opp.crowns

        analyzeOutcome(me, opp, won, drew, mistakes, goodMoves)
        analyzeDeckComposition(me, opp, won, mistakes, goodMoves, tips)
        analyzeCardLevels(me, opp, won, goodMoves, tips)

        val score = computeScore(me, opp, battle.trophyChange, mistakes, goodMoves)
        val summary = buildSummary(won, drew, me, opp, score)

        return AnalysisResult(
            score = score,
            summary = summary,
            mistakes = mistakes,
            goodMoves = goodMoves,
            tips = tips,
            fromLiveCapture = false,
        )
    }

    private fun analyzeOutcome(
        me: PlayerBattleInfo,
        opp: PlayerBattleInfo,
        won: Boolean,
        drew: Boolean,
        mistakes: MutableList<Insight>,
        goodMoves: MutableList<Insight>,
    ) {
        if (me.princessTowerHpFractions.size == 2 && me.crowns >= 2) {
            goodMoves += Insight(
                title = "Défense parfaite côté princesses",
                detail = "Aucune de tes tours princesses n'a été détruite alors que tu as fait tomber ${me.crowns} tour(s) adverse(s).",
                severity = Severity.GOOD,
            )
        }
        if (me.princessTowerHpFractions.size < 2 && !won) {
            mistakes += Insight(
                title = "Perte de tour(s) princesse",
                detail = "Tu as perdu ${2 - me.princessTowerHpFractions.size} tour(s) princesse sur cette défaite : la défense a craqué au moins une fois.",
                severity = Severity.MAJOR,
            )
        }
        if (me.kingTowerHpFraction < 1.0 && me.princessTowerHpFractions.size == 2 && won) {
            mistakes += Insight(
                title = "Tour du roi activée inutilement",
                detail = "Ta tour du roi a pris des dégâts alors que tes deux tours princesses tenaient encore : signe d'une poussée mal gérée malgré la victoire.",
                severity = Severity.MINOR,
            )
        }
        if (opp.kingTowerHpFraction < 0.4 && me.crowns < 3) {
            goodMoves += Insight(
                title = "Pression forte sur la tour du roi adverse",
                detail = "Tu as amené la tour du roi ennemie à ${(opp.kingTowerHpFraction * 100).roundToInt()}% de vie : encore un peu de pression et le 3-0 était possible.",
                severity = Severity.GOOD,
            )
        }
        if (drew) {
            mistakes += Insight(
                title = "Match nul",
                detail = "Ni toi ni l'adversaire n'avez su transformer l'avantage en victoire — revoir la gestion de fin de partie (double élixir / prolongation).",
                severity = Severity.MINOR,
            )
        }
    }

    private fun analyzeDeckComposition(
        me: PlayerBattleInfo,
        opp: PlayerBattleInfo,
        won: Boolean,
        mistakes: MutableList<Insight>,
        goodMoves: MutableList<Insight>,
        tips: MutableList<Insight>,
    ) {
        val myCards = me.deck.map { CardDatabase[it.name] }
        val oppCards = opp.deck.map { CardDatabase[it.name] }

        val myAvgElixir = myCards.map { it.elixirCost }.average()
        val oppAvgElixir = oppCards.map { it.elixirCost }.average()

        val hasWinCondition = myCards.any { CardTag.WIN_CONDITION in it.tags }
        if (!hasWinCondition) {
            tips += Insight(
                title = "Pas de condition de victoire claire",
                detail = "Aucune carte de ton deck n'est taguée comme condition de victoire dédiée. Vérifie que ton deck a un plan clair pour faire des dégâts de tour.",
                severity = Severity.INFO,
            )
        }

        val hasSmallSpell = myCards.any { CardTag.SMALL_SPELL in it.tags }
        val oppHasSwarm = oppCards.any { CardTag.SWARM in it.tags }
        if (!hasSmallSpell && oppHasSwarm && !won) {
            mistakes += Insight(
                title = "Pas de petit sort contre le swarm adverse",
                detail = "L'adversaire avait des cartes de swarm et ton deck n'a pas de petit sort (Zap, Log, Snowball...) pour les gérer proprement.",
                severity = Severity.MAJOR,
            )
        } else if (hasSmallSpell && oppHasSwarm && won) {
            goodMoves += Insight(
                title = "Bonne couverture anti-swarm",
                detail = "Ton deck avait un petit sort adapté face au swarm adverse, un atout qui a probablement pesé dans la victoire.",
                severity = Severity.GOOD,
            )
        }

        val hasAntiAir = myCards.any { CardTag.ANTI_AIR in it.tags }
        val oppHasAir = oppCards.any { CardTag.AIR in it.tags }
        if (!hasAntiAir && oppHasAir && !won) {
            mistakes += Insight(
                title = "Pas de défense anti-air",
                detail = "L'adversaire jouait des cartes aériennes et ton deck n'a aucune carte capable de les cibler.",
                severity = Severity.MAJOR,
            )
        }

        val hasBuildingDefense = myCards.any { CardTag.BUILDING_DEFENSE in it.tags }
        val oppHasTank = oppCards.any { CardTag.TANK in it.tags }
        if (!hasBuildingDefense && oppHasTank && !won) {
            tips += Insight(
                title = "Envisage un bâtiment défensif",
                detail = "Face à un deck avec un gros tank, un bâtiment de défense (Cannon, Tesla, Inferno Tower...) aide à absorber l'aggro sans perdre d'élixir en trade négatif.",
                severity = Severity.INFO,
            )
        }

        val heavyCards = myCards.count { it.elixirCost >= 5 }
        if (heavyCards >= 4 && myAvgElixir - oppAvgElixir > 0.8) {
            mistakes += Insight(
                title = "Deck lourd face à un cycle plus rapide",
                detail = "Ton coût d'élixir moyen (${"%.1f".format(myAvgElixir)}) est nettement plus élevé que celui de l'adversaire (${"%.1f".format(oppAvgElixir)}) : il peut cycler ses réponses plus vite que toi.",
                severity = Severity.MINOR,
            )
        }

        if (won && myAvgElixir <= 3.5) {
            goodMoves += Insight(
                title = "Deck cycle efficace",
                detail = "Avec un coût moyen de ${"%.1f".format(myAvgElixir)} élixir, ton deck permet de répondre vite aux poussées — bien exploité sur cette victoire.",
                severity = Severity.GOOD,
            )
        }
    }

    private fun analyzeCardLevels(
        me: PlayerBattleInfo,
        opp: PlayerBattleInfo,
        won: Boolean,
        goodMoves: MutableList<Insight>,
        tips: MutableList<Insight>,
    ) {
        val myAvgLevel = me.deck.map { it.level }.average()
        val oppAvgLevel = opp.deck.map { it.level }.average()
        val levelGap = oppAvgLevel - myAvgLevel

        if (levelGap >= 1.5 && won) {
            goodMoves += Insight(
                title = "Victoire malgré un désavantage de niveau",
                detail = "L'adversaire avait des cartes en moyenne ${"%.1f".format(levelGap)} niveau(x) au-dessus des tiennes et tu as quand même gagné : bonne exécution.",
                severity = Severity.GOOD,
            )
        } else if (levelGap <= -1.5 && !won) {
            tips += Insight(
                title = "Désavantage de niveau probable",
                detail = "Tes cartes étaient en moyenne ${"%.1f".format(-levelGap)} niveau(x) au-dessus de celles de l'adversaire mais tu as quand même perdu : la défaite est plus probablement due aux décisions qu'au niveau des cartes.",
                severity = Severity.INFO,
            )
        }
    }

    private fun computeScore(
        me: PlayerBattleInfo,
        opp: PlayerBattleInfo,
        trophyChange: Int?,
        mistakes: List<Insight>,
        goodMoves: List<Insight>,
    ): Int {
        var score = 50.0

        // Outcome (crowns differential) dominates the score.
        score += (me.crowns - opp.crowns) * 14

        // Reward keeping your own towers alive, penalize letting opponent's survive when you won.
        val myTowerHealthFraction = (me.kingTowerHpFraction + me.princessTowerHpFractions.sum()) / 3.0
        val oppTowerHealthFraction = (opp.kingTowerHpFraction + opp.princessTowerHpFractions.sum()) / 3.0
        score += (myTowerHealthFraction - oppTowerHealthFraction) * 15

        // Trophy change is a good independent signal of match quality when available.
        trophyChange?.let { score += it * 0.4 }

        score -= mistakes.count { it.severity == Severity.MAJOR } * 8
        score -= mistakes.count { it.severity == Severity.MINOR } * 3
        score += goodMoves.count { it.severity == Severity.GOOD } * 3

        return score.roundToInt().coerceIn(0, 100)
    }

    private fun buildSummary(won: Boolean, drew: Boolean, me: PlayerBattleInfo, opp: PlayerBattleInfo, score: Int): String {
        val result = when {
            drew -> "Match nul (${me.crowns}-${opp.crowns})"
            won -> "Victoire ${me.crowns}-${opp.crowns}"
            else -> "Défaite ${me.crowns}-${opp.crowns}"
        }
        return "$result — score de performance : $score/100"
    }
}
