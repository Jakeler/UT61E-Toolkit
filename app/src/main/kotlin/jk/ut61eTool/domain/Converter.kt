package jk.ut61eTool.domain

import com.jake.ut61e.Mode
import com.jake.ut61e.Ut61eReading
import jk.ut61eTool.data.Settings
import kotlin.math.exp
import kotlin.math.ln

/** Supplies the cold-junction temperature of a thermal zone; injectable for tests. */
fun interface ColdJunctionProvider {
    fun get(zoneId: Int): Temperature
}

/**
 * Applies the user's sensor post-processing to a raw meter reading.
 *
 * The if/else-if chain order (shunt, thermocouple, thermistor, RTD) and its
 * guard conditions are kept as they were: only the first matching mode applies.
 */
class Converter(private val coldJunction: ColdJunctionProvider) {

    fun isIgnored(reading: Ut61eReading, settings: Settings): Boolean =
        settings.ignoreOverload && (reading.isOverload || reading.isUnderload)

    fun convert(reading: Ut61eReading, settings: Settings): DisplayReading {
        val v = reading.value
        val unit = reading.unit

        return when {
            settings.shuntMode && settings.shuntOhm != 0.0 && reading.mode == Mode.VOLTAGE -> {
                val volt = v * unitToFactor(unit)
                DisplayReading(
                    value = volt / settings.shuntOhm, // I = U / R
                    unit = "A (ext. shunt)",
                    detail = "shunt: ${settings.shuntOhm} Ω",
                    source = reading,
                )
            }

            settings.tcMode && settings.tcSensitivity != 0.0 && unit == "mV" -> {
                val reference = if (settings.tcReferenceId == -1) {
                    Temperature(-1, "constant", settings.tcReferenceConstant)
                } else {
                    coldJunction.get(settings.tcReferenceId)
                }
                DisplayReading(
                    value = v / settings.tcSensitivity + reference.celsius,
                    unit = "°C (thermocouple)",
                    detail = "ref.: ${reference.name} @ ${reference.celsius}°C",
                    source = reading,
                )
            }

            settings.trMode && reading.mode == Mode.RESISTANCE -> {
                val ohm = v * unitToFactor(unit)
                // Beta equation, referenced to the nominal resistance at 25 °C.
                val k = settings.trOhm * exp(-settings.trBeta / (273.15 + 25))
                DisplayReading(
                    value = settings.trBeta / ln(ohm / k) - 273.15,
                    unit = "°C (thermistor)",
                    detail = "R25: ${settings.trOhm} Ω, β: ${settings.trBeta}",
                    source = reading,
                )
            }

            settings.rtdMode && settings.rtdAlpha != 0.0 && reading.mode == Mode.RESISTANCE -> {
                val ohm = v * unitToFactor(unit)
                DisplayReading(
                    value = (ohm / settings.rtdOhm - 1) / settings.rtdAlpha,
                    unit = "°C (RTD)",
                    detail = "R0: ${settings.rtdOhm} Ω, α: ${settings.rtdAlpha}",
                    source = reading,
                )
            }

            else -> DisplayReading(value = v, unit = unit, detail = null, source = reading)
        }
    }

    /** Scale factor of the meter's SI prefix; "µ"/"n" never occur in the converted modes. */
    private fun unitToFactor(unitStr: String): Double = when {
        unitStr.startsWith("m") -> 1e-3
        unitStr.startsWith("k") -> 1e3
        unitStr.startsWith("M") -> 1e6
        else -> 1e0
    }
}
