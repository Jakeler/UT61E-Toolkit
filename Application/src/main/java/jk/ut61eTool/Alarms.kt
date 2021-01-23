package jk.ut61eTool

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import java.util.*

/**
 * Created by jan on 23.12.17.
 */
class Alarms(var activity: LogActivity) {
    val CHANNEL_ID = "alarm"

    @JvmField
    var samples = 0
    var samples_ol = 0
    @JvmField
    var condition: String? = null
    @JvmField
    var enabled = false
    @JvmField
    var vibration = false
    @JvmField
    var low_limit = 0.0
    @JvmField
    var high_limit = 0.0

    init {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = activity.getString(R.string.alarm_channel_name)
            // Register the channel with the system
            val channel = NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT)
            channel.description = activity.getString(R.string.alarm_channel_name)
            activity.mNotifyMgr.createNotificationChannel(channel)
        }
    }


    fun isAlarm(value: Double): Boolean {
        if (!enabled) {
            return false
        }
        when (condition) {
            "both" -> if (value > high_limit || value < low_limit) samples_ol++
            "above" -> if (value > high_limit) samples_ol++
            "below" -> if (value < low_limit) samples_ol++
        }
        return if (samples > 0 && samples_ol >= samples) {
            samples_ol = 0
            true
        } else {
            false
        }
    }

    fun alarm(value: String) {
        Log.d("ALARM", "alarm: $value")
        val mBuilder = NotificationCompat.Builder(activity, CHANNEL_ID)
        mBuilder.setContentTitle("Alarm triggered!")
        val i = Arrays.asList(*activity.resources.getStringArray(R.array.alarm_condition_values)).indexOf(condition)
        mBuilder.setContentText(activity.resources.getStringArray(R.array.alarm_conditions)[i].toString() + ": " + value)
        mBuilder.setSmallIcon(R.drawable.ic_error_outline_black_24dp)
        mBuilder.setAutoCancel(true)
        mBuilder.priority = NotificationCompat.PRIORITY_DEFAULT
        if (vibration) mBuilder.setVibrate(longArrayOf(0, 500, 500))
        activity.mNotifyMgr.notify(0, mBuilder.build())
    }
}