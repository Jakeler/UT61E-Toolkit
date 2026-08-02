package jk.ut61eTool.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.lineSeries
import com.patrykandpatrick.vico.compose.cartesian.decoration.Decoration
import com.patrykandpatrick.vico.compose.cartesian.decoration.HorizontalLine
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.marker.CartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.marker.ColumnCartesianLayerMarkerTarget
import com.patrykandpatrick.vico.compose.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.marker.LineCartesianLayerMarkerTarget
import com.patrykandpatrick.vico.compose.cartesian.marker.rememberDefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.DashedShape
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.common.component.LineComponent
import com.patrykandpatrick.vico.compose.common.component.ShapeComponent
import com.patrykandpatrick.vico.compose.common.component.TextComponent
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme
import jk.ut61eTool.R
import jk.ut61eTool.data.WindowStats
import jk.ut61eTool.ui.theme.UT61eToolkitTheme
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Line chart of a measurement series.
 *
 * The y axis is fitted to the data (with headroom) instead of being zero-anchored,
 * so small signals stay readable, and its labels use just enough decimals for the
 * visible range. The x axis is labelled in seconds before now when [sampleRateHz]
 * is known, and by sample index otherwise (recorded logs carry no timing). The
 * viewport follows the newest sample without diff animations; pinch zooms the time
 * axis. Pressing a point reveals value and unit.
 */
@Composable
fun MeasurementChart(
    values: List<Double>,
    modifier: Modifier = Modifier,
    unit: String? = null,
    sampleRateHz: Double? = null,
    /** Null lets the chart fill whatever space its parent gives it. */
    height: Dp? = 220.dp,
    /** When set, min / max / average are drawn as guide lines. */
    guides: WindowStats? = null,
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
            .then(if (height != null) Modifier.height(height) else Modifier.fillMaxHeight()),
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

            val lineColor = MaterialTheme.colorScheme.primary
            val areaFill = remember(lineColor) {
                LineCartesianLayer.AreaFill.single(
                    Fill(
                        Brush.verticalGradient(
                            listOf(lineColor.copy(alpha = 0.35f), Color.Transparent),
                        ),
                    ),
                )
            }

            // Dots mark the individual samples, but only while they are far enough
            // apart to be read as points rather than a thick line.
            val pointProvider = if (values.size <= MAX_DOTTED_SAMPLES) {
                remember(lineColor) {
                    LineCartesianLayer.PointProvider.single(
                        LineCartesianLayer.Point(
                            component = ShapeComponent(fill = Fill(lineColor), shape = CircleShape),
                            size = 5.dp,
                        ),
                    )
                }
            } else {
                null
            }
            val guideLines = guideDecorations(guides)

            ProvideVicoTheme(rememberM3VicoTheme()) {
                CartesianChartHost(
                    chart = rememberCartesianChart(
                        rememberLineCartesianLayer(
                            lineProvider = LineCartesianLayer.LineProvider.series(
                                LineCartesianLayer.rememberLine(
                                    fill = remember(lineColor) {
                                        LineCartesianLayer.LineFill.single(Fill(lineColor))
                                    },
                                    areaFill = areaFill,
                                    pointProvider = pointProvider,
                                ),
                            ),
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
                        bottomAxis = HorizontalAxis.rememberBottom(
                            valueFormatter = remember(sampleRateHz, values.size) {
                                TimeAxisFormatter(sampleRateHz, values.lastIndex)
                            },
                            // Label every whole second: at the default spacing several
                            // labels fall inside one second and read as duplicates.
                            itemPlacer = remember(sampleRateHz, values.size) {
                                HorizontalAxis.ItemPlacer.aligned(
                                    spacing = { labelSpacing(sampleRateHz, values.size) },
                                )
                            },
                        ),
                        marker = rememberDefaultCartesianMarker(
                            label = markerLabel(labelStyle),
                            valueFormatter = markerFormatter,
                        ),
                        decorations = guideLines,
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
                    // fillMaxSize, not just width: otherwise the host keeps its
                    // intrinsic height and floats in the middle of the space it was
                    // given.
                    modifier = Modifier.fillMaxSize(),
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

/**
 * Labels the x axis in seconds before the newest sample when the sample rate is
 * known; recorded logs have no timing, so they fall back to the sample index.
 */
private class TimeAxisFormatter(
    private val sampleRateHz: Double?,
    private val lastIndex: Int,
) : CartesianValueFormatter {
    override fun format(
        context: com.patrykandpatrick.vico.compose.cartesian.CartesianMeasuringContext,
        value: Double,
        verticalAxisPosition: com.patrykandpatrick.vico.compose.cartesian.axis.Axis.Position.Vertical?,
    ): CharSequence {
        return xAxisLabel(value, sampleRateHz, lastIndex)
    }
}

/** Sample index, or seconds relative to the newest sample when the rate is known. */
internal fun xAxisLabel(value: Double, sampleRateHz: Double?, lastIndex: Int): String {
    if (sampleRateHz == null || sampleRateHz <= 0.0) return value.roundToInt().toString()
    return "${((value - lastIndex) / sampleRateHz).roundToInt()}s"
}

/**
 * Sample indices between two x labels, chosen so that labels land on whole seconds
 * (~[X_LABEL_COUNT] of them). Anything finer repeats the same rounded second twice.
 */
internal fun labelSpacing(sampleRateHz: Double?, size: Int): Int {
    if (sampleRateHz == null || sampleRateHz <= 0.0) {
        return max(1, ceil(size.toDouble() / X_LABEL_COUNT).toInt())
    }
    val seconds = max(1.0, ceil(size / sampleRateHz / X_LABEL_COUNT))
    return max(1, (seconds * sampleRateHz).roundToInt())
}

/** Min / max / average as guide lines, in the same order as [ChartLegend]. */
@Composable
private fun guideDecorations(stats: WindowStats?): List<Decoration> {
    if (stats == null) return emptyList()
    val colors = guideColors()
    return remember(stats, colors) {
        listOf(stats.max, stats.average, stats.min).mapIndexed { index, value ->
            HorizontalLine(
                y = { value },
                line = LineComponent(
                    fill = Fill(colors[index]),
                    thickness = GUIDE_THICKNESS,
                    // Dashed, so a guide is never mistaken for the measured trace.
                    shape = DashedShape(dashLength = 8.dp, gapLength = 6.dp),
                ),
            )
        }
    }
}

@Composable
private fun guideColors(): List<Color> = listOf(
    MaterialTheme.colorScheme.error,
    MaterialTheme.colorScheme.tertiary,
    MaterialTheme.colorScheme.secondary,
)

/** Names the guide lines drawn by [MeasurementChart] when it is given statistics. */
@Composable
fun ChartLegend(
    stats: WindowStats,
    modifier: Modifier = Modifier,
    unit: String = "",
) {
    val colors = guideColors()
    val labels = listOf(
        stringResource(R.string.stats_max) to stats.max,
        stringResource(R.string.stats_avg) to stats.average,
        stringResource(R.string.stats_min) to stats.min,
    )
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        labels.forEachIndexed { index, (label, value) ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 14.dp, height = GUIDE_THICKNESS)
                        .background(colors[index]),
                )
                Text(
                    text = "$label ${formatMeasurement(value, unit)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

private const val X_LABEL_COUNT = 5
private const val MAX_DOTTED_SAMPLES = 80
private val GUIDE_THICKNESS = 2.dp

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
