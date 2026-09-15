package com.samirzem.clashanalyzer.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AnalysisSource { LIVE_CAPTURE, POST_MATCH_API }

/**
 * A plain data holder on purpose: Room's KSP processor resolves this entity's full symbol graph,
 * and giving it a companion object that reaches into another Gradle module (:analyzer, for
 * AnalysisResult) was enough to make KSP report unrelated types here as "not present" — see
 * [MatchAnalysisMapper] for the conversions that used to live here.
 */
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
)
