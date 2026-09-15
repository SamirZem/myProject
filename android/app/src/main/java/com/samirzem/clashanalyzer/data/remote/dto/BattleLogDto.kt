package com.samirzem.clashanalyzer.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Mirrors the shape of the official Clash Royale API (`GET /v1/players/{tag}/battlelog`),
 * as passed through unmodified by our backend proxy. Fields the app doesn't use are left out.
 */
@Serializable
data class BattleLogEntryDto(
    val type: String? = null,
    val battleTime: String,
    val gameMode: GameModeDto? = null,
    val team: List<BattlePlayerDto>,
    val opponent: List<BattlePlayerDto>,
)

@Serializable
data class GameModeDto(
    val id: Int? = null,
    val name: String? = null,
)

@Serializable
data class BattlePlayerDto(
    val tag: String,
    val name: String,
    val startingTrophies: Int? = null,
    val trophyChange: Int? = null,
    val crowns: Int = 0,
    val kingTowerHitPoints: Int? = null,
    val princessTowersHitPoints: List<Int>? = null,
    val cards: List<BattleCardDto> = emptyList(),
)

@Serializable
data class BattleCardDto(
    val name: String,
    val level: Int,
)

@Serializable
data class PlayerDto(
    val tag: String,
    val name: String,
    val trophies: Int? = null,
    @SerialName("expLevel") val level: Int? = null,
)
