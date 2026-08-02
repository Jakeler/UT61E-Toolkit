package jk.ut61eTool.domain

import com.jake.ut61e.Ut61eReading

/**
 * A decoded reading after user-configured post-processing (shunt, thermocouple,
 * thermistor, RTD). [value]/[unit] are what the UI shows and the CSV records;
 * [detail] carries extra info (e.g. the cold-junction reference) for the UI only.
 */
data class DisplayReading(
    val value: Double,
    val unit: String,
    val detail: String? = null,
    val source: Ut61eReading,
) {
    val isConverted: Boolean get() = unit != source.unit

    /** CSV line in the historical format, using the post-processed value/unit. */
    fun toCsvLine(): String {
        val sourceLine = source.toCsvLine().split(';')
        return "${"%.4f".format(java.util.Locale.US, value)};$unit;${sourceLine[2]};${sourceLine[3]}"
    }
}
