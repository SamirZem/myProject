package com.samirzem.screentimeanalyzer.data.local

import androidx.room.Entity

/**
 * A durable cache of one app's usage on one calendar day, written once that day is
 * "closed" (fully in the past). This lets the app show accurate multi-month trends
 * even after the OS purges its own [android.app.usage.UsageStats] history.
 */
@Entity(tableName = "daily_app_usage", primaryKeys = ["epochDay", "packageName"])
data class DailyAppUsageEntity(
    val epochDay: Long,
    val packageName: String,
    val totalTimeMs: Long,
    val sessionCount: Int,
    val longestSessionMs: Long,
    val firstUsedAtMs: Long,
    val lastUsedAtMs: Long,
)

@Entity(tableName = "daily_unlock_summary", primaryKeys = ["epochDay"])
data class DailyUnlockSummaryEntity(
    val epochDay: Long,
    val unlockCount: Int,
    val firstUnlockAtMs: Long?,
    val lastUnlockAtMs: Long?,
)

@Entity(tableName = "hourly_usage", primaryKeys = ["epochDay", "hour"])
data class HourlyUsageEntity(
    val epochDay: Long,
    val hour: Int,
    val totalTimeMs: Long,
)

/** Marks an epoch day as fully collected, so we know the cache for it is complete. */
@Entity(tableName = "collected_day", primaryKeys = ["epochDay"])
data class CollectedDayEntity(
    val epochDay: Long,
)
