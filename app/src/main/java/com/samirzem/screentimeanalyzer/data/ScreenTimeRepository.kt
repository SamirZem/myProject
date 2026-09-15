package com.samirzem.screentimeanalyzer.data

import com.samirzem.screentimeanalyzer.data.local.DailyAppUsageEntity
import com.samirzem.screentimeanalyzer.data.local.DailyUnlockSummaryEntity
import com.samirzem.screentimeanalyzer.data.local.HourlyUsageEntity
import com.samirzem.screentimeanalyzer.data.local.UsageDao
import com.samirzem.screentimeanalyzer.util.TimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Single source of truth for screen-time analytics. Days that are fully in the past
 * are served from the Room cache when available (fast, and survives the OS pruning
 * its own usage history); "today" and any day missing from the cache are computed
 * live from [UsageAnalyticsEngine] and, if the day has since closed, written through
 * to Room so future reads don't need to recompute it.
 */
class ScreenTimeRepository(
    private val engine: UsageAnalyticsEngine,
    private val dao: UsageDao,
) {

    suspend fun getDayBuckets(startEpochDay: Long, endEpochDayInclusive: Long): List<DayBucket> =
        withContext(Dispatchers.IO) {
            require(endEpochDayInclusive >= startEpochDay) { "Invalid range" }

            val today = TimeUtils.todayEpochDay()
            val closedEnd = minOf(endEpochDayInclusive, today - 1)

            val cachedDaySet = if (startEpochDay <= closedEnd) {
                dao.collectedDaysIn(startEpochDay, closedEnd).toSet()
            } else {
                emptySet()
            }

            val resultByDay = mutableMapOf<Long, DayBucket>()

            if (cachedDaySet.isNotEmpty()) {
                loadFromRoom(cachedDaySet.min(), cachedDaySet.max())
                    .filterKeys { it in cachedDaySet }
                    .let(resultByDay::putAll)
            }

            val missingDays = (startEpochDay..endEpochDayInclusive).filter { it !in cachedDaySet }
            if (missingDays.isNotEmpty()) {
                val liveStartMs = TimeUtils.startOfDayMs(missingDays.min())
                val liveEndMs = minOf(TimeUtils.endOfDayMs(missingDays.max()), TimeUtils.nowMs())
                if (liveEndMs > liveStartMs) {
                    for (bucket in engine.computeDayBuckets(liveStartMs, liveEndMs)) {
                        resultByDay[bucket.epochDay] = bucket
                        if (bucket.epochDay < today) {
                            persist(bucket)
                        }
                    }
                }
            }

            (startEpochDay..endEpochDayInclusive).map { epochDay ->
                resultByDay[epochDay] ?: emptyDayBucket(epochDay)
            }
        }

    suspend fun getDayBucket(epochDay: Long): DayBucket = getDayBuckets(epochDay, epochDay).first()

    /** Forces [epochDay] (which must already be in the past) to be computed and cached. */
    suspend fun ensureDayPersisted(epochDay: Long) = withContext(Dispatchers.IO) {
        val today = TimeUtils.todayEpochDay()
        require(epochDay < today) { "Only closed days can be persisted" }
        val startMs = TimeUtils.startOfDayMs(epochDay)
        val endMs = TimeUtils.endOfDayMs(epochDay)
        val bucket = engine.computeDayBuckets(startMs, endMs).firstOrNull { it.epochDay == epochDay }
            ?: emptyDayBucket(epochDay)
        persist(bucket)
    }

    suspend fun pruneOlderThan(epochDay: Long) = withContext(Dispatchers.IO) {
        dao.pruneAppUsageBefore(epochDay)
        dao.pruneHourlyBefore(epochDay)
        dao.pruneUnlockSummaryBefore(epochDay)
        dao.pruneCollectedBefore(epochDay)
    }

    private suspend fun loadFromRoom(startEpochDay: Long, endEpochDay: Long): Map<Long, DayBucket> {
        val appRows = dao.appUsageIn(startEpochDay, endEpochDay).groupBy { it.epochDay }
        val unlockRows = dao.unlockSummaryIn(startEpochDay, endEpochDay).associateBy { it.epochDay }
        val hourlyRows = dao.hourlyIn(startEpochDay, endEpochDay).groupBy { it.epochDay }

        val days = appRows.keys + unlockRows.keys + hourlyRows.keys
        return days.associateWith { epochDay ->
            val perApp = (appRows[epochDay] ?: emptyList())
                .map {
                    AppUsageStat(
                        packageName = it.packageName,
                        totalTimeMs = it.totalTimeMs,
                        sessionCount = it.sessionCount,
                        longestSessionMs = it.longestSessionMs,
                        firstUsedAtMs = it.firstUsedAtMs,
                        lastUsedAtMs = it.lastUsedAtMs,
                    )
                }
                .sortedByDescending { it.totalTimeMs }

            val hourlyMs = LongArray(24)
            (hourlyRows[epochDay] ?: emptyList()).forEach { hourlyMs[it.hour] = it.totalTimeMs }

            val unlock = unlockRows[epochDay]

            DayBucket(
                epochDay = epochDay,
                totalScreenTimeMs = perApp.sumOf { it.totalTimeMs },
                perApp = perApp,
                hourlyMs = hourlyMs,
                unlockCount = unlock?.unlockCount ?: 0,
                firstUnlockAtMs = unlock?.firstUnlockAtMs,
                lastUnlockAtMs = unlock?.lastUnlockAtMs,
            )
        }
    }

    private suspend fun persist(bucket: DayBucket) {
        val appRows = bucket.perApp.map { stat ->
            DailyAppUsageEntity(
                epochDay = bucket.epochDay,
                packageName = stat.packageName,
                totalTimeMs = stat.totalTimeMs,
                sessionCount = stat.sessionCount,
                longestSessionMs = stat.longestSessionMs,
                firstUsedAtMs = stat.firstUsedAtMs,
                lastUsedAtMs = stat.lastUsedAtMs,
            )
        }
        val hourlyRows = bucket.hourlyMs.mapIndexedNotNull { hour, ms ->
            if (ms > 0) HourlyUsageEntity(bucket.epochDay, hour, ms) else null
        }
        val unlockRow = DailyUnlockSummaryEntity(
            epochDay = bucket.epochDay,
            unlockCount = bucket.unlockCount,
            firstUnlockAtMs = bucket.firstUnlockAtMs,
            lastUnlockAtMs = bucket.lastUnlockAtMs,
        )
        dao.saveDay(bucket.epochDay, appRows, unlockRow, hourlyRows)
    }

    private fun emptyDayBucket(epochDay: Long) = DayBucket(
        epochDay = epochDay,
        totalScreenTimeMs = 0,
        perApp = emptyList(),
        hourlyMs = LongArray(24),
        unlockCount = 0,
        firstUnlockAtMs = null,
        lastUnlockAtMs = null,
    )
}
