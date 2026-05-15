#include <android/asset_manager_jni.h>
#include <jni.h>

#include "gpu.h"
#include "ncnn_runtime.h"

namespace {

NcnnRuntimeNative* from_handle(jlong handle) {
    return reinterpret_cast<NcnnRuntimeNative*>(handle);
}

jstring to_jstring(JNIEnv* env, const std::string& value) {
    return env->NewStringUTF(value.c_str());
}

std::string to_string(JNIEnv* env, jstring value) {
    if (!value) return {};
    const char* chars = env->GetStringUTFChars(value, nullptr);
    std::string out(chars ? chars : "");
    env->ReleaseStringUTFChars(value, chars);
    return out;
}

} // namespace

extern "C" JNIEXPORT void JNICALL
Java_com_mnnode_app_model_NcnnRuntime_nativeInit(JNIEnv*, jclass) {
    ncnn::create_gpu_instance();
}

extern "C" JNIEXPORT jint JNICALL
Java_com_mnnode_app_model_NcnnRuntime_nativeGpuCount(JNIEnv*, jclass) {
    return ncnn::get_gpu_count();
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_mnnode_app_model_NcnnRuntime_nativeCreate(JNIEnv* env, jclass, jobject asset_manager) {
    AAssetManager* manager = AAssetManager_fromJava(env, asset_manager);
    if (!manager) return 0;
    return reinterpret_cast<jlong>(new NcnnRuntimeNative(manager));
}

extern "C" JNIEXPORT void JNICALL
Java_com_mnnode_app_model_NcnnRuntime_nativeRelease(JNIEnv*, jclass, jlong handle) {
    delete from_handle(handle);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_mnnode_app_model_NcnnRuntime_nativeLoadModel(
    JNIEnv* env,
    jclass,
    jlong handle,
    jstring model_id,
    jstring param_asset,
    jstring bin_asset,
    jstring backend,
    jint num_threads) {

    auto* runtime = from_handle(handle);
    if (!runtime) {
        return to_jstring(env, "{\"ok\":false,\"message\":\"runtime closed\"}");
    }

    const auto result = runtime->load_model(
        to_string(env, model_id),
        to_string(env, param_asset),
        to_string(env, bin_asset),
        to_string(env, backend),
        num_threads);
    return to_jstring(env, to_json(result));
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_mnnode_app_model_NcnnRuntime_nativeState(JNIEnv* env, jclass, jlong handle) {
    auto* runtime = from_handle(handle);
    if (!runtime) {
        return to_jstring(env, "{\"loaded\":false,\"state\":\"closed\"}");
    }
    return to_jstring(env, runtime->state_json());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_mnnode_app_model_NcnnRuntime_nativeDetectYuv420(
    JNIEnv* env,
    jclass,
    jlong handle,
    jint width,
    jint height,
    jint rotation,
    jobject y_buffer,
    jobject u_buffer,
    jobject v_buffer,
    jint y_row_stride,
    jint u_row_stride,
    jint v_row_stride,
    jint y_pixel_stride,
    jint u_pixel_stride,
    jint v_pixel_stride,
    jstring input_name,
    jstring output_name,
    jint input_size,
    jfloat confidence_threshold) {

    auto* runtime = from_handle(handle);
    if (!runtime) {
        return to_jstring(env, "{\"ok\":false,\"message\":\"runtime closed\",\"detections\":[]}");
    }

    auto* y = static_cast<unsigned char*>(env->GetDirectBufferAddress(y_buffer));
    auto* u = static_cast<unsigned char*>(env->GetDirectBufferAddress(u_buffer));
    auto* v = static_cast<unsigned char*>(env->GetDirectBufferAddress(v_buffer));

    const std::string result = runtime->detect_yuv420(
        width,
        height,
        rotation,
        y,
        u,
        v,
        y_row_stride,
        u_row_stride,
        v_row_stride,
        y_pixel_stride,
        u_pixel_stride,
        v_pixel_stride,
        to_string(env, input_name),
        to_string(env, output_name),
        input_size,
        confidence_threshold);

    return to_jstring(env, result);
}
