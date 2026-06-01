cmake_minimum_required(VERSION 3.22.1)
project(mnnode_native)

if(ANDROID_ABI)
    set(MNNODE_ABI "${ANDROID_ABI}")
else()
    set(MNNODE_ABI "${CMAKE_ANDROID_ARCH_ABI}")
endif()

set(NCNN_PACKAGE_DIR
    "${CMAKE_CURRENT_SOURCE_DIR}/../../../../../../tools/ncnn-20260113-android-vulkan/${MNNODE_ABI}/lib/cmake/ncnn")
get_filename_component(NCNN_PACKAGE_DIR "${NCNN_PACKAGE_DIR}" ABSOLUTE)
set(ncnn_DIR "${NCNN_PACKAGE_DIR}")

message(STATUS "Using ncnn package: ${NCNN_PACKAGE_DIR}")

find_package(ncnn REQUIRED CONFIG)

set(MNN_PREBUILT_DIR
    "${CMAKE_CURRENT_SOURCE_DIR}/../../../../../../tools/mnn_3.5.0_android_armv7_armv8_cpu_opencl_vulkan/${MNNODE_ABI}")
get_filename_component(MNN_PREBUILT_DIR "${MNN_PREBUILT_DIR}" ABSOLUTE)
set(MNN_SOURCE_DIR
    "${CMAKE_CURRENT_SOURCE_DIR}/../../../../../../tools/mnn_3.5.0_headers")
get_filename_component(MNN_SOURCE_DIR "${MNN_SOURCE_DIR}" ABSOLUTE)

message(STATUS "Using MNN prebuilt: ${MNN_PREBUILT_DIR}")

add_library(mnnode_ncnn SHARED
    ncnn_jni.cpp
    ncnn_runtime.cpp
)

target_include_directories(mnnode_ncnn PRIVATE
    "${CMAKE_CURRENT_SOURCE_DIR}"
)

target_link_libraries(mnnode_ncnn
    ncnn
    android
    log
)

add_library(mnn_llm SHARED IMPORTED)
set_target_properties(mnn_llm PROPERTIES
    IMPORTED_LOCATION "${MNN_PREBUILT_DIR}/libllm.so"
)

add_library(mnn_core SHARED IMPORTED)
set_target_properties(mnn_core PROPERTIES
    IMPORTED_LOCATION "${MNN_PREBUILT_DIR}/libMNN.so"
)

add_library(mnn_express SHARED IMPORTED)
set_target_properties(mnn_express PROPERTIES
    IMPORTED_LOCATION "${MNN_PREBUILT_DIR}/libMNN_Express.so"
)

add_library(mnnode_mnn SHARED
    mnn_jni.cpp
    mnn_runtime.cpp
)

target_include_directories(mnnode_mnn PRIVATE
    "${CMAKE_CURRENT_SOURCE_DIR}"
    "${MNN_SOURCE_DIR}/include"
    "${MNN_SOURCE_DIR}/transformers/llm/engine/include"
)

target_link_libraries(mnnode_mnn
    mnn_llm
    mnn_express
    mnn_core
    android
    jnigraphics
    log
)
