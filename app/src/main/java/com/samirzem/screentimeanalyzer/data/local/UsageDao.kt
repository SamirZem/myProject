package com.samirzem.screentimeanalyzer.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface UsageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppUsage(rows: List<DailyAppUsageEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUnlockSummary(row: DailyUnlockSummaryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHourly(rows: List<HourlyUsageEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHourlyApp(rows: List<HourlyAppUsageEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHourlyUnlock(rows: List<HourlyUnlockEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHourlySwitch(rows: List<HourlySwitchEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun markCollected(row: CollectedDayEntity)

    @Query("SELECT epochDay FROM collected_day WHERE epochDay BETWEEN :startEpochDay AND :endEpochDay")
    suspend fun collectedDaysIn(startEpochDay: Long, endEpochDay: Long): List<Long>

    @Query("SELECT * FROM daily_app_usage WHERE epochDay BETWEEN :startEpochDay AND :endEpochDay")
    suspend fun appUsageIn(startEpochDay: Long, endEpochDay: Long): List<DailyAppUsageEntity>

    @Query("SELECT * FROM daily_unlock_summary WHERE epochDay BETWEEN :startEpochDay AND :endEpochDay")
    suspend fun unlockSummaryIn(startEpochDay: Long, endEpochDay: Long): List<DailyUnlockSummaryEntity>

    @Query("SELECT * FROM hourly_usage WHERE epochDay BETWEEN :startEpochDay AND :endEpochDay")
    suspend fun hourlyIn(startEpochDay: Long, endEpochDay: Long): List<HourlyUsageEntity>

    @Query("SELECT * FROM hourly_app_usage WHERE epochDay BETWEEN :startEpochDay AND :endEpochDay")
    suspend fun hourlyAppIn(startEpochDay: Long, endEpochDay: Long): List<HourlyAppUsageEntity>

    @Query("SELECT * FROM hourly_unlock WHERE epochDay BETWEEN :startEpochDay AND :endEpochDay")
    suspend fun hourlyUnlockIn(startEpochDay: Long, endEpochDay: Long): List<HourlyUnlockEntity>

    @Query("SELECT * FROM hourly_switch WHERE epochDay BETWEEN :startEpochDay AND :endEpochDay")
    suspend fun hourlySwitchIn(startEpochDay: Long, endEpochDay: Long): List<HourlySwitchEntity>

    @Transaction
    suspend fun saveDay(
        epochDay: Long,
        appUsage: List<DailyAppUsageEntity>,
        unlockSummary: DailyUnlockSummaryEntity,
        hourly: List<HourlyUsageEntity>,
        hourlyApp: List<HourlyAppUsageEntity>,
        hourlyUnlock: List<HourlyUnlockEntity>,
        hourlySwitch: List<HourlySwitchEntity>,
    ) {
        insertAppUsage(appUsage)
        insertUnlockSummary(unlockSummary)
        insertHourly(hourly)
        insertHourlyApp(hourlyApp)
        insertHourlyUnlock(hourlyUnlock)
        insertHourlySwitch(hourlySwitch)
        markCollected(CollectedDayEntity(epochDay))
    }

    @Query("DELETE FROM daily_app_usage WHERE epochDay < :beforeEpochDay")
    suspend fun pruneAppUsageBefore(beforeEpochDay: Long)

    @Query("DELETE FROM hourly_usage WHERE epochDay < :beforeEpochDay")
    suspend fun pruneHourlyBefore(beforeEpochDay: Long)

    @Query("DELETE FROM hourly_app_usage WHERE epochDay < :beforeEpochDay")
    suspend fun pruneHourlyAppBefore(beforeEpochDay: Long)

    @Query("DELETE FROM hourly_unlock WHERE epochDay < :beforeEpochDay")
    suspend fun pruneHourlyUnlockBefore(beforeEpochDay: Long)

    @Query("DELETE FROM hourly_switch WHERE epochDay < :beforeEpochDay")
    suspend fun pruneHourlySwitchBefore(beforeEpochDay: Long)

    @Query("DELETE FROM daily_unlock_summary WHERE epochDay < :beforeEpochDay")
    suspend fun pruneUnlockSummaryBefore(beforeEpochDay: Long)

    @Query("DELETE FROM collected_day WHERE epochDay < :beforeEpochDay")
    suspend fun pruneCollectedBefore(beforeEpochDay: Long)
}
