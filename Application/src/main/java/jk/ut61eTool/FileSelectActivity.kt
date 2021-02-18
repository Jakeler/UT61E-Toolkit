package jk.ut61eTool

import android.app.Activity
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.preference.PreferenceManager
import java.util.*

class FileSelectActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_file)
        actionBar?.setDisplayHomeAsUpEnabled(true)
        populateListView()

    }

    private fun populateListView() {
        val uriEnc = PreferenceManager.getDefaultSharedPreferences(this).getString("log_folder", null)
        if (uriEnc == null) {
            Toast.makeText(this, getString(R.string.error_folder_not_setup), Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val uri = Uri.parse(uriEnc)
        val dUri = DocumentsContract.buildChildDocumentsUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri))

        val proj = arrayOf(
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_SIZE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
        )
        // filter not working on fs:
        // https://stackoverflow.com/questions/56263620/contentresolver-query-on-documentcontract-lists-all-files-disregarding-selection
        val sel = "text/comma-separated-values"
        val order = DocumentsContract.Document.COLUMN_SIZE + " ASC"

        var cursor: Cursor? = null;
        try {
            cursor = contentResolver.query(dUri, proj, sel, null, order)
        } catch (e: Exception) {
            Log.w("FILE SELECT", "Error: ${e.message}")
            Toast.makeText(this, getString(R.string.error_folder_missing), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val files = arrayListOf<FileEntry>()
        cursor?.use {
            while (it.moveToNext()) {
                // Workaround: manual filter
                if (it.getString(4) != sel)
                    continue

                FileEntry(
                        it.getString(0),
                        it.getLong(1),
                        Date(it.getLong(2)),
                        it.getString(3)
                ).let {fileEntry -> files.add(fileEntry) }
            }
        }

        if (files.isEmpty())
            Toast.makeText(this, getString(R.string.error_folder_empty), Toast.LENGTH_SHORT).show()

        val arrayAdapter = object : ArrayAdapter<FileEntry>(this, R.layout.listitem_files, R.id.filename, files) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val file = getItem(position) ?: return view

                view.findViewById<TextView>(R.id.filename)?.text = file.name
                view.findViewById<TextView>(R.id.logfile_info)?.text =
                        "Filesize: ${file.size/1000.0} KB\nModified: ${file.lastMod}"
                return view
            }
        }
        val fileListView = findViewById<ListView>(R.id.fileList)
        fileListView.adapter = arrayAdapter

        fileListView.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            val intent = Intent(this@FileSelectActivity, ViewLogActivity::class.java)
            DocumentsContract.buildDocumentUriUsingTree(uri, files[position].id).let {
                intent.putExtra("filename", it)
            }
            startActivity(intent)
        }
    }

}

data class FileEntry(
        val name: String,
        val size: Long,
        val lastMod: Date,
        val id: String,
)
