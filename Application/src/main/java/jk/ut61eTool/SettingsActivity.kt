package jk.ut61eTool

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import java.util.*

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportFragmentManager.beginTransaction().replace(android.R.id.content, MyPreferenceFragment()).commit()
    }

    class MyPreferenceFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
//            super.onCreate(savedInstanceState)
            setPreferencesFromResource(R.xml.prefs, rootKey)
            val preference = findPreference<Preference>("info")
            preference?.summary = """
                ${Date(BuildConfig.TIMESTAMP)}
                Version: ${BuildConfig.VERSION_NAME} 
                """.trimIndent()
        }
    }
}