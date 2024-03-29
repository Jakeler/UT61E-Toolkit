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
import android.widget.ProgressBar
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

    companion object {
        private const val REQUEST_ENABLE_BT = 1
        private const val BLE_PERMISSION = 7

        // Stops scanning after time ins ms
        private const val SCAN_PERIOD: Long = 30_000

        const val EXTRAS_DEVICE_NAME = "DEVICE_NAME"
        const val EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS"
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        actionBar?.setTitle(R.string.title_devices)
        mHandler = Handler()

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter.let {
            // Checks if Bluetooth is supported on the device.
            if (it == null) {
                Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
                finish();
                return;
            } else {
                mBluetoothAdapter = it
            }
        }

        checkRequestBluetoothPermission()
    }

    private fun checkRequestBluetoothPermission() {
        // Check for scan permission to allow BLE scanning
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(
                // TODO request old ones level 30 and below
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            ), BLE_PERMISSION)
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

        // Initializes list view adapter.
        mLeDeviceListAdapter = LeDeviceListAdapter()
        listAdapter = mLeDeviceListAdapter

        // Ensures Bluetooth is enabled on the device. Needs connect permission
        checkRequestBluetoothPermission()
        if (!mBluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        } else {
            scanLeDevice(true)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_CANCELED) {
            finish()
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
        scanLeDevice(true)
    }

    override fun onPause() {
        super.onPause()
        scanLeDevice(false)
        mLeDeviceListAdapter?.clear()
    }

    override fun onListItemClick(l: ListView, v: View, position: Int, id: Long) {
        checkRequestBluetoothPermission()
        val device = mLeDeviceListAdapter?.getDevice(position) ?: return
        val intent = Intent(this, LogActivity::class.java)
        intent.putExtra(EXTRAS_DEVICE_NAME, device.name)
        intent.putExtra(EXTRAS_DEVICE_ADDRESS, device.address)
        if (mScanning) {
            scanLeDevice(enable = false)
        }
        startActivity(intent)
    }

    private fun scanLeDevice(enable: Boolean) {
        checkRequestBluetoothPermission()
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
        lateinit var deviceName: TextView
        lateinit var deviceAddress: TextView
        lateinit var deviceRssi: TextView
        lateinit var deviceBar: ProgressBar
    }

    // Adapter for holding devices found through scanning.
    private inner class LeDeviceListAdapter : BaseAdapter() {
        private val mLeDevices: ArrayList<ScanResult> = ArrayList()

        fun addDevice(result: ScanResult) {
            var index = -1
            mLeDevices.forEachIndexed { i, el ->
                index = if (el.device.address == result.device.address) i else index
            }
            // Replace with new result if its same device address
            if (index != -1)
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
                viewHolder.deviceBar = view.findViewById<ProgressBar>(R.id.rssi_bar)
                view.tag = viewHolder
            } else {
                view = pview
                viewHolder = view.tag as ViewHolder
            }

            val item = getItem(i) as ScanResult
            val deviceName = item.device.name
            if (deviceName != null && deviceName.isNotEmpty())
                viewHolder.deviceName.text = "$deviceName"
            else
                viewHolder.deviceName.setText(R.string.unknown_device)
            viewHolder.deviceAddress.text = "MAC: ${item.device.address}"
            viewHolder.deviceRssi.text = "RSSI: ${item.rssi} dBm"
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                viewHolder.deviceBar.setProgress(127 + item.rssi, true)
            } else {
                viewHolder.deviceBar.progress = 127+item.rssi
            }

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
        Log.i("perm", "onRequestPermissionsResult: $requestCode $grantResults")
        if (grantResults.any {it == PackageManager.PERMISSION_DENIED}) {
            Toast.makeText(this, R.string.location_permission_exp, Toast.LENGTH_LONG).show()
            finish()
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}