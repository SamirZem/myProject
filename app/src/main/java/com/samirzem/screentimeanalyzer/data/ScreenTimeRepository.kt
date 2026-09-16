package com.samirzem.screentimeanalyzer.data

import com.samirzem.screentimeanalyzer.data.local.DailyAppUsageEntity
import com.samirzem.screentimeanalyzer.data.local.DailyUnlockSummaryEntity
import com.samirzem.screentimeanalyzer.data.local.HourlyAppUsageEntity
import com.samirzem.screentimeanalyzer.data.local.HourlySwitchEntity
import com.samirzem.screentimeanalyzer.data.local.HourlyUnlockEntity
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
        dao.pruneHourlyAppBefore(epochDay)
        dao.pruneHourlyUnlockBefore(epochDay)
        dao.pruneUnlockSummaryBefore(epochDay)
        dao.pruneCollectedBefore(epochDay)
    }

    private suspend fun loadFromRoom(startEpochDay: Long, endEpochDay: Long): Map<Long, DayBucket> {
        val appRows = dao.appUsageIn(startEpochDay, endEpochDay).groupBy { it.epochDay }
        val unlockRows = dao.unlockSummaryIn(startEpochDay, endEpochDay).associateBy { it.epochDay }
        val hourlyRows = dao.hourlyIn(startEpochDay, endEpochDay).groupBy { it.epochDay }
        val hourlyAppRows = dao.hourlyAppIn(startEpochDay, endEpochDay).groupBy { it.epochDay }
        val hourlyUnlockRows = dao.hourlyUnlockIn(startEpochDay, endEpochDay).groupBy { it.epochDay }
        val hourlySwitchRows = dao.hourlySwitchIn(startEpochDay, endEpochDay).groupBy { it.epochDay }

        val days = appRows.keys + unlockRows.keys + hourlyRows.keys
        return days.associateWith { epochDay ->
            val hourlyByPackage = (hourlyAppRows[epochDay] ?: emptyList()).groupBy { it.packageName }

            val perApp = (appRows[epochDay] ?: emptyList())
                .map { row ->
                    val appHourlyMs = LongArray(24)
                    hourlyByPackage[row.packageName]?.forEach { appHourlyMs[it.hour] = it.totalTimeMs }
                    AppUsageStat(
                        packageName = row.packageName,
                        totalTimeMs = row.totalTimeMs,
                        sessionCount = row.sessionCount,
                        longestSessionMs = row.longestSessionMs,
                        firstUsedAtMs = row.firstUsedAtMs,
                        lastUsedAtMs = row.lastUsedAtMs,
                        hourlyMs = appHourlyMs,
                    )
                }
                .sortedByDescending { it.totalTimeMs }

            val hourlyMs = LongArray(24)
            (hourlyRows[epochDay] ?: emptyList()).forEach { hourlyMs[it.hour] = it.totalTimeMs }

            val unlockHourly = IntArray(24)
            (hourlyUnlockRows[epochDay] ?: emptyList()).forEach { unlockHourly[it.hour] = it.count }

            val switchHourly = IntArray(24)
            (hourlySwitchRows[epochDay] ?: emptyList()).forEach { switchHourly[it.hour] = it.count }

            val unlock = unlockRows[epochDay]

            DayBucket(
                epochDay = epochDay,
                totalScreenTimeMs = perApp.sumOf { it.totalTimeMs },
                perApp = perApp,
                hourlyMs = hourlyMs,
                unlockCount = unlock?.unlockCount ?: 0,
                firstUnlockAtMs = unlock?.firstUnlockAtMs,
                lastUnlockAtMs = unlock?.lastUnlockAtMs,
                unlockHourly = unlockHourly,
                switchHourly = switchHourly,
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
        // LongArray/IntArray have no mapIndexedNotNull (only defined for Iterable/Array<T>),
        // so go through withIndex() first.
        val hourlyRows = bucket.hourlyMs.withIndex().mapNotNull { (hour, ms) ->
            if (ms > 0) HourlyUsageEntity(bucket.epochDay, hour, ms) else null
        }
        val hourlyAppRows = bucket.perApp.flatMap { stat ->
            stat.hourlyMs.withIndex().mapNotNull { (hour, ms) ->
                if (ms > 0) HourlyAppUsageEntity(bucket.epochDay, hour, stat.packageName, ms) else null
            }
        }
        val hourlyUnlockRows = bucket.unlockHourly.withIndex().mapNotNull { (hour, count) ->
            if (count > 0) HourlyUnlockEntity(bucket.epochDay, hour, count) else null
        }
        val hourlySwitchRows = bucket.switchHourly.withIndex().mapNotNull { (hour, count) ->
            if (count > 0) HourlySwitchEntity(bucket.epochDay, hour, count) else null
        }
        val unlockRow = DailyUnlockSummaryEntity(
            epochDay = bucket.epochDay,
            unlockCount = bucket.unlockCount,
            firstUnlockAtMs = bucket.firstUnlockAtMs,
            lastUnlockAtMs = bucket.lastUnlockAtMs,
        )
        dao.saveDay(bucket.epochDay, appRows, unlockRow, hourlyRows, hourlyAppRows, hourlyUnlockRows, hourlySwitchRows)
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
