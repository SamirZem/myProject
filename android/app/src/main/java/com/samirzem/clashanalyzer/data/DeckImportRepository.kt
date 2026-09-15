package com.samirzem.clashanalyzer.data

import com.samirzem.clashanalyzer.analyzer.DeckLinkParser
import com.samirzem.clashanalyzer.data.remote.BackendApi

sealed class DeckImportResult {
    data class Success(val cardNames: List<String>) : DeckImportResult()
    data class PartialSuccess(val resolved: List<String>, val unresolvedCount: Int) : DeckImportResult()
    object NotADeckLink : DeckImportResult()
    data class Error(val message: String) : DeckImportResult()
}

/**
 * Resolves a Clash Royale "copy deck link" (pasted from the in-game deck menu) into the 8 card
 * names it contains, via the official card list (`GET /v1/cards`, through our backend proxy) —
 * the link itself only carries Supercell's internal numeric card IDs.
 */
class DeckImportRepository(private val apiProvider: suspend () -> BackendApi) {

    private var cachedCatalog: Map<Long, String>? = null

    suspend fun importFromLink(link: String): DeckImportResult {
        val ids = DeckLinkParser.extractCardIds(link)
        if (ids.isEmpty()) return DeckImportResult.NotADeckLink

        val catalog = try {
            cachedCatalog ?: fetchCatalog().also { cachedCatalog = it }
        } catch (e: Exception) {
            return DeckImportResult.Error(e.message ?: "Impossible de récupérer la liste des cartes officielle.")
        }

        val resolved = ids.mapNotNull { catalog[it] }
        return when {
            resolved.size == ids.size -> DeckImportResult.Success(resolved)
            resolved.isEmpty() -> DeckImportResult.Error("Aucune des cartes de ce lien n'a été reconnue.")
            else -> DeckImportResult.PartialSuccess(resolved, ids.size - resolved.size)
        }
    }

    private suspend fun fetchCatalog(): Map<Long, String> =
        apiProvider().getCards().items.associate { it.id to it.name }
}
