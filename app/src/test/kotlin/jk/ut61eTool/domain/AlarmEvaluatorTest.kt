package jk.ut61eTool.domain

import jk.ut61eTool.data.AlarmCondition
import jk.ut61eTool.data.Settings
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmEvaluatorTest {

    private val above = Settings(
        alarmEnabled = true,
        alarmCondition = AlarmCondition.ABOVE,
        alarmSamples = 3,
        highLimit = 10.0,
        lowLimit = 0.0,
    )

    @Test
    fun `disabled never fires`() {
        val e = AlarmEvaluator()
        repeat(10) { assertFalse(e.evaluate(1000.0, above.copy(alarmEnabled = false))) }
    }

    @Test
    fun `needs n consecutive out of range samples`() {
        val e = AlarmEvaluator()
        assertFalse(e.evaluate(11.0, above))
        assertFalse(e.evaluate(11.0, above))
        assertTrue(e.evaluate(11.0, above))
    }

    @Test
    fun `counter resets on an in-range sample`() {
        val e = AlarmEvaluator()
        assertFalse(e.evaluate(11.0, above))
        assertFalse(e.evaluate(11.0, above))
        assertFalse(e.evaluate(5.0, above))
        assertFalse(e.evaluate(11.0, above))
        assertFalse(e.evaluate(11.0, above))
        assertTrue(e.evaluate(11.0, above))
    }

    @Test
    fun `fires again after the counter is consumed`() {
        val e = AlarmEvaluator()
        repeat(3) { e.evaluate(11.0, above) }
        assertFalse(e.evaluate(11.0, above))
        assertFalse(e.evaluate(11.0, above))
        assertTrue(e.evaluate(11.0, above))
    }

    @Test
    fun `below condition`() {
        val s = above.copy(alarmCondition = AlarmCondition.BELOW, lowLimit = 1.0, alarmSamples = 2)
        val e = AlarmEvaluator()
        assertFalse(e.evaluate(100.0, s))
        assertFalse(e.evaluate(0.5, s))
        assertTrue(e.evaluate(0.5, s))
    }

    @Test
    fun `both condition triggers on either side`() {
        val s = above.copy(alarmCondition = AlarmCondition.BOTH, lowLimit = 1.0, highLimit = 10.0, alarmSamples = 2)
        val e = AlarmEvaluator()
        assertFalse(e.evaluate(0.0, s))
        assertTrue(e.evaluate(20.0, s))
    }

    @Test
    fun `reset clears the counter`() {
        val e = AlarmEvaluator()
        e.evaluate(11.0, above)
        e.evaluate(11.0, above)
        e.reset()
        assertFalse(e.evaluate(11.0, above))
    }

    @Test
    fun `zero samples never fires`() {
        val e = AlarmEvaluator()
        val s = above.copy(alarmSamples = 0)
        repeat(10) { assertFalse(e.evaluate(11.0, s)) }
    }
}
