package com.boringdroid.settings;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class BoringdroidSettings extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_main);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings_container, new BoringdroidSettingsFragment())
                .commit();
    }
}
