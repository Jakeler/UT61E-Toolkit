package jk.ut61eTool.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.CartesianDrawingContext
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.columnSeries
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.marker.CartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.marker.ColumnCartesianLayerMarkerTarget
import com.patrykandpatrick.vico.compose.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.marker.rememberDefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.common.component.TextComponent
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme
import jk.ut61eTool.R
import jk.ut61eTool.ui.theme.UT61eToolkitTheme
import java.util.Locale

/**
 * Column (bar) chart of a measurement series, mirroring the legacy
 * MPAndroidChart bar graph.
 *
 * The x axis is the plain sample index, so it carries no labels; the y axis
 * autoscales to the data. Pressing a column reveals a marker with the value and
 * its unit.
 */
@Composable
fun MeasurementChart(
    values: List<Double>,
    modifier: Modifier = Modifier,
    unit: String? = null,
    height: androidx.compose.ui.unit.Dp = 220.dp,
) {
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(values) {
        if (values.isEmpty()) return@LaunchedEffect
        modelProducer.runTransaction {
            columnSeries { series(values) }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        contentAlignment = Alignment.Center,
    ) {
        if (values.isEmpty()) {
            Text(
                text = stringResource(R.string.no_data),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            val labelStyle = MaterialTheme.typography.labelMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
            )
            val markerFormatter = remember(unit) { MeasurementValueFormatter(unit.orEmpty()) }

            ProvideVicoTheme(rememberM3VicoTheme()) {
                CartesianChartHost(
                    chart = rememberCartesianChart(
                        rememberColumnCartesianLayer(),
                        startAxis = VerticalAxis.rememberStart(),
                        marker = rememberDefaultCartesianMarker(
                            label = markerLabel(labelStyle),
                            valueFormatter = markerFormatter,
                        ),
                    ),
                    modelProducer = modelProducer,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun markerLabel(style: TextStyle): TextComponent =
    remember(style) { TextComponent(textStyle = style) }

/** Renders the pressed column as `value unit`, with adaptive precision. */
private class MeasurementValueFormatter(private val unit: String) :
    DefaultCartesianMarker.ValueFormatter {

    override fun format(
        context: CartesianDrawingContext,
        targets: List<CartesianMarker.Target>,
    ): CharSequence {
        val value = targets
            .filterIsInstance<ColumnCartesianLayerMarkerTarget>()
            .flatMap { it.columns }
            .firstOrNull()
            ?.entry
            ?.y
            ?: return ""
        return formatMeasurement(value, unit)
    }
}

/**
 * Formats a measurement for display: four decimals like the legacy CSV format,
 * but with trailing zeros dropped so that the readout stays compact.
 */
fun formatMeasurement(value: Double, unit: String = ""): String {
    val text = "%.4f".format(Locale.US, value)
        .trimEnd('0')
        .trimEnd('.')
        .ifEmpty { "0" }
    return if (unit.isBlank()) text else "$text $unit"
}

@Preview(showBackground = true)
@Composable
private fun MeasurementChartPreview() {
    UT61eToolkitTheme(dynamicColor = false) {
        MeasurementChart(
            values = listOf(1.2, 1.4, 1.35, 1.9, 2.1, 1.7, 1.55, 1.8),
            unit = "V",
        )
    }
}
