package com.samirzem.screentimeanalyzer.data.export

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.samirzem.screentimeanalyzer.data.AppInfoResolver
import com.samirzem.screentimeanalyzer.data.ScreenTimeRepository
import com.samirzem.screentimeanalyzer.util.Formatters
import com.samirzem.screentimeanalyzer.util.TimeUtils
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Writes the app's whole local history to plain CSV files so it survives beyond
 * the app itself (uninstalling loses the Room cache) - the closest thing to a
 * backup/export short of a full database dump.
 */
class UsageDataExporter(
    private val context: Context,
    private val repository: ScreenTimeRepository,
    private val appInfoResolver: AppInfoResolver,
) {
    /** Regenerates the export files under the app's cache dir and returns them. */
    suspend fun exportToCsvFiles(daysBack: Long = EXPORT_DAYS): List<File> = withContext(Dispatchers.IO) {
        val today = TimeUtils.todayEpochDay()
        val buckets = repository.getDayBuckets(today - (daysBack - 1), today)

        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }

        val appUsageFile = File(exportDir, "temps_ecran_par_app.csv")
        appUsageFile.bufferedWriter().use { writer ->
            writer.appendLine("date,package,application,duree_ms,duree,sessions,session_la_plus_longue_ms")
            for (bucket in buckets) {
                val date = LocalDate.ofEpochDay(bucket.epochDay)
                for (stat in bucket.perApp) {
                    val label = appInfoResolver.resolve(stat.packageName).label
                    writer.appendLine(
                        listOf(
                            date.toString(),
                            csvField(stat.packageName),
                            csvField(label),
                            stat.totalTimeMs.toString(),
                            csvField(Formatters.duration(stat.totalTimeMs)),
                            stat.sessionCount.toString(),
                            stat.longestSessionMs.toString(),
                        ).joinToString(","),
                    )
                }
            }
        }

        val unlockFile = File(exportDir, "deverrouillages_par_jour.csv")
        unlockFile.bufferedWriter().use { writer ->
            writer.appendLine("date,nombre_deverrouillages")
            for (bucket in buckets) {
                writer.appendLine("${LocalDate.ofEpochDay(bucket.epochDay)},${bucket.unlockCount}")
            }
        }

        listOf(appUsageFile, unlockFile)
    }

    fun uriFor(file: File): Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    private fun csvField(value: String): String =
        if (value.contains(',') || value.contains('"') || value.contains('\n')) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }

    private companion object {
        // Matches UsageSnapshotWorker's own retention window, so the export never
        // claims to cover more history than the app actually still has cached.
        const val EXPORT_DAYS = 400L
    }
}
