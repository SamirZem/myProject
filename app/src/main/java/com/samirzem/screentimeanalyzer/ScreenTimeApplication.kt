package com.samirzem.screentimeanalyzer

import android.app.Application
import com.samirzem.screentimeanalyzer.data.AppInfoResolver
import com.samirzem.screentimeanalyzer.data.ScreenTimeRepository
import com.samirzem.screentimeanalyzer.data.UsageAnalyticsEngine
import com.samirzem.screentimeanalyzer.data.export.UsageDataExporter
import com.samirzem.screentimeanalyzer.data.local.AppDatabase
import com.samirzem.screentimeanalyzer.permission.UsagePermission
import com.samirzem.screentimeanalyzer.worker.UsageSnapshotWorker

class ScreenTimeApplication : Application() {

    val repository: ScreenTimeRepository by lazy {
        ScreenTimeRepository(
            engine = UsageAnalyticsEngine(this),
            dao = AppDatabase.getInstance(this).usageDao(),
        )
    }

    val appInfoResolver: AppInfoResolver by lazy { AppInfoResolver(this) }

    val dataExporter: UsageDataExporter by lazy { UsageDataExporter(this, repository, appInfoResolver) }

    override fun onCreate() {
        super.onCreate()
        if (UsagePermission.isGranted(this)) {
            UsageSnapshotWorker.schedule(this)
        }
    }
}
