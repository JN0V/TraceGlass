package io.github.jn0v.traceglass.feature.onboarding

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.rememberAsyncImagePainter
import io.github.jn0v.traceglass.core.camera.CameraManager
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import java.io.File

@Composable
fun WalkthroughScreen(
    viewModel: WalkthroughViewModel = koinViewModel(),
    cameraManager: CameraManager = koinInject(),
    walkthroughAnalyzer: WalkthroughAnalyzer = koinInject(),
    onComplete: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val markersFound by walkthroughAnalyzer.markersFound.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var permissionGranted by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            val internalUri = copyImageToInternal(context, it)
            if (internalUri != null) {
                viewModel.onImagePicked(internalUri.toString())
            }
        }
    }

    // Request camera permission on first composition
    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // Start detection when permission is granted
    LaunchedEffect(permissionGranted) {
        if (permissionGranted) {
            viewModel.startDetection()
        }
    }

    // Bridge marker detection to ViewModel
    LaunchedEffect(markersFound) {
        if (markersFound && uiState.step == WalkthroughStep.DETECTING_MARKERS) {
            viewModel.onMarkersDetected()
        }
    }

    // Auto-launch photo picker when step transitions to PICK_IMAGE
    LaunchedEffect(uiState.step) {
        if (uiState.step == WalkthroughStep.PICK_IMAGE) {
            photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
    }

    // Navigate to tracing when walkthrough completes
    LaunchedEffect(uiState.step) {
        if (uiState.step == WalkthroughStep.COMPLETED) {
            onComplete()
        }
    }

    if (!permissionGranted) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.walkthrough_camera_permission_required),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(32.dp)
            )
        }
        return
    }

    // Main walkthrough content with camera preview
    Box(modifier = Modifier.fillMaxSize()) {
        // Camera preview
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).also { previewView ->
                    cameraManager.bindPreview(
                        lifecycleOwner,
                        previewView.surfaceProvider,
                        walkthroughAnalyzer
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay image when in SHOW_TOOLTIP state
        if (uiState.imageUri != null && uiState.step == WalkthroughStep.SHOW_TOOLTIP) {
            Image(
                painter = rememberAsyncImagePainter(model = Uri.parse(uiState.imageUri)),
                contentDescription = "Selected reference image",
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.5f),
                contentScale = ContentScale.Fit
            )
        }

        // Step-specific overlays
        Box(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
            when (uiState.step) {
                WalkthroughStep.DETECTING_MARKERS -> {
                    DetectingOverlay(
                        elapsedSeconds = uiState.elapsedSeconds,
                        showGuidance = uiState.showGuidance
                    )
                }

                WalkthroughStep.MARKERS_FOUND -> {
                    MarkersFoundOverlay(
                        onContinue = viewModel::onProceedToPickImage
                    )
                }

                WalkthroughStep.PICK_IMAGE -> {
                    // Photo picker is launched via LaunchedEffect above
                    // Show a subtle waiting message
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.walkthrough_select_reference_image),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                WalkthroughStep.SHOW_TOOLTIP -> {
                    TooltipOverlay(onDismiss = viewModel::onTooltipDismissed)
                }

                WalkthroughStep.COMPLETED -> {
                    // Navigation handled by LaunchedEffect
                }
            }
        }
    }
}

private fun copyImageToInternal(context: android.content.Context, sourceUri: Uri): Uri? {
    return try {
        val stream = context.contentResolver.openInputStream(sourceUri) ?: return null
        stream.use { input ->
            val mimeType = context.contentResolver.getType(sourceUri)
            val extension = when {
                mimeType?.contains("png") == true -> ".png"
                mimeType?.contains("webp") == true -> ".webp"
                else -> ".jpg"
            }
            val file = File(context.filesDir, "reference_image$extension")
            val tempFile = File(context.filesDir, "reference_image_tmp$extension")
            tempFile.outputStream().use { output -> input.copyTo(output) }
            if (!tempFile.renameTo(file)) {
                tempFile.delete()
                return@use null
            }
            context.filesDir.listFiles { f ->
                f.name.startsWith("reference_image") && f != file
            }?.forEach { it.delete() }
            Uri.fromFile(file)
        }
    } catch (_: Exception) {
        null
    }
}
