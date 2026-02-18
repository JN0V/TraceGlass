package io.github.jn0v.traceglass.feature.tracing

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.jn0v.traceglass.core.camera.CameraManager
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
    // Collect render matrix as a separate State — read only in draw phase to avoid recomposition
    val renderMatrixState = viewModel.renderMatrix.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.restoreSession()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> scope.launch { viewModel.saveSession() }
                Lifecycle.Event.ON_RESUME -> cameraManager.reapplyZoom()
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
        uri?.let {
            val internalUri = ImageFileHelper.copyImageToInternal(context, it)
            viewModel.onImageSelected(internalUri ?: it)
        }
    }

    if (uiState.showResumeSessionDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onResumeSessionDeclined() },
            title = { Text("Resume session?") },
            text = { Text("A previous tracing session was found. Resume with the same settings?") },
            confirmButton = {
                TextButton(onClick = { viewModel.onResumeSessionAccepted() }) {
                    Text("Resume")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onResumeSessionDeclined() }) {
                    Text("New session")
                }
            }
        )
    }

    when (uiState.permissionState) {
        PermissionState.NOT_REQUESTED -> {
            LaunchedEffect(Unit) {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        PermissionState.GRANTED -> {
            TracingContent(
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
                renderMatrixState = renderMatrixState,
                onOverlayGesture = viewModel::onOverlayGesture,
                onSetViewDimensions = viewModel::setViewDimensions,
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
                },
                isOverlayLocked = uiState.isOverlayLocked,
                viewportZoom = uiState.viewportZoom,
                viewportPanX = uiState.viewportPanX,
                viewportPanY = uiState.viewportPanY,
                showUnlockConfirmDialog = uiState.showUnlockConfirmDialog,
                onToggleLock = viewModel::onToggleLock,
                onRequestUnlock = viewModel::onRequestUnlock,
                onConfirmUnlock = viewModel::onConfirmUnlock,
                onDismissUnlockDialog = viewModel::onDismissUnlockDialog,
                onViewportZoom = viewModel::onViewportZoom,
                onViewportPan = viewModel::onViewportPan,
                showLockSnackbar = uiState.showLockSnackbar,
                onLockSnackbarShown = viewModel::onLockSnackbarShown
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
