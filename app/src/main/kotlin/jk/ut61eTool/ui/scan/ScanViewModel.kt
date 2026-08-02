package jk.ut61eTool.ui.scan

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.le.ScanResult
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import jk.ut61eTool.ble.BleScanner
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** One advertising device, de-duplicated by [address]. */
data class ScannedDevice(
    val address: String,
    val name: String?,
    val rssi: Int,
)

class ScanViewModel(application: Application) : AndroidViewModel(application) {

    private val scanner = BleScanner(application)

    private val _devices = MutableStateFlow<List<ScannedDevice>>(emptyList())
    val devices: StateFlow<List<ScannedDevice>> = _devices.asStateFlow()

    private val _scanning = MutableStateFlow(false)
    val scanning: StateFlow<Boolean> = _scanning.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var scanJob: Job? = null

    val isBluetoothSupported: Boolean get() = scanner.isBluetoothSupported

    fun isBluetoothEnabled(): Boolean = scanner.isBluetoothEnabled

    fun startScan() {
        if (scanJob?.isActive == true) return
        _devices.value = emptyList()
        _error.value = null
        scanJob = viewModelScope.launch {
            _scanning.value = true
            try {
                // Same 30 s budget as the legacy DeviceScanActivity.
                withTimeoutOrNull(SCAN_PERIOD_MILLIS) {
                    scanner.scan()
                        .catch { e -> _error.value = e.message ?: e.toString() }
                        .collect { onResult(it) }
                }
            } finally {
                _scanning.value = false
            }
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        scanJob = null
    }

    override fun onCleared() {
        stopScan()
    }

    @SuppressLint("MissingPermission")
    private fun onResult(result: ScanResult) {
        // The advertised name needs no BLUETOOTH_CONNECT; the cached device name
        // is only a fallback and can throw when the permission was revoked.
        val name = result.scanRecord?.deviceName?.takeIf { it.isNotBlank() }
            ?: runCatching { result.device.name }.getOrNull()?.takeIf { it.isNotBlank() }
        val incoming = ScannedDevice(result.device.address, name, result.rssi)

        _devices.update { current ->
            val index = current.indexOfFirst { it.address == incoming.address }
            val merged = if (index >= 0) {
                current.toMutableList().apply {
                    // Advertisements alternate between with and without a name.
                    this[index] = incoming.copy(name = incoming.name ?: current[index].name)
                }
            } else {
                current + incoming
            }
            // Stable ordering: named devices first, alphabetically, then by MAC —
            // sorting by RSSI would make rows jump on every advertisement.
            merged.sortedWith(
                compareBy({ it.name == null }, { it.name.orEmpty().lowercase() }, { it.address }),
            )
        }
    }

    private companion object {
        const val SCAN_PERIOD_MILLIS = 30_000L
    }
}
