cmake_minimum_required(VERSION 3.22.1)
project(lociant_native)

if(ANDROID_ABI)
    set(LOCIANT_ABI "${ANDROID_ABI}")
else()
    set(LOCIANT_ABI "${CMAKE_ANDROID_ARCH_ABI}")
endif()

set(NCNN_PACKAGE_DIR
    "${CMAKE_CURRENT_SOURCE_DIR}/../../../../../../tools/ncnn-20260113-android-vulkan/${LOCIANT_ABI}/lib/cmake/ncnn")
get_filename_component(NCNN_PACKAGE_DIR "${NCNN_PACKAGE_DIR}" ABSOLUTE)
set(ncnn_DIR "${NCNN_PACKAGE_DIR}")

message(STATUS "Using ncnn package: ${NCNN_PACKAGE_DIR}")

find_package(ncnn REQUIRED CONFIG)

add_library(lociant_ncnn SHARED
    ncnn_jni.cpp
    ncnn_runtime.cpp
)

target_include_directories(lociant_ncnn PRIVATE
    "${CMAKE_CURRENT_SOURCE_DIR}"
)

target_link_libraries(lociant_ncnn
    ncnn
    android
    log
)
