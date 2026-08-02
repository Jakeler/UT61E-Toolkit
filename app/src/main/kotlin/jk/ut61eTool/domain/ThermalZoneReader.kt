package jk.ut61eTool.domain

import java.io.File

data class Temperature(
    val id: Int,
    val name: String,
    val celsius: Double,
)

/**
 * Reads the kernel thermal zones directly from sysfs (the legacy version shelled
 * out to `sh -c cat`, which is both slow and blocked on modern Android).
 */
class ThermalZoneReader(private val root: File = File(DEFAULT_ROOT)) {

    fun listZones(): List<Temperature> {
        val zones = root.listFiles { f -> f.name.startsWith(ZONE_PREFIX) } ?: return emptyList()
        return zones
            .mapNotNull { dir -> dir.name.removePrefix(ZONE_PREFIX).toIntOrNull() }
            .sorted()
            .map { getTempById(it) }
    }

    fun getTempById(id: Int): Temperature {
        val dir = File(root, "$ZONE_PREFIX$id")
        return try {
            val name = File(dir, "type").readText().trim()
            val raw = File(dir, "temp").readText().trim().toDouble()
            Temperature(id, name.ifEmpty { UNKNOWN }, normalize(raw))
        } catch (_: Exception) {
            Temperature(id, UNKNOWN, Double.NaN)
        }
    }

    /**
     * Zones report their value in °C, d°C, c°C or m°C depending on the SoC and
     * there is no reliable way to tell them apart, so fall back to the legacy
     * heuristic: scale down until the value lands in a plausible range.
     */
    private fun normalize(value: Double): Double = when {
        value > -40 && value < 85 -> value // Industrial temp range, already a valid temperature
        value > -1_000 && value < 1_000 -> value / 10
        value > -10_000 && value < 10_000 -> value / 100
        value > -100_000 && value < 100_000 -> value / 1_000
        value > -1_000_000 && value < 1_000_000 -> value / 10_000
        else -> Double.NaN
    }

    companion object {
        const val DEFAULT_ROOT = "/sys/class/thermal"
        private const val ZONE_PREFIX = "thermal_zone"
        private const val UNKNOWN = "unknown"
    }
}
