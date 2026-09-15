package com.samirzem.screentimeanalyzer.permission

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Process
import android.provider.Settings

/**
 * "Usage access" is a special app-op, not a runtime permission: it can only be
 * granted by the user from Settings, so we can only detect and deep-link to it.
 */
object UsagePermission {

    @Suppress("DEPRECATION") // checkOpNoThrow works back to API 19; the "unsafe" rename only landed in API 29.
    fun isGranted(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun settingsIntent(context: Context): Intent {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        // On most OEMs (including Pixel/AOSP) this extra jumps straight to our entry
        // in the list instead of leaving the user to find it themselves.
        intent.data = Uri.parse("package:${context.packageName}")
        return intent
    }
}
