package jk.ut61eTool;


import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.View;

import java.util.Date;


public class SettingsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener{
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.prefs);
            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
            Preference preference = findPreference("info");
            preference.setSummary("Version: " + BuildConfig.VERSION_NAME + "\n" + new Date(BuildConfig.TIMESTAMP));
        }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
    }
}
