package jk.ut61eTool.ui.live

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SampleRateTest {

    private fun ratesFrom(raw: List<Double>): List<Double?> {
        var shown: Double? = null
        return raw.map { shown = settleRate(shown, it); shown }
    }

    /** Regression: jitter around 2 Hz reshuffled the whole time axis every sample. */
    @Test
    fun `jitter around the current rate does not move it`() {
        val jitter = listOf(2.00, 1.98, 2.03, 1.96, 2.05, 1.99, 2.02)
        assertEquals(List(jitter.size) { 2.0 }, ratesFrom(jitter))
    }

    @Test
    fun `a real change is adopted`() {
        val rates = ratesFrom(listOf(2.0, 2.01, 5.02, 5.0))
        assertEquals(2.0, rates[1])
        assertEquals(5.0, rates[2])
        assertEquals(5.0, rates[3])
    }

    @Test
    fun `the first measurement is quantised to tenths`() {
        assertEquals(2.0, settleRate(null, 1.9734))
        assertEquals(0.5, settleRate(null, 0.512))
    }

    @Test
    fun `a missing measurement keeps the last rate`() {
        assertEquals(2.0, settleRate(2.0, null))
        assertNull(settleRate(null, null))
        assertEquals(2.0, settleRate(2.0, 0.0))
    }

    @Test
    fun `rate needs at least two timestamps`() {
        assertNull(measureRate(emptyList()))
        assertNull(measureRate(listOf(1_000L)))
        assertNull(measureRate(listOf(5L, 5L)))
    }

    @Test
    fun `rate is samples per second over the span`() {
        // Five arrivals, 500 ms apart -> 2 Hz.
        val times = (0..4).map { it * 500_000_000L }
        assertEquals(2.0, measureRate(times)!!, 1e-9)
    }
}
