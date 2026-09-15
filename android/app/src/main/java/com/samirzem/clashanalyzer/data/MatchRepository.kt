package com.samirzem.clashanalyzer.data

import com.samirzem.clashanalyzer.analyzer.BattleAnalyzer
import com.samirzem.clashanalyzer.analyzer.LiveBattleAnalyzer
import com.samirzem.clashanalyzer.analyzer.model.AnalysisResult
import com.samirzem.clashanalyzer.analyzer.model.TelemetrySample
import com.samirzem.clashanalyzer.data.local.MatchAnalysisDao
import com.samirzem.clashanalyzer.data.local.MatchAnalysisEntity
import com.samirzem.clashanalyzer.data.remote.BackendApi
import com.samirzem.clashanalyzer.data.remote.BattleMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class MatchRepository(
    private val dao: MatchAnalysisDao,
    private val settings: SettingsDataStore,
    private val apiProvider: suspend () -> BackendApi,
) {

    fun observeMatches(): Flow<List<MatchAnalysisEntity>> = dao.observeAll()

    suspend fun getMatch(id: Long): MatchAnalysisEntity? = dao.getById(id)

    /** Runs the live, screen-capture-based analysis and persists the result. Returns the new row id. */
    suspend fun saveLiveCaptureResult(samples: List<TelemetrySample>, opponentName: String?): Long {
        val result = LiveBattleAnalyzer.analyze(samples)
        return dao.insert(MatchAnalysisEntity.from(result, opponentName))
    }

    /**
     * Fetches the player's most recent battle from the official API (via our backend proxy)
     * and saves the deck/result-level analysis for it. Useful both on its own and as a
     * cross-check of the score produced by a live capture.
     */
    suspend fun fetchAndSaveLatestFromApi(): Result<Long> = runCatching {
        val tag = settings.playerTag.first() ?: error("Aucun tag joueur configuré dans les paramètres.")
        val api = apiProvider()
        val battleLog = api.getBattleLog(tag)
        val latest = battleLog.firstOrNull() ?: error("Aucune partie récente trouvée pour ce joueur.")
        val battleRecord = BattleMapper.toBattleRecord(latest, tag) ?: error("Impossible d'interpréter la réponse de l'API pour cette partie.")
        val result: AnalysisResult = BattleAnalyzer.analyze(battleRecord)
        dao.insert(MatchAnalysisEntity.from(result, battleRecord.opponent.name))
    }

    suspend fun delete(id: Long) = dao.delete(id)
}
