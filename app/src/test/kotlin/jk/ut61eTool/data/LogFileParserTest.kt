package jk.ut61eTool.data

import org.junit.Assert.assertEquals
import org.junit.Test

class LogFileParserTest {

    private val fixture = """
        # Sat Dec 23 12:00:00 GMT+01:00 2017
        Value;Unit;Type;Overloaded
        1.2340;V;DC;
        -0.0010;V;DC;
        0.0000;V;DC;OL

        # Sun Dec 24 09:30:00 GMT+01:00 2017
        Value;Unit;Type;Overloaded
        45.6410;°C (thermocouple)
        ref.: cpu @ 30.0°C;DC;
        231.0000;V;DC;
        0.0000;V;AC;UL
    """.trimIndent()

    @Test
    fun `parses two sessions with their labels`() {
        val sessions = LogFileParser.parse(fixture)
        assertEquals(2, sessions.size)
        assertEquals("Sat Dec 23 12:00:00 GMT+01:00 2017", sessions[0].label)
        assertEquals("Sun Dec 24 09:30:00 GMT+01:00 2017", sessions[1].label)
    }

    @Test
    fun `header and blank lines are skipped`() {
        val s = LogFileParser.parse(fixture)[0]
        assertEquals(listOf(1.234, -0.001, 0.0), s.points.map { it.value })
        assertEquals(listOf("V", "V", "V"), s.points.map { it.unit })
    }

    @Test
    fun `multi-line thermocouple unit continuation line is dropped`() {
        val s = LogFileParser.parse(fixture)[1]
        // 45.641 is kept (its unit field is the truncated first line), the
        // "ref.: …" continuation has no numeric first field and is skipped.
        assertEquals(listOf(45.641, 231.0, 0.0), s.points.map { it.value })
        assertEquals("°C (thermocouple)", s.points[0].unit)
    }

    @Test
    fun `rows before the first session get a placeholder label`() {
        val sessions = LogFileParser.parse("1.0;V;DC;\n# later\n2.0;V;DC;")
        assertEquals(2, sessions.size)
        assertEquals(LogFileParser.NO_TIMESTAMP_LABEL, sessions[0].label)
        assertEquals(listOf(1.0), sessions[0].points.map { it.value })
        assertEquals("later", sessions[1].label)
    }

    @Test
    fun `an empty session is still reported`() {
        val sessions = LogFileParser.parse("# empty run\nValue;Unit;Type;Overloaded\n")
        assertEquals(1, sessions.size)
        assertEquals(emptyList<LogPoint>(), sessions[0].points)
    }

    @Test
    fun `empty input yields no sessions`() {
        assertEquals(emptyList<LogSession>(), LogFileParser.parse(""))
    }
}
