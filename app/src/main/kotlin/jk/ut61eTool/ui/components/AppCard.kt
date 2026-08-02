package jk.ut61eTool.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

/**
 * The card used across the app: Material's elevated card, plus an edge in light theme.
 *
 * The elevated card fills with `surfaceContainerLow`, the tone closest to the screen
 * background, and leaves its shadow to separate the two. Some vendor palettes (One UI,
 * light theme) flatten the neutral tones until card and background are the same grey
 * and the shadow is invisible, so the cards disappear.
 *
 * The fix is confined to light theme, where the problem is: a dark scheme has plenty of
 * tonal room and lifting the cards there only makes the screen glow. `ElevatedCard`
 * takes no border, hence `Card` carrying the elevated card's own shape and elevation.
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    colors: CardColors = appCardColors(),
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        shape = CardDefaults.elevatedShape,
        colors = colors,
        elevation = CardDefaults.elevatedCardElevation(),
        border = cardBorder(),
        content = content,
    )
}

/** [AppCard] for a card that is itself a button. */
@Composable
fun AppCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: CardColors = appCardColors(),
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = CardDefaults.elevatedShape,
        colors = colors,
        elevation = CardDefaults.elevatedCardElevation(),
        border = cardBorder(),
        content = content,
    )
}

/**
 * Card colors for [AppCard]: in light theme one container step further from the
 * background than Material's elevated default, for whatever separation the palette
 * offers; in dark theme the elevated default, unchanged.
 */
@Composable
fun appCardColors(
    containerColor: Color = appCardContainer(),
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
): CardColors = CardDefaults.elevatedCardColors(
    containerColor = containerColor,
    contentColor = contentColor,
)

/** The default [AppCard] fill, for cards that animate their own container color. */
@Composable
fun appCardContainer(): Color = with(MaterialTheme.colorScheme) {
    if (isDark) surfaceContainerLow else surfaceContainer
}

/**
 * A hairline in light theme only. `outlineVariant` is a mid-tone by construction, so
 * it stays visible whatever the palette does to the card and the background.
 */
@Composable
private fun cardBorder(): BorderStroke? = MaterialTheme.colorScheme.takeUnless { it.isDark }
    ?.let { BorderStroke(CARD_BORDER, it.outlineVariant) }

/**
 * Read off the scheme rather than `isSystemInDarkTheme()`, so a card is never at odds
 * with the theme it is actually drawn in.
 */
private val ColorScheme.isDark: Boolean get() = surface.luminance() < 0.5f

private val CARD_BORDER = 1.dp
