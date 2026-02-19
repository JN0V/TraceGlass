package io.github.jn0v.traceglass.feature.tracing

import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jn0v.traceglass.core.camera.FlashlightController
import io.github.jn0v.traceglass.core.cv.MarkerResult
import io.github.jn0v.traceglass.core.overlay.HomographySolver
import io.github.jn0v.traceglass.core.overlay.MatrixUtils
import io.github.jn0v.traceglass.core.overlay.OverlayTransform
import io.github.jn0v.traceglass.core.overlay.OverlayTransformCalculator
import io.github.jn0v.traceglass.core.overlay.TrackingStateManager
import io.github.jn0v.traceglass.core.overlay.TrackingStatus
import io.github.jn0v.traceglass.core.session.SessionData
import io.github.jn0v.traceglass.core.session.SessionRepository
import io.github.jn0v.traceglass.core.session.SettingsRepository
import io.github.jn0v.traceglass.core.timelapse.CompilationResult
import io.github.jn0v.traceglass.core.timelapse.ExportResult
import io.github.jn0v.traceglass.core.timelapse.SnapshotStorage
import io.github.jn0v.traceglass.core.timelapse.TimelapseCompiler
import io.github.jn0v.traceglass.core.timelapse.TimelapseSession
import io.github.jn0v.traceglass.core.timelapse.TimelapseState
import io.github.jn0v.traceglass.core.timelapse.VideoExporter
import io.github.jn0v.traceglass.core.timelapse.VideoSharer
import java.util.concurrent.atomic.AtomicBoolean
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

class TracingViewModel(
    private val flashlightController: FlashlightController,
    private val transformCalculator: OverlayTransformCalculator = OverlayTransformCalculator(),
    private val trackingStateManager: TrackingStateManager = TrackingStateManager(),
    private val sessionRepository: SessionRepository,
    private val settingsRepository: SettingsRepository,
    private val snapshotStorage: SnapshotStorage? = null,
    private val frameAnalyzer: FrameAnalyzer? = null,
    private val timelapseCompiler: TimelapseCompiler? = null,
    private val videoExporter: VideoExporter? = null,
    private val videoSharer: VideoSharer? = null,
    private val cacheDir: java.io.File? = null,
    private val ioDispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO,
    private val mainDispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.Main
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        TracingUiState(hasFlashlight = flashlightController.hasFlashlight)
    )
    val uiState: StateFlow<TracingUiState> = _uiState.asStateFlow()

    // Separate high-frequency flow for render matrix — avoids full tree recomposition
    private val _renderMatrix = MutableStateFlow<FloatArray?>(null)
    val renderMatrix: StateFlow<FloatArray?> = _renderMatrix.asStateFlow()

    private var breakReminderEnabled = false
    private var breakReminderIntervalMinutes = 30
    private var audioFeedbackEnabled = false
    private var breakTimerJob: Job? = null
    private var debounceSaveJob: Job? = null

    private val restoreAttempted = AtomicBoolean(false)
    private val restoreCompleted = AtomicBoolean(false)
    private var previousTransform: OverlayTransform = OverlayTransform.IDENTITY
    private var manualOffset: Offset = Offset.Zero
    private var manualScaleFactor: Float = 1f
    private var manualRotation: Float = 0f

    // View dimensions for frame→screen coordinate scaling
    private var viewWidth: Float = 1080f
    private var viewHeight: Float = 1920f
    private var lastFrameWidth: Float = 1080f
    private var lastFrameHeight: Float = 1920f

    // Cached preview→screen scale and coordinate transform matrices
    // (recomputed only when dimensions change)
    private var cachedPvScale: Float = 1f
    private var cachedF2S: FloatArray = MatrixUtils.identity()

    // Timelapse state
    private var timelapseSession: TimelapseSession? = null
    private var timelapseObserverJob: Job? = null

    init {
        flashlightController.isTorchOn
            .onEach { torchOn -> _uiState.update { it.copy(isTorchOn = torchOn) } }
            .launchIn(viewModelScope)

        settingsRepository.settingsData
            .onEach { data ->
                val intervalChanged = breakReminderIntervalMinutes != data.breakReminderIntervalMinutes
                val enabledChanged = breakReminderEnabled != data.breakReminderEnabled
                breakReminderEnabled = data.breakReminderEnabled
                breakReminderIntervalMinutes = data.breakReminderIntervalMinutes
                audioFeedbackEnabled = data.audioFeedbackEnabled
                _uiState.update { it.copy(audioFeedbackEnabled = data.audioFeedbackEnabled) }
                if (intervalChanged || enabledChanged) {
                    restartBreakTimer()
                }
            }
            .launchIn(viewModelScope)
    }

    override fun onCleared() {
        super.onCleared()
        frameAnalyzer?.close()
    }

    fun onPermissionResult(granted: Boolean) {
        _uiState.update {
            it.copy(
                permissionState = if (granted) PermissionState.GRANTED else PermissionState.DENIED
            )
        }
    }

    fun onToggleTorch() {
        flashlightController.toggleTorch()
    }

    fun onImageSelected(uri: Uri?) {
        uri ?: return
        _uiState.update { it.copy(overlayImageUri = uri) }
        viewModelScope.launch { saveSession() }
    }

    fun onOpacityChanged(opacity: Float) {
        _uiState.update { it.copy(overlayOpacity = opacity.coerceIn(0f, 1f)) }
        debounceSave()
    }

    fun onToggleOpacitySlider() {
        _uiState.update { it.copy(isOpacitySliderVisible = !it.isOpacitySliderVisible) }
    }

    fun onColorTintChanged(tint: ColorTint) {
        _uiState.update { it.copy(colorTint = tint) }
        debounceSave()
    }

    fun onToggleInvertedMode() {
        _uiState.update { it.copy(isInvertedMode = !it.isInvertedMode) }
        debounceSave()
    }

    fun onOverlayGesture(centroid: Offset, pan: Offset, zoom: Float, rotation: Float) {
        if (_uiState.value.isOverlayLocked) return
        manualOffset += pan
        if (zoom != 1f) {
            // Adjust offset so scale originates from the gesture centroid, not the view center
            val viewCenter = Offset(viewWidth / 2f, viewHeight / 2f)
            manualOffset += (centroid - viewCenter) * (1f - zoom)
            manualScaleFactor *= zoom
        }
        if (rotation != 0f) {
            manualRotation += rotation
        }
        updateOverlayFromCombined()
        debounceSave()
    }

    fun onOverlayDrag(delta: Offset) {
        if (_uiState.value.isOverlayLocked) return
        manualOffset += delta
        updateOverlayFromCombined()
        debounceSave()
    }

    fun onOverlayScale(scaleFactor: Float) {
        if (_uiState.value.isOverlayLocked) return
        manualScaleFactor *= scaleFactor
        updateOverlayFromCombined()
        debounceSave()
    }

    fun onOverlayRotate(angleDelta: Float) {
        if (_uiState.value.isOverlayLocked) return
        manualRotation += angleDelta
        updateOverlayFromCombined()
        debounceSave()
    }

    fun onToggleLock() {
        _uiState.update { it.copy(isOverlayLocked = true, showLockSnackbar = true) }
        debounceSave()
    }

    fun onLockSnackbarShown() {
        _uiState.update { it.copy(showLockSnackbar = false) }
    }

    fun onRequestUnlock() {
        _uiState.update { it.copy(showUnlockConfirmDialog = true) }
    }

    fun onConfirmUnlock() {
        _uiState.update {
            it.copy(
                isOverlayLocked = false,
                viewportZoom = 1f,
                viewportPanX = 0f,
                viewportPanY = 0f,
                showUnlockConfirmDialog = false
            )
        }
        debounceSave()
    }

    fun onDismissUnlockDialog() {
        _uiState.update { it.copy(showUnlockConfirmDialog = false) }
    }

    fun onViewportZoom(zoomFactor: Float) {
        if (!_uiState.value.isOverlayLocked) return
        _uiState.update {
            val newZoom = (it.viewportZoom * zoomFactor).coerceIn(1f, 5f)
            val maxPanX = (newZoom - 1f) * viewWidth / 2f
            val maxPanY = (newZoom - 1f) * viewHeight / 2f
            it.copy(
                viewportZoom = newZoom,
                viewportPanX = it.viewportPanX.coerceIn(-maxPanX, maxPanX),
                viewportPanY = it.viewportPanY.coerceIn(-maxPanY, maxPanY)
            )
        }
        debounceSave()
    }

    fun onViewportPan(delta: Offset) {
        if (!_uiState.value.isOverlayLocked) return
        val state = _uiState.value
        val maxPanX = (state.viewportZoom - 1f) * viewWidth / 2f
        val maxPanY = (state.viewportZoom - 1f) * viewHeight / 2f
        _uiState.update {
            it.copy(
                viewportPanX = (it.viewportPanX + delta.x).coerceIn(-maxPanX, maxPanX),
                viewportPanY = (it.viewportPanY + delta.y).coerceIn(-maxPanY, maxPanY)
            )
        }
        debounceSave()
    }

    private fun debounceSave() {
        debounceSaveJob?.cancel()
        debounceSaveJob = viewModelScope.launch {
            delay(500)
            saveSession()
        }
    }

    fun setViewDimensions(width: Float, height: Float) {
        if (width > 0f && height > 0f) {
            viewWidth = width
            viewHeight = height
            recomputeCachedMatrices()
        }
    }

    fun onToggleSession() {
        val wasActive = _uiState.value.isSessionActive
        _uiState.update { it.copy(isSessionActive = !it.isSessionActive) }
        if (wasActive) {
            stopTimelapse()
        }
        restartBreakTimer()
        viewModelScope.launch { saveSession() }
    }

    fun onToggleControlsVisibility() {
        _uiState.update { it.copy(areControlsVisible = !it.areControlsVisible) }
    }

    fun startTimelapse() {
        val storage = snapshotStorage ?: return
        val analyzer = frameAnalyzer ?: return

        // Resume existing paused session instead of creating a new one
        val existing = timelapseSession
        if (existing != null) {
            if (_uiState.value.isTimelapsePaused) {
                existing.resume(viewModelScope)
            }
            return
        }

        val session = TimelapseSession(onCapture = { index ->
            analyzer.snapshotCallback = { bytes ->
                storage.saveSnapshot(bytes, index)
            }
        })
        timelapseSession = session
        session.start(viewModelScope)
        observeTimelapseState(session)
    }

    fun pauseTimelapse() {
        val session = timelapseSession ?: return
        session.pause()
        frameAnalyzer?.snapshotCallback = null
    }

    fun resumeTimelapse() {
        val session = timelapseSession ?: return
        session.resume(viewModelScope)
    }

    fun stopTimelapse() {
        val session = timelapseSession ?: return
        session.stop()
        frameAnalyzer?.snapshotCallback = null
        timelapseObserverJob?.cancel()
        timelapseObserverJob = null
        timelapseSession = null

        _uiState.update {
            it.copy(
                isTimelapseRecording = false,
                isTimelapsePaused = false,
                snapshotCount = 0
            )
        }

        viewModelScope.launch(ioDispatcher) {
            val snapshotFiles = snapshotStorage?.getSnapshotFiles() ?: emptyList()
            if (snapshotFiles.isNotEmpty() && timelapseCompiler != null && cacheDir != null) {
                compileTimelapse(snapshotFiles)
            }
        }
    }

    private fun compileTimelapse(snapshotFiles: List<java.io.File>) {
        if (_uiState.value.isCompiling) return
        val compiler = timelapseCompiler ?: return
        val outputFile = java.io.File(cacheDir, "timelapse_output.mp4")

        _uiState.update { it.copy(isCompiling = true, compilationProgress = 0f, compilationError = null) }

        viewModelScope.launch(ioDispatcher) {
            val result = compiler.compile(
                snapshotFiles = snapshotFiles,
                outputFile = outputFile,
                fps = 10,
                onProgress = { progress ->
                    _uiState.update { it.copy(compilationProgress = progress) }
                }
            )
            when (result) {
                is CompilationResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isCompiling = false,
                            compilationProgress = 1f,
                            compiledVideoPath = result.outputFile.absolutePath,
                            showPostCompilationDialog = true
                        )
                    }
                }
                is CompilationResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isCompiling = false,
                            compilationProgress = 0f,
                            compilationError = result.message
                        )
                    }
                }
            }
        }
    }

    fun onCompilationErrorShown() {
        _uiState.update { it.copy(compilationError = null) }
    }

    fun onCompilationCompleteShown() {
        _uiState.update { it.copy(compiledVideoPath = null) }
    }

    fun exportTimelapse() {
        performExport(shareAfter = false)
    }

    fun shareTimelapse() {
        val sharer = videoSharer ?: return
        val uri = _uiState.value.exportedVideoUri
        if (uri != null) {
            _uiState.update { it.copy(showPostCompilationDialog = false) }
            sharer.shareVideo(uri)
        } else {
            performExport(shareAfter = true)
        }
    }

    private fun performExport(shareAfter: Boolean) {
        val exporter = videoExporter ?: return
        val videoPath = _uiState.value.compiledVideoPath ?: return
        val videoFile = java.io.File(videoPath)
        if (!videoFile.exists()) return

        val displayName = "TraceGlass_${DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now())}.mp4"

        _uiState.update { it.copy(isExporting = true, exportError = null) }

        viewModelScope.launch(ioDispatcher) {
            when (val result = exporter.exportToGallery(videoFile, displayName)) {
                is ExportResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isExporting = false,
                            exportedVideoUri = result.uri,
                            exportSuccessMessage = if (!shareAfter) "Video saved to Movies/TraceGlass/" else null,
                            showPostCompilationDialog = false
                        )
                    }
                    cleanupTempFiles()
                    if (shareAfter) {
                        val sharer = videoSharer ?: return@launch
                        withContext(mainDispatcher) {
                            sharer.shareVideo(result.uri)
                        }
                    }
                }
                is ExportResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isExporting = false,
                            exportError = result.message
                        )
                    }
                }
            }
        }
    }

    fun discardTimelapse() {
        if (_uiState.value.isExporting) return
        val videoPath = _uiState.value.compiledVideoPath
        _uiState.update {
            it.copy(
                compiledVideoPath = null,
                exportedVideoUri = null,
                showPostCompilationDialog = false
            )
        }
        viewModelScope.launch(ioDispatcher) {
            if (videoPath != null) {
                java.io.File(videoPath).delete()
            }
            cleanupTempFiles()
        }
    }

    fun onDismissPostCompilationDialog() {
        _uiState.update { it.copy(showPostCompilationDialog = false) }
    }

    fun onExportSuccessShown() {
        _uiState.update { it.copy(exportSuccessMessage = null) }
    }

    fun onExportErrorShown() {
        _uiState.update { it.copy(exportError = null) }
    }

    fun onCameraErrorShown() {
        _uiState.update { it.copy(cameraError = null) }
    }

    private fun cleanupTempFiles() {
        snapshotStorage?.clear()
    }

    private fun observeTimelapseState(session: TimelapseSession) {
        timelapseObserverJob?.cancel()
        timelapseObserverJob = viewModelScope.launch {
            launch {
                session.state.collect { state ->
                    _uiState.update {
                        it.copy(
                            isTimelapseRecording = state == TimelapseState.RECORDING,
                            isTimelapsePaused = state == TimelapseState.PAUSED
                        )
                    }
                }
            }
            launch {
                session.snapshotCount.collect { count ->
                    _uiState.update { it.copy(snapshotCount = count) }
                }
            }
        }
    }

    /** Called from ON_STOP — launches in viewModelScope; saveSession() uses NonCancellable internally. */
    fun saveOnStop() {
        viewModelScope.launch { saveSession() }
    }

    suspend fun saveSession() {
        if (!restoreCompleted.get()) return
        val state = _uiState.value

        withContext(NonCancellable) {
            sessionRepository.save(
                SessionData(
                    imageUri = state.overlayImageUri?.toString(),
                    overlayOffsetX = manualOffset.x,
                    overlayOffsetY = manualOffset.y,
                    overlayScale = manualScaleFactor,
                    overlayRotation = manualRotation,
                    overlayOpacity = state.overlayOpacity,
                    colorTint = state.colorTint.name,
                    isInvertedMode = state.isInvertedMode,
                    isSessionActive = state.isSessionActive,
                    timelapseSnapshotCount = state.snapshotCount,
                    isTimelapseRecording = state.isTimelapseRecording,
                    isTimelapsePaused = state.isTimelapsePaused,
                    isOverlayLocked = state.isOverlayLocked,
                    viewportZoom = state.viewportZoom,
                    viewportPanX = state.viewportPanX,
                    viewportPanY = state.viewportPanY
                )
            )
        }
    }

    suspend fun restoreSession() {
        if (!restoreAttempted.compareAndSet(false, true)) return
        try {
            val data = sessionRepository.sessionData.first()

            if (!data.hasSavedOverlay) {
                restoreCompleted.set(true)
                return
            }
            // Store pending restore data and show dialog
            pendingRestoreData = data

            _uiState.update { it.copy(showResumeSessionDialog = true) }
        } catch (e: kotlin.coroutines.cancellation.CancellationException) {
            throw e
        } catch (e: Exception) {
            // DataStore read failed (corruption, I/O) — allow retry next time
            restoreAttempted.set(false)
            restoreCompleted.set(true)
        }
    }

    @Volatile private var pendingRestoreData: SessionData? = null

    fun onResumeSessionAccepted() {
        val data = pendingRestoreData ?: return
        val imageUri = data.imageUri?.let { Uri.parse(it) }

        manualOffset = Offset(data.overlayOffsetX, data.overlayOffsetY)
        manualScaleFactor = data.overlayScale
        manualRotation = data.overlayRotation
        _uiState.update {
            it.copy(
                showResumeSessionDialog = false,
                overlayImageUri = imageUri,
                overlayOpacity = data.overlayOpacity,
                colorTint = ColorTint.entries.find { t -> t.name == data.colorTint }.also {
                    if (it == null) android.util.Log.w("TracingViewModel", "Unknown colorTint '${data.colorTint}', defaulting to NONE")
                } ?: ColorTint.NONE,
                isInvertedMode = data.isInvertedMode,
                isSessionActive = data.isSessionActive,
                isOverlayLocked = data.isOverlayLocked,
                viewportZoom = data.viewportZoom,
                viewportPanX = data.viewportPanX,
                viewportPanY = data.viewportPanY
            )
        }
        pendingRestoreData = null
        restoreCompleted.set(true)
        updateOverlayFromCombined()
        restartBreakTimer()
        restoreTimelapseState(data)
    }

    fun onResumeSessionDeclined() {
        pendingRestoreData = null
        restoreCompleted.set(true)
        _uiState.update { it.copy(showResumeSessionDialog = false) }
        resetTracking()
        viewModelScope.launch {
            sessionRepository.clear()
            cleanupTempFiles()
        }
    }

    private fun restoreTimelapseState(data: SessionData) {
        if (!data.isTimelapseRecording && !data.isTimelapsePaused) return
        val storage = snapshotStorage ?: return
        frameAnalyzer ?: return

        viewModelScope.launch(ioDispatcher) {
            val diskCount = storage.getSnapshotCount()
            if (diskCount <= 0) return@launch

            pendingTimelapseResumeRecording = data.isTimelapseRecording
            _uiState.update {
                it.copy(
                    showTimelapseRestoreDialog = true,
                    pendingTimelapseSnapshotCount = diskCount
                )
            }
        }
    }

    private var pendingTimelapseResumeRecording = false

    fun onTimelapseRestoreContinue() {
        val storage = snapshotStorage ?: return
        val analyzer = frameAnalyzer ?: return
        val diskCount = _uiState.value.pendingTimelapseSnapshotCount
        if (diskCount <= 0) return

        _uiState.update { it.copy(showTimelapseRestoreDialog = false, pendingTimelapseSnapshotCount = 0) }

        val session = TimelapseSession(onCapture = { index ->
            analyzer.snapshotCallback = { bytes ->
                storage.saveSnapshot(bytes, index)
            }
        })
        timelapseSession = session
        session.restoreFromExisting(diskCount)
        observeTimelapseState(session)

        if (pendingTimelapseResumeRecording) {
            session.resume(viewModelScope)
        }
    }

    fun onTimelapseRestoreCompile() {
        _uiState.update { it.copy(showTimelapseRestoreDialog = false, pendingTimelapseSnapshotCount = 0) }

        viewModelScope.launch(ioDispatcher) {
            val snapshotFiles = snapshotStorage?.getSnapshotFiles() ?: emptyList()
            if (snapshotFiles.isNotEmpty() && timelapseCompiler != null && cacheDir != null) {
                compileTimelapse(snapshotFiles)
            }
        }
    }

    fun onTimelapseRestoreDiscard() {
        _uiState.update { it.copy(showTimelapseRestoreDialog = false, pendingTimelapseSnapshotCount = 0) }
        viewModelScope.launch(ioDispatcher) {
            cleanupTempFiles()
        }
    }

    private fun resetTracking() {
        trackingStateManager.reset()
        transformCalculator.resetReference()
        previousTransform = OverlayTransform.IDENTITY
    }

    fun onBreakReminderDismissed() {
        _uiState.update { it.copy(showBreakReminder = false) }
        restartBreakTimer()
    }

    private fun restartBreakTimer() {
        breakTimerJob?.cancel()
        if (!breakReminderEnabled || !_uiState.value.isSessionActive) return
        val intervalMs = breakReminderIntervalMinutes.coerceIn(1, 120).toLong() * 60_000L
        breakTimerJob = viewModelScope.launch {
            delay(intervalMs)
            _uiState.update { it.copy(showBreakReminder = true) }
        }
    }

    fun onMarkerResultReceived(result: MarkerResult) {
        val status = trackingStateManager.onMarkerResult(result)
        val trackingState = when (status) {
            TrackingStatus.INACTIVE -> TrackingState.INACTIVE
            TrackingStatus.TRACKING -> TrackingState.TRACKING
            TrackingStatus.LOST -> TrackingState.LOST
        }

        val frameW = result.frameWidth.toFloat().takeIf { it > 0f } ?: lastFrameWidth
        val frameH = result.frameHeight.toFloat().takeIf { it > 0f } ?: lastFrameHeight
        if (frameW != lastFrameWidth || frameH != lastFrameHeight) {
            lastFrameWidth = frameW
            lastFrameHeight = frameH
            recomputeCachedMatrices()
        }

        val transform = transformCalculator.computeSmoothed(
            result, frameW, frameH, previousTransform
        )
        previousTransform = transform

        _uiState.update {
            it.copy(
                trackingState = trackingState,
                detectedMarkerCount = result.markerCount
            )
        }
        updateOverlayFromCombined()
    }

    private fun recomputeCachedMatrices() {
        val pvScale = maxOf(viewWidth / lastFrameWidth, viewHeight / lastFrameHeight)
        cachedPvScale = pvScale
        val ox = (viewWidth - lastFrameWidth * pvScale) / 2f
        val oy = (viewHeight - lastFrameHeight * pvScale) / 2f
        cachedF2S = floatArrayOf(
            pvScale, 0f, ox,
            0f, pvScale, oy,
            0f, 0f, 1f
        )
    }

    private fun updateOverlayFromCombined() {
        val pvScale = cachedPvScale
        val cx = viewWidth / 2f
        val cy = viewHeight / 2f

        val corners = previousTransform.paperCornersFrame
        val renderMatrix: FloatArray

        if (corners != null && corners.size == 4) {
            // Paper-mapping branch: map overlay view rect onto the paper quadrilateral.
            // Convert frame-space paper corners to screen space via F2S.
            val screenCorners = corners.map { (fx, fy) ->
                Pair(
                    cachedF2S[0] * fx + cachedF2S[1] * fy + cachedF2S[2],
                    cachedF2S[3] * fx + cachedF2S[4] * fy + cachedF2S[5]
                )
            }

            // Source rect: centered in view, matching paper aspect ratio (cached from reference)
            val paperAR = previousTransform.paperAspectRatio
            val srcW: Float
            val srcH: Float
            if (paperAR <= 0f) {
                srcW = viewWidth; srcH = viewHeight
            } else {
                val viewAR = viewWidth / viewHeight
                if (paperAR > viewAR) {
                    srcW = viewWidth; srcH = viewWidth / paperAR
                } else {
                    srcH = viewHeight; srcW = viewHeight * paperAR
                }
            }
            val srcX = (viewWidth - srcW) / 2f
            val srcY = (viewHeight - srcH) / 2f

            val viewCorners = listOf(
                Pair(srcX, srcY),
                Pair(srcX + srcW, srcY),
                Pair(srcX + srcW, srcY + srcH),
                Pair(srcX, srcY + srcH)
            )
            val h = HomographySolver.solveHomography(viewCorners, screenCorners)

            if (h != null) {
                // Manual rotation/scale in VIEW space (before H) so the image
                // rotates/scales "within" the paper, not the warped result.
                val vcx = viewWidth / 2f
                val vcy = viewHeight / 2f
                val mView = MatrixUtils.compose(
                    MatrixUtils.translate(vcx, vcy),
                    MatrixUtils.rotate(manualRotation),
                    MatrixUtils.scale(manualScaleFactor),
                    MatrixUtils.translate(-vcx, -vcy)
                )
                // Offset in screen space (after H) so drag feels natural
                val mOffset = MatrixUtils.translate(manualOffset.x, manualOffset.y)
                renderMatrix = MatrixUtils.compose(mOffset, h, mView)
            } else {
                renderMatrix = computeAffineMatrix(pvScale, cx, cy)
            }
        } else {
            // Affine branch (2 markers or fewer)
            renderMatrix = computeAffineMatrix(pvScale, cx, cy)
        }

        _renderMatrix.value = renderMatrix

        _uiState.update {
            it.copy(
                overlayOffset = Offset(
                    previousTransform.offsetX * pvScale + manualOffset.x,
                    previousTransform.offsetY * pvScale + manualOffset.y
                ),
                overlayScale = previousTransform.scale * manualScaleFactor,
                overlayRotation = previousTransform.rotation + manualRotation
            )
        }
    }

    private fun computeAffineMatrix(pvScale: Float, cx: Float, cy: Float): FloatArray {
        val tx = previousTransform.offsetX * pvScale + manualOffset.x
        val ty = previousTransform.offsetY * pvScale + manualOffset.y
        val s = previousTransform.scale * manualScaleFactor
        val r = previousTransform.rotation + manualRotation
        return MatrixUtils.compose(
            MatrixUtils.translate(tx, ty),
            MatrixUtils.translate(cx, cy),
            MatrixUtils.rotate(r),
            MatrixUtils.scale(s),
            MatrixUtils.translate(-cx, -cy)
        )
    }

}
