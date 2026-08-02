package jk.ut61eTool.data

import jk.ut61eTool.domain.DisplayReading
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ConnectionState { DISCONNECTED, CONNECTING, CONNECTED }

data class LoggingState(
    val running: Boolean = false,
    val fileName: String? = null,
    val fileSizeBytes: Long = 0,
    val lineCount: Int = 0,
    val error: String? = null,
)

data class WindowStats(
    val min: Double,
    val max: Double,
    val average: Double,
    val meanDeviation: Double,
)

data class DeviceInfo(val address: String, val name: String?)

/** Commands the UI issues; implemented by the meter service layer. */
interface MeterCommander {
    fun connect(device: DeviceInfo)
    fun disconnect()
    fun startLogging(fileName: String)
    fun stopLogging()
}

/**
 * Process-wide state holder between the meter service (producer) and the UI
 * (consumer). Replaces the legacy implicit broadcasts.
 */
class MeterRepository {
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _device = MutableStateFlow<DeviceInfo?>(null)
    val device: StateFlow<DeviceInfo?> = _device.asStateFlow()

    private val _latestReading = MutableStateFlow<DisplayReading?>(null)
    val latestReading: StateFlow<DisplayReading?> = _latestReading.asStateFlow()

    /** Rolling window of the last `viewport` readings for the live chart. */
    private val _window = MutableStateFlow<List<DisplayReading>>(emptyList())
    val window: StateFlow<List<DisplayReading>> = _window.asStateFlow()

    /**
     * How many readings [window] holds once it is full. The chart pins its time axis
     * to this, so the axis does not stretch while the window is still filling.
     */
    private val _windowCapacity = MutableStateFlow(0)
    val windowCapacity: StateFlow<Int> = _windowCapacity.asStateFlow()

    private val _loggingState = MutableStateFlow(LoggingState())
    val loggingState: StateFlow<LoggingState> = _loggingState.asStateFlow()

    private val _alarmActive = MutableStateFlow(false)
    val alarmActive: StateFlow<Boolean> = _alarmActive.asStateFlow()

    /**
     * Arrival time of the newest sample, in nanoseconds. Readings themselves are
     * conflated when the value does not change, so the UI derives the sample rate
     * from this instead — a steady meter still ticks here.
     */
    private val _lastSampleNanos = MutableStateFlow(0L)
    val lastSampleNanos: StateFlow<Long> = _lastSampleNanos.asStateFlow()

    /** Installed by the service layer; null while no service is available. */
    var commander: MeterCommander? = null

    // Producer side (service workstream)

    fun publishConnectionState(state: ConnectionState, device: DeviceInfo? = null) {
        _connectionState.value = state
        if (device != null) _device.value = device
        if (state == ConnectionState.DISCONNECTED) _alarmActive.value = false
    }

    /**
     * [includeInWindow] = false keeps the live readout updating while an ignored
     * (overloaded) sample stays out of the graph window, matching the legacy app.
     */
    fun publishReading(reading: DisplayReading, windowSize: Int, includeInWindow: Boolean = true) {
        _latestReading.value = reading
        _lastSampleNanos.value = System.nanoTime()
        _windowCapacity.value = windowSize.coerceAtLeast(1)
        if (includeInWindow) {
            _window.value = (_window.value + reading).takeLast(windowSize.coerceAtLeast(1))
        }
    }

    fun publishLoggingState(state: LoggingState) {
        _loggingState.value = state
    }

    fun publishAlarm(active: Boolean) {
        _alarmActive.value = active
    }

    fun clearSession() {
        _latestReading.value = null
        _window.value = emptyList()
        _alarmActive.value = false
        _lastSampleNanos.value = 0L
    }
}
