@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package jk.ut61eTool.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jk.ut61eTool.BuildConfig
import jk.ut61eTool.R
import jk.ut61eTool.appContainer
import jk.ut61eTool.data.ConnectionState
import jk.ut61eTool.ui.theme.UT61eToolkitTheme

/**
 * Landing screen, replacing the legacy StartActivity: three entry points plus a
 * shortcut back into a session that is still running in the background.
 */
@Composable
fun HomeScreen(
    onConnect: () -> Unit,
    onViewLogs: () -> Unit,
    onSettings: () -> Unit,
    onResumeSession: (address: String, name: String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val repository = LocalContext.current.appContainer.meterRepository
    val connectionState by repository.connectionState.collectAsStateWithLifecycle()
    val device by repository.device.collectAsStateWithLifecycle()

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.home_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(8.dp))

            val activeDevice = device
            if (connectionState != ConnectionState.DISCONNECTED && activeDevice != null) {
                SessionCard(
                    deviceLabel = activeDevice.name ?: activeDevice.address,
                    onClick = { onResumeSession(activeDevice.address, activeDevice.name) },
                )
            }

            HomeCard(
                title = stringResource(R.string.home_connect_title),
                description = stringResource(R.string.home_connect_desc),
                icon = Icons.AutoMirrored.Filled.BluetoothSearching,
                onClick = onConnect,
            )
            HomeCard(
                title = stringResource(R.string.home_logs_title),
                description = stringResource(R.string.home_logs_desc),
                icon = Icons.Default.FolderOpen,
                onClick = onViewLogs,
            )
            HomeCard(
                title = stringResource(R.string.home_settings_title),
                description = stringResource(R.string.home_settings_desc),
                icon = Icons.Default.Settings,
                onClick = onSettings,
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.home_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun HomeCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp),
            )
            Spacer(Modifier.width(20.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SessionCard(
    deviceLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Timeline,
                contentDescription = null,
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.home_session_title),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = "$deviceLabel • ${stringResource(R.string.home_session_desc)}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeCardPreview() {
    UT61eToolkitTheme(dynamicColor = false) {
        HomeCard(
            title = "Connect meter",
            description = "Search for a BLE multimeter",
            icon = Icons.AutoMirrored.Filled.BluetoothSearching,
            onClick = {},
        )
    }
}
