package jk.ut61eTool

import android.util.Log
import java.io.InputStreamReader

data class Temperature(
        val id: Int,
        val name: String,
        val celsius: Double,
)

object TemperatureReader {
    const val basePath = "/sys/class/thermal/thermal_zone"

    private fun runShellForOutput(cmd: Array<String>): List<String> {
        var lines: List<String>
        Runtime.getRuntime().exec(cmd).let { process ->
            process.waitFor()
            lines = InputStreamReader(process.inputStream).use { it.readLines() }
            process.destroy()
        }
        Log.d("TemperatureReader", "runShellForOutput: ${lines.joinToString(";")}")
        return lines
    }

    fun getSensorCount(): Int {
        val cmd = arrayOf("sh", "-c", "echo $basePath* | wc -w")
        runShellForOutput(cmd).let {
            return it[0].trim().toInt()
        }
    }

    fun getAllTemps(count: Int = getSensorCount()): List<Temperature> {
        val dirs = (0 until count).joinToString(separator = " "){ i ->
            "$basePath$i"
        }
        val cmd = arrayOf("sh", "-c", "for dir in $dirs; do echo $(cat \$dir/type) $(cat \$dir/temp); done ")
        runShellForOutput(cmd).let {
            return it.mapIndexed(::lineToTemp)
        }
    }

    fun getTempByID(id: Int): Temperature {
        val dir = "$basePath$id"
        runShellForOutput(arrayOf("sh", "-c", "echo $(cat $dir/type) $(cat $dir/temp)")).let {
            return lineToTemp(id, it[0])
        }
    }

    private fun lineToTemp(index: Int, s: String): Temperature {
        val tokens = s.split(" ")
        if (tokens.size < 2) {
            return Temperature(index, "NOT FOUND", Double.NaN)
        }
        val name = tokens[0]
        val value = tokens[1].toDouble()
        return Temperature(index, name, when {
            (value > -40 && value < 85) -> value // Industrial temp range, already valid temp
            (value > -1000 && value < 1000) -> value/10 // range 100-1000
            (value > -10_000 && value < 10_000) -> value/100 // range 1_000-10_000
            (value > -100_000 && value < 100_000) -> value/1000 // range 10_000-100_000
            (value > -1_000_000 && value < 1_000_000) -> value/10000 // range 100_000-1_000_000
            else -> Double.NaN
        })
    }
}
