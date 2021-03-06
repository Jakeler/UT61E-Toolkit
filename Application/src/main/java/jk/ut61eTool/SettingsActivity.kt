package jk.ut61eTool

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.text.InputType.*
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportFragmentManager.beginTransaction().replace(android.R.id.content, MyPreferenceFragment()).commit()
    }

    class MyPreferenceFragment : PreferenceFragmentCompat() {
        private val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        private val DIR_PREF_KEY = "log_folder"

        private var dirPref: Preference? = null
        private lateinit var prefs: SharedPreferences

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.prefs, rootKey)
            prefs = PreferenceManager.getDefaultSharedPreferences(activity)

            setupUI()
            refreshUI()
        }

        private fun setupUI() {
            val infoPref = findPreference<Preference>("info")
            infoPref?.summary = """
                ${Date(BuildConfig.TIMESTAMP)}
                Version: ${BuildConfig.VERSION_NAME} 
                """.trimIndent()

            dirPref = findPreference<Preference>("log_folder")
            dirPref?.setOnPreferenceClickListener {
                openDirectory()
                true
            }

            setNumberInputType("viewport")
            setNumberInputType("shunt_ohm", TYPE_NUMBER_FLAG_DECIMAL)
            setNumberInputType("tc_sens", TYPE_NUMBER_FLAG_DECIMAL)
            setNumberInputType("tc_ref_constant", TYPE_NUMBER_FLAG_SIGNED or TYPE_NUMBER_FLAG_DECIMAL)
            setNumberInputType("tr_ohm", TYPE_NUMBER_FLAG_DECIMAL)
            setNumberInputType("tr_beta", TYPE_NUMBER_FLAG_DECIMAL)
            setNumberInputType("rtd_ohm", TYPE_NUMBER_FLAG_DECIMAL)
            setNumberInputType("rtd_alpha", TYPE_NUMBER_FLAG_DECIMAL)
            setNumberInputType("samples")
            setNumberInputType("low_limit", TYPE_NUMBER_FLAG_SIGNED or TYPE_NUMBER_FLAG_DECIMAL)
            setNumberInputType("high_limit", TYPE_NUMBER_FLAG_SIGNED or TYPE_NUMBER_FLAG_DECIMAL)
        }

        private fun setNumberInputType(key: String, flags: Int = 0) {
            findPreference<EditTextPreference>(key)
                    ?.setOnBindEditTextListener { it.inputType = TYPE_CLASS_NUMBER or flags}
        }

        private fun refreshUI() {
            prefs.getString(DIR_PREF_KEY, "").let {
                val uri = Uri.parse(it)
                if (uri.scheme == null) {
                    prefs.edit().remove(DIR_PREF_KEY).apply()
                    dirPref?.title = getString(R.string.pref_logdir, "NOT SET")
                    Log.d("Settings", "refreshUI: deleted wrong uri '$it'")
                } else {
                    dirPref?.title = getString(R.string.pref_logdir, uri.lastPathSegment)
                }
            }

            GlobalScope.launch(Dispatchers.IO) {
                // long running shell command
                val temps = TemperatureReader.getAllTemps().sortedBy { it.celsius }
                withContext(Dispatchers.Main) {
                    // TODO: exception handling?
                    val refTempPref = findPreference<ListPreference>("tc_reference")
                    temps.forEach {
                        refTempPref?.entryValues = refTempPref?.entryValues?.plus(it.id.toString())
                        refTempPref?.entries = refTempPref?.entries?.plus("[Device] ${it.name}:\n    now = ${it.celsius}°C")
                    }
                }
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
            prefs.edit().apply {
//                putString(DIR_PREF_KEY, uri.toString())
                putString(DIR_PREF_KEY, uri.toString())
                commit()
            }

            refreshUI()
            Log.d("Settings", "onActivityResult: $requestCode $resultCode ${uri.lastPathSegment}")

            activity?.contentResolver?.takePersistableUriPermission(uri, flags)
        }

    }
}