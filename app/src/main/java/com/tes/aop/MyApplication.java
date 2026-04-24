package com.tes.aop;

import android.app.Application;
import dagger.hilt.android.HiltAndroidApp;

/**
 * MyApplication — required by Hilt.
 * Already declared in AndroidManifest.xml (android:name=".MyApplication").
 * TODO: Add global app-level initialization here if needed.
 */
@HiltAndroidApp
public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // TODO: Initialize crash reporting, logging, etc.
    }
}
