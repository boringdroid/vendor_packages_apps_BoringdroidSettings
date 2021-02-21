package com.boringdroid.settings

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class BoringdroidSettingsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.getDefaultNightMode())
    }
}
