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
        CollectedDayEntity::class,
    ],
    version = 1,
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
                ).build().also { instance = it }
            }
    }
}
