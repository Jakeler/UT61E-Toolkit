package jk.ut61eTool

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.databinding.DataBindingUtil
import jk.ut61eTool.databinding.ActivityStartBinding

class StartActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivityStartBinding =
                DataBindingUtil.setContentView(this, R.layout.activity_start)
        binding.version.text = "Version: ${BuildConfig.VERSION_NAME}"
    }

    fun startScan(v : View) {
        startActivity(Intent(this, DeviceScanActivity::class.java))
    }

    fun startView(v: View) {
        startActivity(Intent(this, FileSelectActivity::class.java))
//        Toast.makeText(this,"Not implemented, stay tuned for updates!", Toast.LENGTH_SHORT).show()
    }

    fun startSettings(v: View) {
        startActivity(Intent(this, SettingsActivity::class.java))
    }
}
