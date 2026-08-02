package jk.ut61eTool.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

/**
 * Runs [content] at a density scaled by [factor].
 *
 * Scaling the density rather than the text style zooms everything inside at once —
 * font sizes, fixed slot widths and paddings keep their proportions — which is what
 * a readout needs: it should get smaller, not rearrange itself.
 */
@Composable
internal fun ScaledDensity(factor: Float, content: @Composable () -> Unit) {
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(
            density = density.density * factor,
            fontScale = density.fontScale,
        ),
        content = content,
    )
}

/**
 * The factor that fits [required] pixels into [available], never scaling up.
 *
 * At a large display zoom or accessibility font size a readout needs more room than
 * its line has, and a single-line Text simply drops what does not fit — the meter
 * showed `-4.` instead of `-4.9312`, silently losing the least significant digits.
 * Shrinking keeps every digit.
 */
internal fun fitFactor(required: Float, available: Int): Float =
    if (required <= available || required <= 0f) {
        1f
    } else {
        available / required * FIT_MARGIN
    }

/**
 * Slack left when scaling down. Glyph widths do not scale perfectly linearly, so an
 * exact fit computed from the unscaled measurement still lost its last character.
 */
private const val FIT_MARGIN = 0.97f
