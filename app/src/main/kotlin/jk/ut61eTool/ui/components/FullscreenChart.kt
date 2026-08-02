package jk.ut61eTool.ui.components

import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import jk.ut61eTool.R
import jk.ut61eTool.data.WindowStats

/**
 * The graph alone, in landscape, with min / max / average drawn as guide lines over
 * the deviation band. Rotation is forced while it is open and released on the way
 * out; back closes it.
 *
 * Shared by the live view and the log viewer — a recorded series deserves the same
 * reading aids as a live one, the only difference being that logs carry no timing,
 * so they pass no [sampleRateHz] and the x axis stays in sample indices.
 */
@Composable
fun FullscreenChart(
    values: List<Double>,
    unit: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "",
    sampleRateHz: Double? = null,
    stats: WindowStats? = null,
    capacity: Int? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val activity = LocalActivity.current
    DisposableEffect(activity) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
    BackHandler(onBack = onClose)

    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    actions()
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.FullscreenExit,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                }
            }
            MeasurementChart(
                values = values,
                unit = unit,
                sampleRateHz = sampleRateHz,
                height = null,
                guides = stats,
                capacity = capacity,
                modifier = Modifier.weight(1f),
            )
            stats?.let { ChartLegend(stats = it, unit = unit) }
        }
    }
}
