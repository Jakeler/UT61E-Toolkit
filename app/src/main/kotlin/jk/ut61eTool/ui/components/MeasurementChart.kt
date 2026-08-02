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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.AutoScrollCondition
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.CartesianDrawingContext
import com.patrykandpatrick.vico.compose.cartesian.Scroll
import com.patrykandpatrick.vico.compose.cartesian.Zoom
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.lineSeries
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.marker.CartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.marker.ColumnCartesianLayerMarkerTarget
import com.patrykandpatrick.vico.compose.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.marker.LineCartesianLayerMarkerTarget
import com.patrykandpatrick.vico.compose.cartesian.marker.rememberDefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.common.component.TextComponent
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme
import jk.ut61eTool.R
import jk.ut61eTool.ui.theme.UT61eToolkitTheme
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.log10

/**
 * Line chart of a measurement series.
 *
 * The x axis is the plain sample index. The y axis is fitted to the data
 * (with headroom) instead of being zero-anchored, so small signals stay
 * readable, and its labels use just enough decimals for the visible range.
 * The viewport follows the newest sample without diff animations; pinch
 * zooms the time axis. Pressing a point reveals value and unit.
 */
@Composable
fun MeasurementChart(
    values: List<Double>,
    modifier: Modifier = Modifier,
    unit: String? = null,
    height: Dp = 220.dp,
) {
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(values) {
        if (values.isEmpty()) return@LaunchedEffect
        modelProducer.runTransaction {
            lineSeries { series(values) }
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
            val range = remember(values) { yRange(values) }

            ProvideVicoTheme(rememberM3VicoTheme()) {
                CartesianChartHost(
                    chart = rememberCartesianChart(
                        rememberLineCartesianLayer(
                            rangeProvider = remember(range) {
                                CartesianLayerRangeProvider.fixed(minY = range.min, maxY = range.max)
                            },
                        ),
                        startAxis = VerticalAxis.rememberStart(
                            valueFormatter = remember(range) { AdaptiveAxisFormatter(range) },
                            // The default step placer labels only "round" values, which can
                            // collapse to a single line on a fitted (non-zero-based) range.
                            itemPlacer = remember { VerticalAxis.ItemPlacer.count({ AXIS_LABEL_COUNT }) },
                        ),
                        marker = rememberDefaultCartesianMarker(
                            label = markerLabel(labelStyle),
                            valueFormatter = markerFormatter,
                        ),
                    ),
                    modelProducer = modelProducer,
                    scrollState = rememberVicoScrollState(
                        initialScroll = Scroll.Absolute.End,
                        autoScroll = Scroll.Absolute.End,
                        autoScrollCondition = AutoScrollCondition.OnModelGrowth,
                    ),
                    zoomState = rememberVicoZoomState(
                        zoomEnabled = true,
                        initialZoom = Zoom.Content,
                    ),
                    animationSpec = null,
                    animateIn = false,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

private data class YRange(val min: Double, val max: Double) {
    val span: Double get() = max - min
}

/** Fits the axis to the data with 10% headroom; flat series get a symmetric band. */
private fun yRange(values: List<Double>): YRange {
    val min = values.min()
    val max = values.max()
    val pad = when {
        max > min -> (max - min) * 0.1
        min != 0.0 -> abs(min) * 0.1
        else -> 1.0
    }
    return YRange(min - pad, max + pad)
}

private const val AXIS_LABEL_COUNT = 5

/** Axis labels with just enough decimals to tell adjacent gridlines apart. */
private class AdaptiveAxisFormatter(range: YRange) : CartesianValueFormatter {
    private val decimals: Int = run {
        val step = range.span / (AXIS_LABEL_COUNT - 1)
        if (step <= 0.0) 0 else ceil(-log10(step)).toInt().coerceIn(0, 6)
    }

    override fun format(
        context: com.patrykandpatrick.vico.compose.cartesian.CartesianMeasuringContext,
        value: Double,
        verticalAxisPosition: com.patrykandpatrick.vico.compose.cartesian.axis.Axis.Position.Vertical?,
    ): CharSequence = "%.${decimals}f".format(Locale.US, value)
}

@Composable
private fun markerLabel(style: TextStyle): TextComponent =
    remember(style) { TextComponent(textStyle = style) }

/** Renders the pressed point as `value unit`, with adaptive precision. */
private class MeasurementValueFormatter(private val unit: String) :
    DefaultCartesianMarker.ValueFormatter {

    override fun format(
        context: CartesianDrawingContext,
        targets: List<CartesianMarker.Target>,
    ): CharSequence {
        val value = targets.firstNotNullOfOrNull { target ->
            when (target) {
                is LineCartesianLayerMarkerTarget -> target.points.firstOrNull()?.entry?.y
                is ColumnCartesianLayerMarkerTarget -> target.columns.firstOrNull()?.entry?.y
                else -> null
            }
        } ?: return ""
        return formatMeasurement(value, unit)
    }
}

/**
 * Formats a measurement for display.
 *
 * With [decimals] the value is padded to exactly that many places — the digit count
 * then only changes when the meter changes range, so the big readout stays still.
 * Without it, four decimals are formatted and trailing zeros dropped, which keeps
 * derived numbers (statistics, chart markers, the log viewer) compact.
 */
fun formatMeasurement(value: Double, unit: String = "", decimals: Int? = null): String {
    val text = if (decimals != null) {
        "%.${decimals}f".format(Locale.US, value)
    } else {
        "%.4f".format(Locale.US, value)
            .trimEnd('0')
            .trimEnd('.')
            .ifEmpty { "0" }
    }
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
