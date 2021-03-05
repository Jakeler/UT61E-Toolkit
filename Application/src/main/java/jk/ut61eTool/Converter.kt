package jk.ut61eTool

import com.jake.UT61e_decoder

class Converter {
    @JvmField
    var ignore_ol = false
    @JvmField
    var shunt_mode = false
    @JvmField
    var shunt_value = 0.0

    fun isIgnored(ut61e: UT61e_decoder): Boolean {
        //  Log.i("GRAPH/LOG", "Skipped ol/ul value");
        return ignore_ol && (ut61e.isOL() || ut61e.isUL())
    }

    fun adjust(ut61e: UT61e_decoder) {
        if (shunt_mode && shunt_value != 0.0 && ut61e.mode == ut61e.MODE_VOLTAGE) {
            ut61e.value = ut61e.value / shunt_value //I = U/R
            if (ut61e.unit_str == "mV")
                ut61e.value /= 1000.0

            ut61e.unit_str = "A (EXT. SHUNT)"
        }
    }
}