package com.samirzem.screentimeanalyzer.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        DailyAppUsageEntity::class,
        DailyUnlockSummaryEntity::class,
        HourlyUsageEntity::class,
        HourlyAppUsageEntity::class,
        HourlyUnlockEntity::class,
        HourlySwitchEntity::class,
        CollectedDayEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun usageDao(): UsageDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "screen_time.db",
                )
                    // This is just a rebuildable cache of what UsageAnalyticsEngine can
                    // recompute from UsageStatsManager, so a schema bump can safely wipe
                    // and start fresh instead of carrying a real migration.
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
    }
}
