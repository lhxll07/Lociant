#include <jni.h>
#include <vector>

#include "mnn_runtime.h"

namespace {

MnnRuntimeNative* from_handle(jlong handle) {
    return reinterpret_cast<MnnRuntimeNative*>(handle);
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

extern "C" JNIEXPORT jlong JNICALL
Java_com_mnnode_app_model_MnnRuntime_nativeCreate(JNIEnv*, jclass) {
    return reinterpret_cast<jlong>(new MnnRuntimeNative());
}

extern "C" JNIEXPORT void JNICALL
Java_com_mnnode_app_model_MnnRuntime_nativeRelease(JNIEnv*, jclass, jlong handle) {
    delete from_handle(handle);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_mnnode_app_model_MnnRuntime_nativeLoad(
    JNIEnv* env,
    jclass,
    jlong handle,
    jstring config_path,
    jstring tmp_path,
    jstring config_json) {

    auto* runtime = from_handle(handle);
    if (!runtime) {
        return to_jstring(env, "{\"ok\":false,\"message\":\"runtime closed\"}");
    }
    return to_jstring(env, runtime->load(
        to_string(env, config_path),
        to_string(env, tmp_path),
        to_string(env, config_json)));
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_mnnode_app_model_MnnRuntime_nativeChatText(
    JNIEnv* env,
    jclass,
    jlong handle,
    jobjectArray roles,
    jobjectArray contents,
    jint max_tokens,
    jstring session_id,
    jboolean use_session_cache,
    jstring config_json) {

    auto* runtime = from_handle(handle);
    if (!runtime) {
        return to_jstring(env, "{\"ok\":false,\"message\":\"runtime closed\"}");
    }

    std::vector<std::pair<std::string, std::string>> messages;
    const jsize count = contents ? env->GetArrayLength(contents) : 0;
    const jsize role_count = roles ? env->GetArrayLength(roles) : 0;
    messages.reserve(static_cast<size_t>(count));
    for (jsize i = 0; i < count; ++i) {
        auto role = role_count > i ? static_cast<jstring>(env->GetObjectArrayElement(roles, i)) : nullptr;
        auto content = static_cast<jstring>(env->GetObjectArrayElement(contents, i));
        messages.emplace_back(to_string(env, role), to_string(env, content));
        if (role) env->DeleteLocalRef(role);
        if (content) env->DeleteLocalRef(content);
    }
    return to_jstring(env, runtime->chat_text(
        messages,
        max_tokens,
        to_string(env, session_id),
        use_session_cache == JNI_TRUE,
        to_string(env, config_json)));
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_mnnode_app_model_MnnRuntime_nativeChatTextStream(
    JNIEnv* env,
    jclass,
    jlong handle,
    jobjectArray roles,
    jobjectArray contents,
    jint max_tokens,
    jstring session_id,
    jboolean use_session_cache,
    jstring config_json,
    jobject callback) {

    auto* runtime = from_handle(handle);
    if (!runtime) {
        return to_jstring(env, "{\"ok\":false,\"message\":\"runtime closed\"}");
    }
    if (!callback) {
        return to_jstring(env, "{\"ok\":false,\"message\":\"stream callback required\"}");
    }

    std::vector<std::pair<std::string, std::string>> messages;
    const jsize count = contents ? env->GetArrayLength(contents) : 0;
    const jsize role_count = roles ? env->GetArrayLength(roles) : 0;
    messages.reserve(static_cast<size_t>(count));
    for (jsize i = 0; i < count; ++i) {
        auto role = role_count > i ? static_cast<jstring>(env->GetObjectArrayElement(roles, i)) : nullptr;
        auto content = static_cast<jstring>(env->GetObjectArrayElement(contents, i));
        messages.emplace_back(to_string(env, role), to_string(env, content));
        if (role) env->DeleteLocalRef(role);
        if (content) env->DeleteLocalRef(content);
    }

    auto callback_class = env->GetObjectClass(callback);
    auto on_chunk = env->GetMethodID(callback_class, "onChunk", "(Ljava/lang/String;Z)V");
    if (!on_chunk) {
        env->DeleteLocalRef(callback_class);
        return to_jstring(env, "{\"ok\":false,\"message\":\"stream callback method not found\"}");
    }

    auto result = runtime->chat_text_stream(
        messages,
        max_tokens,
        to_string(env, session_id),
        use_session_cache == JNI_TRUE,
        to_string(env, config_json),
        [env, callback, on_chunk](const std::string& text, bool done) {
            jstring chunk = env->NewStringUTF(text.c_str());
            env->CallVoidMethod(callback, on_chunk, chunk, done ? JNI_TRUE : JNI_FALSE);
            env->DeleteLocalRef(chunk);
        });
    env->DeleteLocalRef(callback_class);
    return to_jstring(env, result);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_mnnode_app_model_MnnRuntime_nativeChatImage(
    JNIEnv* env,
    jclass,
    jlong handle,
    jobject bitmap,
    jstring prompt,
    jint max_tokens,
    jstring config_json) {

    auto* runtime = from_handle(handle);
    if (!runtime) {
        return to_jstring(env, "{\"ok\":false,\"message\":\"runtime closed\"}");
    }
    return to_jstring(env, runtime->chat_image(env, bitmap, to_string(env, prompt), max_tokens, to_string(env, config_json)));
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_mnnode_app_model_MnnRuntime_nativeChatImageStream(
    JNIEnv* env,
    jclass,
    jlong handle,
    jobject bitmap,
    jstring prompt,
    jint max_tokens,
    jstring config_json,
    jobject callback) {

    auto* runtime = from_handle(handle);
    if (!runtime) {
        return to_jstring(env, "{\"ok\":false,\"message\":\"runtime closed\"}");
    }
    if (!callback) {
        return to_jstring(env, "{\"ok\":false,\"message\":\"stream callback required\"}");
    }

    auto callback_class = env->GetObjectClass(callback);
    auto on_chunk = env->GetMethodID(callback_class, "onChunk", "(Ljava/lang/String;Z)V");
    if (!on_chunk) {
        env->DeleteLocalRef(callback_class);
        return to_jstring(env, "{\"ok\":false,\"message\":\"stream callback method not found\"}");
    }

    const auto prompt_text = to_string(env, prompt);
    auto result = runtime->chat_image_stream(env, bitmap, prompt_text, max_tokens, to_string(env, config_json), [env, callback, on_chunk](const std::string& text, bool done) {
        jstring chunk = env->NewStringUTF(text.c_str());
        env->CallVoidMethod(callback, on_chunk, chunk, done ? JNI_TRUE : JNI_FALSE);
        env->DeleteLocalRef(chunk);
    });
    env->DeleteLocalRef(callback_class);
    return to_jstring(env, result);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_mnnode_app_model_MnnRuntime_nativeState(JNIEnv* env, jclass, jlong handle) {
    auto* runtime = from_handle(handle);
    if (!runtime) {
        return to_jstring(env, "{\"loaded\":false,\"message\":\"runtime closed\"}");
    }
    return to_jstring(env, runtime->state_json());
}

extern "C" JNIEXPORT void JNICALL
Java_com_mnnode_app_model_MnnRuntime_nativeCancel(JNIEnv*, jclass, jlong handle) {
    auto* runtime = from_handle(handle);
    if (runtime) runtime->cancel();
}
