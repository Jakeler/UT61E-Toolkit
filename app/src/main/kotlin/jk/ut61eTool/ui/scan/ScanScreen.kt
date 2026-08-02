package jk.ut61eTool.ui.scan

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import jk.ut61eTool.R

/**
 * Compose replacement for the legacy DeviceScanActivity: permission flow,
 * adapter-off handling and a live list of advertising BLE devices.
 */
@Composable
fun ScanScreen(onDeviceSelected: (address: String, name: String?) -> Unit) {
    val viewModel: ScanViewModel = viewModel()
    val context = LocalContext.current

    val devices by viewModel.devices.collectAsStateWithLifecycle()
    val scanning by viewModel.scanning.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    var permissionsGranted by remember { mutableStateOf(context.hasScanPermissions()) }
    var permissionsRequested by remember { mutableStateOf(false) }
    var bluetoothEnabled by remember { mutableStateOf(viewModel.isBluetoothEnabled()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        permissionsRequested = true
        permissionsGranted = context.hasScanPermissions()
        bluetoothEnabled = viewModel.isBluetoothEnabled()
    }

    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        bluetoothEnabled = viewModel.isBluetoothEnabled()
    }

    LaunchedEffect(Unit) {
        if (!permissionsGranted) permissionLauncher.launch(scanPermissions())
    }

    LaunchedEffect(permissionsGranted, bluetoothEnabled) {
        if (permissionsGranted && bluetoothEnabled) viewModel.startScan()
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.stopScan() }
    }

    Scaffold(
        floatingActionButton = {
            if (permissionsGranted && bluetoothEnabled) {
                FloatingActionButton(
                    onClick = { if (scanning) viewModel.stopScan() else viewModel.startScan() },
                ) {
                    Icon(
                        imageVector = if (scanning) Icons.Filled.Stop else Icons.Filled.BluetoothSearching,
                        contentDescription = stringResource(
                            if (scanning) R.string.scan_stop else R.string.scan_start,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                !viewModel.isBluetoothSupported ->
                    Message(stringResource(R.string.scan_bluetooth_unsupported))

                !permissionsGranted -> PermissionRationale(
                    requested = permissionsRequested,
                    onGrant = { permissionLauncher.launch(scanPermissions()) },
                )

                !bluetoothEnabled -> BluetoothOff {
                    enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                }

                else -> DeviceList(
                    devices = devices,
                    scanning = scanning,
                    error = error,
                    onDeviceSelected = { device ->
                        viewModel.stopScan()
                        onDeviceSelected(device.address, device.name)
                    },
                )
            }
        }
    }
}

@Composable
private fun DeviceList(
    devices: List<ScannedDevice>,
    scanning: Boolean,
    error: String?,
    onDeviceSelected: (ScannedDevice) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (scanning) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        if (error != null) {
            Text(
                text = stringResource(R.string.scan_error, error),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        HorizontalDivider()

        if (devices.isEmpty()) {
            Message(
                stringResource(
                    if (scanning) R.string.scan_empty else R.string.scan_empty_done,
                ),
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(devices, key = { it.address }) { device ->
                    DeviceRow(device) { onDeviceSelected(device) }
                }
            }
        }
    }
}

@Composable
private fun DeviceRow(device: ScannedDevice, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name ?: stringResource(R.string.scan_unknown_device),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.scan_device_address, device.address),
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = stringResource(R.string.scan_device_rssi, device.rssi),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            // RSSI runs from about -127 dBm (unusable) to 0 dBm (touching).
            LinearProgressIndicator(
                progress = { ((127 + device.rssi) / 127f).coerceIn(0f, 1f) },
                modifier = Modifier.width(64.dp),
            )
        }
    }
}

@Composable
private fun PermissionRationale(requested: Boolean, onGrant: () -> Unit) {
    val rationale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        R.string.scan_permission_rationale
    } else {
        R.string.scan_permission_rationale_legacy
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(rationale),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onGrant) {
            Text(stringResource(R.string.scan_permission_grant))
        }
        if (requested) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.scan_permission_denied),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BluetoothOff(onEnable: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.scan_bluetooth_off),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onEnable) {
            Text(stringResource(R.string.scan_bluetooth_enable))
        }
    }
}

@Composable
private fun Message(text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * API 31+ replaced the location-based BLE permissions with the dedicated
 * Bluetooth ones; POST_NOTIFICATIONS (33+) is asked for in the same round so the
 * foreground-service notification is visible, but it is not required to scan.
 */
private fun scanPermissions(): Array<String> = buildList {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        add(Manifest.permission.BLUETOOTH_SCAN)
        add(Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        add(Manifest.permission.POST_NOTIFICATIONS)
    }
}.toTypedArray()

private fun Context.hasScanPermissions(): Boolean =
    scanPermissions()
        .filter { it != Manifest.permission.POST_NOTIFICATIONS }
        .all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }
