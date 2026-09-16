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
 *
 * Foreground time is additionally intersected with the device's actual screen-on
 * periods (see [ScreenStateTracker]): Android does not always emit
 * MOVE_TO_BACKGROUND when the screen locks with an app still open, so without this
 * a session can otherwise "run" for as long as the phone sits locked - counting
 * background/idle time as if it were real activity.
 */
class UsageAnalyticsEngine(context: Context) {

    private val usageStatsManager =
        context.applicationContext.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    private val zoneId: ZoneId = ZoneId.systemDefault()

    /**
     * Computes one [DayBucket] per calendar day touched by [startMs, endMs), in the
     * device's current timezone. A single pass over the event log is used for the
     * whole range for efficiency, so this is cheap even for a 90-day window.
     */
    fun computeDayBuckets(startMs: Long, endMs: Long): List<DayBucket> {
        require(endMs > startMs) { "endMs must be after startMs" }

        val days = mutableMapOf<Long, MutableDayAccumulator>()
        fun dayFor(epochDay: Long) = days.getOrPut(epochDay) { MutableDayAccumulator(epochDay) }

        val openSessions = mutableMapOf<String, Long>()
        val screenState = ScreenStateTracker()
        val events = usageStatsManager.queryEvents(startMs, endMs)
        val event = UsageEvents.Event()

        // Events come back from queryEvents in chronological order, so by the time we
        // close out any session below, screenState already reflects every screen on/off
        // transition up to that point - it's safe to consult it mid-pass.
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    val packageName = event.packageName ?: continue
                    // Only one app is genuinely foregrounded at a time. Anything still
                    // marked "open" here (same package included, on a stray repeat
                    // event) means its MOVE_TO_BACKGROUND was missed - close every such
                    // session out right now instead of leaving it open until it
                    // resurfaces (possibly hours later) or the query window ends, which
                    // is what let totals balloon to hours for an app barely touched.
                    if (openSessions.isNotEmpty()) {
                        for ((strayPackage, strayStart) in openSessions) {
                            recordSession(::dayFor, strayPackage, strayStart, event.timeStamp, startMs, endMs, screenState)
                        }
                        openSessions.clear()
                    }
                    openSessions[packageName] = event.timeStamp
                }

                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    val packageName = event.packageName ?: continue
                    val start = openSessions.remove(packageName) ?: continue
                    recordSession(::dayFor, packageName, start, event.timeStamp, startMs, endMs, screenState)
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

                UsageEvents.Event.SCREEN_NON_INTERACTIVE -> screenState.onScreenOff(event.timeStamp)
                UsageEvents.Event.SCREEN_INTERACTIVE -> screenState.onScreenOn(event.timeStamp)

                else -> Unit
            }
        }

        // Sessions still in the foreground when the query window closed (e.g. "today").
        for ((packageName, start) in openSessions) {
            recordSession(::dayFor, packageName, start, endMs, startMs, endMs, screenState)
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
        screenState: ScreenStateTracker,
    ) {
        if (packageName in EXCLUDED_USAGE_PACKAGES) return
        val start = rawStart.coerceIn(rangeStart, rangeEnd)
        val clampedEnd = rawEnd.coerceIn(rangeStart, rangeEnd)
        if (clampedEnd <= start) return

        // A session whose MOVE_TO_BACKGROUND was never logged (the OS killing a
        // backgrounded/cached process doesn't always emit one) would otherwise be
        // timed all the way to "now" or the end of the query window. Cap any single
        // continuous session to a ceiling no genuine foreground use would hit - a
        // secondary safety net on top of the screen-on filtering below.
        val end = minOf(clampedEnd, start + MAX_SESSION_DURATION_MS)
        if (end <= start) return

        // Only count the parts of this session where the screen was actually on:
        // Android can keep an app "foregrounded" for as long as you leave the phone
        // locked with it open, which is not real activity.
        val activeSegments = screenState.subtractOffPeriods(start, end)
        val realDurationMs = activeSegments.sumOf { it.second - it.first }
        if (realDurationMs <= 0) return

        val startDayBucket = dayFor(epochDayOf(start))
        val appAtStart = startDayBucket.appAccumulator(packageName)
        appAtStart.sessionCount++
        if (realDurationMs > appAtStart.longestSessionMs) {
            appAtStart.longestSessionMs = realDurationMs
        }

        // Split each active segment's wall-clock time across the hour (and, rarely,
        // day) slices it actually occupies so totals and the hourly heatmap stay
        // accurate even across an hour or midnight boundary.
        for ((segmentStart, segmentEnd) in activeSegments) {
            var cursor = segmentStart
            while (cursor < segmentEnd) {
                val zdt = Instant.ofEpochMilli(cursor).atZone(zoneId)
                val hourStartMs = zdt.truncatedTo(ChronoUnit.HOURS).toInstant().toEpochMilli()
                val hourEndMs = hourStartMs + ONE_HOUR_MS
                val sliceEnd = minOf(segmentEnd, hourEndMs)

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
    }

    private fun epochDayOf(epochMs: Long): Long =
        Instant.ofEpochMilli(epochMs).atZone(zoneId).toLocalDate().toEpochDay()

    /**
     * Tracks SCREEN_INTERACTIVE/SCREEN_NON_INTERACTIVE transitions as they're seen and
     * can subtract the accumulated "screen off" periods from an arbitrary [start, end)
     * range. Assumes the screen is on unless a SCREEN_NON_INTERACTIVE event has been
     * observed (fails open: a device that never logs these events simply isn't filtered).
     */
    private class ScreenStateTracker {
        private val closedOffPeriods = mutableListOf<Pair<Long, Long>>()
        private var openOffStart: Long? = null

        fun onScreenOff(atMs: Long) {
            if (openOffStart == null) openOffStart = atMs
        }

        fun onScreenOn(atMs: Long) {
            openOffStart?.let { closedOffPeriods += it to atMs }
            openOffStart = null
        }

        /** Returns the sub-ranges of [start, end) that do NOT fall in a screen-off period. */
        fun subtractOffPeriods(start: Long, end: Long): List<Pair<Long, Long>> {
            val offPeriods = openOffStart?.let { closedOffPeriods + (it to end) } ?: closedOffPeriods
            if (offPeriods.isEmpty()) return listOf(start to end)

            val onSegments = mutableListOf<Pair<Long, Long>>()
            var cursor = start
            for ((offStart, offEnd) in offPeriods) {
                if (offEnd <= cursor || offStart >= end) continue
                val clippedStart = maxOf(offStart, cursor)
                val clippedEnd = minOf(offEnd, end)
                if (clippedStart > cursor) onSegments += cursor to clippedStart
                cursor = maxOf(cursor, clippedEnd)
                if (cursor >= end) break
            }
            if (cursor < end) onSegments += cursor to end
            return onSegments
        }
    }

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
