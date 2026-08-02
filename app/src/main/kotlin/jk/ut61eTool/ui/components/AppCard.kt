package jk.ut61eTool.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * The card used across the app: an elevated card that also states its own edge.
 *
 * Material's elevated card fills with `surfaceContainerLow`, the tone closest to the
 * screen background, and leaves its shadow to separate the two. Some vendor palettes
 * (One UI, light theme) flatten the neutral tones until card and background are the
 * same grey and the shadow is invisible, so the cards disappear. The hairline outline
 * does not depend on that tonal gap. `ElevatedCard` takes no border, hence `Card`
 * carrying the elevated card's own colors and elevation.
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
 * Card colors for [AppCard], one container step further from the background than
 * Material's elevated default so the fill helps where the palette allows it.
 */
@Composable
fun appCardColors(
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
): CardColors = CardDefaults.elevatedCardColors(
    containerColor = containerColor,
    contentColor = contentColor,
)

/**
 * `outlineVariant` is a mid-tone by construction, so it stays visible against the
 * card and the background whatever the palette does to those two.
 */
@Composable
private fun cardBorder(): BorderStroke =
    BorderStroke(CARD_BORDER, MaterialTheme.colorScheme.outlineVariant)

private val CARD_BORDER = 1.dp
