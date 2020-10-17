package com.boringdroid.settings;

import android.os.Bundle;
import androidx.annotation.Nullable;
import com.boringdroid.settings.R;

public class BoringdroidSettings extends SettingsActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_main);
    }
}
