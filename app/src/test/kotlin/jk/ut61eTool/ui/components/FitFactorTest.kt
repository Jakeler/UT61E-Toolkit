package jk.ut61eTool.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * At a large display zoom the readout was clipped to `-4.` instead of `-4.9312`.
 * The fit factor is what keeps every digit on screen.
 */
class FitFactorTest {

    @Test
    fun `content that fits keeps its size`() {
        assertEquals(1f, fitFactor(required = 300f, available = 400), 0f)
        assertEquals(1f, fitFactor(required = 400f, available = 400), 0f)
    }

    @Test
    fun `content that does not fit is scaled below the ratio`() {
        val factor = fitFactor(required = 800f, available = 400)
        assertTrue("$factor should leave a margin under 0.5", factor < 0.5f)
        assertTrue("$factor should stay close to 0.5", factor > 0.45f)
    }

    @Test
    fun `an unmeasurable line is left alone`() {
        assertEquals(1f, fitFactor(required = 0f, available = 400), 0f)
        assertEquals(1f, fitFactor(required = -1f, available = 400), 0f)
    }

    @Test
    fun `the scaled content actually fits`() {
        for (required in listOf(401f, 500f, 1200f, 5000f)) {
            val fitted = required * fitFactor(required, available = 400)
            assertTrue("required=$required fitted to $fitted", fitted <= 400f)
        }
    }
}
