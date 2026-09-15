package com.samirzem.clashanalyzer.data.remote

import com.samirzem.clashanalyzer.analyzer.model.BattleRecord
import com.samirzem.clashanalyzer.analyzer.model.CardUsed
import com.samirzem.clashanalyzer.analyzer.model.PlayerBattleInfo
import com.samirzem.clashanalyzer.data.remote.dto.BattleLogEntryDto
import com.samirzem.clashanalyzer.data.remote.dto.BattlePlayerDto

/**
 * The official API reports tower HP as absolute remaining hit points, not a fraction of max —
 * and max HP depends on tower level, which isn't in the battle log. Rather than hardcode a
 * level->maxHP table that could silently go stale, we estimate each tower's max HP as the
 * highest HP value observed for that tower type across both players in this battle (a
 * full-health tower on either side is a good proxy for "max" in a matched game). This is an
 * approximation used only for relative scoring, not an exact reading.
 */
object BattleMapper {

    fun toBattleRecord(dto: BattleLogEntryDto, myTag: String): BattleRecord? {
        val me = dto.team.firstOrNull { normalizeTag(it.tag) == normalizeTag(myTag) } ?: dto.team.firstOrNull()
        val opp = dto.opponent.firstOrNull()
        if (me == null || opp == null) return null

        val maxKingHp = maxOf(me.kingTowerHitPoints ?: 0, opp.kingTowerHitPoints ?: 0).coerceAtLeast(1)
        val maxPrincessHp = (me.princessTowersHitPoints.orEmpty() + opp.princessTowersHitPoints.orEmpty())
            .maxOrNull()?.coerceAtLeast(1) ?: 1

        return BattleRecord(
            battleTime = dto.battleTime,
            gameMode = dto.gameMode?.name ?: "Unknown",
            isLadder = dto.gameMode?.name?.contains("Ladder", ignoreCase = true) ?: false,
            trophyChange = me.trophyChange,
            me = me.toPlayerBattleInfo(maxKingHp, maxPrincessHp),
            opponent = opp.toPlayerBattleInfo(maxKingHp, maxPrincessHp),
        )
    }

    private fun BattlePlayerDto.toPlayerBattleInfo(maxKingHp: Int, maxPrincessHp: Int): PlayerBattleInfo {
        val kingHp = kingTowerHitPoints ?: maxKingHp
        val princessHps = princessTowersHitPoints.orEmpty()
        return PlayerBattleInfo(
            tag = tag,
            name = name,
            startingTrophies = startingTrophies ?: 0,
            crowns = crowns,
            kingTowerHpFraction = (kingHp.toDouble() / maxKingHp).coerceIn(0.0, 1.0),
            princessTowerHpFractions = princessHps.filter { it > 0 }.map { (it.toDouble() / maxPrincessHp).coerceIn(0.0, 1.0) },
            deck = cards.map { CardUsed(it.name, it.level) },
        )
    }

    private fun normalizeTag(tag: String): String = tag.uppercase().removePrefix("#")
}
