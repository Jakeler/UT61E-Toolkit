package jk.ut61eTool

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.preference.PreferenceManager
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.io.File
import java.util.*

class FileSelectActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_file)
        actionBar?.setDisplayHomeAsUpEnabled(true)


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            populateListView()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 7)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            populateListView()
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun populateListView() {
        val folder = PreferenceManager.getDefaultSharedPreferences(this).getString("log_folder", getString(R.string.log_folder))
        val dir = File(Environment.getExternalStorageDirectory().toString(), folder)
        val files = dir.list()
        if (files == null) {
            Log.w("FILE SELECT", "no files")
            Toast.makeText(this, "Folder not existing, check your settings", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        if (files.isEmpty())
            Toast.makeText(this, "Folder contains no files, check your settings", Toast.LENGTH_SHORT).show()

        val arrayAdapter = object : ArrayAdapter<String>(this, R.layout.listitem_files, R.id.filename, files) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                val view = super.getView(position, convertView, parent)
                val file = dir.listFiles()[position]

                view.findViewById<TextView>(R.id.logfile_info)?.text = "Filesize: ${file.length()/1000.0} KB\nModified: ${Date(file.lastModified())}"
                return view
            }
        }
        val fileListView = findViewById<ListView>(R.id.fileList)
        fileListView.adapter = arrayAdapter

        fileListView.onItemClickListener = object : AdapterView.OnItemClickListener {
            override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val intent = Intent(this@FileSelectActivity, ViewLogActivity::class.java)
                intent.putExtra("filename", dir.listFiles()[position])
                startActivity(intent)
            }
        }
    }

}
