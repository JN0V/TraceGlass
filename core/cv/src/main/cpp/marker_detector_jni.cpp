#include <jni.h>
#include <android/log.h>
#include <opencv2/core.hpp>
#include <opencv2/imgproc.hpp>
#include <opencv2/objdetect/aruco_detector.hpp>
#include <vector>
#include <chrono>

#define LOG_TAG "TraceGlassCV"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Helper: throw RuntimeException and return nullptr on JNI lookup failure
#define JNI_CHECK_NULL(ptr, env, msg) \
    if ((ptr) == nullptr) { \
        LOGE("JNI lookup failed: %s", (msg)); \
        jclass rte = (env)->FindClass("java/lang/RuntimeException"); \
        if (rte) (env)->ThrowNew(rte, (msg)); \
        return nullptr; \
    }

extern "C" {

JNIEXPORT jobject JNICALL
Java_io_github_jn0v_traceglass_core_cv_impl_OpenCvMarkerDetector_nativeDetect(
    JNIEnv *env,
    jobject /* this */,
    jobject byteBuffer,
    jint width,
    jint height,
    jint rowStride,
    jint rotation
) {
    auto startTime = std::chrono::steady_clock::now();

    // Get frame buffer
    auto *bufferAddr = static_cast<uint8_t *>(env->GetDirectBufferAddress(byteBuffer));
    if (bufferAddr == nullptr) {
        LOGE("Failed to get buffer address");
        // Return empty result
        jclass resultClass = env->FindClass("io/github/jn0v/traceglass/core/cv/MarkerResult");
        JNI_CHECK_NULL(resultClass, env, "MarkerResult class not found");
        jclass listClass = env->FindClass("java/util/ArrayList");
        JNI_CHECK_NULL(listClass, env, "ArrayList class not found");
        jmethodID listInit = env->GetMethodID(listClass, "<init>", "()V");
        JNI_CHECK_NULL(listInit, env, "ArrayList.<init> not found");
        jobject emptyList = env->NewObject(listClass, listInit);
        jmethodID resultInit = env->GetMethodID(resultClass, "<init>", "(Ljava/util/List;JII)V");
        JNI_CHECK_NULL(resultInit, env, "MarkerResult.<init> not found");
        return env->NewObject(resultClass, resultInit, emptyList, (jlong)0, (jint)0, (jint)0);
    }

    // Create grayscale Mat from YUV (first plane is Y = grayscale)
    // rowStride as step handles padding between rows correctly
    cv::Mat gray(height, width, CV_8UC1, bufferAddr, (size_t)rowStride);

    // Apply rotation if needed
    if (rotation == 90) {
        cv::rotate(gray, gray, cv::ROTATE_90_CLOCKWISE);
    } else if (rotation == 180) {
        cv::rotate(gray, gray, cv::ROTATE_180);
    } else if (rotation == 270) {
        cv::rotate(gray, gray, cv::ROTATE_90_COUNTERCLOCKWISE);
    }

    // ArUco marker detection
    cv::aruco::Dictionary dictionary = cv::aruco::getPredefinedDictionary(cv::aruco::DICT_4X4_50);
    cv::aruco::DetectorParameters detectorParams;
    cv::aruco::ArucoDetector detector(dictionary, detectorParams);

    std::vector<std::vector<cv::Point2f>> corners;
    std::vector<int> ids;
    detector.detectMarkers(gray, corners, ids);

    auto endTime = std::chrono::steady_clock::now();
    auto durationMs = std::chrono::duration_cast<std::chrono::milliseconds>(endTime - startTime).count();

    // Build Java result objects — look up classes and methods with null checks
    jclass listClass = env->FindClass("java/util/ArrayList");
    JNI_CHECK_NULL(listClass, env, "ArrayList class not found");
    jmethodID listInit = env->GetMethodID(listClass, "<init>", "()V");
    JNI_CHECK_NULL(listInit, env, "ArrayList.<init> not found");
    jmethodID listAdd = env->GetMethodID(listClass, "add", "(Ljava/lang/Object;)Z");
    JNI_CHECK_NULL(listAdd, env, "ArrayList.add not found");

    jclass pairClass = env->FindClass("kotlin/Pair");
    JNI_CHECK_NULL(pairClass, env, "kotlin/Pair class not found");
    jmethodID pairInit = env->GetMethodID(pairClass, "<init>", "(Ljava/lang/Object;Ljava/lang/Object;)V");
    JNI_CHECK_NULL(pairInit, env, "Pair.<init> not found");

    jclass floatClass = env->FindClass("java/lang/Float");
    JNI_CHECK_NULL(floatClass, env, "java/lang/Float class not found");
    jmethodID floatInit = env->GetMethodID(floatClass, "<init>", "(F)V");
    JNI_CHECK_NULL(floatInit, env, "Float.<init> not found");

    jclass markerClass = env->FindClass("io/github/jn0v/traceglass/core/cv/DetectedMarker");
    JNI_CHECK_NULL(markerClass, env, "DetectedMarker class not found");
    jmethodID markerInit = env->GetMethodID(markerClass, "<init>", "(IFFLjava/util/List;F)V");
    JNI_CHECK_NULL(markerInit, env, "DetectedMarker.<init> not found");

    jobject markerList = env->NewObject(listClass, listInit);

    for (size_t i = 0; i < ids.size(); i++) {
        // Compute center
        float cx = 0, cy = 0;
        jobject cornerList = env->NewObject(listClass, listInit);

        for (int j = 0; j < 4; j++) {
            cx += corners[i][j].x;
            cy += corners[i][j].y;

            jobject fx = env->NewObject(floatClass, floatInit, corners[i][j].x);
            jobject fy = env->NewObject(floatClass, floatInit, corners[i][j].y);
            jobject pair = env->NewObject(pairClass, pairInit, fx, fy);
            env->CallBooleanMethod(cornerList, listAdd, pair);

            env->DeleteLocalRef(fx);
            env->DeleteLocalRef(fy);
            env->DeleteLocalRef(pair);
        }
        cx /= 4.0f;
        cy /= 4.0f;

        jobject marker = env->NewObject(markerClass, markerInit,
            ids[i], cx, cy, cornerList, 1.0f);
        env->CallBooleanMethod(markerList, listAdd, marker);

        env->DeleteLocalRef(cornerList);
        env->DeleteLocalRef(marker);
    }

    // Effective dimensions after rotation
    jint effectiveWidth = (rotation == 90 || rotation == 270) ? height : width;
    jint effectiveHeight = (rotation == 90 || rotation == 270) ? width : height;

    jclass resultClass = env->FindClass("io/github/jn0v/traceglass/core/cv/MarkerResult");
    JNI_CHECK_NULL(resultClass, env, "MarkerResult class not found (result)");
    jmethodID resultInit = env->GetMethodID(resultClass, "<init>", "(Ljava/util/List;JII)V");
    JNI_CHECK_NULL(resultInit, env, "MarkerResult.<init> not found (result)");
    jobject result = env->NewObject(resultClass, resultInit, markerList, (jlong)durationMs,
        effectiveWidth, effectiveHeight);

    return result;
}

} // extern "C"
