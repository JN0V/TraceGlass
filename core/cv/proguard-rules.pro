# JNI-referenced classes — must not be obfuscated by R8
# These classes are looked up by fully-qualified name from native code (marker_detector_jni.cpp)
-keep class io.github.jn0v.traceglass.core.cv.MarkerResult { *; }
-keep class io.github.jn0v.traceglass.core.cv.DetectedMarker { *; }
-keep class io.github.jn0v.traceglass.core.cv.impl.OpenCvMarkerDetector {
    native <methods>;
}
