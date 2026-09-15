package com.samirzem.screentimeanalyzer.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.samirzem.screentimeanalyzer.ScreenTimeApplication

@Composable
fun rememberApp(): ScreenTimeApplication {
    val context = LocalContext.current
    return remember { context.applicationContext as ScreenTimeApplication }
}

/** Small stable [ViewModelProvider.Factory] that just runs a lambda - avoids pulling in DI. */
class LambdaViewModelFactory<T : ViewModel>(private val create: () -> T) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <VM : ViewModel> create(modelClass: Class<VM>): VM = create() as VM
}
