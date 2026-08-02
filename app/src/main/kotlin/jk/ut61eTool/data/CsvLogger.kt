package jk.ut61eTool.data

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.jake.ut61e.Ut61eReading
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStreamWriter
import java.util.Calendar

/**
 * Appends CSV lines to a file in the user-picked SAF tree, exactly like the
 * legacy DataLogger: append mode, one session comment line plus a header on
 * start, and a flush after every single line so an unexpected kill loses
 * nothing.
 */
class CsvLogger(private val context: Context) {

    private val _state = MutableStateFlow(LoggingState())
    val state: StateFlow<LoggingState> = _state.asStateFlow()

    private val mutex = Mutex()

    private var writer: OutputStreamWriter? = null
    private var logFile: DocumentFile? = null

    // Remembered so a failed write can reopen the same target once.
    private var treeUri: String? = null
    private var fileName: String? = null
    private var reuseExisting: Boolean = true

    suspend fun start(treeUri: String, fileName: String, reuseExisting: Boolean): Result<Unit> =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                this@CsvLogger.treeUri = treeUri
                this@CsvLogger.fileName = fileName
                this@CsvLogger.reuseExisting = reuseExisting
                closeQuietly()
                runCatching { open(treeUri, fileName, reuseExisting) }
                    .onSuccess {
                        _state.value = LoggingState(
                            running = true,
                            fileName = logFile?.name ?: fileName,
                            fileSizeBytes = logFile?.length() ?: 0L,
                            lineCount = 0,
                        )
                    }
                    .onFailure { e ->
                        closeQuietly()
                        _state.value = LoggingState(error = e.message ?: e.toString())
                    }
            }
        }

    suspend fun log(line: String) {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                if (writer == null) return@withLock
                try {
                    writeLine(line)
                } catch (e: IOException) {
                    // One reopen + retry, mirroring the legacy retry-once behaviour.
                    _state.value = _state.value.copy(error = e.message ?: e.toString())
                    val tree = treeUri
                    val name = fileName
                    if (tree == null || name == null) {
                        stopLocked()
                        return@withLock
                    }
                    try {
                        closeQuietly()
                        open(tree, name, reuseExisting)
                        writeLine(line)
                        _state.value = _state.value.copy(running = true, error = null)
                    } catch (retry: Exception) {
                        closeQuietly()
                        _state.value = _state.value.copy(
                            running = false,
                            error = retry.message ?: retry.toString(),
                        )
                    }
                }
            }
        }
    }

    suspend fun stop() {
        withContext(Dispatchers.IO) {
            mutex.withLock { stopLocked() }
        }
    }

    private fun stopLocked() {
        closeQuietly()
        _state.value = _state.value.copy(running = false)
    }

    private fun writeLine(line: String) {
        val w = writer ?: return
        w.write(line)
        w.write("\n")
        w.flush()
        _state.value = _state.value.copy(
            lineCount = _state.value.lineCount + 1,
            fileSizeBytes = logFile?.length() ?: _state.value.fileSizeBytes,
        )
    }

    /** Opens (or creates) the target document and writes the session header. */
    private fun open(treeUri: String, fileName: String, reuseExisting: Boolean) {
        val uri = treeUri.toUri()
        if (uri.scheme == null) throw IOException("Log folder is not set up")

        val dir = DocumentFile.fromTreeUri(context, uri)
            ?: throw IOException("Log folder is not accessible")

        val file = (if (reuseExisting) dir.findFile(fileName) else null)
            ?: dir.createFile(MIME_CSV, fileName)
            ?: throw IOException("Could not create '$fileName' in the log folder")

        val stream = context.contentResolver.openOutputStream(file.uri, "wa")
            ?: throw IOException("Could not open '$fileName' for writing")

        val w = stream.writer()
        w.write("# " + Calendar.getInstance().time.toString() + "\n")
        w.write(Ut61eReading.CSV_HEADER + "\n")
        w.flush()

        logFile = file
        writer = w
    }

    private fun closeQuietly() {
        try {
            writer?.close()
        } catch (_: Exception) {
            // Nothing useful to do while tearing down.
        }
        writer = null
    }

    private companion object {
        const val MIME_CSV = "text/csv"
    }
}
