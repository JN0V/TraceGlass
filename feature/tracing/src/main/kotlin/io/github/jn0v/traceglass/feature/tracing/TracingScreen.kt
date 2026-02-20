package io.github.jn0v.traceglass.feature.tracing

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
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
    val cameraError by cameraManager.cameraError.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(Unit) {
        viewModel.restoreSession()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> viewModel.saveOnStop()
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

    if (uiState.showTimelapseRestoreDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onTimelapseRestoreDiscard() },
            title = { Text(stringResource(R.string.timelapse_restore_title)) },
            text = { Text(pluralStringResource(R.plurals.timelapse_restore_message, uiState.pendingTimelapseSnapshotCount, uiState.pendingTimelapseSnapshotCount)) },
            confirmButton = {
                TextButton(onClick = { viewModel.onTimelapseRestoreContinue() }) {
                    Text(stringResource(R.string.timelapse_restore_continue))
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { viewModel.onTimelapseRestoreDiscard() }) {
                        Text(stringResource(R.string.timelapse_restore_discard))
                    }
                    TextButton(onClick = { viewModel.onTimelapseRestoreCompile() }) {
                        Text(stringResource(R.string.timelapse_restore_compile))
                    }
                }
            }
        )
    }

    if (uiState.showResumeSessionDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onResumeSessionDeclined() },
            title = { Text(stringResource(R.string.resume_session_title)) },
            text = { Text(stringResource(R.string.resume_session_message)) },
            confirmButton = {
                TextButton(onClick = { viewModel.onResumeSessionAccepted() }) {
                    Text(stringResource(R.string.resume_session_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onResumeSessionDeclined() }) {
                    Text(stringResource(R.string.resume_session_decline))
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
                previousTrackingState = uiState.previousTrackingState,
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
                onLockSnackbarShown = viewModel::onLockSnackbarShown,
                isTimelapseRecording = uiState.isTimelapseRecording,
                isTimelapsePaused = uiState.isTimelapsePaused,
                snapshotCount = uiState.snapshotCount,
                onStartTimelapse = viewModel::startTimelapse,
                onPauseTimelapse = viewModel::pauseTimelapse,
                onResumeTimelapse = viewModel::resumeTimelapse,
                onStopTimelapse = viewModel::stopTimelapse,
                isCompiling = uiState.isCompiling,
                compilationProgress = uiState.compilationProgress,
                compiledVideoPath = uiState.compiledVideoPath,
                compilationError = uiState.compilationError,
                onCompilationErrorShown = viewModel::onCompilationErrorShown,
                onCompilationCompleteShown = viewModel::onCompilationCompleteShown,
                showPostCompilationDialog = uiState.showPostCompilationDialog,
                isExporting = uiState.isExporting,
                exportSuccessMessage = uiState.exportSuccessMessage,
                exportError = uiState.exportError,
                onExportToGallery = viewModel::exportTimelapse,
                onShare = viewModel::shareTimelapse,
                onDiscard = viewModel::discardTimelapse,
                onDismissPostCompilationDialog = viewModel::onDismissPostCompilationDialog,
                onExportSuccessShown = viewModel::onExportSuccessShown,
                onExportErrorShown = viewModel::onExportErrorShown,
                cameraError = cameraError,
                onCameraErrorShown = viewModel::onCameraErrorShown
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
