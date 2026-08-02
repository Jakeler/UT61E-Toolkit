@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package jk.ut61eTool.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PowerInput
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import jk.ut61eTool.BuildConfig
import jk.ut61eTool.R
import jk.ut61eTool.data.AlarmCondition
import jk.ut61eTool.data.Settings
import jk.ut61eTool.data.SettingsKeys
import jk.ut61eTool.domain.Temperature
import jk.ut61eTool.ui.components.ActionPreferenceRow
import jk.ut61eTool.ui.components.InfoPreferenceRow
import jk.ut61eTool.ui.components.ListPreferenceEntry
import jk.ut61eTool.ui.components.ListPreferenceRow
import jk.ut61eTool.ui.components.PreferenceCategory
import jk.ut61eTool.ui.components.SwitchPreferenceRow
import jk.ut61eTool.ui.components.TextFieldPreferenceRow

/** Compose port of the legacy `prefs.xml` preference screen. */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings = uiState.settings
    val context = LocalContext.current

    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, flags)
            }
            viewModel.setString(SettingsKeys.LOG_FOLDER, uri.toString())
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
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
                .verticalScroll(rememberScrollState()),
        ) {
            ConnectionSection(settings, viewModel)
            DisplaySection(settings, viewModel)
            FilesystemSection(settings, viewModel, onPickFolder = { folderPicker.launch(null) })
            DataProcessingSection(settings, uiState.thermalZones, viewModel)
            AlarmSection(settings, viewModel)
            InfoSection()
        }
    }
}

@Composable
private fun ConnectionSection(settings: Settings, viewModel: SettingsViewModel) {
    PreferenceCategory(stringResource(R.string.category_connection))
    TextFieldPreferenceRow(
        title = stringResource(R.string.pref_uuid),
        value = settings.characteristicUuid,
        onValueChange = { viewModel.setString(SettingsKeys.UUID, it) },
        icon = Icons.Default.Bluetooth,
    )
}

@Composable
private fun DisplaySection(settings: Settings, viewModel: SettingsViewModel) {
    PreferenceCategory(stringResource(R.string.category_display))
    TextFieldPreferenceRow(
        title = stringResource(R.string.pref_viewport),
        value = settings.viewportSize.toString(),
        onValueChange = { viewModel.setString(SettingsKeys.VIEWPORT, it) },
        icon = Icons.Default.Timeline,
        keyboardType = KeyboardType.Number,
    )
}

@Composable
private fun FilesystemSection(
    settings: Settings,
    viewModel: SettingsViewModel,
    onPickFolder: () -> Unit,
) {
    PreferenceCategory(stringResource(R.string.category_filesystem))
    ActionPreferenceRow(
        title = stringResource(R.string.pref_logdir),
        summary = settings.logFolderUri.ifBlank {
            stringResource(R.string.pref_logdir_desc)
        },
        onClick = onPickFolder,
        icon = Icons.Default.FolderOpen,
    )
    SwitchPreferenceRow(
        title = stringResource(R.string.pref_reuse_log),
        summary = stringResource(R.string.pref_reuse_log_desc),
        checked = settings.noLogfileReuse,
        onCheckedChange = { viewModel.setBoolean(SettingsKeys.NO_LOGFILE_REUSE, it) },
        icon = Icons.Default.ContentCopy,
    )
}

@Composable
private fun DataProcessingSection(
    settings: Settings,
    thermalZones: List<Temperature>,
    viewModel: SettingsViewModel,
) {
    PreferenceCategory(stringResource(R.string.category_data_processing))

    SwitchPreferenceRow(
        title = stringResource(R.string.pref_ignore_ol),
        summary = stringResource(R.string.pref_ignore_ol_desc),
        checked = settings.ignoreOverload,
        onCheckedChange = { viewModel.setBoolean(SettingsKeys.IGNORE_OL, it) },
        icon = Icons.Default.VerticalAlignTop,
    )

    // Shunt
    SwitchPreferenceRow(
        title = stringResource(R.string.pref_shunt_mode),
        summary = stringResource(R.string.pref_shunt_mode_desc),
        checked = settings.shuntMode,
        onCheckedChange = { viewModel.setBoolean(SettingsKeys.SHUNT_MODE, it) },
        icon = Icons.Default.PowerInput,
    )
    TextFieldPreferenceRow(
        title = stringResource(R.string.pref_shunt_ohm),
        value = settings.shuntOhm.toString(),
        onValueChange = { viewModel.setString(SettingsKeys.SHUNT_OHM, it) },
        enabled = settings.shuntMode,
        keyboardType = KeyboardType.Decimal,
    )

    // Thermocouple
    SwitchPreferenceRow(
        title = stringResource(R.string.pref_thermocouple_mode),
        summary = stringResource(R.string.pref_thermocouple_desc),
        checked = settings.tcMode,
        onCheckedChange = { viewModel.setBoolean(SettingsKeys.TC_MODE, it) },
        icon = Icons.Default.DeviceThermostat,
    )
    TextFieldPreferenceRow(
        title = stringResource(R.string.pref_thermocouple_coeff),
        value = settings.tcSensitivity.toString(),
        onValueChange = { viewModel.setString(SettingsKeys.TC_SENS, it) },
        enabled = settings.tcMode,
        keyboardType = KeyboardType.Decimal,
    )
    ListPreferenceRow(
        title = stringResource(R.string.pref_thermocouple_ref),
        entries = thermalZoneEntries(thermalZones),
        selectedValue = settings.tcReferenceId,
        onValueSelected = { viewModel.setString(SettingsKeys.TC_REFERENCE, it.toString()) },
        enabled = settings.tcMode,
    )
    TextFieldPreferenceRow(
        title = stringResource(R.string.pref_thermocouple_ref_constant),
        value = settings.tcReferenceConstant.toString(),
        onValueChange = { viewModel.setString(SettingsKeys.TC_REF_CONSTANT, it) },
        enabled = settings.tcMode,
        keyboardType = KeyboardType.Decimal,
    )

    // Thermistor
    SwitchPreferenceRow(
        title = stringResource(R.string.pref_thermistor_mode),
        summary = stringResource(R.string.pref_thermistor_desc),
        checked = settings.trMode,
        onCheckedChange = { viewModel.setBoolean(SettingsKeys.TR_MODE, it) },
        icon = Icons.Default.DeviceThermostat,
    )
    TextFieldPreferenceRow(
        title = stringResource(R.string.pref_thermistor_ohm),
        value = settings.trOhm.toString(),
        onValueChange = { viewModel.setString(SettingsKeys.TR_OHM, it) },
        enabled = settings.trMode,
        keyboardType = KeyboardType.Decimal,
    )
    TextFieldPreferenceRow(
        title = stringResource(R.string.pref_thermistor_beta),
        value = settings.trBeta.toString(),
        onValueChange = { viewModel.setString(SettingsKeys.TR_BETA, it) },
        enabled = settings.trMode,
        keyboardType = KeyboardType.Decimal,
    )

    // RTD
    SwitchPreferenceRow(
        title = stringResource(R.string.pref_rtd_mode),
        summary = stringResource(R.string.pref_rtd_desc),
        checked = settings.rtdMode,
        onCheckedChange = { viewModel.setBoolean(SettingsKeys.RTD_MODE, it) },
        icon = Icons.Default.DeviceThermostat,
    )
    TextFieldPreferenceRow(
        title = stringResource(R.string.pref_rtd_ohm),
        value = settings.rtdOhm.toString(),
        onValueChange = { viewModel.setString(SettingsKeys.RTD_OHM, it) },
        enabled = settings.rtdMode,
        keyboardType = KeyboardType.Decimal,
    )
    TextFieldPreferenceRow(
        title = stringResource(R.string.pref_rtd_alpha),
        value = settings.rtdAlpha.toString(),
        onValueChange = { viewModel.setString(SettingsKeys.RTD_ALPHA, it) },
        enabled = settings.rtdMode,
        keyboardType = KeyboardType.Decimal,
    )
}

@Composable
private fun thermalZoneEntries(zones: List<Temperature>): List<ListPreferenceEntry<Int>> {
    val constant = ListPreferenceEntry(
        value = -1,
        label = stringResource(R.string.pref_thermocouple_ref_constant_entry),
    )
    return listOf(constant) + zones.map { zone ->
        ListPreferenceEntry(
            value = zone.id,
            label = stringResource(R.string.pref_thermocouple_ref_zone, zone.name, zone.celsius),
        )
    }
}

@Composable
private fun AlarmSection(settings: Settings, viewModel: SettingsViewModel) {
    PreferenceCategory(stringResource(R.string.category_alarm))

    SwitchPreferenceRow(
        title = stringResource(R.string.pref_alarm_enabled),
        checked = settings.alarmEnabled,
        onCheckedChange = { viewModel.setBoolean(SettingsKeys.ALARM_ENABLED, it) },
        icon = Icons.Default.Alarm,
    )
    ListPreferenceRow(
        title = stringResource(R.string.pref_alarm_condition),
        entries = listOf(
            ListPreferenceEntry(
                AlarmCondition.ABOVE,
                stringResource(R.string.pref_alarm_condition_above),
            ),
            ListPreferenceEntry(
                AlarmCondition.BOTH,
                stringResource(R.string.pref_alarm_condition_both),
            ),
            ListPreferenceEntry(
                AlarmCondition.BELOW,
                stringResource(R.string.pref_alarm_condition_below),
            ),
        ),
        selectedValue = settings.alarmCondition,
        onValueSelected = {
            viewModel.setString(SettingsKeys.ALARM_CONDITION, it.storageValue)
        },
        enabled = settings.alarmEnabled,
    )
    TextFieldPreferenceRow(
        title = stringResource(R.string.pref_alarm_sample),
        value = settings.alarmSamples.toString(),
        onValueChange = { viewModel.setString(SettingsKeys.ALARM_SAMPLES, it) },
        summaryOverride = stringResource(R.string.pref_alarm_sample_desc),
        enabled = settings.alarmEnabled,
        keyboardType = KeyboardType.Number,
    )
    TextFieldPreferenceRow(
        title = stringResource(R.string.pref_alarm_low),
        value = settings.lowLimit.toString(),
        onValueChange = { viewModel.setString(SettingsKeys.LOW_LIMIT, it) },
        icon = Icons.AutoMirrored.Filled.TrendingDown,
        enabled = settings.alarmEnabled,
        keyboardType = KeyboardType.Decimal,
    )
    TextFieldPreferenceRow(
        title = stringResource(R.string.pref_alarm_high),
        value = settings.highLimit.toString(),
        onValueChange = { viewModel.setString(SettingsKeys.HIGH_LIMIT, it) },
        icon = Icons.AutoMirrored.Filled.TrendingUp,
        enabled = settings.alarmEnabled,
        keyboardType = KeyboardType.Decimal,
    )
    SwitchPreferenceRow(
        title = stringResource(R.string.pref_vibration),
        checked = settings.vibration,
        onCheckedChange = { viewModel.setBoolean(SettingsKeys.VIBRATION, it) },
        icon = Icons.Default.Vibration,
        enabled = settings.alarmEnabled,
    )
}

@Composable
private fun InfoSection() {
    PreferenceCategory(stringResource(R.string.category_info))
    InfoPreferenceRow(
        title = stringResource(R.string.pref_version),
        summary = BuildConfig.VERSION_NAME,
        icon = Icons.Default.Info,
    )
    InfoPreferenceRow(
        title = stringResource(R.string.pref_credits),
        summary = stringResource(R.string.pref_credits_desc),
    )
}
