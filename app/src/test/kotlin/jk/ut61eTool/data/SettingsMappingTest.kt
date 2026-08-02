package jk.ut61eTool.data

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsMappingTest {

    private fun prefs(vararg pairs: Pair<String, Any>) = mutablePreferencesOf().apply {
        pairs.forEach { (key, value) ->
            when (value) {
                is Boolean -> set(booleanPreferencesKey(key), value)
                is String -> set(stringPreferencesKey(key), value)
                else -> error("unsupported test value $value")
            }
        }
    }

    @Test
    fun `empty preferences give the legacy defaults`() {
        assertEquals(Settings(), prefs().toSettings())
    }

    @Test
    fun `numbers are read from their string representation`() {
        val s = prefs(
            SettingsKeys.VIEWPORT to "120",
            SettingsKeys.SHUNT_OHM to "0.01",
            SettingsKeys.TC_SENS to "0.041",
            SettingsKeys.TC_REFERENCE to "4",
            SettingsKeys.TC_REF_CONSTANT to "21.5",
            SettingsKeys.TR_OHM to "10000",
            SettingsKeys.TR_BETA to "3950",
            SettingsKeys.RTD_OHM to "100",
            SettingsKeys.RTD_ALPHA to "0.00391",
            SettingsKeys.ALARM_SAMPLES to "5",
            SettingsKeys.LOW_LIMIT to "-1.5",
            SettingsKeys.HIGH_LIMIT to "2.5",
        ).toSettings()

        assertEquals(120, s.viewportSize)
        assertEquals(0.01, s.shuntOhm, 1e-12)
        assertEquals(0.041, s.tcSensitivity, 1e-12)
        assertEquals(4, s.tcReferenceId)
        assertEquals(21.5, s.tcReferenceConstant, 1e-12)
        assertEquals(10_000.0, s.trOhm, 1e-12)
        assertEquals(3950.0, s.trBeta, 1e-12)
        assertEquals(100.0, s.rtdOhm, 1e-12)
        assertEquals(0.00391, s.rtdAlpha, 1e-12)
        assertEquals(5, s.alarmSamples)
        assertEquals(-1.5, s.lowLimit, 1e-12)
        assertEquals(2.5, s.highLimit, 1e-12)
    }

    @Test
    fun `booleans stay booleans`() {
        val s = prefs(
            SettingsKeys.NO_LOGFILE_REUSE to true,
            SettingsKeys.IGNORE_OL to true,
            SettingsKeys.SHUNT_MODE to true,
            SettingsKeys.TC_MODE to true,
            SettingsKeys.TR_MODE to true,
            SettingsKeys.RTD_MODE to true,
            SettingsKeys.ALARM_ENABLED to true,
            SettingsKeys.VIBRATION to false,
        ).toSettings()

        assertEquals(true, s.noLogfileReuse)
        assertEquals(true, s.ignoreOverload)
        assertEquals(true, s.shuntMode)
        assertEquals(true, s.tcMode)
        assertEquals(true, s.trMode)
        assertEquals(true, s.rtdMode)
        assertEquals(true, s.alarmEnabled)
        assertEquals(false, s.vibration)
    }

    @Test
    fun `malformed numbers fall back to the default instead of crashing`() {
        val s = prefs(
            SettingsKeys.VIEWPORT to "",
            SettingsKeys.SHUNT_OHM to "not a number",
            SettingsKeys.ALARM_SAMPLES to "3.7",
            SettingsKeys.TR_BETA to "  4092  ",
        ).toSettings()

        val d = Settings()
        assertEquals(d.viewportSize, s.viewportSize)
        assertEquals(d.shuntOhm, s.shuntOhm, 1e-12)
        assertEquals(d.alarmSamples, s.alarmSamples)
        assertEquals(4092.0, s.trBeta, 1e-12)
    }

    @Test
    fun `alarm condition maps from its legacy storage value`() {
        assertEquals(
            AlarmCondition.BELOW,
            prefs(SettingsKeys.ALARM_CONDITION to "below").toSettings().alarmCondition,
        )
        assertEquals(
            AlarmCondition.BOTH,
            prefs(SettingsKeys.ALARM_CONDITION to "both").toSettings().alarmCondition,
        )
        // The historical default was the meaningless "0" -> falls back to ABOVE.
        assertEquals(
            AlarmCondition.ABOVE,
            prefs(SettingsKeys.ALARM_CONDITION to "0").toSettings().alarmCondition,
        )
    }

    @Test
    fun `blank uuid falls back to the default characteristic`() {
        assertEquals(
            Settings().characteristicUuid,
            prefs(SettingsKeys.UUID to "").toSettings().characteristicUuid,
        )
        assertEquals(
            "0000ffe0-0000-1000-8000-00805f9b34fb",
            prefs(SettingsKeys.UUID to "0000ffe0-0000-1000-8000-00805f9b34fb").toSettings().characteristicUuid,
        )
    }
}
