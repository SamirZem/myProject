package com.samirzem.clashanalyzer

import android.app.Application
import com.samirzem.clashanalyzer.di.ServiceLocator

class ClashAnalyzerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
    }
}
