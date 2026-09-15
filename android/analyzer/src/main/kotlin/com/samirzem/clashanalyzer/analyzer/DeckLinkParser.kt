package com.samirzem.clashanalyzer.analyzer

/**
 * Parses a Clash Royale "copy deck link" (the share link the in-game deck menu generates), e.g.
 * `https://link.clashroyale.com/deck/en?deck=26000046;26000036;26000042;28000000;26000062;26000004;26000050;28000008`
 * or the older `https://link.clashroyale.com/en/?clashroyale://copyDeck?deck=...&l=Royals` shape.
 *
 * Both put the deck as a `deck=<id>;<id>;...` run somewhere in the URL, so this looks for that
 * pattern directly rather than trying to fully parse every historical URL variant.
 *
 * The numeric IDs are Supercell's internal card IDs, not names — resolving them requires the
 * official API's card list (`GET /v1/cards`), which this module intentionally doesn't fetch
 * itself (no network access here); see the app's CardCatalogRepository for that.
 */
object DeckLinkParser {
    private val DECK_IDS_PATTERN = Regex("""deck=([0-9;]+)""")

    /** Returns the card IDs found in [link], in order, or an empty list if it doesn't look like a deck link. */
    fun extractCardIds(link: String): List<Long> {
        val match = DECK_IDS_PATTERN.find(link) ?: return emptyList()
        return match.groupValues[1].split(';').mapNotNull { it.toLongOrNull() }
    }
}
