package jk.ut61eTool.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ThermalZoneReaderTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun zone(id: Int, type: String, temp: String) {
        val dir = File(tmp.root, "thermal_zone$id")
        dir.mkdirs()
        File(dir, "type").writeText("$type\n")
        File(dir, "temp").writeText("$temp\n")
    }

    @Test
    fun `lists zones sorted by id`() {
        zone(0, "cpu-therm", "45000")
        zone(2, "battery", "31.0")
        zone(1, "gpu-therm", "412")

        val zones = ThermalZoneReader(tmp.root).listZones()
        assertEquals(listOf(0, 1, 2), zones.map { it.id })
        assertEquals(listOf("cpu-therm", "gpu-therm", "battery"), zones.map { it.name })
    }

    @Test
    fun `normalizes the reported magnitude`() {
        zone(0, "a", "31")      // already celsius
        zone(1, "b", "412")     // deci-celsius
        zone(2, "c", "4500")    // centi-celsius
        zone(3, "d", "45000")   // milli-celsius
        zone(4, "e", "450000")  // 1e-4 celsius
        zone(5, "f", "45000000") // implausible

        val r = ThermalZoneReader(tmp.root)
        assertEquals(31.0, r.getTempById(0).celsius, 1e-9)
        assertEquals(41.2, r.getTempById(1).celsius, 1e-9)
        assertEquals(45.0, r.getTempById(2).celsius, 1e-9)
        assertEquals(45.0, r.getTempById(3).celsius, 1e-9)
        assertEquals(45.0, r.getTempById(4).celsius, 1e-9)
        assertTrue(r.getTempById(5).celsius.isNaN())
    }

    @Test
    fun `missing zone falls back to unknown`() {
        val t = ThermalZoneReader(tmp.root).getTempById(7)
        assertEquals(7, t.id)
        assertEquals("unknown", t.name)
        assertTrue(t.celsius.isNaN())
    }

    @Test
    fun `unparseable temp falls back to unknown`() {
        val dir = File(tmp.root, "thermal_zone0")
        dir.mkdirs()
        File(dir, "type").writeText("cpu\n")
        File(dir, "temp").writeText("n/a\n")
        assertTrue(ThermalZoneReader(tmp.root).getTempById(0).celsius.isNaN())
    }

    @Test
    fun `missing root yields no zones`() {
        assertEquals(emptyList<Temperature>(), ThermalZoneReader(File(tmp.root, "nope")).listZones())
    }
}
