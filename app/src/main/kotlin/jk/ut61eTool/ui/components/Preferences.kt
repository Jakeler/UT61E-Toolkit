package jk.ut61eTool.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import jk.ut61eTool.R

/**
 * Material 3 replacements for the legacy androidx.preference widgets.
 *
 * Every row supports a disabled state so that "child" rows can be greyed out
 * while their parent switch is off (the legacy `android:dependency` chains).
 */

/** Alpha applied to the whole row when [enabled] is false. */
private const val DISABLED_ALPHA = 0.38f

@Composable
fun PreferenceCategory(
    title: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider()
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
        )
    }
}

@Composable
private fun PreferenceRowLayout(
    title: String,
    summary: String?,
    icon: ImageVector?,
    enabled: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    val contentAlpha = if (enabled) 1f else DISABLED_ALPHA
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable(enabled = enabled, onClick = onClick)
                else Modifier
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = LocalContentColor.current.copy(alpha = contentAlpha),
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(16.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
            )
            if (!summary.isNullOrBlank()) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(16.dp))
            trailing()
        }
    }
}

/** Replacement for CheckBoxPreference. */
@Composable
fun SwitchPreferenceRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    PreferenceRowLayout(
        title = title,
        summary = summary,
        icon = icon,
        enabled = enabled,
        onClick = { onCheckedChange(!checked) },
        modifier = modifier,
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
            )
        },
    )
}

/**
 * Replacement for EditTextPreference: the current [value] doubles as the row
 * summary, tapping opens a small dialog with a single text field.
 */
@Composable
fun TextFieldPreferenceRow(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    summaryOverride: String? = null,
) {
    var dialogOpen by rememberSaveable { mutableStateOf(false) }

    PreferenceRowLayout(
        title = title,
        summary = summaryOverride ?: value,
        icon = icon,
        enabled = enabled,
        onClick = { dialogOpen = true },
        modifier = modifier,
    )

    if (dialogOpen) {
        TextFieldPreferenceDialog(
            title = title,
            initialValue = value,
            keyboardType = keyboardType,
            onDismiss = { dialogOpen = false },
            onConfirm = {
                dialogOpen = false
                onValueChange(it)
            },
        )
    }
}

@Composable
private fun TextFieldPreferenceDialog(
    title: String,
    initialValue: String,
    keyboardType: KeyboardType,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember { mutableStateOf(initialValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }) {
                Text(stringResource(R.string.action_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

/** One selectable option of a [ListPreferenceRow]. */
data class ListPreferenceEntry<T>(val value: T, val label: String)

/** Replacement for ListPreference: a radio-group dialog. */
@Composable
fun <T> ListPreferenceRow(
    title: String,
    entries: List<ListPreferenceEntry<T>>,
    selectedValue: T,
    onValueSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    var dialogOpen by rememberSaveable { mutableStateOf(false) }
    val selectedLabel = entries.firstOrNull { it.value == selectedValue }?.label

    PreferenceRowLayout(
        title = title,
        summary = selectedLabel,
        icon = icon,
        enabled = enabled,
        onClick = { dialogOpen = true },
        modifier = modifier,
    )

    if (dialogOpen) {
        AlertDialog(
            onDismissRequest = { dialogOpen = false },
            title = { Text(title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    entries.forEach { entry ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = entry.value == selectedValue,
                                    onClick = {
                                        dialogOpen = false
                                        onValueSelected(entry.value)
                                    },
                                )
                                .padding(vertical = 8.dp),
                        ) {
                            RadioButton(
                                selected = entry.value == selectedValue,
                                onClick = null,
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(entry.label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { dialogOpen = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

/** Non-interactive informational row (version, credits …). */
@Composable
fun InfoPreferenceRow(
    title: String,
    summary: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    PreferenceRowLayout(
        title = title,
        summary = summary,
        icon = icon,
        enabled = true,
        onClick = null,
        modifier = modifier,
    )
}

/** Clickable row without its own value, e.g. the log folder picker. */
@Composable
fun ActionPreferenceRow(
    title: String,
    summary: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    PreferenceRowLayout(
        title = title,
        summary = summary,
        icon = icon,
        enabled = enabled,
        onClick = onClick,
        modifier = modifier,
    )
}
