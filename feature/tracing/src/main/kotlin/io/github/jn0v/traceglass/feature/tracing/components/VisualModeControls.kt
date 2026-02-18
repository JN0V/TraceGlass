package io.github.jn0v.traceglass.feature.tracing.components

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
import androidx.compose.ui.unit.dp
import io.github.jn0v.traceglass.feature.tracing.ColorTint

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
                    label = { Text(tint.label, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        FilterChip(
            selected = isInvertedMode,
            onClick = onToggleInvertedMode,
            label = { Text("Inverted", style = MaterialTheme.typography.labelSmall) }
        )
    }
}

private val ColorTint.label: String
    get() = when (this) {
        ColorTint.NONE -> "None"
        ColorTint.RED -> "Red"
        ColorTint.GREEN -> "Green"
        ColorTint.BLUE -> "Blue"
        ColorTint.GRAYSCALE -> "Gray"
    }
