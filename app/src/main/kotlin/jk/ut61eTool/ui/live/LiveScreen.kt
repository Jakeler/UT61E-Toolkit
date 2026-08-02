@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package jk.ut61eTool.ui.live

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import jk.ut61eTool.R
import jk.ut61eTool.data.ConnectionState
import jk.ut61eTool.data.LoggingState
import jk.ut61eTool.ui.components.MeasurementChart
import jk.ut61eTool.ui.components.StatsRow
import jk.ut61eTool.ui.components.ValueDisplay
import jk.ut61eTool.ui.components.previewReading
import jk.ut61eTool.ui.theme.UT61eToolkitTheme

/**
 * Live measurement view, replacing the legacy LogActivity: readout, rolling
 * chart with window statistics, alarm banner and the logging controls.
 */
@Composable
fun LiveScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LiveViewModel = viewModel(factory = LiveViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LiveScreenContent(
        uiState = uiState,
        onBack = onBack,
        onDisconnect = {
            viewModel.disconnect()
            onBack()
        },
        onFileNameChange = viewModel::onFileNameChange,
        onToggleLogging = viewModel::setLogging,
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
    modifier: Modifier = Modifier,
) {
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
                        Text(
                            text = connectionLabel(uiState.connectionState),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (uiState.alarmActive) {
                AlarmBanner()
            }

            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(
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
                }
            }

            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MeasurementChart(
                        values = uiState.windowValues,
                        unit = uiState.unit,
                    )
                    StatsRow(
                        stats = uiState.stats,
                        unit = uiState.unit,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                    Text(
                        text = stringResource(R.string.live_samples, uiState.sampleCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                }
            }

            LoggingCard(
                fileName = uiState.fileName,
                logging = uiState.logging,
                onFileNameChange = onFileNameChange,
                onToggleLogging = onToggleLogging,
            )
        }
    }
}

@Composable
private fun AlarmBanner(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.NotificationsActive, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.live_alarm_active),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun LoggingCard(
    fileName: String,
    logging: LoggingState,
    onFileNameChange: (String) -> Unit,
    onToggleLogging: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.live_logging_title),
                style = MaterialTheme.typography.titleSmall,
            )

            OutlinedTextField(
                value = fileName,
                onValueChange = onFileNameChange,
                label = { Text(stringResource(R.string.live_logging_filename)) },
                singleLine = true,
                enabled = !logging.running,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = { onToggleLogging(!logging.running) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    stringResource(
                        if (logging.running) R.string.live_logging_stop
                        else R.string.live_logging_start
                    )
                )
            }

            val activeName = logging.fileName
            if (activeName != null) {
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
private fun connectionLabel(state: ConnectionState): String = stringResource(
    when (state) {
        ConnectionState.CONNECTED -> R.string.state_connected
        ConnectionState.CONNECTING -> R.string.state_connecting
        ConnectionState.DISCONNECTED -> R.string.state_disconnected
    }
)

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
                windowValues = listOf(4.90, 4.93, 4.95, 4.91, 4.88, 4.94),
                unit = "V",
                logging = LoggingState(running = true, fileName = "logfile.csv", lineCount = 42),
            ),
            onBack = {},
            onDisconnect = {},
            onFileNameChange = {},
            onToggleLogging = {},
        )
    }
}
