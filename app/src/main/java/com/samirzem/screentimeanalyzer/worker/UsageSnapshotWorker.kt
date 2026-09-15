package com.samirzem.screentimeanalyzer.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.samirzem.screentimeanalyzer.ScreenTimeApplication
import com.samirzem.screentimeanalyzer.util.TimeUtils
import java.util.concurrent.TimeUnit

/**
 * Periodically archives closed days into Room so long-term trends survive even
 * after the OS eventually prunes its own [android.app.usage.UsageStats] history,
 * and keeps that archive from growing without bound.
 */
class UsageSnapshotWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as ScreenTimeApplication
        val repository = app.repository
        val today = TimeUtils.todayEpochDay()

        return try {
            for (daysAgo in 1..BACKFILL_DAYS) {
                repository.ensureDayPersisted(today - daysAgo)
            }
            repository.pruneOlderThan(today - RETENTION_DAYS)
            Result.success()
        } catch (t: Throwable) {
            Result.retry()
        }
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "usage_snapshot_worker"
        private const val BACKFILL_DAYS = 3
        private const val RETENTION_DAYS = 400L

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<UsageSnapshotWorker>(6, TimeUnit.HOURS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
