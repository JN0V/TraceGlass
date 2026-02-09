package io.github.jn0v.traceglass.feature.tracing

import android.Manifest
import android.net.Uri
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.rememberAsyncImagePainter
import io.github.jn0v.traceglass.core.camera.CameraManager
import io.github.jn0v.traceglass.feature.tracing.components.OpacityFab
import io.github.jn0v.traceglass.feature.tracing.components.TrackingIndicator
import io.github.jn0v.traceglass.feature.tracing.components.VisualModeControls
import org.koin.compose.koinInject
import org.koin.androidx.compose.koinViewModel

@Composable
fun TracingScreen(
    viewModel: TracingViewModel = koinViewModel(),
    cameraManager: CameraManager = koinInject(),
    frameAnalyzer: FrameAnalyzer = koinInject(),
    onNavigateToSettings: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.restoreSession()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> scope.launch { viewModel.saveSession() }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(frameAnalyzer) {
        frameAnalyzer.latestResult.collect { result ->
            viewModel.onMarkerResultReceived(result)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onPermissionResult(granted)
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        viewModel.onImageSelected(uri)
    }

    when (uiState.permissionState) {
        PermissionState.NOT_REQUESTED -> {
            LaunchedEffect(Unit) {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        PermissionState.GRANTED -> {
            CameraPreviewContent(
                cameraManager = cameraManager,
                frameAnalyzer = frameAnalyzer,
                isTorchOn = uiState.isTorchOn,
                hasFlashlight = uiState.hasFlashlight,
                onToggleTorch = viewModel::onToggleTorch,
                overlayImageUri = uiState.overlayImageUri,
                overlayOpacity = uiState.overlayOpacity,
                isOpacitySliderVisible = uiState.isOpacitySliderVisible,
                onToggleOpacitySlider = viewModel::onToggleOpacitySlider,
                onOpacityChanged = viewModel::onOpacityChanged,
                colorTint = uiState.colorTint,
                isInvertedMode = uiState.isInvertedMode,
                onColorTintChanged = viewModel::onColorTintChanged,
                onToggleInvertedMode = viewModel::onToggleInvertedMode,
                overlayOffset = uiState.overlayOffset,
                overlayScale = uiState.overlayScale,
                overlayRotation = uiState.overlayRotation,
                onOverlayDrag = viewModel::onOverlayDrag,
                onOverlayScale = viewModel::onOverlayScale,
                isSessionActive = uiState.isSessionActive,
                areControlsVisible = uiState.areControlsVisible,
                onToggleSession = viewModel::onToggleSession,
                onToggleControlsVisibility = viewModel::onToggleControlsVisibility,
                trackingState = uiState.trackingState,
                showBreakReminder = uiState.showBreakReminder,
                audioFeedbackEnabled = uiState.audioFeedbackEnabled,
                onBreakReminderDismissed = viewModel::onBreakReminderDismissed,
                onNavigateToSettings = onNavigateToSettings,
                onPickImage = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            )
        }

        PermissionState.DENIED -> {
            PermissionDeniedContent(
                onRequestPermission = {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }
            )
        }
    }
}

@Composable
private fun CameraPreviewContent(
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
    overlayOffset: Offset,
    overlayScale: Float,
    overlayRotation: Float,
    onOverlayDrag: (Offset) -> Unit,
    onOverlayScale: (Float) -> Unit,
    isSessionActive: Boolean,
    areControlsVisible: Boolean,
    onToggleSession: () -> Unit,
    onToggleControlsVisibility: () -> Unit,
    trackingState: TrackingState,
    showBreakReminder: Boolean,
    audioFeedbackEnabled: Boolean,
    onBreakReminderDismissed: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onPickImage: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(showBreakReminder) {
        if (showBreakReminder) {
            if (audioFeedbackEnabled) {
                AudioFeedbackPlayer(context).playBreakReminderTone()
            }
            snackbarHostState.showSnackbar("Time for a break!")
            onBreakReminderDismissed()
        }
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

    Box(modifier = Modifier.fillMaxSize()) {
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
            Image(
                painter = rememberAsyncImagePainter(model = overlayImageUri),
                contentDescription = "Overlay reference image",
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = overlayOffset.x
                        translationY = overlayOffset.y
                        scaleX = overlayScale
                        scaleY = overlayScale
                        rotationZ = overlayRotation
                    }
                    .alpha(effectiveOpacity)
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            onOverlayDrag(pan)
                            if (zoom != 1f) onOverlayScale(zoom)
                        }
                    },
                contentScale = ContentScale.Fit,
                colorFilter = colorTint.toColorFilter()
            )
        }

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
            }

            FloatingActionButton(
                onClick = onPickImage,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Pick reference image"
                )
            }

            if (overlayImageUri != null) {
                VisualModeControls(
                    colorTint = colorTint,
                    isInvertedMode = isInvertedMode,
                    onColorTintChanged = onColorTintChanged,
                    onToggleInvertedMode = onToggleInvertedMode,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(16.dp)
                )

                FloatingActionButton(
                    onClick = onToggleSession,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .size(56.dp),
                    containerColor = if (isSessionActive)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary
                ) {
                    Text(if (isSessionActive) "Stop" else "Start")
                }
            }

            TrackingIndicator(
                trackingState = trackingState,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(16.dp)
            )

            FloatingActionButton(
                onClick = onNavigateToSettings,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(16.dp)
                    .size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Settings"
                )
            }

            if (hasFlashlight) {
                FloatingActionButton(
                    onClick = onToggleTorch,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                        .size(48.dp)
                ) {
                    Icon(
                        imageVector = if (isTorchOn) Icons.Filled.Star else Icons.Outlined.Star,
                        contentDescription = if (isTorchOn) "Turn off flashlight" else "Turn on flashlight"
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
                .padding(bottom = 80.dp)
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraManager.unbind()
        }
    }
}

@Composable
private fun PermissionDeniedContent(onRequestPermission: () -> Unit) {
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
