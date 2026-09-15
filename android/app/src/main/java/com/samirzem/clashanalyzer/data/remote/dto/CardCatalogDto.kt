package com.samirzem.clashanalyzer.data.remote.dto

import kotlinx.serialization.Serializable

/** `GET /v1/cards` — the full card list, used to resolve a deck-link's numeric card IDs to names. */
@Serializable
data class CardCatalogResponseDto(
    val items: List<CardCatalogEntryDto> = emptyList(),
)

@Serializable
data class CardCatalogEntryDto(
    val id: Long,
    val name: String,
)
