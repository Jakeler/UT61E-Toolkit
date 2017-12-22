package jk.ut61eTool

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Environment
import android.preference.PreferenceManager
import android.support.v4.app.ActivityCompat
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.View
import android.widget.*
import com.jake.UT61e_decoder
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.util.*

/**
 * Created by jan on 22.12.17.
 */

class DataLogger(val context: Context, val mNotifyMgr : NotificationManager, val fileInfo: TextView, val filename: EditText, val logRunning: ProgressBar) {

    var lineCount = 0
    var logFile = File(Environment.getExternalStorageDirectory().toString() + File.separator + context.getString(R.string.log_folder) + File.separator + filename.getText())
    var fWriter = FileWriter(logFile, true)


    private fun createFolder(): Boolean {

        val folder = File(Environment.getExternalStorageDirectory().toString() + File.separator + context.getString(R.string.log_folder))
        return if (!folder.exists()) {
            folder.mkdirs()
        } else false
    }


    fun logData(data: String) {
        try {
            fWriter.write(data + "\n")
            fWriter.flush()
            lineCount++
        } catch (e: IOException) {
            e.printStackTrace()
        }

        fileInfo.setText("Path: " + logFile.getPath() + "\n" +
                "Size: " + String.format("%.2f", logFile.length() / 1000.0) + " KB  (" + lineCount + " Data points)")
        putNotify(lineCount)

    }

    private fun putNotify(points: Int) {
        val mBuilder = NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.tile)
                .setContentTitle("Logging Running")
                .setContentText(points.toString() + " Data points")
        mBuilder.setOngoing(true)

        val resultIntent = Intent(context, LogActivity::class.java)
        resultIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        resultIntent.setAction("android.intent.action.MAIN")
        resultIntent.addCategory("android.intent.category.LAUNCHER")
        val resultPendingIntent = PendingIntent.getActivity(context, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        mBuilder.setContentIntent(resultPendingIntent)

        mNotifyMgr.notify(1, mBuilder.build())

    }

    fun startLog() {
        createFolder()
        logFile = File(Environment.getExternalStorageDirectory().toString() + File.separator + context.getString(R.string.log_folder) + File.separator + filename.getText())
        //val sw = findViewById<View>(R.id.switch1) as Switch
        try {
            fWriter = FileWriter(logFile, true)
            fWriter.write("### " + Calendar.getInstance().time.toString() + " ###\n")
            fWriter.write(UT61e_decoder.csvHeader + "\n")
            fWriter.flush()
            filename.setEnabled(false)
            logRunning.setIndeterminate(true)
            lineCount = 0
            //sw.isChecked = true
        } catch (e: IOException) {
            Toast.makeText(context, context.getString(R.string.storage_exp) + e.message, Toast.LENGTH_LONG).show()
            //sw.isChecked = false
        }

    }

    fun stopLog() {
        try {
            fWriter.flush()
            fWriter.close()
            filename.setEnabled(true)
            logRunning.setIndeterminate(false)
            mNotifyMgr.cancel(1)
        } catch (e: IOException) {
            Toast.makeText(context, context.getString(R.string.storage_exp) + e.message, Toast.LENGTH_LONG).show()
        } catch (e: NullPointerException) {
            Toast.makeText(context, context.getString(R.string.storage_exp) + e.message, Toast.LENGTH_LONG).show()
        }

    }

}

