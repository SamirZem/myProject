package com.samirzem.clashanalyzer

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.samirzem.clashanalyzer.capture.CaptureForegroundService
import com.samirzem.clashanalyzer.di.ServiceLocator
import com.samirzem.clashanalyzer.ui.navigation.AppNavHost
import com.samirzem.clashanalyzer.ui.theme.ClashAnalyzerTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op: capture still works, just without a visible notification */ }

    private lateinit var captureLauncher: androidx.activity.result.ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        captureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data
            if (result.resultCode == Activity.RESULT_OK && data != null) {
                startForegroundService(CaptureForegroundService.startIntent(this, result.resultCode, data))
                ServiceLocator.matchSessionController.start()
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            ClashAnalyzerTheme {
                AppNavHost(
                    onRequestCapture = { requestCapture() },
                    onStopCapture = { stopCaptureService() },
                )
            }
        }
    }

    private fun requestCapture() {
        val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        captureLauncher.launch(manager.createScreenCaptureIntent())
    }

    private fun stopCaptureService() {
        lifecycleScope.launch {
            stopService(Intent(this@MainActivity, CaptureForegroundService::class.java))
        }
    }
}
