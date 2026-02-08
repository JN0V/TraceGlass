package io.github.jn0v.traceglass.feature.tracing.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun OpacityFab(
    opacity: Float,
    isSliderVisible: Boolean,
    onToggleSlider: () -> Unit,
    onOpacityChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    if (isSliderVisible) {
        LaunchedEffect(Unit) {
            delay(3000)
            onToggleSlider()
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        AnimatedVisibility(
            visible = isSliderVisible,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text(
                    text = "${(opacity * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Slider(
                    value = opacity,
                    onValueChange = onOpacityChanged,
                    valueRange = 0f..1f,
                    steps = 19,
                    modifier = Modifier
                        .height(200.dp)
                        .semantics {
                            contentDescription =
                                "Overlay opacity: ${(opacity * 100).toInt()}%"
                        }
                )
            }
        }

        FloatingActionButton(
            onClick = onToggleSlider,
            modifier = Modifier.size(48.dp)
        ) {
            Text(
                text = "${(opacity * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
