package io.github.jn0v.traceglass.core.camera

object FocalLengthCalculator {

    /**
     * Computes focal length in analysis-frame pixels.
     *
     * @param focalLengthMm physical focal length in millimeters
     * @param sensorWidthMm sensor physical width in millimeters
     * @param analysisWidth analysis frame width in pixels (e.g. 1280)
     * @param zoomRatio effective zoom ratio (digital zoom crops the sensor)
     * @return focal length in analysis pixels, or null if inputs are invalid
     */
    fun computePixels(
        focalLengthMm: Float,
        sensorWidthMm: Float,
        analysisWidth: Int,
        zoomRatio: Float
    ): Float? {
        if (focalLengthMm <= 0f || !focalLengthMm.isFinite()) return null
        if (sensorWidthMm <= 0f || !sensorWidthMm.isFinite()) return null
        if (analysisWidth <= 0) return null
        if (zoomRatio <= 0f || !zoomRatio.isFinite()) return null

        return focalLengthMm * analysisWidth / sensorWidthMm * zoomRatio
    }
}
