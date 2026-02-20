package io.github.jn0v.traceglass.feature.tracing.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.jn0v.traceglass.feature.tracing.R
import kotlinx.coroutines.delay

data class ExpandableMenuItem(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit
)

@Composable
fun ExpandableMenu(
    items: List<ExpandableMenuItem>,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    var interactionCounter by remember { mutableIntStateOf(0) }

    if (isExpanded) {
        LaunchedEffect(isExpanded, interactionCounter) {
            delay(5000)
            isExpanded = false
        }
    }

    val context = LocalContext.current
    val menuDescription = context.resources.getQuantityString(R.plurals.menu_options_count, items.size, items.size)
    Column(
        modifier = modifier.semantics {
            contentDescription = menuDescription
        },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items.forEachIndexed { index, item ->
            val delayMs = (items.size - 1 - index) * 50
            val targetAlpha = if (isExpanded) 1f else 0f
            val targetTranslationY = if (isExpanded) 0f else 40f

            val animatedAlpha by animateFloatAsState(
                targetValue = targetAlpha,
                animationSpec = tween(durationMillis = 200, delayMillis = delayMs),
                label = "menuItemAlpha_$index"
            )
            val animatedTranslationY by animateFloatAsState(
                targetValue = targetTranslationY,
                animationSpec = tween(durationMillis = 200, delayMillis = delayMs),
                label = "menuItemTranslation_$index"
            )

            if (isExpanded || animatedAlpha > 0f) {
                SmallFloatingActionButton(
                    onClick = {
                        item.onClick()
                        isExpanded = false
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .graphicsLayer {
                            alpha = animatedAlpha
                            translationY = animatedTranslationY
                        }
                        .semantics {
                            contentDescription = item.label
                        }
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        FloatingActionButton(
            onClick = {
                isExpanded = !isExpanded
                if (isExpanded) {
                    interactionCounter++
                }
            },
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = stringResource(if (isExpanded) R.string.menu_close else R.string.menu_open)
            )
        }
    }
}
