cmake_minimum_required(VERSION 3.22)
project(traceglass_cv)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# F-Droid reproducibility flags
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -ffile-prefix-map=${CMAKE_SOURCE_DIR}=.")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -ffile-prefix-map=${CMAKE_SOURCE_DIR}=.")
set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -Wl,--build-id=none -Wl,--hash-style=gnu")

# OpenCV
set(OpenCV_DIR "${CMAKE_SOURCE_DIR}/../../../../../sdk/OpenCV-android-sdk/sdk/native/jni")
find_package(OpenCV REQUIRED)

add_library(traceglass_cv SHARED
    marker_detector_jni.cpp
)

target_include_directories(traceglass_cv PRIVATE
    ${OpenCV_INCLUDE_DIRS}
)

target_link_libraries(traceglass_cv
    ${OpenCV_LIBS}
    log
)
