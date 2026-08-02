package jk.ut61eTool.ui.logfiles

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import jk.ut61eTool.appContainer
import jk.ut61eTool.data.LogFileEntry
import jk.ut61eTool.data.LogFileRepository
import jk.ut61eTool.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Why the list is empty — the UI shows a different hint for each case. */
enum class LogFilesEmptyReason { NONE, NO_FOLDER, FOLDER_EMPTY, FOLDER_INACCESSIBLE }

data class LogFilesUiState(
    val loading: Boolean = true,
    val files: List<LogFileEntry> = emptyList(),
    val emptyReason: LogFilesEmptyReason = LogFilesEmptyReason.NONE,
)

class LogFilesViewModel(
    private val settingsRepository: SettingsRepository,
    private val logFileRepository: LogFileRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LogFilesUiState())
    val uiState: StateFlow<LogFilesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settings
                .map { it.logFolderUri }
                .distinctUntilChanged()
                .collect { load(it) }
        }
    }

    private suspend fun load(folderUri: String) {
        if (folderUri.isBlank()) {
            _uiState.value = LogFilesUiState(
                loading = false,
                emptyReason = LogFilesEmptyReason.NO_FOLDER,
            )
            return
        }

        _uiState.value = _uiState.value.copy(loading = true)
        val files = runCatching { logFileRepository.listLogFiles(folderUri) }
        _uiState.value = files.fold(
            onSuccess = { list ->
                LogFilesUiState(
                    loading = false,
                    files = list,
                    emptyReason = if (list.isEmpty()) LogFilesEmptyReason.FOLDER_EMPTY
                    else LogFilesEmptyReason.NONE,
                )
            },
            onFailure = {
                LogFilesUiState(
                    loading = false,
                    emptyReason = LogFilesEmptyReason.FOLDER_INACCESSIBLE,
                )
            },
        )
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as Application).appContainer
                LogFilesViewModel(
                    settingsRepository = container.settingsRepository,
                    logFileRepository = container.logFileRepository,
                )
            }
        }
    }
}
