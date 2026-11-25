package io.github.jn0v.traceglass.core.cv

/** How the four paper corners are recognised in the camera frame. */
enum class DetectionMode {
    /** Printed ArUco markers (ids 0..3). The default. */
    ARUCO,

    /** Four solid dark shapes drawn by hand on white paper (experimental). */
    CUSTOM_SHAPES
}
