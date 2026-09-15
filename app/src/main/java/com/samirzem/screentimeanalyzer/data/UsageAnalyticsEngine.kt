package com.samirzem.screentimeanalyzer.data

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Computes per-day, per-app, per-hour screen time analytics directly from the raw
 * [UsageEvents] stream, rather than relying on the OS's own pre-aggregated
 * [android.app.usage.UsageStats] buckets. Working from raw foreground/background
 * transitions lets us derive session counts, the longest single session, and a
 * true hour-of-day heatmap - none of which the aggregated API exposes.
 */
class UsageAnalyticsEngine(context: Context) {

    private val usageStatsManager =
        context.applicationContext.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    private val zoneId: ZoneId = ZoneId.systemDefault()

    /**
     * Computes one [DayBucket] per calendar day touched by [startMs, endMs), in the
     * device's current timezone. A single pass over the event log is used for the
     * whole range for efficiency, so this is cheap even for a 30-day window.
     */
    fun computeDayBuckets(startMs: Long, endMs: Long): List<DayBucket> {
        require(endMs > startMs) { "endMs must be after startMs" }

        val days = mutableMapOf<Long, MutableDayAccumulator>()
        fun dayFor(epochDay: Long) = days.getOrPut(epochDay) { MutableDayAccumulator(epochDay) }

        val openSessions = mutableMapOf<String, Long>()
        val events = usageStatsManager.queryEvents(startMs, endMs)
        val event = UsageEvents.Event()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val packageName = event.packageName ?: continue
            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    // A stray extra MOVE_TO_FOREGROUND for a package that's already
                    // "open" means we missed its MOVE_TO_BACKGROUND - close that
                    // session out here instead of silently discarding its time.
                    openSessions[packageName]?.let { previousStart ->
                        recordSession(::dayFor, packageName, previousStart, event.timeStamp, startMs, endMs)
                    }
                    openSessions[packageName] = event.timeStamp
                }

                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    val start = openSessions.remove(packageName) ?: continue
                    recordSession(::dayFor, packageName, start, event.timeStamp, startMs, endMs)
                }

                UsageEvents.Event.KEYGUARD_HIDDEN -> {
                    val zdt = Instant.ofEpochMilli(event.timeStamp).atZone(zoneId)
                    val bucket = dayFor(zdt.toLocalDate().toEpochDay())
                    bucket.unlockCount++
                    bucket.unlockHourly[zdt.hour]++
                    if (bucket.firstUnlockAtMs == null || event.timeStamp < bucket.firstUnlockAtMs!!) {
                        bucket.firstUnlockAtMs = event.timeStamp
                    }
                    if (bucket.lastUnlockAtMs == null || event.timeStamp > bucket.lastUnlockAtMs!!) {
                        bucket.lastUnlockAtMs = event.timeStamp
                    }
                }

                else -> Unit
            }
        }

        // Sessions still in the foreground when the query window closed (e.g. "today").
        for ((packageName, start) in openSessions) {
            recordSession(::dayFor, packageName, start, endMs, startMs, endMs)
        }

        return days.values
            .sortedBy { it.epochDay }
            .map { it.toDayBucket() }
    }

    private fun recordSession(
        dayFor: (Long) -> MutableDayAccumulator,
        packageName: String,
        rawStart: Long,
        rawEnd: Long,
        rangeStart: Long,
        rangeEnd: Long,
    ) {
        if (packageName in EXCLUDED_USAGE_PACKAGES) return
        val start = rawStart.coerceIn(rangeStart, rangeEnd)
        val clampedEnd = rawEnd.coerceIn(rangeStart, rangeEnd)
        if (clampedEnd <= start) return

        // A session whose MOVE_TO_BACKGROUND was never logged (the OS killing a
        // backgrounded/cached process doesn't always emit one - a known platform
        // quirk) would otherwise be timed all the way to "now" or the end of the
        // query window, inflating it to hours or even days. Cap any single
        // continuous session to a ceiling no genuine foreground use would hit.
        val end = minOf(clampedEnd, start + MAX_SESSION_DURATION_MS)

        val sessionDurationMs = end - start
        val startDayBucket = dayFor(epochDayOf(start))
        val appAtStart = startDayBucket.appAccumulator(packageName)
        appAtStart.sessionCount++
        if (sessionDurationMs > appAtStart.longestSessionMs) {
            appAtStart.longestSessionMs = sessionDurationMs
        }

        // Split the session's wall-clock time across the hour (and, rarely, day) slices
        // it actually occupies so totals and the hourly heatmap stay accurate even for
        // sessions that cross an hour or midnight boundary.
        var cursor = start
        while (cursor < end) {
            val zdt = Instant.ofEpochMilli(cursor).atZone(zoneId)
            val hourStartMs = zdt.truncatedTo(ChronoUnit.HOURS).toInstant().toEpochMilli()
            val hourEndMs = hourStartMs + ONE_HOUR_MS
            val sliceEnd = minOf(end, hourEndMs)

            val bucket = dayFor(zdt.toLocalDate().toEpochDay())
            val app = bucket.appAccumulator(packageName)
            val sliceMs = sliceEnd - cursor
            app.totalTimeMs += sliceMs
            app.firstUsedAtMs = minOf(app.firstUsedAtMs, cursor)
            app.lastUsedAtMs = maxOf(app.lastUsedAtMs, sliceEnd)
            app.hourlyMs[zdt.hour] += sliceMs
            bucket.hourlyMs[zdt.hour] += sliceMs

            cursor = sliceEnd
        }
    }

    private fun epochDayOf(epochMs: Long): Long =
        Instant.ofEpochMilli(epochMs).atZone(zoneId).toLocalDate().toEpochDay()

    private class MutableAppAccumulator {
        var totalTimeMs: Long = 0
        var sessionCount: Int = 0
        var longestSessionMs: Long = 0
        var firstUsedAtMs: Long = Long.MAX_VALUE
        var lastUsedAtMs: Long = 0
        val hourlyMs = LongArray(24)
    }

    private class MutableDayAccumulator(val epochDay: Long) {
        val apps = mutableMapOf<String, MutableAppAccumulator>()
        val hourlyMs = LongArray(24)
        val unlockHourly = IntArray(24)
        var unlockCount: Int = 0
        var firstUnlockAtMs: Long? = null
        var lastUnlockAtMs: Long? = null

        fun appAccumulator(packageName: String) = apps.getOrPut(packageName) { MutableAppAccumulator() }

        fun toDayBucket(): DayBucket {
            val perApp = apps.map { (pkg, acc) ->
                AppUsageStat(
                    packageName = pkg,
                    totalTimeMs = acc.totalTimeMs,
                    sessionCount = acc.sessionCount,
                    longestSessionMs = acc.longestSessionMs,
                    firstUsedAtMs = acc.firstUsedAtMs,
                    lastUsedAtMs = acc.lastUsedAtMs,
                    hourlyMs = acc.hourlyMs,
                )
            }.sortedByDescending { it.totalTimeMs }

            return DayBucket(
                epochDay = epochDay,
                totalScreenTimeMs = perApp.sumOf { it.totalTimeMs },
                perApp = perApp,
                hourlyMs = hourlyMs,
                unlockCount = unlockCount,
                firstUnlockAtMs = firstUnlockAtMs,
                lastUnlockAtMs = lastUnlockAtMs,
                unlockHourly = unlockHourly,
            )
        }
    }

    private companion object {
        const val ONE_HOUR_MS = 3_600_000L
        const val MAX_SESSION_DURATION_MS = 3 * 60 * 60 * 1000L
    }
}
