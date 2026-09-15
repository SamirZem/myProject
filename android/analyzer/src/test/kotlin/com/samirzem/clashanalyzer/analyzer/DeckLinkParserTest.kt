package com.samirzem.clashanalyzer.analyzer

import kotlin.test.Test
import kotlin.test.assertEquals

class DeckLinkParserTest {

    @Test
    fun `parses the current deck link format`() {
        val link = "https://link.clashroyale.com/deck/en?deck=26000046;26000036;26000042;28000000;26000062;26000004;26000050;28000008"

        val ids = DeckLinkParser.extractCardIds(link)

        assertEquals(
            listOf(26000046L, 26000036L, 26000042L, 28000000L, 26000062L, 26000004L, 26000050L, 28000008L),
            ids,
        )
    }

    @Test
    fun `parses the legacy clashroyale-scheme deck link with a trailing locale param`() {
        val link = "https://link.clashroyale.com/en/?clashroyale://copyDeck?deck=26000000;26000001;26000002;26000003;26000004;26000005;26000006;26000007&l=Royals"

        val ids = DeckLinkParser.extractCardIds(link)

        assertEquals(8, ids.size)
        assertEquals(26000000L, ids.first())
        assertEquals(26000007L, ids.last())
    }

    @Test
    fun `returns an empty list for text that is not a deck link`() {
        assertEquals(emptyList(), DeckLinkParser.extractCardIds("not a deck link"))
        assertEquals(emptyList(), DeckLinkParser.extractCardIds(""))
    }
}
