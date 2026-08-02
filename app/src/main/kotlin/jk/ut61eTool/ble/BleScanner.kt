package jk.ut61eTool.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** A `ScanCallback.SCAN_FAILED_*` code reported by the BLE stack. */
class BleScanFailedException(val errorCode: Int) :
    Exception("BLE scan failed, error code $errorCode")

/**
 * Cold flow wrapper around [android.bluetooth.le.BluetoothLeScanner].
 *
 * Scans without filters, like the legacy DeviceScanActivity: the HM-11 module
 * advertises no service UUID we could filter on. The caller owns the scan
 * duration (`withTimeoutOrNull`) — cancelling the collection stops the scan.
 *
 * The caller guarantees BLUETOOTH_SCAN (API 31+) / ACCESS_FINE_LOCATION.
 */
class BleScanner(context: Context) {

    private val appContext = context.applicationContext

    private val adapter
        get() = appContext.getSystemService(BluetoothManager::class.java)?.adapter

    val isBluetoothSupported: Boolean get() = adapter != null

    val isBluetoothEnabled: Boolean get() = adapter?.isEnabled == true

    @SuppressLint("MissingPermission")
    fun scan(): Flow<ScanResult> = callbackFlow {
        val scanner = adapter?.takeIf { it.isEnabled }?.bluetoothLeScanner
        if (scanner == null) {
            close(IllegalStateException("Bluetooth is not available or not enabled"))
            return@callbackFlow
        }

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                trySend(result)
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>) {
                results.forEach { trySend(it) }
            }

            override fun onScanFailed(errorCode: Int) {
                close(BleScanFailedException(errorCode))
            }
        }

        try {
            scanner.startScan(callback)
        } catch (e: Exception) {
            close(e)
            return@callbackFlow
        }

        awaitClose {
            runCatching { scanner.stopScan(callback) }
        }
    }
}
