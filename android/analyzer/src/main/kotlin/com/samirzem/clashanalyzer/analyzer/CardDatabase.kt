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

    // French names are best-effort translations of the official Clash Royale FR client (not
    // read from any API — Supercell's API always returns English names regardless of the
    // player's game language). Only used for display in pickers; every other part of the app
    // (storage, deck matching, template lookup) keeps using the canonical English `name`.
    private val cards: Map<String, CardInfo> = listOf(
        // Win conditions
        CardInfo("Hog Rider", "Bélier", 4, CardType.TROOP, setOf(CardTag.WIN_CONDITION, CardTag.MINI_TANK)),
        CardInfo("Giant", "Géant", 5, CardType.TROOP, setOf(CardTag.WIN_CONDITION, CardTag.TANK)),
        CardInfo("Royal Giant", "Géant Royal", 6, CardType.TROOP, setOf(CardTag.WIN_CONDITION, CardTag.TANK)),
        CardInfo("Golem", "Golem", 8, CardType.TROOP, setOf(CardTag.WIN_CONDITION, CardTag.TANK)),
        CardInfo("Lava Hound", "Chien de Lave", 7, CardType.TROOP, setOf(CardTag.WIN_CONDITION, CardTag.TANK, CardTag.AIR)),
        CardInfo("Balloon", "Ballon", 5, CardType.TROOP, setOf(CardTag.WIN_CONDITION, CardTag.AIR)),
        CardInfo("Graveyard", "Cimetière", 5, CardType.SPELL, setOf(CardTag.WIN_CONDITION)),
        CardInfo("X-Bow", "Arbalète", 6, CardType.BUILDING, setOf(CardTag.WIN_CONDITION)),
        CardInfo("Mortar", "Mortier", 4, CardType.BUILDING, setOf(CardTag.WIN_CONDITION)),
        CardInfo("Miner", "Mineur", 3, CardType.TROOP, setOf(CardTag.WIN_CONDITION, CardTag.CYCLE)),
        CardInfo("Goblin Barrel", "Tonneau de Gobelins", 3, CardType.SPELL, setOf(CardTag.WIN_CONDITION, CardTag.CYCLE)),
        CardInfo("Wall Breakers", "Casse-Murailles", 2, CardType.TROOP, setOf(CardTag.WIN_CONDITION, CardTag.CYCLE)),
        CardInfo("Ram Rider", "Chevaucheur de Bélier", 5, CardType.TROOP, setOf(CardTag.WIN_CONDITION, CardTag.MINI_TANK)),
        CardInfo("Battle Ram", "Bélier de Guerre", 4, CardType.TROOP, setOf(CardTag.WIN_CONDITION, CardTag.MINI_TANK)),
        CardInfo("Elixir Golem", "Golem d'Élixir", 3, CardType.TROOP, setOf(CardTag.WIN_CONDITION, CardTag.TANK)),
        CardInfo("Goblin Giant", "Géant Gobelin", 6, CardType.TROOP, setOf(CardTag.WIN_CONDITION, CardTag.TANK)),
        CardInfo("Electro Giant", "Géant Électrique", 8, CardType.TROOP, setOf(CardTag.WIN_CONDITION, CardTag.TANK)),
        CardInfo("Three Musketeers", "Trois Mousquetaires", 9, CardType.TROOP, setOf(CardTag.WIN_CONDITION, CardTag.RANGED_SUPPORT)),
        CardInfo("Skeleton Barrel", "Tonneau de Squelettes", 3, CardType.TROOP, setOf(CardTag.WIN_CONDITION, CardTag.AIR, CardTag.CYCLE)),
        CardInfo("Royal Hogs", "Sangliers Royaux", 5, CardType.TROOP, setOf(CardTag.WIN_CONDITION, CardTag.SWARM)),

        // Small spells (<= 3 elixir)
        CardInfo("Zap", "Étincelle", 2, CardType.SPELL, setOf(CardTag.SMALL_SPELL, CardTag.CYCLE)),
        CardInfo("The Log", "Rondin", 2, CardType.SPELL, setOf(CardTag.SMALL_SPELL, CardTag.CYCLE)),
        CardInfo("Snowball", "Boule de Neige", 2, CardType.SPELL, setOf(CardTag.SMALL_SPELL, CardTag.CYCLE)),
        CardInfo("Barbarian Barrel", "Tonneau de Barbare", 2, CardType.SPELL, setOf(CardTag.SMALL_SPELL, CardTag.CYCLE)),
        CardInfo("Giant Snowball", "Grosse Boule de Neige", 2, CardType.SPELL, setOf(CardTag.SMALL_SPELL, CardTag.CYCLE)),
        CardInfo("Arrows", "Flèches", 3, CardType.SPELL, setOf(CardTag.SMALL_SPELL, CardTag.SPLASH)),
        CardInfo("Tornado", "Tornade", 3, CardType.SPELL, setOf(CardTag.SMALL_SPELL)),
        CardInfo("Royal Delivery", "Livraison Royale", 3, CardType.SPELL, setOf(CardTag.SMALL_SPELL)),
        CardInfo("Earthquake", "Tremblement de Terre", 3, CardType.SPELL, setOf(CardTag.SMALL_SPELL)),
        CardInfo("Void", "Vide", 3, CardType.SPELL, setOf(CardTag.SMALL_SPELL)),

        // Big spells (>= 4 elixir)
        CardInfo("Fireball", "Boule de Feu", 4, CardType.SPELL, setOf(CardTag.BIG_SPELL, CardTag.SPLASH)),
        CardInfo("Poison", "Poison", 4, CardType.SPELL, setOf(CardTag.BIG_SPELL, CardTag.SPLASH)),
        CardInfo("Lightning", "Éclair", 6, CardType.SPELL, setOf(CardTag.BIG_SPELL)),
        CardInfo("Rocket", "Roquette", 6, CardType.SPELL, setOf(CardTag.BIG_SPELL, CardTag.SPLASH)),
        CardInfo("Freeze", "Gel", 4, CardType.SPELL, setOf(CardTag.BIG_SPELL)),
        CardInfo("Rage", "Rage", 2, CardType.SPELL, emptySet()),
        CardInfo("Clone", "Clonage", 3, CardType.SPELL, emptySet()),
        CardInfo("Mirror", "Miroir", 1, CardType.SPELL, emptySet()),

        // Swarms
        CardInfo("Skeleton Army", "Armée de Squelettes", 3, CardType.TROOP, setOf(CardTag.SWARM, CardTag.CYCLE)),
        CardInfo("Goblin Gang", "Gang de Gobelins", 3, CardType.TROOP, setOf(CardTag.SWARM, CardTag.CYCLE)),
        CardInfo("Minion Horde", "Horde de Sbires", 5, CardType.TROOP, setOf(CardTag.SWARM, CardTag.AIR)),
        CardInfo("Minions", "Sbires", 3, CardType.TROOP, setOf(CardTag.SWARM, CardTag.AIR, CardTag.ANTI_AIR)),
        CardInfo("Guards", "Gardes", 3, CardType.TROOP, setOf(CardTag.SWARM)),
        CardInfo("Skeletons", "Squelettes", 1, CardType.TROOP, setOf(CardTag.SWARM, CardTag.CYCLE)),
        CardInfo("Bats", "Chauves-souris", 2, CardType.TROOP, setOf(CardTag.SWARM, CardTag.AIR, CardTag.ANTI_AIR, CardTag.CYCLE)),
        CardInfo("Spear Goblins", "Gobelins à Lance", 2, CardType.TROOP, setOf(CardTag.SWARM, CardTag.CYCLE)),
        CardInfo("Goblins", "Gobelins", 2, CardType.TROOP, setOf(CardTag.SWARM, CardTag.CYCLE)),
        CardInfo("Rascals", "Garnements", 5, CardType.TROOP, setOf(CardTag.SWARM)),
        CardInfo("Royal Recruits", "Recrues Royales", 7, CardType.TROOP, setOf(CardTag.SWARM)),
        CardInfo("Dart Goblin", "Gobelin Sarbacane", 3, CardType.TROOP, setOf(CardTag.CYCLE, CardTag.RANGED_SUPPORT)),
        CardInfo("Firecracker", "Pétard", 3, CardType.TROOP, setOf(CardTag.CYCLE, CardTag.RANGED_SUPPORT, CardTag.SPLASH)),

        // Splash / defense troops
        CardInfo("Wizard", "Sorcier", 5, CardType.TROOP, setOf(CardTag.SPLASH, CardTag.ANTI_AIR)),
        CardInfo("Executioner", "Bourreau", 5, CardType.TROOP, setOf(CardTag.SPLASH, CardTag.ANTI_AIR)),
        CardInfo("Baby Dragon", "Bébé Dragon", 4, CardType.TROOP, setOf(CardTag.SPLASH, CardTag.AIR, CardTag.ANTI_AIR)),
        CardInfo("Inferno Dragon", "Dragon Infernal", 4, CardType.TROOP, setOf(CardTag.AIR, CardTag.ANTI_AIR)),
        CardInfo("Bomber", "Bombardier", 2, CardType.TROOP, setOf(CardTag.SPLASH, CardTag.CYCLE)),
        CardInfo("Electro Wizard", "Sorcier Électrique", 4, CardType.TROOP, setOf(CardTag.ANTI_AIR, CardTag.RANGED_SUPPORT)),
        CardInfo("Magic Archer", "Archer Magique", 4, CardType.TROOP, setOf(CardTag.ANTI_AIR, CardTag.RANGED_SUPPORT)),
        CardInfo("Musketeer", "Mousquetaire", 4, CardType.TROOP, setOf(CardTag.ANTI_AIR, CardTag.RANGED_SUPPORT)),
        CardInfo("Archers", "Archères", 3, CardType.TROOP, setOf(CardTag.ANTI_AIR, CardTag.RANGED_SUPPORT, CardTag.CYCLE)),
        CardInfo("Princess", "Princesse", 3, CardType.TROOP, setOf(CardTag.RANGED_SUPPORT, CardTag.SPLASH, CardTag.ANTI_AIR, CardTag.CYCLE)),
        CardInfo("Bowler", "Bouliste", 5, CardType.TROOP, setOf(CardTag.SPLASH)),
        CardInfo("Sparky", "Sparky", 6, CardType.TROOP, setOf(CardTag.SPLASH)),
        CardInfo("Hunter", "Chasseur", 4, CardType.TROOP, setOf(CardTag.SPLASH)),

        // Tanks / mini tanks
        CardInfo("P.E.K.K.A", "P.E.K.K.A", 7, CardType.TROOP, setOf(CardTag.TANK)),
        CardInfo("Mega Knight", "Méga Chevalier", 7, CardType.TROOP, setOf(CardTag.TANK, CardTag.SPLASH)),
        CardInfo("Knight", "Chevalier", 3, CardType.TROOP, setOf(CardTag.MINI_TANK, CardTag.CYCLE)),
        CardInfo("Valkyrie", "Valkyrie", 4, CardType.TROOP, setOf(CardTag.MINI_TANK, CardTag.SPLASH)),
        CardInfo("Mini P.E.K.K.A", "Mini P.E.K.K.A", 4, CardType.TROOP, setOf(CardTag.MINI_TANK)),
        CardInfo("Giant Skeleton", "Squelette Géant", 6, CardType.TROOP, setOf(CardTag.TANK)),
        CardInfo("Dark Prince", "Prince Ténébreux", 4, CardType.TROOP, setOf(CardTag.MINI_TANK, CardTag.SPLASH)),
        CardInfo("Prince", "Prince", 5, CardType.TROOP, setOf(CardTag.MINI_TANK)),
        CardInfo("Bandit", "Bandit", 3, CardType.TROOP, setOf(CardTag.MINI_TANK, CardTag.CYCLE)),
        CardInfo("Royal Ghost", "Fantôme Royal", 3, CardType.TROOP, setOf(CardTag.MINI_TANK, CardTag.CYCLE)),
        CardInfo("Lumberjack", "Bûcheron", 4, CardType.TROOP, setOf(CardTag.MINI_TANK)),
        CardInfo("Ice Golem", "Golem de Glace", 2, CardType.TROOP, setOf(CardTag.MINI_TANK, CardTag.CYCLE)),
        CardInfo("Fisherman", "Pêcheur", 3, CardType.TROOP, setOf(CardTag.MINI_TANK, CardTag.CYCLE)),
        CardInfo("Cannon Cart", "Chariot Canon", 5, CardType.TROOP, setOf(CardTag.MINI_TANK)),
        CardInfo("Battle Healer", "Guérisseuse de Combat", 4, CardType.TROOP, setOf(CardTag.MINI_TANK)),

        // Buildings (defense)
        CardInfo("Cannon", "Canon", 3, CardType.BUILDING, setOf(CardTag.BUILDING_DEFENSE, CardTag.CYCLE)),
        CardInfo("Tesla", "Tesla", 4, CardType.BUILDING, setOf(CardTag.BUILDING_DEFENSE, CardTag.ANTI_AIR)),
        CardInfo("Inferno Tower", "Tour Infernale", 5, CardType.BUILDING, setOf(CardTag.BUILDING_DEFENSE, CardTag.ANTI_AIR)),
        CardInfo("Bomb Tower", "Tour à Bombes", 4, CardType.BUILDING, setOf(CardTag.BUILDING_DEFENSE, CardTag.SPLASH)),
        CardInfo("Tombstone", "Pierre Tombale", 3, CardType.BUILDING, setOf(CardTag.BUILDING_DEFENSE, CardTag.CYCLE)),
        CardInfo("Goblin Cage", "Cage à Gobelin", 4, CardType.BUILDING, setOf(CardTag.BUILDING_DEFENSE)),
        CardInfo("Goblin Hut", "Hutte de Gobelins", 5, CardType.BUILDING, setOf(CardTag.BUILDING_DEFENSE)),
        CardInfo("Furnace", "Fournaise", 4, CardType.BUILDING, setOf(CardTag.BUILDING_DEFENSE, CardTag.ANTI_AIR)),
        CardInfo("Barbarian Hut", "Hutte de Barbares", 6, CardType.BUILDING, setOf(CardTag.BUILDING_DEFENSE)),
        CardInfo("Elixir Collector", "Collecteur d'Élixir", 6, CardType.BUILDING, emptySet()),

        // Air troops
        CardInfo("Mega Minion", "Méga Sbire", 3, CardType.TROOP, setOf(CardTag.AIR, CardTag.ANTI_AIR, CardTag.CYCLE)),
        CardInfo("Flying Machine", "Machine Volante", 4, CardType.TROOP, setOf(CardTag.AIR, CardTag.ANTI_AIR, CardTag.RANGED_SUPPORT)),
        CardInfo("Electro Dragon", "Dragon Électrique", 5, CardType.TROOP, setOf(CardTag.AIR, CardTag.ANTI_AIR, CardTag.SPLASH)),
        CardInfo("Phoenix", "Phénix", 4, CardType.TROOP, setOf(CardTag.AIR, CardTag.ANTI_AIR)),
        CardInfo("Skeleton Dragons", "Dragons Squelettes", 4, CardType.TROOP, setOf(CardTag.AIR, CardTag.ANTI_AIR, CardTag.SWARM)),

        // Barbarians family
        CardInfo("Barbarians", "Barbares", 5, CardType.TROOP, setOf(CardTag.SWARM)),
        CardInfo("Elite Barbarians", "Barbares d'Élite", 6, CardType.TROOP, setOf(CardTag.SWARM)),

        // Cycle support
        CardInfo("Ice Spirit", "Esprit de Glace", 1, CardType.TROOP, setOf(CardTag.CYCLE, CardTag.ANTI_AIR)),
        CardInfo("Fire Spirit", "Esprit de Feu", 1, CardType.TROOP, setOf(CardTag.CYCLE, CardTag.ANTI_AIR)),
        CardInfo("Electro Spirit", "Esprit Électrique", 1, CardType.TROOP, setOf(CardTag.CYCLE, CardTag.ANTI_AIR)),
        CardInfo("Heal Spirit", "Esprit de Soin", 1, CardType.TROOP, setOf(CardTag.CYCLE)),
        CardInfo("Zappies", "Zappys", 4, CardType.TROOP, setOf(CardTag.ANTI_AIR, CardTag.RANGED_SUPPORT)),
        CardInfo("Night Witch", "Sorcière de Nuit", 4, CardType.TROOP, setOf(CardTag.SWARM, CardTag.MINI_TANK)),
        CardInfo("Witch", "Sorcière", 5, CardType.TROOP, setOf(CardTag.SWARM, CardTag.SPLASH, CardTag.ANTI_AIR)),
        CardInfo("Golden Knight", "Chevalier Doré", 4, CardType.TROOP, setOf(CardTag.MINI_TANK, CardTag.CYCLE)),
        CardInfo("Archer Queen", "Reine Archère", 5, CardType.TROOP, setOf(CardTag.ANTI_AIR, CardTag.RANGED_SUPPORT)),
        CardInfo("Skeleton King", "Roi Squelette", 4, CardType.TROOP, setOf(CardTag.MINI_TANK)),
        CardInfo("Mighty Miner", "Mineur Balèze", 4, CardType.TROOP, setOf(CardTag.MINI_TANK)),
        CardInfo("Monk", "Moine", 5, CardType.TROOP, setOf(CardTag.MINI_TANK, CardTag.ANTI_AIR)),
        CardInfo("Little Prince", "Petit Prince", 3, CardType.TROOP, setOf(CardTag.RANGED_SUPPORT, CardTag.ANTI_AIR)),
        CardInfo("Suspicious Bush", "Buisson Suspect", 3, CardType.TROOP, setOf(CardTag.CYCLE)),
        CardInfo("Goblin Machine", "Machine à Gobelins", 5, CardType.TROOP, setOf(CardTag.MINI_TANK)),
        CardInfo("Goblinstein", "Gobelinstein", 5, CardType.TROOP, setOf(CardTag.SWARM)),
        CardInfo("Boss Bandit", "Bandit en Chef", 5, CardType.TROOP, setOf(CardTag.MINI_TANK)),
        CardInfo("Berserker", "Berserker", 3, CardType.TROOP, setOf(CardTag.CYCLE)),
    ).associateBy { it.name }

    operator fun get(name: String): CardInfo = cards[name] ?: UNKNOWN_CARD.copy(name = name, frenchName = name)

    fun contains(name: String): Boolean = cards.containsKey(name)

    val all: Collection<CardInfo> get() = cards.values
}
