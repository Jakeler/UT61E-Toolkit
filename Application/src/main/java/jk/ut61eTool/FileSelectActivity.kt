package jk.ut61eTool

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import java.io.File

class FileSelectActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_file)
        actionBar?.setDisplayHomeAsUpEnabled(true)


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 7)
        } else {
            populateListView()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            populateListView()
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    fun populateListView() {
        val dir = File(Environment.getExternalStorageDirectory().toString(), getString(R.string.log_folder))

        val arrayAdapter = ArrayAdapter<String>(this, R.layout.listitem_files, R.id.filename, dir.list())
        val fileListView = findViewById<ListView>(R.id.fileList)
        fileListView.adapter = arrayAdapter

        fileListView.onItemClickListener = object : AdapterView.OnItemClickListener {
            override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                Log.d("onclick", dir.list()[position])
                val intent = Intent(this@FileSelectActivity, ViewLogActivity::class.java)
                intent.putExtra("filename", dir.listFiles()[position])
                startActivity(intent)
            }
        }
    }

}