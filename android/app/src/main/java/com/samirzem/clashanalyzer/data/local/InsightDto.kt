package com.samirzem.clashanalyzer.data.local

import com.samirzem.clashanalyzer.analyzer.model.Insight
import com.samirzem.clashanalyzer.analyzer.model.Severity
import kotlinx.serialization.Serializable

/**
 * Storage-only mirror of [Insight]. Kept separate from the analyzer's domain model so the
 * on-disk JSON format doesn't have to change every time the analysis engine does.
 */
@Serializable
data class InsightDto(
    val title: String,
    val detail: String,
    val severity: String,
    val timestampMs: Long? = null,
)

fun Insight.toDto() = InsightDto(title, detail, severity.name, timestampMs)

fun InsightDto.toDomain() = Insight(
    title = title,
    detail = detail,
    severity = runCatching { Severity.valueOf(severity) }.getOrDefault(Severity.INFO),
    timestampMs = timestampMs,
)
