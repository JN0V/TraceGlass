package io.github.jn0v.traceglass.feature.tracing.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.jn0v.traceglass.feature.tracing.TrackingState

@Composable
fun TrackingIndicator(
    trackingState: TrackingState,
    modifier: Modifier = Modifier
) {
    if (trackingState == TrackingState.INACTIVE) return

    Row(
        modifier = modifier
            .background(
                color = Color.Black.copy(alpha = 0.5f),
                shape = MaterialTheme.shapes.small
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (trackingState) {
            TrackingState.TRACKING -> {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50))
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Tracking",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }
            TrackingState.LOST -> {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(TriangleShape)
                        .background(Color(0xFFFF9800))
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Lost",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }
            else -> {}
        }
    }
}

private val TriangleShape = GenericShape { size, _ ->
    moveTo(size.width / 2f, 0f)
    lineTo(size.width, size.height)
    lineTo(0f, size.height)
    close()
}
