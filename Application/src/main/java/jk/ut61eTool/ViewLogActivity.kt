package jk.ut61eTool

import android.app.Activity
import android.os.Bundle
import android.util.Log
import java.io.File

class ViewLogActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_log)

        val file = intent.extras["filename"]

        if (file is File) {
            Log.d("FILE", file.toString())
            Log.w("PATH", file.path)
        }
    }
}
