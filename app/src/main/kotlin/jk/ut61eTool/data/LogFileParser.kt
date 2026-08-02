package jk.ut61eTool.data

/** One data row of a log file: the numeric value and the unit string next to it. */
data class LogPoint(val value: Double, val unit: String)

/** A `# <timestamp>` block of a log file. */
data class LogSession(val label: String, val points: List<LogPoint>)

/**
 * Hand-rolled, deliberately forgiving parser for the semicolon-separated logs.
 *
 * Anything whose first field is not a number is skipped, which transparently
 * handles the CSV header lines, blank lines, and the continuation lines of the
 * old multi-line thermocouple unit strings ("°C (thermocouple)\nref.: …").
 */
object LogFileParser {

    const val NO_TIMESTAMP_LABEL = "(no timestamp)"

    fun parse(lines: Sequence<String>): List<LogSession> {
        val sessions = mutableListOf<LogSession>()
        var label: String? = null
        var points = mutableListOf<LogPoint>()

        fun flush() {
            if (label != null || points.isNotEmpty()) {
                sessions += LogSession(label ?: NO_TIMESTAMP_LABEL, points)
            }
        }

        for (raw in lines) {
            val line = raw.trim()
            if (line.startsWith("#")) {
                flush()
                label = line.removePrefix("#").trim()
                points = mutableListOf()
                continue
            }
            if (line.isEmpty()) continue

            val fields = line.split(';')
            val value = fields[0].trim().toDoubleOrNull() ?: continue
            points += LogPoint(value, fields.getOrNull(1)?.trim().orEmpty())
        }
        flush()

        return sessions
    }

    fun parse(text: String): List<LogSession> = parse(text.lineSequence())
}
