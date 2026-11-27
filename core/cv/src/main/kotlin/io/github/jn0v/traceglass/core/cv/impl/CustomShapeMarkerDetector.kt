package io.github.jn0v.traceglass.core.cv.impl

import io.github.jn0v.traceglass.core.cv.DetectedMarker
import io.github.jn0v.traceglass.core.cv.MarkerDetector
import io.github.jn0v.traceglass.core.cv.MarkerResult
import java.nio.ByteBuffer

/**
 * Experimental detector for four hand-drawn corner shapes: solid dark marks on
 * white paper, one per corner.
 *
 * The luma plane is reduced to at most [WORKING_SIZE] pixels on its long side,
 * split into ink and paper with Otsu's threshold, and the ink is cut into
 * connected blobs. Blobs that look like a solid mark (size, fill, not touching
 * the frame edge) are candidates. Only when exactly four candidates remain, and
 * they sit one in each corner of their quadrilateral, are markers reported:
 * ids 0..3 = top-left, top-right, bottom-right, bottom-left, the convention of
 * the ArUco markers. `corners[id]` of each marker is the blob's outermost point
 * towards its paper corner, which is the point the overlay uses.
 */
class CustomShapeMarkerDetector : MarkerDetector {

    override fun detect(
        frameBuffer: ByteBuffer,
        width: Int,
        height: Int,
        rowStride: Int,
        rotation: Int
    ): MarkerResult {
        val start = System.nanoTime()
        val markers = try {
            findMarkers(frameBuffer, width, height, rowStride, rotation)
        } catch (_: IndexOutOfBoundsException) {
            emptyList()
        }
        val quarterTurn = rotation == 90 || rotation == 270
        return MarkerResult(
            markers = markers,
            detectionTimeMs = (System.nanoTime() - start) / 1_000_000L,
            frameWidth = if (quarterTurn) height else width,
            frameHeight = if (quarterTurn) width else height
        )
    }

    private fun findMarkers(
        buffer: ByteBuffer,
        width: Int,
        height: Int,
        rowStride: Int,
        rotation: Int
    ): List<DetectedMarker> {
        if (width <= 0 || height <= 0 || rowStride < width) return emptyList()
        if (buffer.capacity().toLong() < (height - 1).toLong() * rowStride + width) return emptyList()

        val quarterTurn = rotation == 90 || rotation == 270
        val frameW = if (quarterTurn) height else width
        val frameH = if (quarterTurn) width else height
        val scale = maxOf(1, (maxOf(frameW, frameH) + WORKING_SIZE - 1) / WORKING_SIZE)
        val w = frameW / scale
        val h = frameH / scale
        if (w < MIN_WORKING_SIDE || h < MIN_WORKING_SIDE) return emptyList()

        val gray = sample(buffer, width, height, rowStride, rotation, scale, w, h)
        val threshold = otsuThreshold(gray) ?: return emptyList()
        val candidates = findBlobs(gray, w, h, threshold)
        val corners = assignCorners(candidates) ?: return emptyList()

        return corners.mapIndexed { id, blob ->
            val pts = blob.outline.map { (x, y) -> (x + 0.5f) * scale to (y + 0.5f) * scale }
            DetectedMarker(
                id = id,
                centerX = (blob.sumX / blob.area + 0.5f) * scale,
                centerY = (blob.sumY / blob.area + 0.5f) * scale,
                corners = pts,
                confidence = 1f
            )
        }
    }

    /** Box-samples the rotated frame down to w x h luma values. */
    private fun sample(
        buffer: ByteBuffer,
        width: Int,
        height: Int,
        rowStride: Int,
        rotation: Int,
        scale: Int,
        w: Int,
        h: Int
    ): IntArray {
        val out = IntArray(w * h)
        val frameW = if (rotation == 90 || rotation == 270) height else width
        val frameH = if (rotation == 90 || rotation == 270) width else height
        val step = maxOf(1, scale / 2)
        for (oy in 0 until h) {
            for (ox in 0 until w) {
                var sum = 0
                var n = 0
                var dy = 0
                while (dy < scale) {
                    var dx = 0
                    while (dx < scale) {
                        val fx = ox * scale + dx
                        val fy = oy * scale + dy
                        // Frame (rotated) coordinates back to source pixels.
                        val sx: Int
                        val sy: Int
                        when (rotation) {
                            90 -> { sx = fy; sy = frameW - 1 - fx }
                            180 -> { sx = frameW - 1 - fx; sy = frameH - 1 - fy }
                            270 -> { sx = frameH - 1 - fy; sy = fx }
                            else -> { sx = fx; sy = fy }
                        }
                        sum += buffer.get(sy * rowStride + sx).toInt() and 0xFF
                        n++
                        dx += step
                    }
                    dy += step
                }
                out[oy * w + ox] = sum / n
            }
        }
        return out
    }

    /** Otsu's threshold, or null when ink and paper are too close to tell apart. */
    private fun otsuThreshold(gray: IntArray): Int? {
        val hist = IntArray(256)
        for (v in gray) hist[v]++
        val total = gray.size.toDouble()
        var sumAll = 0.0
        for (i in 0 until 256) sumAll += i * hist[i].toDouble()

        var weightLow = 0.0
        var sumLow = 0.0
        var bestVariance = -1.0
        var best = 0
        var bestMeanLow = 0.0
        var bestMeanHigh = 0.0
        for (t in 0 until 256) {
            weightLow += hist[t]
            if (weightLow == 0.0) continue
            val weightHigh = total - weightLow
            if (weightHigh == 0.0) break
            sumLow += t * hist[t].toDouble()
            val meanLow = sumLow / weightLow
            val meanHigh = (sumAll - sumLow) / weightHigh
            val variance = weightLow * weightHigh * (meanLow - meanHigh) * (meanLow - meanHigh)
            if (variance > bestVariance) {
                bestVariance = variance
                best = t
                bestMeanLow = meanLow
                bestMeanHigh = meanHigh
            }
        }
        if (bestVariance < 0 || bestMeanHigh - bestMeanLow < MIN_CONTRAST) return null
        return best
    }

    private class Blob(
        val area: Int,
        val sumX: Float,
        val sumY: Float,
        /** Outermost pixel towards top-left, top-right, bottom-right, bottom-left. */
        val outline: List<Pair<Int, Int>>
    ) {
        val cx: Float get() = sumX / area
        val cy: Float get() = sumY / area
    }

    /** 4-connected blobs of dark pixels that look like a solid hand-drawn mark. */
    private fun findBlobs(gray: IntArray, w: Int, h: Int, threshold: Int): List<Blob> {
        val visited = BooleanArray(w * h)
        val stack = IntArray(w * h)
        val frameArea = w * h
        val blobs = ArrayList<Blob>()

        for (seed in 0 until w * h) {
            if (visited[seed] || gray[seed] > threshold) continue

            var sp = 0
            stack[sp++] = seed
            visited[seed] = true
            var area = 0
            var sumX = 0f
            var sumY = 0f
            var minX = w; var maxX = -1; var minY = h; var maxY = -1
            // Extremes along x+y (min = TL, max = BR) and x-y (max = TR, min = BL).
            var tl = seed; var br = seed; var tr = seed; var bl = seed
            while (sp > 0) {
                val p = stack[--sp]
                val x = p % w
                val y = p / w
                area++
                sumX += x
                sumY += y
                if (x < minX) minX = x
                if (x > maxX) maxX = x
                if (y < minY) minY = y
                if (y > maxY) maxY = y
                if (x + y < tl % w + tl / w) tl = p
                if (x + y > br % w + br / w) br = p
                if (x - y > tr % w - tr / w) tr = p
                if (x - y < bl % w - bl / w) bl = p
                if (x > 0 && !visited[p - 1] && gray[p - 1] <= threshold) { visited[p - 1] = true; stack[sp++] = p - 1 }
                if (x < w - 1 && !visited[p + 1] && gray[p + 1] <= threshold) { visited[p + 1] = true; stack[sp++] = p + 1 }
                if (y > 0 && !visited[p - w] && gray[p - w] <= threshold) { visited[p - w] = true; stack[sp++] = p - w }
                if (y < h - 1 && !visited[p + w] && gray[p + w] <= threshold) { visited[p + w] = true; stack[sp++] = p + w }
            }

            val boxW = maxX - minX + 1
            val boxH = maxY - minY + 1
            val touchesEdge = minX == 0 || minY == 0 || maxX == w - 1 || maxY == h - 1
            val solid = area.toFloat() / (boxW * boxH) >= MIN_FILL
            val ratio = boxW.toFloat() / boxH
            if (touchesEdge || !solid) continue
            if (area < frameArea * MIN_AREA_FRACTION || area > frameArea * MAX_AREA_FRACTION) continue
            if (ratio < 1f / MAX_ASPECT || ratio > MAX_ASPECT) continue

            blobs += Blob(
                area, sumX, sumY,
                listOf(tl, tr, br, bl).map { it % w to it / w }
            )
        }
        return blobs
    }

    /**
     * Picks the four marks and orders them TL, TR, BR, BL. With more than four
     * candidates the four largest are kept. Null unless the four sit in four
     * different corners of their quadrilateral.
     */
    private fun assignCorners(candidates: List<Blob>): List<Blob>? {
        if (candidates.size < 4) return null
        val four = candidates.sortedByDescending { it.area }.take(4)
        // Marks drawn by hand differ in size, but not by an order of magnitude.
        if (four.last().area * MAX_AREA_SPREAD < four.first().area) return null
        // Fewer than four candidates left over after the largest four: any extra
        // mark of similar size makes the choice ambiguous.
        if (candidates.size > 4 && candidates.sortedByDescending { it.area }[4].area * 2 > four.last().area) {
            return null
        }

        val tl = four.minBy { it.cx + it.cy }
        val br = four.maxBy { it.cx + it.cy }
        val tr = four.maxBy { it.cx - it.cy }
        val bl = four.minBy { it.cx - it.cy }
        val ordered = listOf(tl, tr, br, bl)
        if (ordered.toSet().size != 4) return null
        return ordered
    }

    companion object {
        private const val WORKING_SIZE = 320
        private const val MIN_WORKING_SIDE = 16
        private const val MIN_CONTRAST = 60.0
        private const val MIN_AREA_FRACTION = 0.0004f
        private const val MAX_AREA_FRACTION = 0.05f
        private const val MIN_FILL = 0.3f
        private const val MAX_ASPECT = 4f
        private const val MAX_AREA_SPREAD = 12
    }
}
