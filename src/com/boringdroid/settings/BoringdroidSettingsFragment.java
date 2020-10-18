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

    private static final String SYSTEM_PROPERTIES_CLASS_NAME = "android.os.SystemProperties";

    private static final String PROPERTY_PC_MODE_KEY = "persist.sys.pcmode.enabled";
    private static final String PROPERTY_BD_SYSTEMUI_KEY = "persist.sys.systemuiplugin.enabled";

    private SwitchPreferenceCompat mSwitchEnablePCMode;
    private SwitchPreferenceCompat mSwitchEnabledBoringdroidSystemUI;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.main_preference, rootKey);
        mSwitchEnablePCMode =
                (SwitchPreferenceCompat) findPreference(
                        getString(R.string.key_switch_enable_pc_mode)
                );
        mSwitchEnablePCMode.setChecked(getBooleanSystemProperties(PROPERTY_PC_MODE_KEY));
        mSwitchEnabledBoringdroidSystemUI =
                (SwitchPreferenceCompat) findPreference(
                        getString(R.string.key_switch_enable_bd_nav_bar)
                );
        mSwitchEnabledBoringdroidSystemUI.setChecked(
                getBooleanSystemProperties(PROPERTY_BD_SYSTEMUI_KEY)
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
        setBooleanSystemProperties(PROPERTY_PC_MODE_KEY, enable);
    }

    private void enableBoringdroidSystemUI(boolean enable) {
        Log.d(TAG, "enable Boringdroid SystemUI " + enable);
        setBooleanSystemProperties(PROPERTY_BD_SYSTEMUI_KEY, enable);
    }

    private void setBooleanSystemProperties(String key, boolean value) {
        try {
            @SuppressLint("PrivateApi")
            Class clazz = Class.forName(SYSTEM_PROPERTIES_CLASS_NAME);
            Method setMethod = clazz.getMethod("set", String.class, String.class);
            setMethod.invoke(null, key, value ? "true" : "false");
        } catch (ClassNotFoundException
                | NoSuchMethodException
                | IllegalAccessException
                | InvocationTargetException e) {
            Log.d(TAG, "Failed to set value " + value + " for " + key);
        }
    }

    private boolean getBooleanSystemProperties(String key) {
        try {
            @SuppressLint("PrivateApi")
            Class clazz = Class.forName(SYSTEM_PROPERTIES_CLASS_NAME);
            Method setMethod = clazz.getMethod("getBoolean", String.class, boolean.class);
            return (boolean) setMethod.invoke(null, key, true);
        } catch (ClassNotFoundException
                | NoSuchMethodException
                | IllegalAccessException
                | InvocationTargetException e) {
            Log.d(TAG, "Failed to get value for " + key);
        }
        return true;
    }
}
