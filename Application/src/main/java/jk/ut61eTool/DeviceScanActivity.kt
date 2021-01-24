/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jk.ut61eTool

import android.Manifest
import android.app.ListActivity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
class DeviceScanActivity : ListActivity() {
    private var mLeDeviceListAdapter: LeDeviceListAdapter? = null
    private lateinit var mBluetoothAdapter: BluetoothAdapter
    private var mScanning = false
    private var mHandler: Handler? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        actionBar?.setTitle(R.string.title_devices)
        mHandler = Handler()

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show()
            finish()
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothAdapter = bluetoothManager.adapter

        // Check for coarse location permission to allow BLE scanning
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
            ), LOCATION_PERMISSION)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        if (!mScanning) {
            menu.findItem(R.id.menu_stop).isVisible = false
            menu.findItem(R.id.menu_scan).isVisible = true
            menu.findItem(R.id.menu_refresh).actionView = null
        } else {
            menu.findItem(R.id.menu_stop).isVisible = true
            menu.findItem(R.id.menu_scan).isVisible = false
            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.actionbar_indeterminate_progress)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_scan -> {
                mLeDeviceListAdapter?.clear()
                scanLeDevice(true)
            }
            R.id.menu_stop -> scanLeDevice(false)
        }
        return true
    }

    override fun onResume() {
        super.onResume()

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled) {
            if (!mBluetoothAdapter.isEnabled) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            }
        }

        // Initializes list view adapter.
        mLeDeviceListAdapter = LeDeviceListAdapter()
        listAdapter = mLeDeviceListAdapter
        scanLeDevice(true)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_CANCELED) {
            finish()
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onPause() {
        super.onPause()
        scanLeDevice(false)
        mLeDeviceListAdapter?.clear()
    }

    override fun onListItemClick(l: ListView, v: View, position: Int, id: Long) {
        val device = mLeDeviceListAdapter?.getDevice(position) ?: return
        val intent = Intent(this, LogActivity::class.java)
        intent.putExtra(LogActivity.EXTRAS_DEVICE_NAME, device.name)
        intent.putExtra(LogActivity.EXTRAS_DEVICE_ADDRESS, device.address)
        if (mScanning) {
            scanLeDevice(enable = false)
        }
        startActivity(intent)
    }

    private fun scanLeDevice(enable: Boolean) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler?.postDelayed({
                mScanning = false
                mBluetoothAdapter.bluetoothLeScanner?.stopScan(scanCallback)
                invalidateOptionsMenu()
            }, SCAN_PERIOD)
            mScanning = true

            try {
                mBluetoothAdapter.bluetoothLeScanner?.startScan(scanCallback)

            } catch (e: Exception) {
                Toast.makeText(this, "Error: $e", Toast.LENGTH_LONG).show()
            }
        } else {
            mScanning = false
            mBluetoothAdapter.bluetoothLeScanner?.stopScan(scanCallback)
        }
        invalidateOptionsMenu()
    }

    internal class ViewHolder {
        var deviceName: TextView? = null
        var deviceAddress: TextView? = null
        var deviceRssi: TextView? = null
    }

    // Adapter for holding devices found through scanning.
    private inner class LeDeviceListAdapter : BaseAdapter() {
        private val mLeDevices: ArrayList<ScanResult> = ArrayList()

        fun addDevice(result: ScanResult) {
            var index = -1
            mLeDevices.forEachIndexed { i, el ->
                index = if (el.device.address == result.device.address) i else index
            }
            // Replace with new result if same device address
            if (index > 0)
                mLeDevices[index] = result
            else
                mLeDevices.add(result)
        }

        fun getDevice(position: Int): BluetoothDevice {
            return mLeDevices[position].device
        }

        fun clear() {
            mLeDevices.clear()
        }

        override fun getCount(): Int {
            return mLeDevices.size
        }

        override fun getItem(i: Int): Any {
            return mLeDevices[i]
        }

        override fun getItemId(i: Int): Long {
            return i.toLong()
        }

        override fun getView(i: Int, pview: View?, viewGroup: ViewGroup): View {
            val viewHolder: ViewHolder
            val view: View
            // General ListView optimization code.
            if (pview == null) {
                view = layoutInflater.inflate(R.layout.listitem_device, null)
                viewHolder = ViewHolder()
                viewHolder.deviceAddress = view.findViewById<TextView>(R.id.device_address)
                viewHolder.deviceName = view.findViewById<TextView>(R.id.device_name)
                viewHolder.deviceRssi = view.findViewById<TextView>(R.id.device_rssi)
                view.tag = viewHolder
            } else {
                view = pview
                viewHolder = view.tag as ViewHolder
            }

            val item = getItem(i) as ScanResult
            val deviceName = item.device.name
            if (deviceName != null && deviceName.isNotEmpty())
                viewHolder.deviceName?.text = "$deviceName"
            else
                viewHolder.deviceName?.setText(R.string.unknown_device)
            viewHolder.deviceAddress?.text = "MAC: ${item.device.address}"
            viewHolder.deviceRssi?.text = "Rssi: ${item.rssi}"

            return view
        }
    }

    val scanCallback = object : ScanCallback() {

        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            Log.d("Scan", "onScanResult: $result")

            if(result == null)
                return

            mLeDeviceListAdapter?.addDevice(result);
            mLeDeviceListAdapter?.notifyDataSetChanged();
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.e("Scan", "onScanFailed: $errorCode")
        }

    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
            Toast.makeText(this, R.string.location_permission_exp, Toast.LENGTH_LONG).show()
            finish()
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    companion object {
        private const val REQUEST_ENABLE_BT = 1
        private const val LOCATION_PERMISSION = 7

        // Stops scanning after 10 seconds.
        private const val SCAN_PERIOD: Long = 10000
    }
}