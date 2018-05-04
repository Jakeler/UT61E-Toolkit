package jk.ut61eTool

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.ListView
import java.io.File

class ViewActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view)
        actionBar?.setDisplayHomeAsUpEnabled(true)


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 7)
        }

        val dir = File(Environment.getExternalStorageDirectory().toString(), getString(R.string.log_folder))

        for (file in dir.list()) {
            Log.d("FILES", file)
        }
        val arrayAdapter = ArrayAdapter<String>(this, R.layout.listitem_files, R.id.filename, dir.list())
        findViewById<ListView>(R.id.fileList).adapter = arrayAdapter
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

}
