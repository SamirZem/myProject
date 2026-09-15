package com.samirzem.clashanalyzer.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.samirzem.clashanalyzer.analyzer.model.AnalysisResult
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

enum class AnalysisSource { LIVE_CAPTURE, POST_MATCH_API }

@Entity(tableName = "match_analysis")
data class MatchAnalysisEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAtMs: Long,
    val source: String,
    val opponentName: String?,
    val score: Int,
    val summary: String,
    val mistakesJson: String,
    val goodMovesJson: String,
    val tipsJson: String,
) {
    companion object {
        private val json = Json { ignoreUnknownKeys = true }
        private val listSerializer = ListSerializer(InsightDto.serializer())

        fun from(result: AnalysisResult, opponentName: String?, createdAtMs: Long = System.currentTimeMillis()): MatchAnalysisEntity =
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
    }

    fun toAnalysisResult(): AnalysisResult = AnalysisResult(
        score = score,
        summary = summary,
        mistakes = json.decodeFromString(listSerializer, mistakesJson).map { it.toDomain() },
        goodMoves = json.decodeFromString(listSerializer, goodMovesJson).map { it.toDomain() },
        tips = json.decodeFromString(listSerializer, tipsJson).map { it.toDomain() },
        fromLiveCapture = source == AnalysisSource.LIVE_CAPTURE.name,
    )
}
