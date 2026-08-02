package jk.ut61eTool.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The meter drops the link by itself on a range switch. The first retry has to be
 * quick enough that the reading comes straight back; later ones have to back off so a
 * meter that is switched off does not keep the radio busy.
 */
class ReconnectDelayTest {

    @Test
    fun `the first retry is immediate enough to be unnoticed`() {
        assertTrue(reconnectDelayMs(1) <= 500L)
    }

    @Test
    fun `the delay doubles per consecutive failure`() {
        assertEquals(500L, reconnectDelayMs(1))
        assertEquals(1_000L, reconnectDelayMs(2))
        assertEquals(2_000L, reconnectDelayMs(3))
        assertEquals(4_000L, reconnectDelayMs(4))
    }

    @Test
    fun `the delay is capped`() {
        val capped = reconnectDelayMs(5)
        for (failures in 5..MAX_RECONNECT_ATTEMPTS) {
            assertEquals(capped, reconnectDelayMs(failures))
        }
        assertTrue("$capped should stay within a few seconds", capped <= 10_000L)
    }

    @Test
    fun `giving up takes about a minute of silence`() {
        val total = (1..MAX_RECONNECT_ATTEMPTS).sumOf { reconnectDelayMs(it) }
        assertTrue("$total ms is too impatient", total >= 30_000L)
        assertTrue("$total ms keeps retrying too long", total <= 120_000L)
    }
}
