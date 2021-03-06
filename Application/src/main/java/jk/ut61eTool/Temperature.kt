package jk.ut61eTool

import java.io.InputStreamReader

data class Temperature(
        val id: Int,
        val name: String,
        val value: Double,
)

fun getSensorCount(): Int {
    var text: String
    Runtime.getRuntime().exec(arrayOf("sh", "-c", "echo /sys/class/thermal/thermal_zone* | wc -w")).let { process ->
        process.waitFor()
        text = InputStreamReader(process.inputStream).use { it.readText() }
        process.destroy()
    }
    return text.trim().toInt()
}

fun getAllTemps(count: Int): List<Temperature> {
    val dirs = (0 until count).joinToString(separator = " "){ i ->
        "/sys/class/thermal/thermal_zone$i"
    }
    var lines: List<String>
    val cmd = arrayOf("sh", "-c", "for dir in $dirs; do echo $(cat \$dir/type) $(cat \$dir/temp); done ")
    Runtime.getRuntime().exec(cmd).let { process ->
        process.waitFor()
        lines = InputStreamReader(process.inputStream).use { it.readLines() }
        process.destroy()
    }
    val temps = lines.mapIndexed {index: Int, s: String ->
        val tokens = s.split(" ")
        val name = tokens[0]
        val value = tokens[1].toDouble()
        Temperature(index, name, when {
            (value > -40 && value < 85) -> value // Industrial temp range, already valid temp
            (value > -1000 && value < 1000) -> value/10 // range 100-1000
            (value > -10_000 && value < 10_000) -> value/100 // range 1_000-10_000
            (value > -100_000 && value < 100_000) -> value/1000 // range 10_000-100_000
            (value > -1_000_000 && value < 1_000_000) -> value/10000 // range 100_000-1_000_000
            else -> Double.NaN
        })
    }
    return temps
}