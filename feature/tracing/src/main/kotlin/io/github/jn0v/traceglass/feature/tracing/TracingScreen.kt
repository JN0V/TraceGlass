package io.github.jn0v.traceglass.feature.tracing

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.jn0v.traceglass.core.camera.CameraManager
import org.koin.compose.koinInject
import org.koin.androidx.compose.koinViewModel

@Composable
fun TracingScreen(
    viewModel: TracingViewModel = koinViewModel(),
    cameraManager: CameraManager = koinInject()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onPermissionResult(granted)
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
                isTorchOn = uiState.isTorchOn,
                hasFlashlight = uiState.hasFlashlight,
                onToggleTorch = viewModel::onToggleTorch
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
    isTorchOn: Boolean,
    hasFlashlight: Boolean,
    onToggleTorch: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { context ->
                PreviewView(context).also { previewView ->
                    cameraManager.bindPreview(lifecycleOwner, previewView.surfaceProvider)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (hasFlashlight) {
            FloatingActionButton(
                onClick = onToggleTorch,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
                    .size(48.dp)
            ) {
                Icon(
                    imageVector = if (isTorchOn) Icons.Filled.Info else Icons.Outlined.Info,
                    contentDescription = if (isTorchOn) "Turn off flashlight" else "Turn on flashlight"
                )
            }
        }
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
