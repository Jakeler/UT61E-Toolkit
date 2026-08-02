package jk.ut61eTool.domain

import com.jake.ut61e.Mode
import com.jake.ut61e.Ut61eReading
import jk.ut61eTool.data.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

internal fun reading(
    value: Double,
    unit: String,
    mode: Mode,
    isOverload: Boolean = false,
    isUnderload: Boolean = false,
): Ut61eReading = Ut61eReading(
    value = value,
    unit = unit,
    mode = mode,
    rangeIndex = 0,
    isAC = false,
    isDC = true,
    isAuto = true,
    isNegative = value < 0,
    isOverload = isOverload,
    isUnderload = isUnderload,
    isLowBattery = false,
    raw = ByteArray(0),
)

class ConverterTest {

    private val fixedZone = ColdJunctionProvider { id -> Temperature(id, "cpu", 30.0) }
    private val converter = Converter(fixedZone)

    @Test
    fun `passthrough when no conversion enabled`() {
        val r = converter.convert(reading(1.234, "V", Mode.VOLTAGE), Settings())
        assertEquals(1.234, r.value, 1e-12)
        assertEquals("V", r.unit)
        assertFalse(r.isConverted)
    }

    @Test
    fun `shunt converts volts to amps`() {
        val settings = Settings(shuntMode = true, shuntOhm = 0.001)
        val r = converter.convert(reading(0.1, "V", Mode.VOLTAGE), settings)
        assertEquals(100.0, r.value, 1e-9)
        assertEquals("A (ext. shunt)", r.unit)
    }

    @Test
    fun `shunt honours the millivolt prefix`() {
        val settings = Settings(shuntMode = true, shuntOhm = 0.001)
        val r = converter.convert(reading(100.0, "mV", Mode.VOLTAGE), settings)
        assertEquals(100.0, r.value, 1e-9)
    }

    @Test
    fun `shunt is skipped when resistance is zero`() {
        val settings = Settings(shuntMode = true, shuntOhm = 0.0)
        val r = converter.convert(reading(0.1, "V", Mode.VOLTAGE), settings)
        assertEquals("V", r.unit)
    }

    @Test
    fun `thermocouple with constant reference`() {
        val settings = Settings(
            tcMode = true,
            tcSensitivity = 0.039,
            tcReferenceId = -1,
            tcReferenceConstant = 20.0,
        )
        val r = converter.convert(reading(1.0, "mV", Mode.VOLTAGE), settings)
        assertEquals(1.0 / 0.039 + 20.0, r.value, 1e-9)
        assertEquals("°C (thermocouple)", r.unit)
        assertEquals("ref.: constant @ 20.0°C", r.detail)
    }

    @Test
    fun `thermocouple with thermal zone reference`() {
        val settings = Settings(tcMode = true, tcSensitivity = 0.039, tcReferenceId = 3)
        val r = converter.convert(reading(1.0, "mV", Mode.VOLTAGE), settings)
        assertEquals(1.0 / 0.039 + 30.0, r.value, 1e-9)
        assertEquals("ref.: cpu @ 30.0°C", r.detail)
    }

    @Test
    fun `thermistor beta equation at nominal resistance yields 25 celsius`() {
        val settings = Settings(trMode = true, trOhm = 100_000.0, trBeta = 4092.0)
        val r = converter.convert(reading(100.0, "kΩ", Mode.RESISTANCE), settings)
        assertEquals(25.0, r.value, 1e-9)
        assertEquals("°C (thermistor)", r.unit)
    }

    @Test
    fun `rtd alpha equation`() {
        val settings = Settings(rtdMode = true, rtdOhm = 1000.0, rtdAlpha = 0.00385)
        val r = converter.convert(reading(1385.0, "Ω", Mode.RESISTANCE), settings)
        assertEquals(100.0, r.value, 1e-9)
        assertEquals("°C (RTD)", r.unit)
    }

    @Test
    fun `thermistor wins over rtd when both enabled`() {
        val settings = Settings(
            trMode = true, trOhm = 100_000.0, trBeta = 4092.0,
            rtdMode = true, rtdOhm = 1000.0, rtdAlpha = 0.00385,
        )
        val r = converter.convert(reading(100.0, "kΩ", Mode.RESISTANCE), settings)
        assertEquals("°C (thermistor)", r.unit)
    }

    @Test
    fun `overload is ignored only when the setting is on`() {
        val ol = reading(0.0, "V", Mode.VOLTAGE, isOverload = true)
        assertFalse(converter.isIgnored(ol, Settings()))
        assertTrue(converter.isIgnored(ol, Settings(ignoreOverload = true)))
        val ul = reading(0.0, "V", Mode.VOLTAGE, isUnderload = true)
        assertTrue(converter.isIgnored(ul, Settings(ignoreOverload = true)))
        assertFalse(converter.isIgnored(reading(1.0, "V", Mode.VOLTAGE), Settings(ignoreOverload = true)))
    }
}
