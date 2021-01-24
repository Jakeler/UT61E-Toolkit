package jk.ut61eTool

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import com.jake.UT61e_decoder
import kotlinx.android.synthetic.main.log_activity.*
import java.io.FileWriter
import java.io.IOException
import java.util.*


class DataLogger(private val context: LogActivity) {
    val CHANNEL_ID = "log"

    private var fWriter: FileWriter? = null
    private lateinit var logFile: DocumentFile
    lateinit var log_dir : String

    private var filename: EditText = context.filename
    private var fileInfo: TextView = context.fileInfo
    private var logRunning: ProgressBar = context.logRunning
    private val switch = context.switch1
    @JvmField var lineCount = 0

    init {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.log_channel_name)
            // Register the channel with the system
            val channel = NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_LOW)
            channel.description = context.getString(R.string.log_channel_name)
            context.mNotifyMgr.createNotificationChannel(channel)
        }
    }

    private fun putLogNotify() {
        val mBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.tile)
                .setContentTitle("Logging Running")
                .setContentText("${this.lineCount} Data points")

        val resultIntent = Intent(context, LogActivity::class.java)
        resultIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        resultIntent.action = "android.intent.action.MAIN"
        resultIntent.addCategory("android.intent.category.LAUNCHER")
        val resultPendingIntent = PendingIntent.getActivity(context, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        mBuilder.setContentIntent(resultPendingIntent)
        context.mNotifyMgr.notify(42, mBuilder.build())
    }



    fun startLog() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(context, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 7)
            setRunning(false)
            return
        }

        val uri = Uri.parse(log_dir)
        val dirFile = DocumentFile.fromTreeUri(context, uri) ?: return
        logFile = dirFile.createFile("text/csv", filename.text.toString()) ?: return
        val fd = context.contentResolver.openFileDescriptor(logFile.uri, "w")

        try {
            fWriter = FileWriter(fd?.fileDescriptor)
            fWriter?.write("# " + Calendar.getInstance().time.toString() + "\n")
            fWriter?.write(UT61e_decoder.csvHeader + "\n")
            fWriter?.flush()
            setRunning(true)
            lineCount = 0
        } catch (e: IOException) {
            Toast.makeText(context, context.getString(R.string.storage_exp) + e.message, Toast.LENGTH_LONG).show()
            setRunning(false)
        }

    }

    fun stopLog() {
        if (fWriter == null) return
        try {
            fWriter?.flush()
            fWriter?.close()
            fWriter = null
            setRunning(false)
            context.mNotifyMgr.cancel(42)
        } catch (e: IOException) {
            Toast.makeText(context, context.getString(R.string.storage_exp) + e.message, Toast.LENGTH_LONG).show()
        }
    }

    private fun setRunning(running: Boolean) {
        filename.isEnabled = !running
        logRunning.isIndeterminate = running
        switch.isChecked = running
    }

    fun isRunning(): Boolean {
        return fWriter != null
    }

    fun logData(data: String) {
        if (!isRunning()) return

        try {
            fWriter?.write(data + "\n")
            fWriter?.flush()
            lineCount++
        } catch (e: IOException) {
            Toast.makeText(context, context.getString(R.string.storage_exp) + e.message, Toast.LENGTH_LONG).show()
        }
        fileInfo.text = context.getString(R.string.logfile_info, logFile.name, logFile.length()/1000.0, lineCount)

        putLogNotify()
    }
}