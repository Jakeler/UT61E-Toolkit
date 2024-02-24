package jk.ut61eTool

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.documentfile.provider.DocumentFile
import com.jake.UT61e_decoder
import java.io.IOException
import java.io.OutputStreamWriter
import java.util.Calendar


class DataLogger(private val context: LogActivity) {
    val CHANNEL_ID = "log"

    private var fWriter: OutputStreamWriter? = null
    private lateinit var logFile: DocumentFile
    lateinit var log_dir : String
    var reuseLogfile = true

    private var filename: EditText = context.binding.filename
    private var fileInfo: TextView = context.binding.fileInfo
    private var logRunning: ProgressBar = context.binding.logRunning
    private val switch = context.binding.logSwitch
    private var lineCount = 0

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
        val resultPendingIntent = PendingIntent.getActivity(context, 0, resultIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        mBuilder.setContentIntent(resultPendingIntent)
        context.mNotifyMgr.notify(42, mBuilder.build())
    }



    fun startLog() {
        val uri = Uri.parse(log_dir)
        if (uri.scheme == null) {
            handleErr(context.getString(R.string.error_folder_not_setup))
            return
        }
        try {
            val dirFile = DocumentFile.fromTreeUri(context, uri)
                    ?: throw IOException(context.getString(R.string.error_folder_inacc))
            // Open file if it exists, respect setting
            val dfile = if (reuseLogfile) dirFile.findFile(filename.text.toString()) else null
            logFile = dfile // Create new if not existing, throw exception if null
                    ?: (dirFile.createFile("text/csv", filename.text.toString())
                            ?: throw IOException(context.getString(R.string.error_folder_inacc)))

            fWriter = context.contentResolver.openOutputStream(logFile.uri, "wa").let {
                it?.writer()
            }

            fWriter?.run {
                write("# " + Calendar.getInstance().time.toString() + "\n")
                write(UT61e_decoder.csvHeader + "\n")
                flush()

                setRunning(true)
            }
            lineCount = 0
        } catch (e: Exception) {
            handleErr(e)
        }
    }

    fun stopLog() {
        if (fWriter == null) return
        try {
            fWriter?.close()
            fWriter = null
            setRunning(false)
            context.mNotifyMgr.cancel(42)
        } catch (e: Exception) {
            handleErr(e)
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

    fun handleErr(err: Any) {
        if (err is String) {
            Toast.makeText(context, err, Toast.LENGTH_LONG).show()
            Log.e("DataLogger", "handleErr: $err")
        } else if (err is Exception) {
            Toast.makeText(context, context.getString(R.string.storage_exp, err.message), Toast.LENGTH_LONG).show()
            Log.e("DataLogger", "handleErr: $err")
        }
        setRunning(false)
        fWriter = null
    }

    fun logData(data: String) {
        if (!isRunning()) return

        try {
            fWriter?.write(data + "\n")
            fWriter?.flush()
            lineCount++
        } catch (e: IOException) {
            handleErr(e)

            // Retry to open file, does not get reached again because of isRunning
            startLog()
            logData(data)
        }
        fileInfo.text = context.getString(R.string.logfile_info, logFile.uri.lastPathSegment, logFile.length()/1000.0, lineCount)

        putLogNotify()
    }
}