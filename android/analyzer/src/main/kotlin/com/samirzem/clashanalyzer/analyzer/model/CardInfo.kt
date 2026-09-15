package com.samirzem.clashanalyzer.analyzer.model

enum class CardType {
    TROOP,
    SPELL,
    BUILDING,
}

/**
 * A card's role tags, used by the heuristics in BattleAnalyzer to reason about
 * deck composition (e.g. "does this deck have an answer to swarms?").
 */
object CardTag {
    const val WIN_CONDITION = "win_condition"
    const val SMALL_SPELL = "small_spell"
    const val BIG_SPELL = "big_spell"
    const val SWARM = "swarm"
    const val SPLASH = "splash"
    const val TANK = "tank"
    const val MINI_TANK = "mini_tank"
    const val BUILDING_DEFENSE = "building_defense"
    const val AIR = "air"
    const val ANTI_AIR = "anti_air"
    const val CYCLE = "cycle"
    const val RANGED_SUPPORT = "ranged_support"
}

data class CardInfo(
    /** Canonical name, matching the official Clash Royale API's "name" field (always English, regardless of the player's game language). Used for storage, deck matching, and template lookup — never shown to the user directly. */
    val name: String,
    /** Display name as it appears in the French Clash Royale client, shown in pickers/UI. Best-effort translation; falls back to [name] when unset. */
    val frenchName: String,
    val elixirCost: Int,
    val type: CardType,
    val tags: Set<String> = emptySet(),
)

/** Fallback used for any card the API returns that isn't in [CardDatabase] (e.g. a brand new release). */
val UNKNOWN_CARD = CardInfo(name = "unknown", frenchName = "Carte inconnue", elixirCost = 4, type = CardType.TROOP, tags = emptySet())
