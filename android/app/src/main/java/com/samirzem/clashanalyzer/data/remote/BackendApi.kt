package com.samirzem.clashanalyzer.data.remote

import com.samirzem.clashanalyzer.data.remote.dto.BattleLogEntryDto
import com.samirzem.clashanalyzer.data.remote.dto.CardCatalogResponseDto
import com.samirzem.clashanalyzer.data.remote.dto.PlayerDto
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Talks to our own backend proxy, never directly to Supercell's API: the official API key is
 * locked to a fixed IP address, so it must live server-side. The proxy's base URL is whatever
 * the user configured in Settings (their own deployed instance).
 */
interface BackendApi {

    @GET("api/player/{tag}")
    suspend fun getPlayer(@Path("tag") tag: String): PlayerDto

    @GET("api/player/{tag}/battlelog")
    suspend fun getBattleLog(@Path("tag") tag: String): List<BattleLogEntryDto>

    @GET("api/cards")
    suspend fun getCards(): CardCatalogResponseDto
}
