package com.tes.kk

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * MyApplication — required by Hilt.
 * Already declared in AndroidManifest.xml (android:name=\".MyApplication\").
 * TODO: Add global app-level initialization here if needed.
 */
@HiltAndroidApp
class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // TODO: Initialize crash reporting, logging, etc.
    }
}
