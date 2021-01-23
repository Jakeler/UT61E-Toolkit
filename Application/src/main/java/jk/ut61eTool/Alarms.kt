package jk.ut61eTool;

import android.net.Uri;

import androidx.core.app.NotificationCompat;

import java.util.Arrays;

/**
 * Created by jan on 23.12.17.
 */

public class Alarms {
    LogActivity activity;

    int samples, samples_ol;
    String condition, sound;
    boolean enabled, vibration;
    double low_limit, high_limit;

    public Alarms(LogActivity a) {
        activity = a;
    }


    public boolean isAlarm(double value) {
        if (!enabled) {
            return false;
        }
        switch (condition) {
            case "both":
                if (value > high_limit || value < low_limit) samples_ol++;
                break;
            case "above":
                if (value > high_limit) samples_ol++;
                break;
            case "below":
                if (value < low_limit) samples_ol++;
        }
        if (samples > 0 && samples_ol >= samples) {
            samples_ol = 0;
            return true;
        } else {
            return false;
        }
    }

    public void alarm(String value) {
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(activity);
        mBuilder.setContentTitle("Alarm triggered!");
        int i = Arrays.asList(activity.getResources().getStringArray(R.array.alarm_condition_values)).indexOf(condition);
        mBuilder.setContentText(activity.getResources().getStringArray(R.array.alarm_conditions)[i] + ": " + value);
        mBuilder.setSmallIcon(R.drawable.ic_error_outline_black_24dp);
        if (vibration) mBuilder.setVibrate(new long[]{0, 500, 500});
        mBuilder.setAutoCancel(true);
        mBuilder.setSound(Uri.parse(sound));

        activity.mNotifyMgr.notify(17, mBuilder.build());
    }
}
