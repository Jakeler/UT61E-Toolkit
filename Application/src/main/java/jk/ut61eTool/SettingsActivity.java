package jk.ut61eTool;


import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;

import java.util.Date;


public class SettingsActivity extends PreferenceActivity{

    @Override
    protected void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new MyPreferenceFragment()).commit();
    }

    public static class MyPreferenceFragment extends PreferenceFragment
    {
        @Override
        public void onCreate(final Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.prefs);
            Preference preference = findPreference("info");
            preference.setSummary("Version: " + BuildConfig.VERSION_NAME + "\n" + new Date(BuildConfig.TIMESTAMP));
        }
    }
}
