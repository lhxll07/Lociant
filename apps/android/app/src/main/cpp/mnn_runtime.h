#pragma once

#include <android/bitmap.h>
#include <functional>
#include <atomic>
#include <memory>
#include <mutex>
#include <string>
#include <utility>
#include <vector>

namespace MNN {
namespace Transformer {
class Llm;
}
}

class MnnRuntimeNative {
public:
    MnnRuntimeNative();
    ~MnnRuntimeNative();

    std::string load(const std::string& config_path, const std::string& tmp_path, const std::string& config_json);
    std::string chat_text(const std::vector<std::pair<std::string, std::string>>& messages, int max_tokens, const std::string& session_id, bool use_session_cache, const std::string& config_json);
    std::string chat_text_stream(const std::vector<std::pair<std::string, std::string>>& messages, int max_tokens, const std::string& session_id, bool use_session_cache, const std::string& config_json, const std::function<void(const std::string&, bool)>& on_chunk);
    std::string chat_image(JNIEnv* env, jobject bitmap, const std::string& prompt, int max_tokens, const std::string& config_json);
    std::string chat_image_stream(JNIEnv* env, jobject bitmap, const std::string& prompt, int max_tokens, const std::string& config_json, const std::function<void(const std::string&, bool)>& on_chunk);
    void cancel();
    std::string state_json() const;

private:
    MNN::Transformer::Llm* llm_ = nullptr;
    mutable std::mutex mutex_;
    bool loaded_ = false;
    std::string config_path_;
    std::string last_error_;
    std::string active_runtime_config_;
    std::string active_cache_session_id_;
    std::atomic_bool cancel_requested_{false};
};
