package io.github.jn0v.traceglass.feature.timelapse

import android.graphics.BitmapFactory
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MediaCodecCompiler : TimelapseCompiler {

    override suspend fun compile(
        snapshotFiles: List<File>,
        outputFile: File,
        fps: Int,
        onProgress: (Float) -> Unit
    ): CompilationResult = withContext(Dispatchers.IO) {
        if (snapshotFiles.isEmpty()) {
            return@withContext CompilationResult.Error("No snapshots to compile")
        }

        try {
            val firstBitmap = BitmapFactory.decodeFile(snapshotFiles.first().absolutePath)
                ?: return@withContext CompilationResult.Error("Cannot decode first snapshot")

            val width = align16(firstBitmap.width)
            val height = align16(firstBitmap.height)
            firstBitmap.recycle()

            val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, 4_000_000)
                setInteger(MediaFormat.KEY_FRAME_RATE, fps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }

            val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)

            val inputSurface = codec.createInputSurface()
            codec.start()

            val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var trackIndex = -1
            var muxerStarted = false
            val bufferInfo = MediaCodec.BufferInfo()
            val frameDurationUs = 1_000_000L / fps

            for ((i, file) in snapshotFiles.withIndex()) {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: continue
                val canvas = inputSurface.lockCanvas(null)
                canvas.drawBitmap(bitmap, 0f, 0f, null)
                inputSurface.unlockCanvasAndPost(canvas)
                bitmap.recycle()

                drainEncoder(codec, bufferInfo, muxer, trackIndex, muxerStarted) { track, started ->
                    trackIndex = track
                    muxerStarted = started
                }

                onProgress((i + 1).toFloat() / snapshotFiles.size)
            }

            codec.signalEndOfInputStream()
            drainEncoder(codec, bufferInfo, muxer, trackIndex, muxerStarted, drainAll = true) { track, started ->
                trackIndex = track
                muxerStarted = started
            }

            codec.stop()
            codec.release()
            if (muxerStarted) muxer.stop()
            muxer.release()
            inputSurface.release()

            CompilationResult.Success(outputFile)
        } catch (e: Exception) {
            CompilationResult.Error(e.message ?: "Unknown compilation error")
        }
    }

    private fun drainEncoder(
        codec: MediaCodec,
        bufferInfo: MediaCodec.BufferInfo,
        muxer: MediaMuxer,
        trackIndex: Int,
        muxerStarted: Boolean,
        drainAll: Boolean = false,
        onTrackReady: (Int, Boolean) -> Unit
    ) {
        var currentTrack = trackIndex
        var started = muxerStarted

        while (true) {
            val outputIndex = codec.dequeueOutputBuffer(bufferInfo, if (drainAll) 10_000L else 0L)
            when {
                outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    currentTrack = muxer.addTrack(codec.outputFormat)
                    muxer.start()
                    started = true
                    onTrackReady(currentTrack, started)
                }
                outputIndex >= 0 -> {
                    val outputBuffer = codec.getOutputBuffer(outputIndex) ?: continue
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                        bufferInfo.size = 0
                    }
                    if (bufferInfo.size > 0 && started) {
                        muxer.writeSampleData(currentTrack, outputBuffer, bufferInfo)
                    }
                    codec.releaseOutputBuffer(outputIndex, false)
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) return
                }
                else -> {
                    if (!drainAll) return
                }
            }
        }
    }

    private fun align16(value: Int): Int = (value + 15) and 0x7FFFFFF0
}
