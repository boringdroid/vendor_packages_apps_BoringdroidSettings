package com.boringdroid.settings;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;

import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class BoringdroidSettingsFragment extends PreferenceFragmentCompat {
    private static final String TAG = "BDSettingsFragment";

    private SwitchPreferenceCompat mSwitchEnablePCMode;
    private SwitchPreferenceCompat mSwitchEnabledBoringdroidSystemUI;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.main_preference, rootKey);
        mSwitchEnablePCMode =
                (SwitchPreferenceCompat) findPreference(
                        getString(R.string.key_switch_enable_pc_mode)
                );
        mSwitchEnabledBoringdroidSystemUI =
                (SwitchPreferenceCompat) findPreference(
                        getString(R.string.key_switch_enable_bd_nav_bar)
                );

        mSwitchEnablePCMode.setOnPreferenceClickListener(preference -> {
            enablePCMode(((SwitchPreferenceCompat) preference).isChecked());
            return true;
        });
        mSwitchEnabledBoringdroidSystemUI.setOnPreferenceClickListener(preference -> {
            enableBoringdroidSystemUI(((SwitchPreferenceCompat) preference).isChecked());
            return true;
        });
    }

    private void enablePCMode(boolean enable) {
        Log.d(TAG, "enable pc mode " + enable);
        setBooleanSystemProperties("persist.sys.pcmode.enabled", enable);
    }

    private void enableBoringdroidSystemUI(boolean enable) {
        Log.d(TAG, "enable Boringdroid SystemUI " + enable);
        setBooleanSystemProperties("persist.sys.systemuiplugin.enabled", enable);
    }

    private void setBooleanSystemProperties(String key, boolean value) {
        try {
            @SuppressLint("PrivateApi")
            Class clazz = Class.forName("android.os.SystemProperties");
            Method setMethod = clazz.getMethod("set", String.class, String.class);
            setMethod.invoke(null, key, value ? "true" : "false");
        } catch (ClassNotFoundException
                | NoSuchMethodException
                | IllegalAccessException
                | InvocationTargetException e) {
            Log.d(TAG, "Failed to set value " + value + " for " + key);
        }
    }
}
