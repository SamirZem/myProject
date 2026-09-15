package com.samirzem.screentimeanalyzer.ui.common

/** Multi-day analysis window shared by the Trends and app-detail screens. */
enum class AnalysisPeriod(val label: String, val days: Long) {
    WEEK("7 jours", 7),
    MONTH("30 jours", 30),
}
