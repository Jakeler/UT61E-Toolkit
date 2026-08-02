package jk.ut61eTool.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import jk.ut61eTool.R
import jk.ut61eTool.data.WindowStats
import jk.ut61eTool.ui.theme.StatValueStyle
import jk.ut61eTool.ui.theme.UT61eToolkitTheme

/**
 * Compact min / max / average / mean-deviation summary of the visible window.
 *
 * The unit is stated once above the row instead of on every cell: repeating it four
 * times only widened the columns and pushed the numbers around.
 */
@Composable
fun StatsRow(
    stats: WindowStats?,
    modifier: Modifier = Modifier,
    unit: String = "",
) {
    val cells = listOf(
        stringResource(R.string.stats_min) to stats?.min,
        stringResource(R.string.stats_max) to stats?.max,
        stringResource(R.string.stats_avg) to stats?.average,
        stringResource(R.string.stats_dev) to stats?.meanDeviation,
    )

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        // Four equal columns of single-line text: without shrinking, a large font
        // scale cuts the labels and the numbers short ("Mea", "119").
        ScaledDensity(rowScale(cells, constraints.maxWidth)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (unit.isNotBlank()) {
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    cells.forEach { (label, value) ->
                        StatCell(label, value, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/** Scale at which the widest cell still fits its quarter of [maxWidth]. */
@Composable
private fun rowScale(cells: List<Pair<String, Double?>>, maxWidth: Int): Float {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val labelStyle = MaterialTheme.typography.labelSmall
    return remember(cells, maxWidth, density, labelStyle) {
        val widest = cells.maxOf { (label, value) ->
            maxOf(
                measurer.measure(label, labelStyle, softWrap = false).size.width,
                measurer.measure(statText(value), StatValueStyle, softWrap = false).size.width,
            )
        }
        val gaps = with(density) { CELL_GAP.toPx() } * cells.size
        fitFactor(widest * cells.size + gaps, maxWidth)
    }
}

/** Breathing room between the columns, so they never touch at the fitted size. */
private val CELL_GAP = 4.dp

@Composable
private fun StatCell(
    label: String,
    value: Double?,
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
            text = statText(value),
            style = StatValueStyle,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}

/** The cell's number, or a dash before any data has arrived. */
private fun statText(value: Double?): String = value?.let { formatMeasurement(it) } ?: "–"

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
