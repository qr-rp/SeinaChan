package com.seina.chan

import android.app.Application
import com.seina.chan.util.FileLogger
import com.seina.chan.util.UncaughtExceptionHandler
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SeinaChanApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FileLogger.init(this)
        Thread.setDefaultUncaughtExceptionHandler(UncaughtExceptionHandler())
    }
}
