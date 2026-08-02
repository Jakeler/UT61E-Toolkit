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
    fun `the user is warned well before the retries run out`() {
        assertTrue(LINK_LOST_WARNING_ATTEMPTS in 2 until MAX_RECONNECT_ATTEMPTS)

        // A range switch costs one attempt, and must never raise the alarm.
        assertTrue(LINK_LOST_WARNING_ATTEMPTS > 1)

        val untilWarning = (1 until LINK_LOST_WARNING_ATTEMPTS).sumOf { reconnectDelayMs(it) }
        assertTrue("warns after only $untilWarning ms of backoff", untilWarning >= 3_000L)
    }

    @Test
    fun `the backoff alone adds about a minute before giving up`() {
        // Wall-clock time to give up is this plus the attempts themselves, each of
        // which can sit in the stack's connect timeout for tens of seconds.
        val total = (1..MAX_RECONNECT_ATTEMPTS).sumOf { reconnectDelayMs(it) }
        assertTrue("$total ms is too impatient", total >= 30_000L)
        assertTrue("$total ms keeps retrying too long", total <= 120_000L)
    }
}
