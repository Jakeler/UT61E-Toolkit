package jk.ut61eTool.ui.settings

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import jk.ut61eTool.appContainer
import jk.ut61eTool.data.Settings
import jk.ut61eTool.data.SettingsRepository
import jk.ut61eTool.domain.Temperature
import jk.ut61eTool.domain.ThermalZoneReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SettingsUiState(
    val settings: Settings = Settings(),
    val thermalZones: List<Temperature> = emptyList(),
)

/** Backs the preference screen; every edit is written straight through. */
class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val thermalZoneReader: ThermalZoneReader,
) : ViewModel() {

    private val thermalZones = MutableStateFlow<List<Temperature>>(emptyList())

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.settings,
        thermalZones,
    ) { settings, zones ->
        SettingsUiState(settings = settings, thermalZones = zones)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(),
    )

    init {
        viewModelScope.launch {
            // Reading /sys/class/thermal touches the filesystem.
            thermalZones.value = withContext(Dispatchers.IO) { thermalZoneReader.listZones() }
        }
    }

    fun setString(key: String, value: String) {
        viewModelScope.launch { settingsRepository.setString(key, value) }
    }

    fun setBoolean(key: String, value: Boolean) {
        viewModelScope.launch { settingsRepository.setBoolean(key, value) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as Application).appContainer
                SettingsViewModel(
                    settingsRepository = container.settingsRepository,
                    thermalZoneReader = container.thermalZoneReader,
                )
            }
        }
    }
}
