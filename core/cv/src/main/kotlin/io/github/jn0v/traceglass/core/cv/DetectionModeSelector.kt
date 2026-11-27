package io.github.jn0v.traceglass.core.cv

/**
 * Holds the detection mode currently in use. Written from the app wiring when the
 * stored setting changes, read from the camera analysis thread on every frame.
 */
class DetectionModeSelector(initial: DetectionMode = DetectionMode.ARUCO) {
    @Volatile
    var mode: DetectionMode = initial
}
