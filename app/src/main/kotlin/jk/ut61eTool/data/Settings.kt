package jk.ut61eTool.data

enum class AlarmCondition(val storageValue: String) {
    ABOVE("above"),
    BELOW("below"),
    BOTH("both");

    companion object {
        fun fromStorage(value: String?): AlarmCondition =
            entries.firstOrNull { it.storageValue == value } ?: ABOVE
    }
}

/**
 * Typed snapshot of all user settings.
 *
 * Storage keys and defaults match the legacy SharedPreferences exactly
 * (numbers are stored as strings), so existing installs migrate losslessly.
 */
data class Settings(
    val characteristicUuid: String = "0000ffe1-0000-1000-8000-00805f9b34fb",
    val viewportSize: Int = 60,
    val logFolderUri: String = "",
    val noLogfileReuse: Boolean = false,
    val ignoreOverload: Boolean = false,
    val shuntMode: Boolean = false,
    val shuntOhm: Double = 0.001,
    val tcMode: Boolean = false,
    val tcSensitivity: Double = 0.039,
    val tcReferenceId: Int = -1,
    val tcReferenceConstant: Double = 20.0,
    val trMode: Boolean = false,
    val trOhm: Double = 100_000.0,
    val trBeta: Double = 4092.0,
    val rtdMode: Boolean = false,
    val rtdOhm: Double = 1000.0,
    val rtdAlpha: Double = 0.00385,
    val alarmEnabled: Boolean = false,
    val alarmCondition: AlarmCondition = AlarmCondition.ABOVE,
    val alarmSamples: Int = 3,
    val lowLimit: Double = 0.0,
    val highLimit: Double = 0.0,
    val vibration: Boolean = true,
)

/** Legacy SharedPreferences key names — the DataStore keeps using them. */
object SettingsKeys {
    const val UUID = "uuid"
    const val VIEWPORT = "viewport"
    const val LOG_FOLDER = "log_folder"
    const val NO_LOGFILE_REUSE = "no_logfile_reuse"
    const val IGNORE_OL = "ignore_ol"
    const val SHUNT_MODE = "shunt_mode"
    const val SHUNT_OHM = "shunt_ohm"
    const val TC_MODE = "tc_mode"
    const val TC_SENS = "tc_sens"
    const val TC_REFERENCE = "tc_reference"
    const val TC_REF_CONSTANT = "tc_ref_constant"
    const val TR_MODE = "tr_mode"
    const val TR_OHM = "tr_ohm"
    const val TR_BETA = "tr_beta"
    const val RTD_MODE = "rtd_mode"
    const val RTD_OHM = "rtd_ohm"
    const val RTD_ALPHA = "rtd_alpha"
    const val ALARM_ENABLED = "alarm_enabled"
    const val ALARM_CONDITION = "alarm_condition"
    const val ALARM_SAMPLES = "samples"
    const val LOW_LIMIT = "low_limit"
    const val HIGH_LIMIT = "high_limit"
    const val VIBRATION = "vibration"
}
