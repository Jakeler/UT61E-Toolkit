package jk.ut61eTool.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RunningStatsTest {

    @Test
    fun `empty window has no stats`() {
        assertNull(emptyList<Double>().windowStats())
    }

    @Test
    fun `single sample`() {
        val s = listOf(4.2).windowStats()!!
        assertEquals(4.2, s.min, 1e-12)
        assertEquals(4.2, s.max, 1e-12)
        assertEquals(4.2, s.average, 1e-12)
        assertEquals(0.0, s.meanDeviation, 1e-12)
    }

    @Test
    fun `mean absolute deviation not standard deviation`() {
        // avg = 3.0; |dev| = 2,1,0,1,2 -> 6/5 = 1.2 (stdDev would be ~1.414)
        val s = listOf(1.0, 2.0, 3.0, 4.0, 5.0).windowStats()!!
        assertEquals(1.0, s.min, 1e-12)
        assertEquals(5.0, s.max, 1e-12)
        assertEquals(3.0, s.average, 1e-12)
        assertEquals(1.2, s.meanDeviation, 1e-12)
    }

    @Test
    fun `handles negative values`() {
        val s = listOf(-5.0, 5.0).windowStats()!!
        assertEquals(-5.0, s.min, 1e-12)
        assertEquals(5.0, s.max, 1e-12)
        assertEquals(0.0, s.average, 1e-12)
        assertEquals(5.0, s.meanDeviation, 1e-12)
    }
}
