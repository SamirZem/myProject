package com.samirzem.clashanalyzer.data.local

import com.samirzem.clashanalyzer.analyzer.model.AnalysisResult
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/** Conversions between the analyzer's domain model and the Room-storable entity. Kept out of
 * [MatchAnalysisEntity] itself so the entity Room/KSP processes stays a plain data holder. */
object MatchAnalysisMapper {
    private val json = Json { ignoreUnknownKeys = true }
    private val listSerializer = ListSerializer(InsightDto.serializer())

    fun toEntity(result: AnalysisResult, opponentName: String?, createdAtMs: Long = System.currentTimeMillis()): MatchAnalysisEntity =
        MatchAnalysisEntity(
            createdAtMs = createdAtMs,
            source = if (result.fromLiveCapture) AnalysisSource.LIVE_CAPTURE.name else AnalysisSource.POST_MATCH_API.name,
            opponentName = opponentName,
            score = result.score,
            summary = result.summary,
            mistakesJson = json.encodeToString(listSerializer, result.mistakes.map { it.toDto() }),
            goodMovesJson = json.encodeToString(listSerializer, result.goodMoves.map { it.toDto() }),
            tipsJson = json.encodeToString(listSerializer, result.tips.map { it.toDto() }),
        )

    fun toAnalysisResult(entity: MatchAnalysisEntity): AnalysisResult = AnalysisResult(
        score = entity.score,
        summary = entity.summary,
        mistakes = json.decodeFromString(listSerializer, entity.mistakesJson).map { it.toDomain() },
        goodMoves = json.decodeFromString(listSerializer, entity.goodMovesJson).map { it.toDomain() },
        tips = json.decodeFromString(listSerializer, entity.tipsJson).map { it.toDomain() },
        fromLiveCapture = entity.source == AnalysisSource.LIVE_CAPTURE.name,
    )
}
