package io.github.jn0v.traceglass.feature.tracing.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val TealAccent = Color(0xFF009688)

@Composable
internal fun LockButton(
    isLocked: Boolean,
    onLockClick: () -> Unit,
    onUnlockClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isLocked) {
        FilledIconButton(
            onClick = onUnlockClick,
            modifier = modifier.size(48.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = TealAccent
            )
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = "Unlock overlay — currently locked"
            )
        }
    } else {
        IconButton(
            onClick = onLockClick,
            modifier = modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.LockOpen,
                contentDescription = "Lock overlay position"
            )
        }
    }
}
