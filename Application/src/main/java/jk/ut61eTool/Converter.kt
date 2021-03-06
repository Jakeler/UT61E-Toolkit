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

    @JvmField var tr_mode = false
    @JvmField var tr_res = 0.0
    @JvmField var tr_beta = 0.0

    fun isIgnored(ut61e: UT61e_decoder): Boolean {
        //  Log.i("GRAPH/LOG", "Skipped ol/ul value");
        return ignore_ol && (ut61e.isOL() || ut61e.isUL())
    }

    fun adjust(ut61e: UT61e_decoder) {
        if (shunt_mode && shunt_value != 0.0 && ut61e.mode == ut61e.MODE_VOLTAGE) {
            ut61e.value /= shunt_value //I = U/R
            if (ut61e.unit_str == "mV")
                ut61e.value /= 1000.0
            ut61e.unit_str = "A (EXT. SHUNT)"
        } else if (tc_mode && tc_sens != 0.0 && ut61e.unit_str == "mV") {
            ut61e.value /= tc_sens
            ut61e.unit_str = "°C (thermocouple)"
        }else if (tr_mode && ut61e.mode == ut61e.MODE_RESISTANCE) {
            var ohm = ut61e.value
            if (ut61e.unit_str.startsWith("k")) ohm *= 1e3
            val k = tr_res * exp(- tr_beta / (273.15 + 25))
            ut61e.value = tr_beta / ln(ohm / k) - 273.15
            ut61e.unit_str = "°C (thermistor)"
        }
    }
}