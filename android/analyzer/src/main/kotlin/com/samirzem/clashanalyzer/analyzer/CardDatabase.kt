package com.samirzem.clashanalyzer.analyzer

import com.samirzem.clashanalyzer.analyzer.model.CardInfo
import com.samirzem.clashanalyzer.analyzer.model.CardTag
import com.samirzem.clashanalyzer.analyzer.model.CardType
import com.samirzem.clashanalyzer.analyzer.model.UNKNOWN_CARD

/**
 * Static metadata for the cards this analyzer knows how to reason about.
 *
 * This is not exhaustive (Supercell adds cards regularly) — [get] falls back to
 * [UNKNOWN_CARD] for anything missing so the analyzer never crashes on a new release,
 * it just reasons a bit less precisely about that one card.
 *
 * Names must match the "name" field returned by the official Clash Royale API
 * (battle log `team[].cards[].name` / `opponent[].cards[].name`).
 */
object CardDatabase {

    private val cards: Map<String, CardInfo> = listOf(
        // Win conditions
        CardInfo("Hog Rider", 4, CardType.TROOP, setOf(CardTag.WIN_CONDITION, CardTag.MINI_TANK)),
        CardInfo("Giant", 5, CardType.TROOP, setOf(CardTag.WIN_CONDITION, CardTag.TANK)),
        CardInfo("Royal Giant", 6, CardType.TROOP, setOf(CardTag.WIN_CONDITION, CardTag.TANK)),
        CardInfo("Golem", 8, CardType.TROOP, setOf(CardTag.WIN_CONDITION, CardTag.TANK)),
        CardInfo("Lava Hound", 7, CardType.TROOP, setOf(CardTag.WIN_CONDITION, CardTag.TANK, CardTag.AIR)),
        CardInfo("Balloon", 5, CardType.TROOP, setOf(CardTag.WIN_CONDITION, CardTag.AIR)),
        CardInfo("Graveyard", 5, CardType.SPELL, setOf(CardTag.WIN_CONDITION)),
        CardInfo("X-Bow", 6, CardType.BUILDING, setOf(CardTag.WIN_CONDITION)),
        CardInfo("Mortar", 4, CardType.BUILDING, setOf(CardTag.WIN_CONDITION)),
        CardInfo("Miner", 3, CardType.TROOP, setOf(CardTag.WIN_CONDITION, CardTag.CYCLE)),
        CardInfo("Goblin Barrel", 3, CardType.SPELL, setOf(CardTag.WIN_CONDITION, CardTag.CYCLE)),
        CardInfo("Wall Breakers", 2, CardType.TROOP, setOf(CardTag.WIN_CONDITION, CardTag.CYCLE)),
        CardInfo("Ram Rider", 5, CardType.TROOP, setOf(CardTag.WIN_CONDITION, CardTag.MINI_TANK)),
        CardInfo("Battle Ram", 4, CardType.TROOP, setOf(CardTag.WIN_CONDITION, CardTag.MINI_TANK)),
        CardInfo("Elixir Golem", 3, CardType.TROOP, setOf(CardTag.WIN_CONDITION, CardTag.TANK)),
        CardInfo("Goblin Giant", 6, CardType.TROOP, setOf(CardTag.WIN_CONDITION, CardTag.TANK)),
        CardInfo("Electro Giant", 8, CardType.TROOP, setOf(CardTag.WIN_CONDITION, CardTag.TANK)),
        CardInfo("Three Musketeers", 9, CardType.TROOP, setOf(CardTag.WIN_CONDITION, CardTag.RANGED_SUPPORT)),

        // Small spells (<= 3 elixir)
        CardInfo("Zap", 2, CardType.SPELL, setOf(CardTag.SMALL_SPELL, CardTag.CYCLE)),
        CardInfo("The Log", 2, CardType.SPELL, setOf(CardTag.SMALL_SPELL, CardTag.CYCLE)),
        CardInfo("Snowball", 2, CardType.SPELL, setOf(CardTag.SMALL_SPELL, CardTag.CYCLE)),
        CardInfo("Barbarian Barrel", 2, CardType.SPELL, setOf(CardTag.SMALL_SPELL, CardTag.CYCLE)),
        CardInfo("Giant Snowball", 2, CardType.SPELL, setOf(CardTag.SMALL_SPELL, CardTag.CYCLE)),
        CardInfo("Arrows", 3, CardType.SPELL, setOf(CardTag.SMALL_SPELL, CardTag.SPLASH)),
        CardInfo("Tornado", 3, CardType.SPELL, setOf(CardTag.SMALL_SPELL)),
        CardInfo("Royal Delivery", 3, CardType.SPELL, setOf(CardTag.SMALL_SPELL)),
        CardInfo("Earthquake", 3, CardType.SPELL, setOf(CardTag.SMALL_SPELL)),

        // Big spells (>= 4 elixir)
        CardInfo("Fireball", 4, CardType.SPELL, setOf(CardTag.BIG_SPELL, CardTag.SPLASH)),
        CardInfo("Poison", 4, CardType.SPELL, setOf(CardTag.BIG_SPELL, CardTag.SPLASH)),
        CardInfo("Lightning", 6, CardType.SPELL, setOf(CardTag.BIG_SPELL)),
        CardInfo("Rocket", 6, CardType.SPELL, setOf(CardTag.BIG_SPELL, CardTag.SPLASH)),
        CardInfo("Freeze", 4, CardType.SPELL, setOf(CardTag.BIG_SPELL)),
        CardInfo("Rage", 2, CardType.SPELL, emptySet()),
        CardInfo("Clone", 3, CardType.SPELL, emptySet()),
        CardInfo("Mirror", 1, CardType.SPELL, emptySet()),

        // Swarms
        CardInfo("Skeleton Army", 3, CardType.TROOP, setOf(CardTag.SWARM, CardTag.CYCLE)),
        CardInfo("Goblin Gang", 3, CardType.TROOP, setOf(CardTag.SWARM, CardTag.CYCLE)),
        CardInfo("Minion Horde", 5, CardType.TROOP, setOf(CardTag.SWARM, CardTag.AIR)),
        CardInfo("Minions", 3, CardType.TROOP, setOf(CardTag.SWARM, CardTag.AIR, CardTag.ANTI_AIR)),
        CardInfo("Guards", 3, CardType.TROOP, setOf(CardTag.SWARM)),
        CardInfo("Skeletons", 1, CardType.TROOP, setOf(CardTag.SWARM, CardTag.CYCLE)),
        CardInfo("Bats", 2, CardType.TROOP, setOf(CardTag.SWARM, CardTag.AIR, CardTag.ANTI_AIR, CardTag.CYCLE)),
        CardInfo("Spear Goblins", 2, CardType.TROOP, setOf(CardTag.SWARM, CardTag.CYCLE)),
        CardInfo("Goblins", 2, CardType.TROOP, setOf(CardTag.SWARM, CardTag.CYCLE)),
        CardInfo("Rascals", 5, CardType.TROOP, setOf(CardTag.SWARM)),
        CardInfo("Royal Recruits", 7, CardType.TROOP, setOf(CardTag.SWARM)),
        CardInfo("Dart Goblin", 3, CardType.TROOP, setOf(CardTag.CYCLE, CardTag.RANGED_SUPPORT)),
        CardInfo("Firecracker", 3, CardType.TROOP, setOf(CardTag.CYCLE, CardTag.RANGED_SUPPORT, CardTag.SPLASH)),

        // Splash / defense troops
        CardInfo("Wizard", 5, CardType.TROOP, setOf(CardTag.SPLASH, CardTag.ANTI_AIR)),
        CardInfo("Executioner", 5, CardType.TROOP, setOf(CardTag.SPLASH, CardTag.ANTI_AIR)),
        CardInfo("Baby Dragon", 4, CardType.TROOP, setOf(CardTag.SPLASH, CardTag.AIR, CardTag.ANTI_AIR)),
        CardInfo("Inferno Dragon", 4, CardType.TROOP, setOf(CardTag.AIR, CardTag.ANTI_AIR)),
        CardInfo("Bomber", 2, CardType.TROOP, setOf(CardTag.SPLASH, CardTag.CYCLE)),
        CardInfo("Electro Wizard", 4, CardType.TROOP, setOf(CardTag.ANTI_AIR, CardTag.RANGED_SUPPORT)),
        CardInfo("Magic Archer", 4, CardType.TROOP, setOf(CardTag.ANTI_AIR, CardTag.RANGED_SUPPORT)),
        CardInfo("Musketeer", 4, CardType.TROOP, setOf(CardTag.ANTI_AIR, CardTag.RANGED_SUPPORT)),
        CardInfo("Archers", 3, CardType.TROOP, setOf(CardTag.ANTI_AIR, CardTag.RANGED_SUPPORT, CardTag.CYCLE)),

        // Tanks / mini tanks
        CardInfo("P.E.K.K.A", 7, CardType.TROOP, setOf(CardTag.TANK)),
        CardInfo("Mega Knight", 7, CardType.TROOP, setOf(CardTag.TANK, CardTag.SPLASH)),
        CardInfo("Knight", 3, CardType.TROOP, setOf(CardTag.MINI_TANK, CardTag.CYCLE)),
        CardInfo("Valkyrie", 4, CardType.TROOP, setOf(CardTag.MINI_TANK, CardTag.SPLASH)),
        CardInfo("Mini P.E.K.K.A", 4, CardType.TROOP, setOf(CardTag.MINI_TANK)),
        CardInfo("Giant Skeleton", 6, CardType.TROOP, setOf(CardTag.TANK)),
        CardInfo("Dark Prince", 4, CardType.TROOP, setOf(CardTag.MINI_TANK, CardTag.SPLASH)),
        CardInfo("Prince", 5, CardType.TROOP, setOf(CardTag.MINI_TANK)),
        CardInfo("Bandit", 3, CardType.TROOP, setOf(CardTag.MINI_TANK, CardTag.CYCLE)),
        CardInfo("Royal Ghost", 3, CardType.TROOP, setOf(CardTag.MINI_TANK, CardTag.CYCLE)),
        CardInfo("Lumberjack", 4, CardType.TROOP, setOf(CardTag.MINI_TANK)),
        CardInfo("Ice Golem", 2, CardType.TROOP, setOf(CardTag.MINI_TANK, CardTag.CYCLE)),
        CardInfo("Fisherman", 3, CardType.TROOP, setOf(CardTag.MINI_TANK, CardTag.CYCLE)),

        // Buildings (defense)
        CardInfo("Cannon", 3, CardType.BUILDING, setOf(CardTag.BUILDING_DEFENSE, CardTag.CYCLE)),
        CardInfo("Tesla", 4, CardType.BUILDING, setOf(CardTag.BUILDING_DEFENSE, CardTag.ANTI_AIR)),
        CardInfo("Inferno Tower", 5, CardType.BUILDING, setOf(CardTag.BUILDING_DEFENSE, CardTag.ANTI_AIR)),
        CardInfo("Bomb Tower", 4, CardType.BUILDING, setOf(CardTag.BUILDING_DEFENSE, CardTag.SPLASH)),
        CardInfo("Tombstone", 3, CardType.BUILDING, setOf(CardTag.BUILDING_DEFENSE, CardTag.CYCLE)),
        CardInfo("Goblin Cage", 4, CardType.BUILDING, setOf(CardTag.BUILDING_DEFENSE)),
        CardInfo("Goblin Hut", 5, CardType.BUILDING, setOf(CardTag.BUILDING_DEFENSE)),
        CardInfo("Furnace", 4, CardType.BUILDING, setOf(CardTag.BUILDING_DEFENSE, CardTag.ANTI_AIR)),
        CardInfo("Barbarian Hut", 6, CardType.BUILDING, setOf(CardTag.BUILDING_DEFENSE)),
        CardInfo("Elixir Collector", 6, CardType.BUILDING, emptySet()),

        // Air troops
        CardInfo("Mega Minion", 3, CardType.TROOP, setOf(CardTag.AIR, CardTag.ANTI_AIR, CardTag.CYCLE)),
        CardInfo("Flying Machine", 4, CardType.TROOP, setOf(CardTag.AIR, CardTag.ANTI_AIR, CardTag.RANGED_SUPPORT)),
        CardInfo("Electro Dragon", 5, CardType.TROOP, setOf(CardTag.AIR, CardTag.ANTI_AIR, CardTag.SPLASH)),
        CardInfo("Phoenix", 4, CardType.TROOP, setOf(CardTag.AIR, CardTag.ANTI_AIR)),
        CardInfo("Skeleton Dragons", 4, CardType.TROOP, setOf(CardTag.AIR, CardTag.ANTI_AIR, CardTag.SWARM)),

        // Barbarians family
        CardInfo("Barbarians", 5, CardType.TROOP, setOf(CardTag.SWARM)),
        CardInfo("Elite Barbarians", 6, CardType.TROOP, setOf(CardTag.SWARM)),

        // Cycle support
        CardInfo("Ice Spirit", 1, CardType.TROOP, setOf(CardTag.CYCLE, CardTag.ANTI_AIR)),
        CardInfo("Fire Spirit", 1, CardType.TROOP, setOf(CardTag.CYCLE, CardTag.ANTI_AIR)),
        CardInfo("Electro Spirit", 1, CardType.TROOP, setOf(CardTag.CYCLE, CardTag.ANTI_AIR)),
        CardInfo("Heal Spirit", 1, CardType.TROOP, setOf(CardTag.CYCLE)),
        CardInfo("Zappies", 4, CardType.TROOP, setOf(CardTag.ANTI_AIR, CardTag.RANGED_SUPPORT)),
        CardInfo("Night Witch", 4, CardType.TROOP, setOf(CardTag.SWARM, CardTag.MINI_TANK)),
        CardInfo("Witch", 5, CardType.TROOP, setOf(CardTag.SWARM, CardTag.SPLASH, CardTag.ANTI_AIR)),
        CardInfo("Golden Knight", 4, CardType.TROOP, setOf(CardTag.MINI_TANK, CardTag.CYCLE)),
        CardInfo("Archer Queen", 5, CardType.TROOP, setOf(CardTag.ANTI_AIR, CardTag.RANGED_SUPPORT)),
        CardInfo("Skeleton King", 4, CardType.TROOP, setOf(CardTag.MINI_TANK)),
        CardInfo("Mighty Miner", 4, CardType.TROOP, setOf(CardTag.MINI_TANK)),
        CardInfo("Monk", 5, CardType.TROOP, setOf(CardTag.MINI_TANK, CardTag.ANTI_AIR)),
        CardInfo("Little Prince", 3, CardType.TROOP, setOf(CardTag.RANGED_SUPPORT, CardTag.ANTI_AIR)),
        CardInfo("Suspicious Bush", 3, CardType.TROOP, setOf(CardTag.CYCLE)),
        CardInfo("Goblin Machine", 5, CardType.TROOP, setOf(CardTag.MINI_TANK)),
        CardInfo("Goblinstein", 5, CardType.TROOP, setOf(CardTag.SWARM)),
        CardInfo("Boss Bandit", 5, CardType.TROOP, setOf(CardTag.MINI_TANK)),
    ).associateBy { it.name }

    operator fun get(name: String): CardInfo = cards[name] ?: UNKNOWN_CARD.copy(name = name)

    fun contains(name: String): Boolean = cards.containsKey(name)

    val all: Collection<CardInfo> get() = cards.values
}
