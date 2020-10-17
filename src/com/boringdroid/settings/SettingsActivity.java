package com.boringdroid.settings;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

public class SettingsActivity extends FragmentActivity
        implements PreferenceManager.OnPreferenceTreeClickListener,
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback,
        FragmentManager.OnBackStackChangedListener {
    @Override
    public void onBackStackChanged() {

    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragmentCompat preferenceFragmentCompat,
                                             Preference preference) {
        return false;
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        return false;
    }
}