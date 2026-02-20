package io.github.jn0v.traceglass.feature.tracing.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.jn0v.traceglass.feature.tracing.ColorTint
import io.github.jn0v.traceglass.feature.tracing.R

@Composable
fun VisualModeControls(
    colorTint: ColorTint,
    isInvertedMode: Boolean,
    onColorTintChanged: (ColorTint) -> Unit,
    onToggleInvertedMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            ColorTint.entries.forEach { tint ->
                FilterChip(
                    selected = colorTint == tint,
                    onClick = { onColorTintChanged(tint) },
                    label = { Text(stringResource(tint.labelResId), style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        FilterChip(
            selected = isInvertedMode,
            onClick = onToggleInvertedMode,
            label = { Text(stringResource(R.string.visual_mode_inverted), style = MaterialTheme.typography.labelSmall) }
        )
    }
}

private val ColorTint.labelResId: Int
    @StringRes get() = when (this) {
        ColorTint.NONE -> R.string.color_tint_none
        ColorTint.RED -> R.string.color_tint_red
        ColorTint.GREEN -> R.string.color_tint_green
        ColorTint.BLUE -> R.string.color_tint_blue
        ColorTint.GRAYSCALE -> R.string.color_tint_gray
    }
