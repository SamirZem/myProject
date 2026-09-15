package com.samirzem.clashanalyzer.analyzer.model

enum class Severity { INFO, GOOD, MINOR, MAJOR }

data class Insight(
    val title: String,
    val detail: String,
    val severity: Severity,
    /** Battle-time in milliseconds this insight is anchored to, if it came from live telemetry. Null for deck-level insights. */
    val timestampMs: Long? = null,
)

data class AnalysisResult(
    /** 0..100 overall performance score for this match. */
    val score: Int,
    val summary: String,
    val mistakes: List<Insight>,
    val goodMoves: List<Insight>,
    val tips: List<Insight>,
    /** True if this result was built from live screen-capture telemetry (timing/trade level detail), false if from deck/result data only. */
    val fromLiveCapture: Boolean = false,
)
