package jk.ut61eTool.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jake.ut61e.Mode
import com.jake.ut61e.Ut61eReading
import jk.ut61eTool.R
import jk.ut61eTool.domain.DisplayReading
import jk.ut61eTool.ui.theme.MeasurementValueStyle
import jk.ut61eTool.ui.theme.UT61eToolkitTheme

/**
 * The big live readout, laid out as a single value line the way the meter's own
 * display reads:
 *
 * ```
 * AUTO   −4.9312 V DC
 * ```
 *
 * The range flags lead, the current type is part of the unit, and the number sits
 * right-aligned in a slot wide enough for the widest string its range can produce —
 * so digits stay put while the value fluctuates and only a range change resizes it.
 * `Hz`/`%` are not flagged separately: they are already the unit.
 */
@Composable
fun ValueDisplay(
    reading: DisplayReading?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ValueLine(reading)

        val notes = listOfNotNull(
            // The parenthetical of a converted unit ("°C (thermocouple)") is context,
            // not part of the reading — it belongs on the secondary line.
            reading?.unit?.substringAfter(' ', "")?.takeIf { it.isNotBlank() },
            reading?.detail?.takeIf { it.isNotBlank() },
        )
        if (notes.isNotEmpty()) {
            Text(
                text = notes.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ValueLine(reading: DisplayReading?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Bottom,
    ) {
        // Reserved, so appearing flags never nudge the number sideways.
        Box(
            modifier = Modifier
                .width(FLAG_SLOT_WIDTH)
                .padding(end = 8.dp, bottom = 8.dp),
            contentAlignment = Alignment.BottomEnd,
        ) {
            if (reading != null) LeadingFlags(reading.source)
        }

        // The number is the flexible element: a Row measures weighted children last,
        // so the unit keeps its space instead of being squeezed out by a wide value.
        Box(
            modifier = Modifier.weight(1f, fill = false),
            contentAlignment = Alignment.BottomEnd,
        ) {
            NumberSlot(reading)
        }

        Spacer(Modifier.width(8.dp))

        // Fixed too, so switching V <-> mV does not shove the number sideways.
        Box(
            modifier = Modifier.width(UNIT_SLOT_WIDTH),
            contentAlignment = Alignment.BottomStart,
        ) {
            UnitGroup(reading)
        }
    }
}

/** `AUTO` plus the low-battery warning: meter state, not part of the value. */
@Composable
private fun LeadingFlags(source: Ut61eReading) {
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (source.isLowBattery) {
            Icon(
                imageVector = Icons.Default.BatteryAlert,
                contentDescription = stringResource(R.string.badge_low_battery),
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp),
            )
        }
        if (source.isAuto) {
            Text(
                text = stringResource(R.string.badge_auto),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * The number, right-aligned over an invisible template of the widest string this
 * range can produce. Monospace digits plus a fixed slot mean the readout only moves
 * when the meter actually changes range.
 */
@Composable
private fun NumberSlot(reading: DisplayReading?) {
    val overloaded = reading != null && (reading.source.isOverload || reading.source.isUnderload)
    val text = when {
        reading == null -> PLACEHOLDER_TEXT
        reading.source.isOverload -> stringResource(R.string.value_overload)
        reading.source.isUnderload -> stringResource(R.string.value_underload)
        else -> formatMeasurement(reading.value, decimals = reading.decimals)
    }

    Box(contentAlignment = Alignment.BottomEnd) {
        Text(
            text = widthTemplate(reading),
            style = MeasurementValueStyle,
            color = Color.Transparent,
            maxLines = 1,
        )
        Text(
            text = text,
            style = MeasurementValueStyle,
            color = when {
                overloaded -> MaterialTheme.colorScheme.error
                reading == null -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = waitingAlpha())
                else -> MaterialTheme.colorScheme.onSurface
            },
            maxLines = 1,
        )
    }
}

/** Slow pulse of the waiting placeholder, so an idle link is visibly idle, not frozen. */
@Composable
private fun waitingAlpha(): Float = rememberInfiniteTransition(label = "waiting")
    .animateFloat(
        initialValue = 0.15f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "waitingAlpha",
    ).value

/** The unit and, as part of it, the current type: `V DC`. */
@Composable
private fun UnitGroup(reading: DisplayReading?) {
    Row(
        modifier = Modifier.padding(bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = reading?.unit?.substringBefore(' ').orEmpty(),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
        )
        // "°C DC" would be nonsense: after a conversion the current type describes the
        // meter's input, not what is being shown.
        val current = reading?.takeUnless { it.isConverted }?.source?.let {
            when {
                it.isAC -> stringResource(R.string.badge_ac)
                it.isDC -> stringResource(R.string.badge_dc)
                else -> null
            }
        }
        if (current != null) {
            Text(
                text = current,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

private val FLAG_SLOT_WIDTH = 52.dp

/** Wide enough for the longest unit plus AC/DC, e.g. `mV DC`. */
private val UNIT_SLOT_WIDTH = 84.dp

/** Shown before the first sample arrives, so the card does not resize on connect. */
private const val PLACEHOLDER_TEXT = "––––"

/** The meter shows five digits; converted values may need more on the left. */
private const val METER_DIGITS = 5

private fun widthTemplate(reading: DisplayReading?): String {
    if (reading == null) return PLACEHOLDER_TEXT
    val decimals = reading.decimals
    val formatted = formatMeasurement(reading.value, decimals = decimals)
    val integerDigits = maxOf(
        METER_DIGITS - decimals,
        formatted.substringBefore('.').trimStart('-').length,
    )
    val fraction = if (decimals > 0) "." + "8".repeat(decimals) else ""
    return "-" + "8".repeat(integerDigits) + fraction
}

@Preview(showBackground = true)
@Composable
private fun ValueDisplayPreview() {
    UT61eToolkitTheme(dynamicColor = false) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ValueDisplay(reading = previewReading())
            ValueDisplay(reading = previewReading(value = -4.9312))
            ValueDisplay(
                reading = previewReading(
                    value = 225.80,
                    unit = "mV",
                    decimals = 2,
                    isAC = true,
                    isDC = false,
                    isLowBattery = true,
                ),
            )
            ValueDisplay(reading = previewReading(isOverload = true))
            ValueDisplay(
                reading = previewReading(
                    value = 1234.5,
                    unit = "Hz",
                    decimals = 1,
                    mode = Mode.FREQUENCY,
                    isDC = false,
                ),
            )
            ValueDisplay(
                reading = previewReading(
                    value = 24.3,
                    unit = "°C (thermocouple)",
                    decimals = 1,
                    detail = "ref.: cpu @ 21.5°C",
                ),
            )
            ValueDisplay(reading = null)
        }
    }
}

internal fun previewReading(
    value: Double = 4.9312,
    unit: String = "V",
    decimals: Int = 4,
    detail: String? = null,
    mode: Mode = Mode.VOLTAGE,
    isAC: Boolean = false,
    isDC: Boolean = true,
    isOverload: Boolean = false,
    isLowBattery: Boolean = false,
): DisplayReading = DisplayReading(
    value = value,
    unit = unit,
    detail = detail,
    decimals = decimals,
    source = Ut61eReading(
        value = value,
        unit = unit.substringBefore(' '),
        mode = mode,
        rangeIndex = 0,
        decimals = decimals,
        isAC = isAC,
        isDC = isDC,
        isAuto = true,
        isNegative = value < 0,
        isOverload = isOverload,
        isUnderload = false,
        isLowBattery = isLowBattery,
        raw = ByteArray(0),
    ),
)
