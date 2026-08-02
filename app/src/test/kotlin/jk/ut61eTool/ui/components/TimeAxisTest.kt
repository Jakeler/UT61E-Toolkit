package jk.ut61eTool.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The x axis used to place several labels inside the same second, which rendered
 * as duplicates ("0s 0s -2s -2s"). Labels must land on distinct whole seconds.
 */
class TimeAxisTest {

    private fun labels(rate: Double?, size: Int): List<String> {
        val spacing = labelSpacing(rate, size)
        return (0 until size step spacing).map { xAxisLabel(it.toDouble(), rate, size - 1) }
    }

    @Test
    fun `labels are distinct at typical meter rates`() {
        for (rate in listOf(0.5, 1.0, 2.0, 2.5, 5.0, 20.0)) {
            for (size in listOf(10, 60, 100, 500)) {
                val l = labels(rate, size)
                assertEquals("rate=$rate size=$size -> $l", l.size, l.distinct().size)
            }
        }
    }

    @Test
    fun `the newest sample is at zero seconds`() {
        assertEquals("0s", xAxisLabel(99.0, 2.0, 99))
        assertEquals("-5s", xAxisLabel(89.0, 2.0, 99))
    }

    @Test
    fun `without a rate the label is the sample index`() {
        assertEquals("42", xAxisLabel(42.0, null, 99))
        assertEquals("42", xAxisLabel(42.0, 0.0, 99))
    }

    @Test
    fun `spacing is a whole number of seconds, never below one sample`() {
        // At 2 Hz a second is two samples; below 1 Hz one sample already spans more.
        assertEquals(2, labelSpacing(2.0, 1))
        assertEquals(1, labelSpacing(0.5, 4))
        assertTrue(labelSpacing(null, 1) >= 1)
        assertTrue(labelSpacing(20.0, 0) >= 1)
    }
}
