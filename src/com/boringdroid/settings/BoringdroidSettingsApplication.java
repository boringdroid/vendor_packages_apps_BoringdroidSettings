package com.boringdroid.settings;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

public class BoringdroidSettingsApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.getDefaultNightMode());
    }
}
