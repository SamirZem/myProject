package com.samirzem.clashanalyzer.analyzer.model

/** A card as it was played in a specific battle (per the official post-match API). */
data class CardUsed(
    val name: String,
    val level: Int,
)

data class PlayerBattleInfo(
    val tag: String,
    val name: String,
    val startingTrophies: Int,
    val crowns: Int,
    /** 0..1, fraction of the king tower's max HP remaining at the end of the battle. */
    val kingTowerHpFraction: Double,
    /** 0..1 per surviving princess tower; a destroyed tower is simply absent from this list. */
    val princessTowerHpFractions: List<Double>,
    val deck: List<CardUsed>,
)

/**
 * A finished match as reported by the official Clash Royale API battle log
 * (`GET /players/{tag}/battlelog`). This carries deck/result information only —
 * the API does not expose move-by-move play, so it can't say *when* something
 * happened, only *what the outcome was*.
 */
data class BattleRecord(
    val battleTime: String,
    val gameMode: String,
    val isLadder: Boolean,
    val trophyChange: Int?,
    val me: PlayerBattleInfo,
    val opponent: PlayerBattleInfo,
)
