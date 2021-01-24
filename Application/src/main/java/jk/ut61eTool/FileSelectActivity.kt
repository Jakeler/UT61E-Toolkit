package jk.ut61eTool

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
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
        val uriEnc = PreferenceManager.getDefaultSharedPreferences(this).getString("log_folder", getString(R.string.log_folder))
        if (uriEnc == null) {
            Toast.makeText(this, "No folder setup, check your settings", Toast.LENGTH_SHORT).show()
            return
        }
        val dir = DocumentFile.fromTreeUri(this, Uri.parse(uriEnc))
        val files = dir?.listFiles()
        if (files == null) {
            Log.w("FILE SELECT", "no files")
            Toast.makeText(this, "Folder not existing, check your settings", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        if (files.isEmpty())
            Toast.makeText(this, "Folder contains no files, check your settings", Toast.LENGTH_SHORT).show()

        val arrayAdapter = object : ArrayAdapter<DocumentFile>(this, R.layout.listitem_files, R.id.filename, files) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val file = getItem(position) ?: return view

                view.findViewById<TextView>(R.id.filename)?.text = file.name
                view.findViewById<TextView>(R.id.logfile_info)?.text =
                        "Filesize: ${file.length()/1000.0} KB\nModified: ${Date(file.lastModified())}"
                return view
            }
        }
        val fileListView = findViewById<ListView>(R.id.fileList)
        fileListView.adapter = arrayAdapter

        fileListView.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            val intent = Intent(this@FileSelectActivity, ViewLogActivity::class.java)
            intent.putExtra("filename", files[position].uri)
            startActivity(intent)
        }
    }

}
