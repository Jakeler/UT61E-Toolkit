package jk.ut61eTool.domain

import jk.ut61eTool.data.AlarmCondition
import jk.ut61eTool.data.Settings

/**
 * Fires once every time [Settings.alarmSamples] consecutive samples are out of
 * range. The counter is reset both when the alarm fires and when a sample is
 * back in range, so a single spike never triggers.
 */
class AlarmEvaluator {

    private var outOfRangeCount = 0

    fun evaluate(value: Double, settings: Settings): Boolean {
        if (!settings.alarmEnabled) return false

        val outOfRange = when (settings.alarmCondition) {
            AlarmCondition.BOTH -> value > settings.highLimit || value < settings.lowLimit
            AlarmCondition.ABOVE -> value > settings.highLimit
            AlarmCondition.BELOW -> value < settings.lowLimit
        }

        if (outOfRange) outOfRangeCount++ else outOfRangeCount = 0

        return if (settings.alarmSamples > 0 && outOfRangeCount >= settings.alarmSamples) {
            outOfRangeCount = 0
            true
        } else {
            false
        }
    }

    fun reset() {
        outOfRangeCount = 0
    }
}
