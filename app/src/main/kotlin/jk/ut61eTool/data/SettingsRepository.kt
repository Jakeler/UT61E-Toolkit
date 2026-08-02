package jk.ut61eTool.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Every numeric setting is stored as a *string*, and every flag as a boolean.
 * That is not an oversight: the legacy app used `EditTextPreference` /
 * `ListPreference`, which only ever write strings, and the DataStore is seeded
 * from those SharedPreferences by [SharedPreferencesMigration]. Changing the
 * stored types here would silently drop existing users' settings.
 */
class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    val settings: Flow<Settings> = dataStore.data.map { it.toSettings() }

    suspend fun setString(key: String, value: String) {
        dataStore.edit { it[stringPreferencesKey(key)] = value }
    }

    suspend fun setBoolean(key: String, value: Boolean) {
        dataStore.edit { it[booleanPreferencesKey(key)] = value }
    }

    companion object {
        /** Name of the legacy SharedPreferences file written by PreferenceManager. */
        const val LEGACY_PREFS_NAME = "jk.ut61eTool_preferences"

        private const val DATASTORE_NAME = "settings"

        fun create(context: Context): SettingsRepository {
            val appContext = context.applicationContext
            val store = PreferenceDataStoreFactory.create(
                migrations = listOf(SharedPreferencesMigration(appContext, LEGACY_PREFS_NAME)),
                produceFile = { appContext.preferencesDataStoreFile(DATASTORE_NAME) },
            )
            return SettingsRepository(store)
        }
    }
}

private fun Preferences.str(key: String): String? = this[stringPreferencesKey(key)]

private fun Preferences.bool(key: String, default: Boolean): Boolean =
    this[booleanPreferencesKey(key)] ?: default

private fun Preferences.dbl(key: String, default: Double): Double =
    str(key)?.trim()?.toDoubleOrNull() ?: default

private fun Preferences.int(key: String, default: Int): Int =
    str(key)?.trim()?.toIntOrNull() ?: default

/**
 * Pure mapping from stored preferences to the typed snapshot. Malformed values
 * (empty strings, leftovers from older versions) fall back to the default.
 */
internal fun Preferences.toSettings(): Settings {
    val d = Settings()
    return Settings(
        characteristicUuid = str(SettingsKeys.UUID)?.takeIf { it.isNotBlank() } ?: d.characteristicUuid,
        viewportSize = int(SettingsKeys.VIEWPORT, d.viewportSize),
        logFolderUri = str(SettingsKeys.LOG_FOLDER) ?: d.logFolderUri,
        noLogfileReuse = bool(SettingsKeys.NO_LOGFILE_REUSE, d.noLogfileReuse),
        ignoreOverload = bool(SettingsKeys.IGNORE_OL, d.ignoreOverload),
        shuntMode = bool(SettingsKeys.SHUNT_MODE, d.shuntMode),
        shuntOhm = dbl(SettingsKeys.SHUNT_OHM, d.shuntOhm),
        tcMode = bool(SettingsKeys.TC_MODE, d.tcMode),
        tcSensitivity = dbl(SettingsKeys.TC_SENS, d.tcSensitivity),
        tcReferenceId = int(SettingsKeys.TC_REFERENCE, d.tcReferenceId),
        tcReferenceConstant = dbl(SettingsKeys.TC_REF_CONSTANT, d.tcReferenceConstant),
        trMode = bool(SettingsKeys.TR_MODE, d.trMode),
        trOhm = dbl(SettingsKeys.TR_OHM, d.trOhm),
        trBeta = dbl(SettingsKeys.TR_BETA, d.trBeta),
        rtdMode = bool(SettingsKeys.RTD_MODE, d.rtdMode),
        rtdOhm = dbl(SettingsKeys.RTD_OHM, d.rtdOhm),
        rtdAlpha = dbl(SettingsKeys.RTD_ALPHA, d.rtdAlpha),
        alarmEnabled = bool(SettingsKeys.ALARM_ENABLED, d.alarmEnabled),
        alarmCondition = AlarmCondition.fromStorage(str(SettingsKeys.ALARM_CONDITION)),
        alarmSamples = int(SettingsKeys.ALARM_SAMPLES, d.alarmSamples),
        lowLimit = dbl(SettingsKeys.LOW_LIMIT, d.lowLimit),
        highLimit = dbl(SettingsKeys.HIGH_LIMIT, d.highLimit),
        vibration = bool(SettingsKeys.VIBRATION, d.vibration),
    )
}
