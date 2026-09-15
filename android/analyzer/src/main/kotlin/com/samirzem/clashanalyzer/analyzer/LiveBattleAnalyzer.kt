package com.samirzem.clashanalyzer.analyzer

import com.samirzem.clashanalyzer.analyzer.model.AnalysisResult
import com.samirzem.clashanalyzer.analyzer.model.CardTag
import com.samirzem.clashanalyzer.analyzer.model.GameEvent
import com.samirzem.clashanalyzer.analyzer.model.Insight
import com.samirzem.clashanalyzer.analyzer.model.Severity
import com.samirzem.clashanalyzer.analyzer.model.Side
import com.samirzem.clashanalyzer.analyzer.model.TelemetrySample
import kotlin.math.roundToInt

/**
 * Tactical, timing-aware analysis built from on-device screen-capture telemetry.
 *
 * Unlike [BattleAnalyzer] (which only sees the final post-match result), this can reason
 * about *when* elixir was spent and *when* towers took damage — elixir trades, overcommits,
 * missed defensive windows, double-elixir management. All of it is derived from reading UI
 * pixels (elixir bar, tower HP bars, your own hand slots), so it's approximate, not exact
 * game-engine state; a permanent INFO insight says so.
 */
object LiveBattleAnalyzer {

    private const val PUSH_GROUPING_GAP_MS = 2_500L
    private const val PUSH_RESULT_WINDOW_MS = 6_000L
    private const val OVERCOMMIT_ELIXIR_THRESHOLD = 8.0
    private const val GOOD_PUSH_ELIXIR_THRESHOLD = 3.0
    private const val TOWER_DAMAGE_SIGNIFICANT = 0.10
    private const val MISSED_DEFENSE_ELIXIR_THRESHOLD = 7.0
    private const val MISSED_DEFENSE_LOOKBACK_MS = 3_000L
    private const val NO_RESPONSE_WINDOW_MS = 2_000L
    private const val ELIXIR_CAP_THRESHOLD = 9.3
    private const val ELIXIR_WASTE_MIN_DURATION_MS = 4_000L
    private const val SINGLE_ELIXIR_REGEN_PERIOD_MS = 2_800.0
    private const val DOUBLE_ELIXIR_REGEN_PERIOD_MS = 1_400.0
    private const val MIN_PLAYS_FOR_DIVERSITY_CHECK = 6

    fun analyze(samples: List<TelemetrySample>, doubleElixirStartMs: Long = 120_000L, myDeck: List<String> = emptyList()): AnalysisResult {
        val mistakes = mutableListOf<Insight>()
        val goodMoves = mutableListOf<Insight>()
        val tips = mutableListOf<Insight>()

        tips += Insight(
            title = "Analyse basée sur la capture d'écran",
            detail = "L'élixir, les PV des tours et tes cartes jouées sont lus automatiquement sur l'image ; les cartes et le deck de l'adversaire ne sont jamais visibles à l'écran, donc hors de portée de cette analyse.",
            severity = Severity.INFO,
        )

        // Deck-composition feedback only needs your own deck, not the match telemetry, so it
        // still runs even when too little was captured for the timing-based heuristics below.
        analyzeDeckComposition(myDeck, tips)

        if (samples.size < 2) {
            return AnalysisResult(
                score = 50,
                summary = "Pas assez de données capturées pour analyser le déroulé de cette partie.",
                mistakes = mistakes,
                goodMoves = goodMoves,
                tips = tips,
                fromLiveCapture = true,
            )
        }

        val events = GameEventExtractor.extract(samples)
        val myCardPlays = events.filterIsInstance<GameEvent.CardPlayed>().filter { it.side == Side.ME }
        val myTowerDamaged = events.filterIsInstance<GameEvent.TowerDamaged>().filter { it.side == Side.ME }
        val oppTowerDamaged = events.filterIsInstance<GameEvent.TowerDamaged>().filter { it.side == Side.OPPONENT }
        val myTowerDestroyed = events.filterIsInstance<GameEvent.TowerDestroyed>().filter { it.side == Side.ME }
        val oppTowerDestroyed = events.filterIsInstance<GameEvent.TowerDestroyed>().filter { it.side == Side.OPPONENT }

        analyzePushes(myCardPlays, myTowerDamaged, oppTowerDamaged, mistakes, goodMoves)
        analyzeMissedDefense(myTowerDamaged, myCardPlays, samples, mistakes)
        analyzeElixirManagement(samples, doubleElixirStartMs, mistakes, goodMoves, tips)
        analyzeCardDiversity(myCardPlays, myDeck, mistakes, goodMoves, tips)
        analyzeDamageBalance(myTowerDamaged, oppTowerDamaged, goodMoves, mistakes, tips)

        val crownsForMe = oppTowerDestroyed.size
        val crownsForOpp = myTowerDestroyed.size
        val score = computeScore(samples.last(), crownsForMe, crownsForOpp, mistakes, goodMoves)
        val summary = "Résultat estimé $crownsForMe-$crownsForOpp — score de performance : $score/100"

        return AnalysisResult(
            score = score,
            summary = summary,
            mistakes = mistakes,
            goodMoves = goodMoves,
            tips = tips,
            fromLiveCapture = true,
        )
    }

    /** Deck-build feedback based only on your own 8 cards — the opponent's deck is never visible on screen in live capture, so this can't be matchup-specific the way [BattleAnalyzer]'s post-match version is. */
    private fun analyzeDeckComposition(myDeck: List<String>, tips: MutableList<Insight>) {
        if (myDeck.size < 8) return
        val cards = myDeck.map { CardDatabase[it] }

        if (cards.none { CardTag.WIN_CONDITION in it.tags }) {
            tips += Insight(
                title = "Pas de condition de victoire claire",
                detail = "Aucune carte de ton deck n'est taguée comme condition de victoire dédiée — vérifie que ton deck a un plan clair pour faire des dégâts de tour.",
                severity = Severity.INFO,
            )
        }
        if (cards.none { CardTag.SMALL_SPELL in it.tags }) {
            tips += Insight(
                title = "Pas de petit sort dans le deck",
                detail = "Sans petit sort (Zap, Log, Snowball...), les cartes de swarm adverses peuvent être coûteuses à gérer proprement.",
                severity = Severity.INFO,
            )
        }
        if (cards.none { CardTag.ANTI_AIR in it.tags }) {
            tips += Insight(
                title = "Pas de défense anti-air",
                detail = "Aucune carte de ton deck ne cible l'air — un deck aérien adverse (Lava Hound, Balloon...) pourrait être difficile à arrêter.",
                severity = Severity.INFO,
            )
        }
        if (cards.none { CardTag.BUILDING_DEFENSE in it.tags }) {
            tips += Insight(
                title = "Pas de bâtiment défensif",
                detail = "Un bâtiment (Cannon, Tesla, Inferno Tower...) aide à absorber l'aggro sans perdre d'élixir en trade négatif face à un gros tank.",
                severity = Severity.INFO,
            )
        }
        val avgElixir = cards.map { it.elixirCost }.average()
        when {
            avgElixir >= 4.3 -> tips += Insight(
                title = "Deck plutôt lourd",
                detail = "Coût d'élixir moyen de ${"%.1f".format(avgElixir)} : attends-toi à un rythme de jeu plus lent, avec moins de marge pour répondre vite à une poussée rapide.",
                severity = Severity.INFO,
            )
            avgElixir <= 3.2 -> tips += Insight(
                title = "Deck cycle rapide",
                detail = "Coût d'élixir moyen de ${"%.1f".format(avgElixir)} : tu peux répondre vite, mais chaque carte fait individuellement moins de dégâts — la régularité des poussées compte plus que leur taille.",
                severity = Severity.INFO,
            )
        }
    }

    /** Flags over-reliance on a small subset of the deck, or rewards using most of it. */
    private fun analyzeCardDiversity(
        myCardPlays: List<GameEvent.CardPlayed>,
        myDeck: List<String>,
        mistakes: MutableList<Insight>,
        goodMoves: MutableList<Insight>,
        tips: MutableList<Insight>,
    ) {
        if (myCardPlays.size < MIN_PLAYS_FOR_DIVERSITY_CHECK) return
        val distinctCards = myCardPlays.map { it.cardName }.distinct()
        val deckSize = myDeck.size.takeIf { it >= 8 } ?: 8

        when {
            distinctCards.size <= 2 -> mistakes += Insight(
                title = "Rotation de deck très limitée",
                detail = "Seulement ${distinctCards.size} carte(s) différente(s) jouée(s) (${distinctCards.joinToString(", ")}) sur ${myCardPlays.size} coups : le reste du deck n'a presque pas servi.",
                severity = Severity.MINOR,
            )
            distinctCards.size >= (deckSize - 1) -> goodMoves += Insight(
                title = "Bonne rotation du deck",
                detail = "${distinctCards.size} cartes différentes jouées sur les ${deckSize} du deck : bonne utilisation de tout ton arsenal plutôt que de répéter les mêmes cartes.",
                severity = Severity.GOOD,
            )
            else -> tips += Insight(
                title = "Rotation du deck",
                detail = "${distinctCards.size} cartes différentes jouées sur les ${deckSize} du deck.",
                severity = Severity.INFO,
            )
        }
    }

    /** Always-present summary of total tower damage dealt vs taken, independent of any single flagged mistake. */
    private fun analyzeDamageBalance(
        myTowerDamaged: List<GameEvent.TowerDamaged>,
        oppTowerDamaged: List<GameEvent.TowerDamaged>,
        goodMoves: MutableList<Insight>,
        mistakes: MutableList<Insight>,
        tips: MutableList<Insight>,
    ) {
        val dealt = oppTowerDamaged.sumOf { it.hpFractionLost }
        val taken = myTowerDamaged.sumOf { it.hpFractionLost }
        if (dealt < 0.01 && taken < 0.01) return

        val dealtPct = (dealt * 100).roundToInt()
        val takenPct = (taken * 100).roundToInt()
        val detail = "Sur l'ensemble de la partie : environ $dealtPct% de PV de tour infligés à l'adversaire, contre $takenPct% subis sur tes propres tours."
        when {
            dealt >= taken * 1.5 && dealt > 0.05 -> goodMoves += Insight("Bilan de pression favorable", detail, Severity.GOOD)
            taken >= dealt * 1.5 && taken > 0.05 -> mistakes += Insight("Bilan de pression défavorable", detail, Severity.MINOR)
            else -> tips += Insight("Bilan de pression équilibré", detail, Severity.INFO)
        }
    }

    /** Groups my card plays into "pushes" (plays close together in time) and checks how each one resolved. */
    private fun analyzePushes(
        myCardPlays: List<GameEvent.CardPlayed>,
        myTowerDamaged: List<GameEvent.TowerDamaged>,
        oppTowerDamaged: List<GameEvent.TowerDamaged>,
        mistakes: MutableList<Insight>,
        goodMoves: MutableList<Insight>,
    ) {
        if (myCardPlays.isEmpty()) return
        val pushes = mutableListOf<MutableList<GameEvent.CardPlayed>>()
        for (play in myCardPlays.sortedBy { it.timestampMs }) {
            val current = pushes.lastOrNull()
            if (current != null && play.timestampMs - current.last().timestampMs <= PUSH_GROUPING_GAP_MS) {
                current += play
            } else {
                pushes += mutableListOf(play)
            }
        }

        for (push in pushes) {
            val pushStart = push.first().timestampMs
            val pushEnd = push.last().timestampMs
            val totalElixir = push.sumOf { it.elixirCost }
            val windowEnd = pushEnd + PUSH_RESULT_WINDOW_MS

            val damageTakenAfter = myTowerDamaged
                .filter { it.timestampMs in pushEnd..windowEnd }
                .sumOf { it.hpFractionLost }
            val damageDealtAfter = oppTowerDamaged
                .filter { it.timestampMs in pushEnd..windowEnd }
                .sumOf { it.hpFractionLost }

            val cardNames = push.joinToString(", ") { it.cardName }
            when {
                totalElixir >= OVERCOMMIT_ELIXIR_THRESHOLD && damageTakenAfter >= TOWER_DAMAGE_SIGNIFICANT -> {
                    mistakes += Insight(
                        title = "Sur-investissement puni",
                        detail = "Poussée à $totalElixir élixir ($cardNames) suivie d'une contre-attaque qui a touché ta tour peu après : trop investi d'un coup, pas assez gardé pour défendre.",
                        severity = Severity.MAJOR,
                        timestampMs = pushStart,
                    )
                }
                totalElixir >= GOOD_PUSH_ELIXIR_THRESHOLD && damageDealtAfter >= TOWER_DAMAGE_SIGNIFICANT && damageTakenAfter < TOWER_DAMAGE_SIGNIFICANT -> {
                    goodMoves += Insight(
                        title = "Bonne poussée",
                        detail = "$cardNames ont infligé des dégâts à la tour adverse sans contrepartie sur ta propre tour : bonne trade.",
                        severity = Severity.GOOD,
                        timestampMs = pushStart,
                    )
                }
            }
        }
    }

    /** Flags cases where my tower took damage while I had elixir available and hadn't just played a card. */
    private fun analyzeMissedDefense(
        myTowerDamaged: List<GameEvent.TowerDamaged>,
        myCardPlays: List<GameEvent.CardPlayed>,
        samples: List<TelemetrySample>,
        mistakes: MutableList<Insight>,
    ) {
        for (damage in myTowerDamaged) {
            if (damage.hpFractionLost < TOWER_DAMAGE_SIGNIFICANT) continue
            val recentPlay = myCardPlays.any { it.timestampMs in (damage.timestampMs - NO_RESPONSE_WINDOW_MS)..damage.timestampMs }
            if (recentPlay) continue

            val lookbackWindow = samples.filter { it.timestampMs in (damage.timestampMs - MISSED_DEFENSE_LOOKBACK_MS)..damage.timestampMs }
            if (lookbackWindow.isEmpty()) continue
            val avgElixir = lookbackWindow.map { it.myElixir }.average()
            if (avgElixir >= MISSED_DEFENSE_ELIXIR_THRESHOLD) {
                mistakes += Insight(
                    title = "Défense manquée alors que l'élixir était disponible",
                    detail = "Ta tour a perdu ${(damage.hpFractionLost * 100).roundToInt()}% de PV alors que tu avais en moyenne ${"%.1f".format(avgElixir)} élixir disponible juste avant, sans jouer de carte pour défendre.",
                    severity = Severity.MAJOR,
                    timestampMs = damage.timestampMs,
                )
            }
        }
    }

    /** Detects stretches where elixir sat near the cap (wasted regen), especially costly in double elixir. */
    private fun analyzeElixirManagement(
        samples: List<TelemetrySample>,
        doubleElixirStartMs: Long,
        mistakes: MutableList<Insight>,
        goodMoves: MutableList<Insight>,
        tips: MutableList<Insight>,
    ) {
        var streakStart: Long? = null
        var longestCappedStreakMs = 0L
        var cappedInDoubleElixir = false

        for (sample in samples) {
            if (sample.myElixir >= ELIXIR_CAP_THRESHOLD) {
                if (streakStart == null) streakStart = sample.timestampMs
                val streakDuration = sample.timestampMs - (streakStart ?: sample.timestampMs)
                if (streakDuration > longestCappedStreakMs) longestCappedStreakMs = streakDuration
                if (sample.timestampMs >= doubleElixirStartMs) cappedInDoubleElixir = true
            } else {
                streakStart = null
            }
        }

        if (longestCappedStreakMs >= ELIXIR_WASTE_MIN_DURATION_MS) {
            val phase = if (cappedInDoubleElixir) "en double élixir, où c'est particulièrement coûteux" else "en élixir simple"
            mistakes += Insight(
                title = "Élixir gaspillé au plafond",
                detail = "Ton élixir est resté proche du maximum pendant environ ${longestCappedStreakMs / 1000}s $phase : de la régénération perdue faute d'avoir joué une carte.",
                severity = Severity.MINOR,
            )
            tips += Insight(
                title = "Garde une carte prête à jouer",
                detail = "Essaie de ne jamais rester plus de 2-3 secondes à élixir plein, surtout après le passage en double élixir.",
                severity = Severity.INFO,
            )
        } else {
            val afterDoubleSamples = samples.filter { it.timestampMs >= doubleElixirStartMs }
            if (afterDoubleSamples.size >= 4) {
                goodMoves += Insight(
                    title = "Bonne gestion de l'élixir en double élixir",
                    detail = "Tu n'es jamais resté longtemps au plafond d'élixir après le passage en double élixir : bon rythme de jeu.",
                    severity = Severity.GOOD,
                )
            }
        }
    }

    private fun computeScore(
        lastSample: TelemetrySample,
        crownsForMe: Int,
        crownsForOpp: Int,
        mistakes: List<Insight>,
        goodMoves: List<Insight>,
    ): Int {
        var score = 50.0
        score += (crownsForMe - crownsForOpp) * 14

        val myTowerHealth = lastSample.myTowerHpFractions.average().takeIf { !it.isNaN() } ?: 1.0
        val oppTowerHealth = lastSample.oppTowerHpFractions.average().takeIf { !it.isNaN() } ?: 1.0
        score += (myTowerHealth - oppTowerHealth) * 15

        score -= mistakes.count { it.severity == Severity.MAJOR } * 8
        score -= mistakes.count { it.severity == Severity.MINOR } * 3
        score += goodMoves.count { it.severity == Severity.GOOD } * 3

        return score.roundToInt().coerceIn(0, 100)
    }

    /** Exposed for the capture pipeline: estimates elixir spent between two samples, accounting for natural regen. */
    fun estimateElixirSpent(previousElixir: Double, currentElixir: Double, dtMs: Long, isDoubleElixir: Boolean): Double {
        val regenPeriod = if (isDoubleElixir) DOUBLE_ELIXIR_REGEN_PERIOD_MS else SINGLE_ELIXIR_REGEN_PERIOD_MS
        val regenGain = dtMs / regenPeriod
        val projected = minOf(10.0, previousElixir + regenGain)
        return maxOf(0.0, projected - currentElixir)
    }
}
