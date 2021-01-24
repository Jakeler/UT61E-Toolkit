package jk.ut61eTool

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import java.util.*

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportFragmentManager.beginTransaction().replace(android.R.id.content, MyPreferenceFragment()).commit()
    }

    class MyPreferenceFragment : PreferenceFragmentCompat() {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
//            super.onCreate(savedInstanceState)
            setPreferencesFromResource(R.xml.prefs, rootKey)
            val preference = findPreference<Preference>("info")
            preference?.summary = """
                ${Date(BuildConfig.TIMESTAMP)}
                Version: ${BuildConfig.VERSION_NAME} 
                """.trimIndent()

            val dirPref = findPreference<Preference>("log_folder")
            dirPref?.setOnPreferenceClickListener {
                Log.d("TAG", "onCreatePreferences: $it")
                openDirectory()
                true
            }
        }

        // Choose a directory using the system's file picker.
        private fun openDirectory() {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            intent.flags = flags

            startActivityForResult(intent, 0)
        }


        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            super.onActivityResult(requestCode, resultCode, data)
            if (!(requestCode == 0 && resultCode == Activity.RESULT_OK))
                return

            val uri = data?.data ?: return
            PreferenceManager.getDefaultSharedPreferences(activity).edit().apply {
                putString("log_folder", uri.toString())
                apply()
            }
            Log.d("Settings", "onActivityResult: $requestCode $resultCode ${uri.lastPathSegment}")

            activity?.contentResolver?.takePersistableUriPermission(uri, flags)
        }

    }
}