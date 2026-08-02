package jk.ut61eTool.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
 * The big live readout: value + unit, the meter's status flags as badges, and
 * an optional detail line (e.g. the thermocouple cold-junction reference).
 */
@Composable
fun ValueDisplay(
    reading: DisplayReading?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (reading == null) {
            Text(
                text = stringResource(R.string.live_waiting),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }

        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = formatMeasurement(reading.value),
                style = MeasurementValueStyle,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
            Text(
                text = reading.unit,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }

        ReadingBadges(reading)

        val detail = reading.detail
        if (!detail.isNullOrBlank()) {
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ReadingBadges(reading: DisplayReading) {
    val source = reading.source
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (source.isAC) Badge(stringResource(R.string.badge_ac))
        if (source.isDC) Badge(stringResource(R.string.badge_dc))
        if (source.isAuto) Badge(stringResource(R.string.badge_auto))
        if (source.mode == Mode.FREQUENCY) Badge(stringResource(R.string.badge_hz))
        if (source.mode == Mode.DUTY) Badge(stringResource(R.string.badge_duty))
        if (source.isOverload) Badge(stringResource(R.string.badge_ol), emphasis = true)
        if (source.isUnderload) Badge(stringResource(R.string.badge_ul), emphasis = true)
        if (source.isLowBattery) Badge(stringResource(R.string.badge_low_battery), emphasis = true)
    }
}

@Composable
private fun Badge(
    text: String,
    modifier: Modifier = Modifier,
    emphasis: Boolean = false,
) {
    val container: Color
    val content: Color
    if (emphasis) {
        container = MaterialTheme.colorScheme.errorContainer
        content = MaterialTheme.colorScheme.onErrorContainer
    } else {
        container = MaterialTheme.colorScheme.secondaryContainer
        content = MaterialTheme.colorScheme.onSecondaryContainer
    }

    Surface(
        modifier = modifier,
        color = container,
        contentColor = content,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ValueDisplayPreview() {
    UT61eToolkitTheme(dynamicColor = false) {
        ValueDisplay(reading = previewReading())
    }
}

internal fun previewReading(
    value: Double = 4.9312,
    unit: String = "V",
    detail: String? = null,
): DisplayReading = DisplayReading(
    value = value,
    unit = unit,
    detail = detail,
    source = Ut61eReading(
        value = value,
        unit = unit,
        mode = Mode.VOLTAGE,
        rangeIndex = 0,
        isAC = false,
        isDC = true,
        isAuto = true,
        isNegative = false,
        isOverload = false,
        isUnderload = false,
        isLowBattery = false,
        raw = ByteArray(0),
    ),
)
