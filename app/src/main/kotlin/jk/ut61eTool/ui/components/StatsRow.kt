package jk.ut61eTool.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import jk.ut61eTool.R
import jk.ut61eTool.data.WindowStats
import jk.ut61eTool.ui.theme.UT61eToolkitTheme

/**
 * Compact min / max / average / mean-deviation summary of the visible window,
 * replacing the legacy single-line `graphdata_info` label.
 */
@Composable
fun StatsRow(
    stats: WindowStats?,
    modifier: Modifier = Modifier,
    unit: String = "",
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        StatCell(stringResource(R.string.stats_min), stats?.min, unit, Modifier.weight(1f))
        StatCell(stringResource(R.string.stats_max), stats?.max, unit, Modifier.weight(1f))
        StatCell(stringResource(R.string.stats_avg), stats?.average, unit, Modifier.weight(1f))
        StatCell(stringResource(R.string.stats_dev), stats?.meanDeviation, unit, Modifier.weight(1f))
    }
}

@Composable
private fun StatCell(
    label: String,
    value: Double?,
    unit: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Text(
            text = value?.let { formatMeasurement(it, unit) } ?: "–",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StatsRowPreview() {
    UT61eToolkitTheme(dynamicColor = false) {
        StatsRow(
            stats = WindowStats(min = 1.2, max = 2.1, average = 1.63, meanDeviation = 0.21),
            unit = "V",
        )
    }
}
