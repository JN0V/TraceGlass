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

// Cached ArUco detector — single-threaded (CameraX analysis executor)
static cv::aruco::Dictionary s_dictionary = cv::aruco::getPredefinedDictionary(cv::aruco::DICT_4X4_50);
static cv::aruco::DetectorParameters s_detectorParams;
static cv::aruco::ArucoDetector s_detector(s_dictionary, s_detectorParams);

// Cached JNI class/method IDs — initialized in JNI_OnLoad
static jclass g_listClass = nullptr;
static jmethodID g_listInit = nullptr;
static jmethodID g_listAdd = nullptr;
static jclass g_pairClass = nullptr;
static jmethodID g_pairInit = nullptr;
static jclass g_floatClass = nullptr;
static jmethodID g_floatInit = nullptr;
static jclass g_markerClass = nullptr;
static jmethodID g_markerInit = nullptr;
static jclass g_resultClass = nullptr;
static jmethodID g_resultInit = nullptr;

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void * /*reserved*/) {
    JNIEnv *env;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }

    jclass cls;

    cls = env->FindClass("java/util/ArrayList");
    g_listClass = (jclass)env->NewGlobalRef(cls);
    g_listInit = env->GetMethodID(g_listClass, "<init>", "()V");
    g_listAdd = env->GetMethodID(g_listClass, "add", "(Ljava/lang/Object;)Z");
    env->DeleteLocalRef(cls);

    cls = env->FindClass("kotlin/Pair");
    g_pairClass = (jclass)env->NewGlobalRef(cls);
    g_pairInit = env->GetMethodID(g_pairClass, "<init>", "(Ljava/lang/Object;Ljava/lang/Object;)V");
    env->DeleteLocalRef(cls);

    cls = env->FindClass("java/lang/Float");
    g_floatClass = (jclass)env->NewGlobalRef(cls);
    g_floatInit = env->GetMethodID(g_floatClass, "<init>", "(F)V");
    env->DeleteLocalRef(cls);

    cls = env->FindClass("io/github/jn0v/traceglass/core/cv/DetectedMarker");
    g_markerClass = (jclass)env->NewGlobalRef(cls);
    g_markerInit = env->GetMethodID(g_markerClass, "<init>", "(IFFLjava/util/List;F)V");
    env->DeleteLocalRef(cls);

    cls = env->FindClass("io/github/jn0v/traceglass/core/cv/MarkerResult");
    g_resultClass = (jclass)env->NewGlobalRef(cls);
    g_resultInit = env->GetMethodID(g_resultClass, "<init>", "(Ljava/util/List;JII)V");
    env->DeleteLocalRef(cls);

    LOGD("JNI_OnLoad: cached class/method IDs");
    return JNI_VERSION_1_6;
}

// Helper to create an empty MarkerResult
static jobject createEmptyResult(JNIEnv *env) {
    jobject emptyList = env->NewObject(g_listClass, g_listInit);
    return env->NewObject(g_resultClass, g_resultInit, emptyList, (jlong)0, (jint)0, (jint)0);
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
        return createEmptyResult(env);
    }

    // Validate buffer capacity before constructing Mat
    jlong bufferCapacity = env->GetDirectBufferCapacity(byteBuffer);
    jlong requiredSize = (jlong)(height - 1) * rowStride + width;
    if (bufferCapacity < requiredSize) {
        LOGE("Buffer too small: capacity=%lld, required=%lld (w=%d h=%d stride=%d)",
             (long long)bufferCapacity, (long long)requiredSize, width, height, rowStride);
        return createEmptyResult(env);
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

    // ArUco marker detection (using cached static detector)
    std::vector<std::vector<cv::Point2f>> corners;
    std::vector<int> ids;
    s_detector.detectMarkers(gray, corners, ids);

    auto endTime = std::chrono::steady_clock::now();
    auto durationMs = std::chrono::duration_cast<std::chrono::milliseconds>(endTime - startTime).count();

    // Build Java result objects using cached class/method IDs
    jobject markerList = env->NewObject(g_listClass, g_listInit);

    for (size_t i = 0; i < ids.size(); i++) {
        // Compute center
        float cx = 0, cy = 0;
        jobject cornerList = env->NewObject(g_listClass, g_listInit);

        for (int j = 0; j < 4; j++) {
            cx += corners[i][j].x;
            cy += corners[i][j].y;

            jobject fx = env->NewObject(g_floatClass, g_floatInit, corners[i][j].x);
            jobject fy = env->NewObject(g_floatClass, g_floatInit, corners[i][j].y);
            jobject pair = env->NewObject(g_pairClass, g_pairInit, fx, fy);
            env->CallBooleanMethod(cornerList, g_listAdd, pair);

            env->DeleteLocalRef(fx);
            env->DeleteLocalRef(fy);
            env->DeleteLocalRef(pair);
        }
        cx /= 4.0f;
        cy /= 4.0f;

        jobject marker = env->NewObject(g_markerClass, g_markerInit,
            ids[i], cx, cy, cornerList, 1.0f);
        env->CallBooleanMethod(markerList, g_listAdd, marker);

        env->DeleteLocalRef(cornerList);
        env->DeleteLocalRef(marker);
    }

    // Effective dimensions after rotation
    jint effectiveWidth = (rotation == 90 || rotation == 270) ? height : width;
    jint effectiveHeight = (rotation == 90 || rotation == 270) ? width : height;

    jobject result = env->NewObject(g_resultClass, g_resultInit, markerList, (jlong)durationMs,
        effectiveWidth, effectiveHeight);

    return result;
}

} // extern "C"
