package jk.ut61eTool.ui.viewer

import android.app.Application
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import jk.ut61eTool.appContainer
import jk.ut61eTool.data.LogFileRepository
import jk.ut61eTool.data.LogSession
import jk.ut61eTool.data.WindowStats
import jk.ut61eTool.domain.windowStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ViewerUiState(
    val loading: Boolean = true,
    val sessions: List<LogSession> = emptyList(),
    val selectedIndex: Int = 0,
    val selectedUnit: String? = null,
    val failed: Boolean = false,
) {
    val selectedSession: LogSession? get() = sessions.getOrNull(selectedIndex)

    /**
     * Units present in the session, in order of first appearance. A session can
     * contain several when the meter's mode was changed mid-recording — each is
     * graphed separately, since their values share no scale.
     */
    val units: List<String>
        get() = selectedSession?.points?.map { it.unit }?.distinct().orEmpty()

    /** The unit being graphed: the user's pick, or the one with the most points. */
    val activeUnit: String
        get() = selectedUnit?.takeIf { it in units }
            ?: selectedSession?.points
                ?.groupingBy { it.unit }
                ?.eachCount()
                ?.maxByOrNull { it.value }
                ?.key
                .orEmpty()

    val values: List<Double>
        get() = selectedSession?.points
            ?.filter { it.unit == activeUnit }
            ?.map { it.value }
            .orEmpty()

    val stats: WindowStats? get() = values.windowStats()

    val pointCount: Int get() = values.size
}

/** Replaces the legacy ViewLogActivity. */
class ViewerViewModel(
    private val logFileRepository: LogFileRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val fileUri: Uri? =
        savedStateHandle.get<String>("fileUri")?.let(Uri::parse)

    private val _uiState = MutableStateFlow(ViewerUiState())
    val uiState: StateFlow<ViewerUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun selectSession(index: Int) {
        _uiState.value = _uiState.value.copy(selectedIndex = index, selectedUnit = null)
    }

    fun selectUnit(unit: String) {
        _uiState.value = _uiState.value.copy(selectedUnit = unit)
    }

    private fun load() {
        val uri = fileUri
        if (uri == null) {
            _uiState.value = ViewerUiState(loading = false, failed = true)
            return
        }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { logFileRepository.readLogFile(uri) }
            }
            _uiState.value = result.fold(
                onSuccess = { sessions ->
                    ViewerUiState(
                        loading = false,
                        sessions = sessions,
                        // The most recent recording is the interesting one.
                        selectedIndex = (sessions.size - 1).coerceAtLeast(0),
                    )
                },
                onFailure = { ViewerUiState(loading = false, failed = true) },
            )
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as Application).appContainer
                ViewerViewModel(
                    logFileRepository = container.logFileRepository,
                    savedStateHandle = createSavedStateHandle(),
                )
            }
        }
    }
}
