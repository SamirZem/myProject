package com.samirzem.screentimeanalyzer.data

/** Usage accumulated for a single app on a single calendar day. */
data class AppUsageStat(
    val packageName: String,
    val totalTimeMs: Long,
    val sessionCount: Int,
    val longestSessionMs: Long,
    val firstUsedAtMs: Long,
    val lastUsedAtMs: Long,
    /** Index 0..23, this app's foreground milliseconds spent in that hour-of-day. */
    val hourlyMs: LongArray = LongArray(24),
)

/** Everything the engine computed for one calendar day (device-local timezone). */
data class DayBucket(
    val epochDay: Long,
    val totalScreenTimeMs: Long,
    val perApp: List<AppUsageStat>,
    /** Index 0..23, total foreground milliseconds spent in that hour-of-day, all apps combined. */
    val hourlyMs: LongArray,
    val unlockCount: Int,
    val firstUnlockAtMs: Long?,
    val lastUnlockAtMs: Long?,
    /** Index 0..23, number of unlocks that happened in that hour-of-day. */
    val unlockHourly: IntArray = IntArray(24),
) {
    val appCount: Int get() = perApp.size

    fun usageFor(packageName: String): AppUsageStat? = perApp.firstOrNull { it.packageName == packageName }
}

/** Packages we never want to show as "usage" because they are launcher/system chrome. */
val EXCLUDED_USAGE_PACKAGES = setOf(
    "com.google.android.apps.nexuslauncher",
    "com.android.systemui",
    "com.android.launcher",
    "com.android.launcher3",
)
