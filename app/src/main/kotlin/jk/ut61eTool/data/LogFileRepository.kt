package jk.ut61eTool.data

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import android.provider.DocumentsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class LogFileEntry(
    val uri: Uri,
    val name: String,
    val sizeBytes: Long,
    val lastModified: Long,
)

class LogFileRepository(private val context: Context) {

    /**
     * Lists the CSV-ish documents of the user-picked log folder.
     *
     * The selection argument of [DocumentsContract] child queries is ignored by
     * most providers (notably ExternalStorageProvider), so the MIME/extension
     * filtering happens here, on the client side, like it did before.
     */
    suspend fun listLogFiles(treeUri: String): List<LogFileEntry> = withContext(Dispatchers.IO) {
        if (treeUri.isBlank()) return@withContext emptyList()
        val tree = treeUri.toUri()
        if (tree.scheme == null) return@withContext emptyList()

        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            tree,
            DocumentsContract.getTreeDocumentId(tree),
        )
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
        )

        val entries = mutableListOf<LogFileEntry>()
        try {
            context.contentResolver.query(childrenUri, projection, null, null, null)?.use { c ->
                while (c.moveToNext()) {
                    val name = c.getString(0) ?: continue
                    val mime = c.getString(4).orEmpty()
                    if (!isCsvLike(name, mime)) continue
                    val documentId = c.getString(3) ?: continue
                    entries += LogFileEntry(
                        uri = DocumentsContract.buildDocumentUriUsingTree(tree, documentId),
                        name = name,
                        sizeBytes = c.getLong(1),
                        lastModified = c.getLong(2),
                    )
                }
            }
        } catch (_: Exception) {
            return@withContext emptyList()
        }
        entries.sortedByDescending { it.lastModified }
    }

    suspend fun readLogFile(uri: Uri): List<LogSession> = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader().useLines { LogFileParser.parse(it) }
            } ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun isCsvLike(name: String, mime: String): Boolean =
        name.endsWith(".csv", ignoreCase = true) ||
            mime == "text/csv" ||
            mime == "text/comma-separated-values"
}
