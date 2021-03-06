package jk.ut61eTool

import com.jake.UT61e_decoder
import kotlin.math.exp
import kotlin.math.ln

class Converter {
    @JvmField var ignore_ol = false

    @JvmField var shunt_mode = false
    @JvmField var shunt_value = 0.0

    @JvmField var tc_mode = false
    @JvmField var tc_sens = 0.0
    @JvmField var tc_ref_id = -10
    @JvmField var tc_ref_constant = 0.0

    @JvmField var tr_mode = false
    @JvmField var tr_res = 0.0
    @JvmField var tr_beta = 0.0

    @JvmField var rtd_mode = false
    @JvmField var rtd_res = 0.0
    @JvmField var rtd_alpha = 0.0

    fun isIgnored(ut61e: UT61e_decoder): Boolean {
        //  Log.i("GRAPH/LOG", "Skipped ol/ul value");
        return ignore_ol && (ut61e.isOL() || ut61e.isUL())
    }

    fun adjust(ut61e: UT61e_decoder) {
        if (shunt_mode && shunt_value != 0.0 && ut61e.mode == ut61e.MODE_VOLTAGE) {
            val volt = ut61e.value * unitToFactor(ut61e.unit_str)
            ut61e.value = volt / shunt_value //I = U/R
            ut61e.unit_str = "A (EXT. SHUNT)"
        } else if (tc_mode && tc_sens != 0.0 && ut61e.unit_str == "mV") {
            ut61e.value /= tc_sens
            when(tc_ref_id) {
                -1 -> Temperature(-1, "constant", tc_ref_constant)
                else -> TemperatureReader.getTempByID(tc_ref_id)
            }.let {
                ut61e.value += it.celsius
                ut61e.unit_str = "°C (thermocouple) \nref.: ${it.name} @ ${it.celsius}°C"
            }
        } else if (tr_mode && ut61e.mode == ut61e.MODE_RESISTANCE) {
            val ohm = ut61e.value * unitToFactor(ut61e.unit_str)
            val k = tr_res * exp(- tr_beta / (273.15 + 25))
            ut61e.value = tr_beta / ln(ohm / k) - 273.15
            ut61e.unit_str = "°C (thermistor)"
        } else if (rtd_mode && rtd_alpha != 0.0 && ut61e.mode == ut61e.MODE_RESISTANCE) {
            val ohm = ut61e.value * unitToFactor(ut61e.unit_str)
            ut61e.value = (ohm/rtd_res - 1) / rtd_alpha
            ut61e.unit_str = "°C (RTD)"
        }
    }

    private fun unitToFactor(unitStr: String): Double {
        return when {
           unitStr.startsWith("m") -> 1e-3
           unitStr.startsWith("k") -> 1e3
           unitStr.startsWith("M") -> 1e6
           else -> 1e0
        }
    }
}