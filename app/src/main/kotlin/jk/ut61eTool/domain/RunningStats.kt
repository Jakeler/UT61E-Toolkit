package jk.ut61eTool.domain

import jk.ut61eTool.data.WindowStats
import kotlin.math.abs

/**
 * Min/max/average and *mean absolute deviation* of the visible window.
 *
 * The old UI labelled the last figure "stdDev" but always computed the mean
 * deviation; the math is kept, the name fixed.
 */
fun List<Double>.windowStats(): WindowStats? {
    if (isEmpty()) return null

    var min = this[0]
    var max = this[0]
    var sum = 0.0
    for (v in this) {
        sum += v
        if (v < min) min = v
        if (v > max) max = v
    }
    val average = sum / size

    var deviationSum = 0.0
    for (v in this) deviationSum += abs(v - average)

    return WindowStats(
        min = min,
        max = max,
        average = average,
        meanDeviation = deviationSum / size,
    )
}
