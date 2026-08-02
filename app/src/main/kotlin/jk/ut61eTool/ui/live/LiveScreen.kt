@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package jk.ut61eTool.ui.live

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import jk.ut61eTool.R
import jk.ut61eTool.data.ConnectionState
import jk.ut61eTool.data.LoggingState
import jk.ut61eTool.domain.windowStats
import jk.ut61eTool.ui.components.FullscreenChart
import jk.ut61eTool.ui.components.MeasurementChart
import jk.ut61eTool.ui.components.StatsRow
import jk.ut61eTool.ui.components.formatMeasurement
import jk.ut61eTool.ui.components.ValueDisplay
import jk.ut61eTool.ui.components.previewReading
import jk.ut61eTool.ui.theme.UT61eToolkitTheme

/**
 * Live measurement view: the readout, a rolling chart with window statistics, and
 * the logging controls. The layout keeps a fixed shape while values change — the
 * alarm tints the readout instead of inserting a banner, and logging collapses to a
 * single row until the user opens it.
 */
@Composable
fun LiveScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LiveViewModel = viewModel(factory = LiveViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // A bench meter that blanks mid-measurement is useless.
    val view = LocalView.current
    view.keepScreenOn = uiState.connectionState != ConnectionState.DISCONNECTED

    LiveScreenContent(
        uiState = uiState,
        onBack = onBack,
        onDisconnect = {
            viewModel.disconnect()
            onBack()
        },
        onFileNameChange = viewModel::onFileNameChange,
        onToggleLogging = viewModel::setLogging,
        onToggleHold = viewModel::setHold,
        modifier = modifier,
    )
}

@Composable
private fun LiveScreenContent(
    uiState: LiveUiState,
    onBack: () -> Unit,
    onDisconnect: () -> Unit,
    onFileNameChange: (String) -> Unit,
    onToggleLogging: (Boolean) -> Unit,
    onToggleHold: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var fullscreenGraph by rememberSaveable { mutableStateOf(false) }
    if (fullscreenGraph) {
        FullscreenGraph(
            uiState = uiState,
            onToggleHold = onToggleHold,
            onClose = { fullscreenGraph = false },
            modifier = modifier,
        )
        return
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = uiState.deviceName
                                ?: uiState.deviceAddress.ifEmpty {
                                    stringResource(R.string.unknown_device)
                                },
                            style = MaterialTheme.typography.titleMedium,
                        )
                        ConnectionLabel(uiState.connectionState)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onDisconnect) {
                        Icon(
                            imageVector = Icons.Default.BluetoothDisabled,
                            contentDescription = stringResource(R.string.action_disconnect),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // Landscape and tablets: readout beside the graph instead of below it.
            val twoPane = maxWidth >= TWO_PANE_MIN_WIDTH

            if (twoPane) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        ReadoutCard(uiState)
                        LoggingCard(uiState, onFileNameChange, onToggleLogging)
                    }
                    GraphCard(
                        uiState = uiState,
                        onToggleHold = onToggleHold,
                        onFullscreen = { fullscreenGraph = true },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
            } else {
                // Not scrollable: the graph is meant to take whatever the readout and
                // the logging row leave over.
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ReadoutCard(uiState)
                    GraphCard(
                        uiState = uiState,
                        onToggleHold = onToggleHold,
                        onFullscreen = { fullscreenGraph = true },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = MIN_GRAPH_HEIGHT),
                    )
                    LoggingCard(uiState, onFileNameChange, onToggleLogging)
                }
            }
        }
    }
}

/**
 * The readout. On alarm the card itself turns red rather than a banner appearing
 * above it, so nothing on the screen moves.
 */
@Composable
private fun ReadoutCard(uiState: LiveUiState) {
    val container by animateColorAsState(
        targetValue = if (uiState.alarmActive) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        label = "readoutContainer",
    )
    val content = if (uiState.alarmActive) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = container,
            contentColor = content,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp, horizontal = 16.dp),
        ) {
            if (uiState.connectionState == ConnectionState.CONNECTING) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.working))
                }
            } else {
                ValueDisplay(reading = uiState.reading)
            }

            if (uiState.alarmActive) {
                Text(
                    text = stringResource(R.string.live_alarm),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.TopEnd),
                )
            }
        }
    }
}

@Composable
private fun GraphCard(
    uiState: LiveUiState,
    onToggleHold: (Boolean) -> Unit,
    onFullscreen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = uiState.sampleRateHz?.let {
                        stringResource(R.string.live_rate, it, uiState.sampleCount)
                    }.orEmpty(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HoldChip(uiState.held, onToggleHold)
                    IconButton(onClick = onFullscreen) {
                        Icon(
                            imageVector = Icons.Default.Fullscreen,
                            contentDescription = stringResource(R.string.graph_fullscreen),
                        )
                    }
                }
            }
            MeasurementChart(
                values = uiState.windowValues,
                unit = uiState.unit,
                // A held graph is a still image; its x axis is no longer "seconds ago".
                sampleRateHz = uiState.sampleRateHz.takeUnless { uiState.held },
                height = null,
                capacity = uiState.windowCapacity,
                modifier = Modifier.weight(1f),
            )
            StatsRow(
                stats = uiState.stats,
                unit = uiState.unit,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
    }
}

@Composable
private fun HoldChip(held: Boolean, onToggleHold: (Boolean) -> Unit) {
    FilterChip(
        selected = held,
        onClick = { onToggleHold(!held) },
        label = { Text(stringResource(R.string.live_hold)) },
    )
}

/** The live window fullscreen, headed by the current reading. */
@Composable
private fun FullscreenGraph(
    uiState: LiveUiState,
    onToggleHold: (Boolean) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FullscreenChart(
        values = uiState.windowValues,
        unit = uiState.unit,
        onClose = onClose,
        modifier = modifier,
        title = uiState.reading
            ?.let { formatMeasurement(it.value, uiState.unit, it.decimals) }
            .orEmpty(),
        sampleRateHz = uiState.sampleRateHz.takeUnless { uiState.held },
        stats = uiState.stats,
        capacity = uiState.windowCapacity,
        actions = { HoldChip(uiState.held, onToggleHold) },
    )
}

/**
 * Logging, collapsed to one row until the user opens it — the controls are only
 * needed when a recording starts or stops.
 */
@Composable
private fun LoggingCard(
    uiState: LiveUiState,
    onFileNameChange: (String) -> Unit,
    onToggleLogging: (Boolean) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val logging = uiState.logging

    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (logging.running) {
                    RecordingStatus(logging)
                } else {
                    Text(
                        text = stringResource(R.string.live_logging_title),
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
                if (logging.running) {
                    TextButton(onClick = { onToggleLogging(false) }) {
                        Text(stringResource(R.string.live_logging_stop))
                    }
                } else {
                    TextButton(onClick = { expanded = !expanded }) {
                        Text(stringResource(R.string.live_logging_expand))
                    }
                }
            }

            AnimatedVisibility(visible = expanded && !logging.running) {
                LoggingControls(uiState, onFileNameChange, onToggleLogging)
            }

            val activeName = logging.fileName
            if (logging.running && activeName != null) {
                Text(
                    text = stringResource(
                        R.string.live_logging_info,
                        activeName,
                        logging.fileSizeBytes / 1024.0,
                        logging.lineCount,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            val error = logging.error
            if (error != null) {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun LoggingControls(
    uiState: LiveUiState,
    onFileNameChange: (String) -> Unit,
    onToggleLogging: (Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = uiState.fileName,
            onValueChange = onFileNameChange,
            label = { Text(stringResource(R.string.live_logging_filename)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = { onToggleLogging(true) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.live_logging_start))
        }
    }
}

/** `● REC · file.csv · 128 points`, the dot pulsing while data is being written. */
@Composable
private fun RecordingStatus(logging: LoggingState) {
    val pulse by rememberInfiniteTransition(label = "rec").animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "recAlpha",
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .alpha(pulse)
                .size(10.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.error),
        )
        Text(
            text = stringResource(
                R.string.live_logging_recording,
                logging.fileName.orEmpty(),
                logging.lineCount,
            ),
            style = MaterialTheme.typography.titleSmall,
        )
    }
}

/** Connection state as a colored dot plus its label, readable at a glance. */
@Composable
private fun ConnectionLabel(state: ConnectionState) {
    val color = when (state) {
        ConnectionState.CONNECTED -> ConnectedColor
        ConnectionState.CONNECTING -> ConnectingColor
        ConnectionState.DISCONNECTED -> MaterialTheme.colorScheme.outline
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(
            text = stringResource(
                when (state) {
                    ConnectionState.CONNECTED -> R.string.state_connected
                    ConnectionState.CONNECTING -> R.string.state_connecting
                    ConnectionState.DISCONNECTED -> R.string.state_disconnected
                },
            ),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private val TWO_PANE_MIN_WIDTH = 600.dp
private val MIN_GRAPH_HEIGHT = 180.dp

// Link state is a signal, not a brand colour, so these stay outside the M3 scheme.
private val ConnectedColor = Color(0xFF2E7D32)
private val ConnectingColor = Color(0xFFF9A825)

private val PREVIEW_VALUES = listOf(4.90, 4.93, 4.95, 4.91, 4.88, 4.94, 4.92, 4.90, 4.93, 4.96)

@Preview(showBackground = true)
@Composable
private fun LiveScreenPreview() {
    UT61eToolkitTheme(dynamicColor = false) {
        LiveScreenContent(
            uiState = LiveUiState(
                connectionState = ConnectionState.CONNECTED,
                deviceName = "UT61E-BLE",
                deviceAddress = "00:11:22:33:44:55",
                reading = previewReading(),
                windowValues = PREVIEW_VALUES,
                windowCapacity = 30,
                unit = "V",
                stats = PREVIEW_VALUES.windowStats(),
                sampleRateHz = 2.0,
                logging = LoggingState(running = true, fileName = "logfile.csv", lineCount = 42),
            ),
            onBack = {},
            onDisconnect = {},
            onFileNameChange = {},
            onToggleLogging = {},
            onToggleHold = {},
        )
    }
}
