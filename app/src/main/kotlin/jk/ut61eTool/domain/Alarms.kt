package jk.ut61eTool.domain

import jk.ut61eTool.data.AlarmCondition
import jk.ut61eTool.data.Settings

/**
 * Reports whether the alarm condition currently holds: true from the moment
 * [Settings.alarmSamples] consecutive samples are out of range, and staying true
 * until one comes back in range. A single spike never triggers.
 *
 * This is a level, not an edge — consuming it as a one-shot made the alarm flash
 * on and off while the value simply stayed over the limit.
 */
class AlarmEvaluator {

    private var outOfRangeCount = 0

    fun evaluate(value: Double, settings: Settings): Boolean {
        if (!settings.alarmEnabled) {
            outOfRangeCount = 0
            return false
        }

        val outOfRange = when (settings.alarmCondition) {
            AlarmCondition.BOTH -> value > settings.highLimit || value < settings.lowLimit
            AlarmCondition.ABOVE -> value > settings.highLimit
            AlarmCondition.BELOW -> value < settings.lowLimit
        }

        if (!outOfRange) {
            outOfRangeCount = 0
            return false
        }

        outOfRangeCount++
        return settings.alarmSamples > 0 && outOfRangeCount >= settings.alarmSamples
    }

    fun reset() {
        outOfRangeCount = 0
    }
}
