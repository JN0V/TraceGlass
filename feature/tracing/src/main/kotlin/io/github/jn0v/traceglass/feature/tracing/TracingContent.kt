package io.github.jn0v.traceglass.feature.tracing

import android.net.Uri
import android.view.WindowManager
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import coil3.compose.rememberAsyncImagePainter
import io.github.jn0v.traceglass.core.camera.CameraManager
import io.github.jn0v.traceglass.core.overlay.MatrixUtils
import io.github.jn0v.traceglass.feature.tracing.components.ExpandableMenu
import io.github.jn0v.traceglass.feature.tracing.components.ExpandableMenuItem
import io.github.jn0v.traceglass.feature.tracing.components.LockButton
import io.github.jn0v.traceglass.feature.tracing.components.OpacityFab
import io.github.jn0v.traceglass.feature.tracing.components.TrackingIndicator
import io.github.jn0v.traceglass.feature.tracing.components.VisualModeControls

@Composable
internal fun TracingContent(
    cameraManager: CameraManager,
    frameAnalyzer: FrameAnalyzer,
    isTorchOn: Boolean,
    hasFlashlight: Boolean,
    onToggleTorch: () -> Unit,
    overlayImageUri: Uri?,
    overlayOpacity: Float,
    isOpacitySliderVisible: Boolean,
    onToggleOpacitySlider: () -> Unit,
    onOpacityChanged: (Float) -> Unit,
    colorTint: ColorTint,
    isInvertedMode: Boolean,
    onColorTintChanged: (ColorTint) -> Unit,
    onToggleInvertedMode: () -> Unit,
    renderMatrixState: State<FloatArray?>,
    onOverlayGesture: (centroid: Offset, pan: Offset, zoom: Float, rotation: Float) -> Unit,
    onSetViewDimensions: (Float, Float) -> Unit,
    isSessionActive: Boolean,
    areControlsVisible: Boolean,
    onToggleSession: () -> Unit,
    onToggleControlsVisibility: () -> Unit,
    trackingState: TrackingState,
    showBreakReminder: Boolean,
    audioFeedbackEnabled: Boolean,
    onBreakReminderDismissed: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onPickImage: () -> Unit,
    isOverlayLocked: Boolean = false,
    viewportZoom: Float = 1f,
    viewportPanX: Float = 0f,
    viewportPanY: Float = 0f,
    showUnlockConfirmDialog: Boolean = false,
    onToggleLock: () -> Unit = {},
    onRequestUnlock: () -> Unit = {},
    onConfirmUnlock: () -> Unit = {},
    onDismissUnlockDialog: () -> Unit = {},
    onViewportZoom: (Float) -> Unit = {},
    onViewportPan: (Offset) -> Unit = {},
    showLockSnackbar: Boolean = false,
    onLockSnackbarShown: () -> Unit = {}
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val audioPlayer = remember { AudioFeedbackPlayer(context) }

    LaunchedEffect(showBreakReminder) {
        if (showBreakReminder) {
            if (audioFeedbackEnabled) {
                audioPlayer.playBreakReminderTone()
            }
            snackbarHostState.showSnackbar("Time for a break!")
            onBreakReminderDismissed()
        }
    }

    // Show snackbar only on explicit lock action (not on session restore)
    LaunchedEffect(showLockSnackbar) {
        if (showLockSnackbar) {
            snackbarHostState.showSnackbar("Overlay locked")
            onLockSnackbarShown()
        }
    }

    val prevTrackingState = remember { mutableStateOf(TrackingState.INACTIVE) }
    LaunchedEffect(trackingState) {
        if (audioFeedbackEnabled) {
            when {
                prevTrackingState.value == TrackingState.TRACKING && trackingState == TrackingState.LOST ->
                    audioPlayer.playTrackingLostTone()
                prevTrackingState.value == TrackingState.LOST && trackingState == TrackingState.TRACKING ->
                    audioPlayer.playTrackingGainedTone()
            }
        }
        prevTrackingState.value = trackingState
    }

    DisposableEffect(isSessionActive) {
        val window = (context as? android.app.Activity)?.window
        if (isSessionActive) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Unlock confirmation dialog (Task 5)
    if (showUnlockConfirmDialog) {
        AlertDialog(
            onDismissRequest = onDismissUnlockDialog,
            title = { Text("Unlock overlay?") },
            text = { Text("Your current position will be adjustable again.") },
            confirmButton = {
                TextButton(onClick = onConfirmUnlock) {
                    Text("Unlock")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissUnlockDialog) {
                    Text("Cancel")
                }
            }
        )
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .onSizeChanged { size ->
            onSetViewDimensions(size.width.toFloat(), size.height.toFloat())
        }
    ) {
        // Viewport container: camera + overlay zoom/pan together (Task 6)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .graphicsLayer {
                    scaleX = viewportZoom
                    scaleY = viewportZoom
                    translationX = viewportPanX
                    translationY = viewportPanY
                }
        ) {
            AndroidView(
                factory = { context ->
                    PreviewView(context).also { previewView ->
                        cameraManager.bindPreview(lifecycleOwner, previewView.surfaceProvider, frameAnalyzer)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            if (overlayImageUri != null) {
                val effectiveOpacity = if (isInvertedMode) 1f - overlayOpacity else overlayOpacity
                // Cache objects to avoid per-draw allocations
                val matrixObj = remember { android.graphics.Matrix() }
                val identityValues = remember { MatrixUtils.identity() }
                Image(
                    painter = rememberAsyncImagePainter(model = overlayImageUri),
                    contentDescription = "Overlay reference image",
                    modifier = Modifier
                        .fillMaxSize()
                        .drawWithContent {
                            drawIntoCanvas { canvas ->
                                // Read State.value here (draw phase only) — no recomposition triggered
                                matrixObj.setValues(renderMatrixState.value ?: identityValues)
                                canvas.nativeCanvas.save()
                                canvas.nativeCanvas.concat(matrixObj)
                                drawContent()
                                canvas.nativeCanvas.restore()
                            }
                        }
                        .alpha(effectiveOpacity),
                    contentScale = ContentScale.Fit,
                    colorFilter = colorTint.toColorFilter()
                )
            }
        }

        // Gesture detection layer — routes to viewport or overlay based on lock state (Task 3)
        if (overlayImageUri != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(isOverlayLocked) {
                        detectTransformGestures { centroid, pan, zoom, rotation ->
                            if (isOverlayLocked) {
                                // After lock: gestures control viewport (no rotation for viewport)
                                if (zoom != 1f) onViewportZoom(zoom)
                                onViewportPan(pan)
                            } else {
                                // Before lock: gestures control overlay image positioning
                                onOverlayGesture(centroid, pan, zoom, rotation)
                            }
                        }
                    }
            )
        }

        // TrackingIndicator always visible (AC3: visible even when controls hidden)
        TrackingIndicator(
            trackingState = trackingState,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(16.dp)
        )

        if (areControlsVisible) {
            if (overlayImageUri != null) {
                OpacityFab(
                    opacity = overlayOpacity,
                    isSliderVisible = isOpacitySliderVisible,
                    onToggleSlider = onToggleOpacitySlider,
                    onOpacityChanged = onOpacityChanged,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(16.dp)
                )

                VisualModeControls(
                    colorTint = colorTint,
                    isInvertedMode = isInvertedMode,
                    onColorTintChanged = onColorTintChanged,
                    onToggleInvertedMode = onToggleInvertedMode,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(start = 16.dp, top = 80.dp)
                )

                // Lock button (Task 7) — above session FAB
                LockButton(
                    isLocked = isOverlayLocked,
                    onLockClick = onToggleLock,
                    onUnlockClick = onRequestUnlock,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 84.dp)
                )

                FloatingActionButton(
                    onClick = onToggleSession,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(16.dp)
                        .size(56.dp),
                    containerColor = if (isSessionActive)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary
                ) {
                    Text(if (isSessionActive) "Stop" else "Start")
                }

                ExpandableMenu(
                    items = buildList {
                        add(ExpandableMenuItem(
                            icon = Icons.Filled.Settings,
                            label = "Settings",
                            onClick = onNavigateToSettings
                        ))
                        if (hasFlashlight) {
                            add(ExpandableMenuItem(
                                icon = if (isTorchOn) Icons.Filled.FlashlightOn else Icons.Filled.FlashlightOff,
                                label = if (isTorchOn) "Turn off flashlight" else "Turn on flashlight",
                                onClick = onToggleTorch
                            ))
                        }
                        add(ExpandableMenuItem(
                            icon = Icons.Filled.Add,
                            label = "Pick reference image",
                            onClick = onPickImage
                        ))
                    },
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .navigationBarsPadding()
                        .padding(16.dp)
                )
            } else {
                FloatingActionButton(
                    onClick = onPickImage,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .navigationBarsPadding()
                        .padding(16.dp)
                        .size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Pick reference image"
                    )
                }
            }
        }

        if (!areControlsVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { onToggleControlsVisibility() }
                    }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 80.dp)
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraManager.unbind()
        }
    }
}

@Preview(showBackground = true)
@Composable
internal fun PermissionDeniedContent(onRequestPermission: () -> Unit = {}) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "Camera permission required",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "TraceGlass needs camera access to display your drawing surface. No images are stored or sent anywhere.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onRequestPermission) {
                Text("Grant Permission")
            }
        }
    }
}
