package jk.ut61eTool.ui.live

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import jk.ut61eTool.appContainer
import jk.ut61eTool.data.ConnectionState
import jk.ut61eTool.data.DeviceInfo
import jk.ut61eTool.data.LoggingState
import jk.ut61eTool.data.MeterCommander
import jk.ut61eTool.data.MeterRepository
import jk.ut61eTool.data.WindowStats
import jk.ut61eTool.domain.DisplayReading
import jk.ut61eTool.domain.windowStats
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

data class LiveUiState(
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val deviceAddress: String = "",
    val deviceName: String? = null,
    val reading: DisplayReading? = null,
    val windowValues: List<Double> = emptyList(),
    val unit: String = "",
    val stats: WindowStats? = null,
    val logging: LoggingState = LoggingState(),
    val alarmActive: Boolean = false,
    val fileName: String = DEFAULT_LOG_FILE_NAME,
) {
    val sampleCount: Int get() = windowValues.size
}

const val DEFAULT_LOG_FILE_NAME = "logfile.csv"

/** Snapshot of the meter flows, kept separate so `combine` stays readable. */
private data class MeterSnapshot(
    val connectionState: ConnectionState,
    val device: DeviceInfo?,
    val reading: DisplayReading?,
    val window: List<DisplayReading>,
    val alarmActive: Boolean,
)

/**
 * Drives the live view. Mirrors the legacy LogActivity: it owns the connection
 * for the session and forwards logging commands to the service layer.
 */
class LiveViewModel(
    private val repository: MeterRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val address: String = savedStateHandle.get<String>("address").orEmpty()
    private val deviceName: String? = savedStateHandle.get<String>("name")

    /** Non-null once the user edited the field; otherwise the logger's name wins. */
    private val userFileName = MutableStateFlow<String?>(null)

    private val meterSnapshot = combine(
        repository.connectionState,
        repository.device,
        repository.latestReading,
        repository.window,
        repository.alarmActive,
    ) { state, device, reading, window, alarm ->
        MeterSnapshot(state, device, reading, window, alarm)
    }

    val uiState: StateFlow<LiveUiState> = combine(
        meterSnapshot,
        repository.loggingState,
        userFileName,
    ) { meter, logging, editedName ->
        val unit = meter.reading?.unit ?: meter.window.lastOrNull()?.unit.orEmpty()
        // A mode change mid-session leaves old-unit samples in the window;
        // graphing them against the current unit's scale is meaningless.
        val values = meter.window.filter { it.unit == unit }.map { it.value }
        LiveUiState(
            connectionState = meter.connectionState,
            deviceAddress = meter.device?.address ?: address,
            deviceName = meter.device?.name ?: deviceName,
            reading = meter.reading,
            windowValues = values,
            unit = unit,
            stats = values.windowStats(),
            logging = logging,
            alarmActive = meter.alarmActive,
            fileName = editedName ?: logging.fileName ?: DEFAULT_LOG_FILE_NAME,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LiveUiState(deviceAddress = address, deviceName = deviceName),
    )

    init {
        // Exactly once per session: adopt the device the user picked while scanning.
        viewModelScope.launch {
            val commander = awaitCommander() ?: return@launch
            if (repository.connectionState.value == ConnectionState.DISCONNECTED &&
                address.isNotEmpty()
            ) {
                commander.connect(DeviceInfo(address, deviceName))
            }
        }
    }

    fun onFileNameChange(value: String) {
        userFileName.value = value
    }

    fun setLogging(enabled: Boolean) {
        val commander = repository.commander ?: return
        if (enabled) commander.startLogging(uiState.value.fileName) else commander.stopLogging()
    }

    fun disconnect() {
        repository.commander?.disconnect()
    }

    /** The service binds asynchronously, so the commander may not be there yet. */
    private suspend fun awaitCommander(): MeterCommander? = withTimeoutOrNull(COMMANDER_TIMEOUT_MS) {
        var commander = repository.commander
        while (commander == null) {
            delay(50)
            commander = repository.commander
        }
        commander
    }

    companion object {
        private const val COMMANDER_TIMEOUT_MS = 5_000L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY] as Application
                LiveViewModel(
                    repository = application.appContainer.meterRepository,
                    savedStateHandle = createSavedStateHandle(),
                )
            }
        }
    }
}
