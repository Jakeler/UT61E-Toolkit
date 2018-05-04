package jk.ut61eTool

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView

class StartActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)
        findViewById<TextView>(R.id.version).setText("Version: " + BuildConfig.VERSION_NAME)
    }

    fun startScan(v : View) {
        startActivity(Intent(this, DeviceScanActivity::class.java))
    }

    fun startView(v: View) {
        startActivity(Intent(this, ViewActivity::class.java))
//        Toast.makeText(this,"Not implemented, stay tuned for updates!", Toast.LENGTH_SHORT).show()
    }

    fun startSettings(v: View) {
        startActivity(Intent(this, SettingsActivity::class.java))
    }
}
