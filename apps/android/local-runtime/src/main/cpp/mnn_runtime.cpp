#include "mnn_runtime.h"

#include <android/log.h>
#include <algorithm>
#include <cctype>
#include <chrono>
#include <cstring>
#include <functional>
#include <pthread.h>
#include <sched.h>
#include <streambuf>
#include <sstream>
#include <vector>

#include "MNN/MNNDefine.h"
#include "MNN/expr/ExprCreator.hpp"
#include "llm/llm.hpp"

namespace {

constexpr const char* LOG_TAG = "LociantMnnNative";
constexpr int FALLBACK_MIN_OUTPUT_TOKENS = 8;
constexpr int FALLBACK_MAX_OUTPUT_TOKENS = 32768;

#define LOCIANT_LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOCIANT_LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)

std::string escape_json(const std::string& value) {
    std::string out;
    out.reserve(value.size() + 8);
    for (char c : value) {
        switch (c) {
            case '\\': out += "\\\\"; break;
            case '"': out += "\\\""; break;
            case '\n': out += "\\n"; break;
            case '\r': out += "\\r"; break;
            case '\t': out += "\\t"; break;
            default: out += c; break;
        }
    }
    return out;
}

double now_ms() {
    using clock = std::chrono::steady_clock;
    return std::chrono::duration<double, std::milli>(clock::now().time_since_epoch()).count();
}

std::string bool_json(bool value) {
    return value ? "true" : "false";
}

int parse_int_config(const std::string& config_json, const std::string& key, int fallback) {
    const auto marker = "\"" + key + "\"";
    const auto key_pos = config_json.find(marker);
    if (key_pos == std::string::npos) return fallback;
    const auto colon = config_json.find(':', key_pos + marker.size());
    if (colon == std::string::npos) return fallback;
    auto pos = colon + 1;
    while (pos < config_json.size() && std::isspace(static_cast<unsigned char>(config_json[pos]))) {
        ++pos;
    }
    size_t end = pos;
    while (end < config_json.size() && std::isdigit(static_cast<unsigned char>(config_json[end]))) {
        ++end;
    }
    if (end == pos) return fallback;
    try {
        return std::stoi(config_json.substr(pos, end - pos));
    } catch (...) {
        return fallback;
    }
}

int clamp_max_tokens(int value, const std::string& config_json) {
    const int min_tokens = std::max(1, parse_int_config(config_json, "min_output_tokens", FALLBACK_MIN_OUTPUT_TOKENS));
    const int max_tokens = std::max(min_tokens, parse_int_config(config_json, "max_output_tokens", FALLBACK_MAX_OUTPUT_TOKENS));
    return std::max(min_tokens, std::min(max_tokens, value));
}

std::string runtime_config_json(const std::string& config_json) {
    if (config_json.empty()) {
        return "{}";
    }
    return config_json;
}

std::string sanitize_output_text(std::string text) {
    std::string output;
    const std::string start_tag = "<think>";
    const std::string end_tag = "</think>";
    size_t pos = 0;
    while (pos < text.size()) {
        const auto start = text.find(start_tag, pos);
        if (start == std::string::npos) {
            output += text.substr(pos);
            break;
        }
        output += text.substr(pos, start - pos);
        const auto body_start = start + start_tag.size();
        const auto end = text.find(end_tag, body_start);
        if (end == std::string::npos) {
            break;
        }
        pos = end + end_tag.size();
    }
    const auto dangling_end = output.find(end_tag);
    if (dangling_end != std::string::npos) {
        output.erase(0, dangling_end + end_tag.size());
    }
    const auto it = std::find_if(output.begin(), output.end(), [](unsigned char ch) {
        return !std::isspace(ch);
    });
    return std::string(it, output.end());
}

std::string normalize_role(const std::string& role) {
    return role.empty() ? "user" : role;
}

bool can_use_text_session_cache(
    const std::vector<std::pair<std::string, std::string>>& messages,
    bool use_session_cache) {
    if (!use_session_cache || messages.empty()) return false;
    const auto& message = messages.back();
    return normalize_role(message.first) == "user" && !message.second.empty();
}

void reset_text_session_cache(
    MNN::Transformer::Llm* llm,
    std::string& active_runtime_config,
    std::string& active_cache_session_id) {
    if (llm) {
        llm->reset();
    }
    active_runtime_config.clear();
    active_cache_session_id.clear();
}

void boost_current_thread_for_inference() {
    // Best-effort only. Some Android builds reject this for app UIDs.
    pthread_setname_np(pthread_self(), "lociant-mnn-infer");
    sched_param param{};
    param.sched_priority = 0;
    pthread_setschedparam(pthread_self(), SCHED_OTHER, &param);
}

void restore_android_stepping_status_if_needed(MNN::Transformer::Llm* llm) {
    if (!llm) return;
    auto* context = llm->getContext();
    if (!context) return;
    if (context->status == MNN::Transformer::LlmStatus::MAX_TOKENS_FINISHED ||
        context->status == MNN::Transformer::LlmStatus::NORMAL_FINISHED) {
        auto* mutable_context = const_cast<MNN::Transformer::LlmContext*>(context);
        mutable_context->status = MNN::Transformer::LlmStatus::RUNNING;
    }
}

class LlmStreamBuffer : public std::streambuf {
public:
    explicit LlmStreamBuffer(std::function<void(const char*, size_t)> callback)
        : callback_(std::move(callback)) {}

protected:
    std::streamsize xsputn(const char* s, std::streamsize n) override {
        if (callback_ && n > 0) {
            callback_(s, static_cast<size_t>(n));
        }
        return n;
    }

private:
    std::function<void(const char*, size_t)> callback_;
};

class SteppingStreamState {
public:
    SteppingStreamState(
        std::ostringstream& response,
        std::function<void(const std::string&, bool)> on_chunk,
        std::atomic_bool& cancel_requested,
        double started_ms,
        double* first_token_ms)
        : response_(response),
          on_chunk_(std::move(on_chunk)),
          cancel_requested_(cancel_requested),
          started_ms_(started_ms),
          first_token_ms_(first_token_ms) {}

    void process_chunk(const std::string& text) {
        const auto pos = text.find("<eop>");
        auto emit_visible = [this](const std::string& value) {
            if (value.empty()) return;
            if (first_token_ms_ && *first_token_ms_ < 0.0) {
                *first_token_ms_ = now_ms() - started_ms_;
            }
            response_ << value;
            if (on_chunk_) on_chunk_(value, false);
        };
        if (pos == std::string::npos) {
            emit_visible(filter_visible_text(text));
            return;
        }
        if (pos > 0) {
            emit_visible(filter_visible_text(text.substr(0, pos)));
        }
        pending_eop_ = true;
    }

    void resolve(MNN::Transformer::Llm* llm, int current_size, int max_tokens) {
        auto* context = llm ? llm->getContext() : nullptr;
        if (context &&
            context->status == MNN::Transformer::LlmStatus::MAX_TOKENS_FINISHED &&
            !cancel_requested_.load() &&
            current_size < max_tokens) {
            restore_android_stepping_status_if_needed(llm);
            if (pending_eop_) {
                ended_ = false;
                pending_eop_ = false;
            }
            return;
        }
        if (context &&
            context->status == MNN::Transformer::LlmStatus::NORMAL_FINISHED &&
            !pending_eop_ &&
            !cancel_requested_.load() &&
            current_size < max_tokens) {
            restore_android_stepping_status_if_needed(llm);
            return;
        }
        if (pending_eop_) finalize();
    }

    void finalize() {
        if (!pending_eop_) return;
        ended_ = true;
        pending_eop_ = false;
        if (on_chunk_) on_chunk_("", true);
    }

    bool ended() const {
        return ended_;
    }

private:
    std::string filter_visible_text(const std::string& text) {
        std::string visible;
        for (char ch : text) {
            think_buffer_ += ch;
            if (inside_think_) {
                if (ends_with(think_buffer_, "</think>")) {
                    inside_think_ = false;
                    think_buffer_.clear();
                } else if (think_buffer_.size() > std::string("</think>").size()) {
                    think_buffer_.erase(0, think_buffer_.size() - std::string("</think>").size());
                }
                continue;
            }
            if (ends_with(think_buffer_, "<think>")) {
                inside_think_ = true;
                if (think_buffer_.size() > 7) {
                    visible += think_buffer_.substr(0, think_buffer_.size() - 7);
                }
                think_buffer_.clear();
                continue;
            }
            if (!is_possible_think_prefix(think_buffer_)) {
                visible += think_buffer_;
                think_buffer_.clear();
            }
        }
        return visible;
    }

    static bool ends_with(const std::string& value, const std::string& suffix) {
        return value.size() >= suffix.size() &&
               value.compare(value.size() - suffix.size(), suffix.size(), suffix) == 0;
    }

    static bool is_possible_think_prefix(const std::string& value) {
        static const std::string start = "<think>";
        if (value.size() > start.size()) return false;
        return start.compare(0, value.size(), value) == 0;
    }

    std::ostringstream& response_;
    std::function<void(const std::string&, bool)> on_chunk_;
    std::atomic_bool& cancel_requested_;
    double started_ms_ = 0.0;
    double* first_token_ms_ = nullptr;
    std::string think_buffer_;
    bool inside_think_ = false;
    bool pending_eop_ = false;
    bool ended_ = false;
};

class Utf8StreamProcessor {
public:
    explicit Utf8StreamProcessor(std::function<void(const std::string&)> callback)
        : callback_(std::move(callback)) {}

    void process(const char* str, size_t len) {
        buffer_.append(str, len);
        size_t index = 0;
        std::string complete;
        while (index < buffer_.size()) {
            const int length = utf8_char_length(static_cast<unsigned char>(buffer_[index]));
            if (length == 0 || index + length > buffer_.size()) break;
            complete.append(buffer_, index, static_cast<size_t>(length));
            index += static_cast<size_t>(length);
        }
        buffer_ = buffer_.substr(index);
        if (!complete.empty() && callback_) {
            callback_(complete);
        }
    }

private:
    static int utf8_char_length(unsigned char byte) {
        if ((byte & 0x80) == 0) return 1;
        if ((byte & 0xE0) == 0xC0) return 2;
        if ((byte & 0xF0) == 0xE0) return 3;
        if ((byte & 0xF8) == 0xF0) return 4;
        return 0;
    }

    std::string buffer_;
    std::function<void(const std::string&)> callback_;
};

MNN::Express::VARP bitmap_to_rgb_var(JNIEnv* env, jobject bitmap, int& width, int& height) {
    width = 0;
    height = 0;
    if (!bitmap) return nullptr;

    AndroidBitmapInfo info{};
    if (AndroidBitmap_getInfo(env, bitmap, &info) != ANDROID_BITMAP_RESULT_SUCCESS) return nullptr;
    if (info.width <= 0 || info.height <= 0) return nullptr;
    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) return nullptr;

    void* pixels = nullptr;
    if (AndroidBitmap_lockPixels(env, bitmap, &pixels) != ANDROID_BITMAP_RESULT_SUCCESS || !pixels) return nullptr;

    width = static_cast<int>(info.width);
    height = static_cast<int>(info.height);
    std::vector<unsigned char> rgb(width * height * 3);

    auto* src = static_cast<unsigned char*>(pixels);
    for (int y = 0; y < height; ++y) {
        auto* row = src + y * info.stride;
        for (int x = 0; x < width; ++x) {
            const int si = x * 4;
            const int di = (y * width + x) * 3;
            rgb[di] = row[si + 2];
            rgb[di + 1] = row[si + 1];
            rgb[di + 2] = row[si];
        }
    }

    AndroidBitmap_unlockPixels(env, bitmap);

    auto var = MNN::Express::_Input({height, width, 3}, MNN::Express::NHWC, halide_type_of<unsigned char>());
    auto* dst = var->writeMap<unsigned char>();
    std::memcpy(dst, rgb.data(), rgb.size());
    return var;
}

std::string run_text_generation(
    MNN::Transformer::Llm* llm,
    const MNN::Transformer::ChatMessages& messages,
    int max_tokens,
    std::atomic_bool& cancel_requested,
    const std::function<void(const std::string&, bool)>& on_chunk,
    double* first_token_ms) {

    const double started_ms = now_ms();
    if (first_token_ms) *first_token_ms = -1.0;
    std::ostringstream response_text;
    SteppingStreamState stream_state(response_text, on_chunk, cancel_requested, started_ms, first_token_ms);
    Utf8StreamProcessor processor([&stream_state](const std::string& text) {
        stream_state.process_chunk(text);
    });
    LlmStreamBuffer stream_buffer([&processor](const char* str, size_t len) {
        processor.process(str, len);
    });
    std::ostream output(&stream_buffer);

    int current_size = 0;
    boost_current_thread_for_inference();
    restore_android_stepping_status_if_needed(llm);
    llm->response(messages, &output, "<eop>", 0);
    stream_state.resolve(llm, current_size, max_tokens);
    const size_t kv_before_decode = llm->getCurrentHistory();

    while (!cancel_requested.load() && !stream_state.ended() && current_size < max_tokens) {
        const double token_start = now_ms();
        llm->generate(1);
        current_size++;
        const double elapsed = now_ms() - token_start;
        if (elapsed > 5000.0) {
            LOCIANT_LOGW("generate step slow token=%d elapsed=%.2f", current_size, elapsed);
        }
        stream_state.resolve(llm, current_size, max_tokens);
    }
    stream_state.finalize();
    if (cancel_requested.load() && current_size > 0) {
        llm->eraseHistory(kv_before_decode, 0);
    }
    if (on_chunk && !cancel_requested.load() && !stream_state.ended()) {
        on_chunk("", true);
    }
    return sanitize_output_text(response_text.str());
}

std::string run_image_generation(
    MNN::Transformer::Llm* llm,
    const MNN::Transformer::MultimodalPrompt& input,
    int max_tokens,
    std::atomic_bool& cancel_requested,
    const std::function<void(const std::string&, bool)>& on_chunk,
    double* first_token_ms) {

    const double started_ms = now_ms();
    if (first_token_ms) *first_token_ms = -1.0;
    std::ostringstream response_text;
    SteppingStreamState stream_state(response_text, on_chunk, cancel_requested, started_ms, first_token_ms);
    Utf8StreamProcessor processor([&stream_state](const std::string& text) {
        stream_state.process_chunk(text);
    });
    LlmStreamBuffer stream_buffer([&processor](const char* str, size_t len) {
        processor.process(str, len);
    });
    std::ostream output(&stream_buffer);

    int current_size = 0;
    boost_current_thread_for_inference();
    restore_android_stepping_status_if_needed(llm);
    llm->response(input, &output, "<eop>", 0);
    stream_state.resolve(llm, current_size, max_tokens);
    const size_t kv_before_decode = llm->getCurrentHistory();

    while (!cancel_requested.load() && !stream_state.ended() && current_size < max_tokens) {
        const double token_start = now_ms();
        llm->generate(1);
        current_size++;
        const double elapsed = now_ms() - token_start;
        if (elapsed > 5000.0) {
            LOCIANT_LOGW("generate image step slow token=%d elapsed=%.2f", current_size, elapsed);
        }
        stream_state.resolve(llm, current_size, max_tokens);
    }
    stream_state.finalize();
    if (cancel_requested.load() && current_size > 0) {
        llm->eraseHistory(kv_before_decode, 0);
    }
    if (on_chunk && !cancel_requested.load() && !stream_state.ended()) {
        on_chunk("", true);
    }
    return sanitize_output_text(response_text.str());
}

} // namespace

MnnRuntimeNative::MnnRuntimeNative() = default;

MnnRuntimeNative::~MnnRuntimeNative() {
    std::lock_guard<std::mutex> lock(mutex_);
    cancel_requested_.store(true);
    if (llm_) {
        MNN::Transformer::Llm::destroy(llm_);
        llm_ = nullptr;
    }
}

std::string MnnRuntimeNative::load(
    const std::string& config_path,
    const std::string& tmp_path,
    const std::string& config_json) {

    const double start = now_ms();
    std::lock_guard<std::mutex> lock(mutex_);
    cancel_requested_.store(false);
    if (loaded_ && config_path_ == config_path && llm_) {
        return "{\"ok\":true,\"message\":\"already loaded\",\"elapsedMs\":0}";
    }

    if (llm_) {
        MNN::Transformer::Llm::destroy(llm_);
        llm_ = nullptr;
    }

    loaded_ = false;
    config_path_ = config_path;
    last_error_.clear();
    active_runtime_config_.clear();
    active_cache_session_id_.clear();

    if (config_path.empty()) {
        last_error_ = "empty config path";
        return "{\"ok\":false,\"message\":\"empty config path\"}";
    }

    LOCIANT_LOGI("load start config=%s", config_path.c_str());
    llm_ = MNN::Transformer::Llm::createLLM(config_path);
    if (!llm_) {
        last_error_ = "createLLM failed";
        LOCIANT_LOGW("load createLLM failed");
        return "{\"ok\":false,\"message\":\"createLLM failed\"}";
    }

    if (!tmp_path.empty()) {
        llm_->set_config("{\"tmp_path\":\"" + escape_json(tmp_path) + "\"}");
    }
    llm_->set_config("{\"async\":false}");
    if (!config_json.empty()) {
        llm_->set_config(config_json);
    }

    LOCIANT_LOGI("llm load enter");
    if (!llm_->load()) {
        last_error_ = "load failed";
        LOCIANT_LOGW("llm load failed elapsed=%.2f", now_ms() - start);
        MNN::Transformer::Llm::destroy(llm_);
        llm_ = nullptr;
        return "{\"ok\":false,\"message\":\"load failed\",\"elapsedMs\":" + std::to_string(now_ms() - start) + "}";
    }

    loaded_ = true;
    LOCIANT_LOGI("load ok elapsed=%.2f", now_ms() - start);
    return "{\"ok\":true,\"message\":\"loaded\",\"elapsedMs\":" + std::to_string(now_ms() - start) + "}";
}

std::string MnnRuntimeNative::chat_text(
    const std::vector<std::pair<std::string, std::string>>& messages,
    int max_tokens,
    const std::string& session_id,
    bool use_session_cache,
    const std::string& config_json) {

    const double start = now_ms();
    std::lock_guard<std::mutex> lock(mutex_);
    cancel_requested_.store(false);
    if (!loaded_ || !llm_) {
        return "{\"ok\":false,\"message\":\"model not loaded\"}";
    }
    if (messages.empty()) {
        return "{\"ok\":false,\"message\":\"messages are required\"}";
    }

    const auto runtime_config = runtime_config_json(config_json);
    const int tokens = clamp_max_tokens(max_tokens, runtime_config);
    MNN::Transformer::ChatMessages chat_messages;
    for (const auto& message : messages) {
        if (!message.second.empty()) {
            chat_messages.emplace_back(message.first.empty() ? "user" : message.first, message.second);
        }
    }
    if (chat_messages.empty()) {
        return "{\"ok\":false,\"message\":\"message content is required\"}";
    }
    const bool config_changed = active_runtime_config_ != runtime_config;
    const bool cache_capable = can_use_text_session_cache(messages, use_session_cache);
    const bool session_changed = active_cache_session_id_ != session_id;
    if (!cache_capable || config_changed || session_changed) {
        reset_text_session_cache(llm_, active_runtime_config_, active_cache_session_id_);
    }
    active_runtime_config_ = runtime_config;
    active_cache_session_id_ = cache_capable ? session_id : "";

    LOCIANT_LOGI(
        "chat_text stepped enter messages=%zu maxTokens=%d session=%s cacheCapable=%d configChanged=%d sessionChanged=%d",
        chat_messages.size(),
        tokens,
        session_id.c_str(),
        cache_capable ? 1 : 0,
        config_changed ? 1 : 0,
        session_changed ? 1 : 0);
    if (config_changed && !runtime_config.empty()) {
        llm_->set_config(runtime_config);
    }
    double first_token_ms = -1.0;
    const auto text = run_text_generation(llm_, chat_messages, tokens, cancel_requested_, nullptr, &first_token_ms);
    LOCIANT_LOGI("chat_text stepped exit elapsed=%.2f cancelled=%d", now_ms() - start, cancel_requested_.load() ? 1 : 0);
    const auto* context = llm_->getContext();

    std::ostringstream os;
    os << "{"
       << "\"ok\":true,"
       << "\"message\":\"chat completed\","
       << "\"cancelled\":" << bool_json(cancel_requested_.load()) << ","
       << "\"cache\":{\"enabled\":" << bool_json(cache_capable)
       << ",\"hit\":" << bool_json(cache_capable && !config_changed && !session_changed)
       << ",\"sessionId\":\"" << escape_json(session_id) << "\"},"
       << "\"elapsedMs\":" << (now_ms() - start) << ","
       << "\"text\":\"" << escape_json(text) << "\"";
    if (context) {
        const int evaluated_prompt_tokens = context->prompt_len;
        const int full_prompt_tokens = cache_capable
            ? static_cast<int>(llm_->tokenizer_encode(llm_->apply_chat_template(chat_messages)).size())
            : 0;
        const int prompt_tokens = full_prompt_tokens > 0 ? full_prompt_tokens : evaluated_prompt_tokens;
        const int cached_tokens = std::max(0, prompt_tokens - evaluated_prompt_tokens);
        const double visible_first_token_ms = std::max(first_token_ms, context->prefill_us / 1000.0);
        os << ",\"tokens\":{\"prompt\":" << prompt_tokens
           << ",\"generated\":" << context->gen_seq_len
           << ",\"cached\":" << cached_tokens
           << ",\"evaluatedPrompt\":" << evaluated_prompt_tokens << "}"
           << ",\"firstTokenMs\":" << visible_first_token_ms
           << ",\"prefillUs\":" << context->prefill_us
           << ",\"decodeUs\":" << context->decode_us;
    }
    os << "}";
    return os.str();
}

std::string MnnRuntimeNative::chat_text_stream(
    const std::vector<std::pair<std::string, std::string>>& messages,
    int max_tokens,
    const std::string& session_id,
    bool use_session_cache,
    const std::string& config_json,
    const std::function<void(const std::string&, bool)>& on_chunk) {

    const double start = now_ms();
    std::lock_guard<std::mutex> lock(mutex_);
    cancel_requested_.store(false);
    if (!loaded_ || !llm_) {
        return "{\"ok\":false,\"message\":\"model not loaded\"}";
    }
    if (messages.empty()) {
        return "{\"ok\":false,\"message\":\"messages are required\"}";
    }

    const auto runtime_config = runtime_config_json(config_json);
    const int tokens = clamp_max_tokens(max_tokens, runtime_config);
    MNN::Transformer::ChatMessages chat_messages;
    for (const auto& message : messages) {
        if (!message.second.empty()) {
            chat_messages.emplace_back(message.first.empty() ? "user" : message.first, message.second);
        }
    }
    if (chat_messages.empty()) {
        return "{\"ok\":false,\"message\":\"message content is required\"}";
    }
    const bool config_changed = active_runtime_config_ != runtime_config;
    const bool cache_capable = can_use_text_session_cache(messages, use_session_cache);
    const bool session_changed = active_cache_session_id_ != session_id;
    if (!cache_capable || config_changed || session_changed) {
        reset_text_session_cache(llm_, active_runtime_config_, active_cache_session_id_);
    }
    active_runtime_config_ = runtime_config;
    active_cache_session_id_ = cache_capable ? session_id : "";

    LOCIANT_LOGI(
        "chat_text_stream stepped enter messages=%zu maxTokens=%d session=%s cacheCapable=%d configChanged=%d sessionChanged=%d",
        chat_messages.size(),
        tokens,
        session_id.c_str(),
        cache_capable ? 1 : 0,
        config_changed ? 1 : 0,
        session_changed ? 1 : 0);
    if (config_changed && !runtime_config.empty()) {
        llm_->set_config(runtime_config);
    }
    double first_token_ms = -1.0;
    const auto text = run_text_generation(llm_, chat_messages, tokens, cancel_requested_, on_chunk, &first_token_ms);
    LOCIANT_LOGI("chat_text_stream stepped exit elapsed=%.2f cancelled=%d", now_ms() - start, cancel_requested_.load() ? 1 : 0);

    const auto* context = llm_->getContext();
    std::ostringstream os;
    os << "{"
       << "\"ok\":true,"
       << "\"message\":\"chat stream completed\","
       << "\"cancelled\":" << bool_json(cancel_requested_.load()) << ","
       << "\"cache\":{\"enabled\":" << bool_json(cache_capable)
       << ",\"hit\":" << bool_json(cache_capable && !config_changed && !session_changed)
       << ",\"sessionId\":\"" << escape_json(session_id) << "\"},"
       << "\"elapsedMs\":" << (now_ms() - start);
    if (context) {
        const int evaluated_prompt_tokens = context->prompt_len;
        const int full_prompt_tokens = cache_capable
            ? static_cast<int>(llm_->tokenizer_encode(llm_->apply_chat_template(chat_messages)).size())
            : 0;
        const int prompt_tokens = full_prompt_tokens > 0 ? full_prompt_tokens : evaluated_prompt_tokens;
        const int cached_tokens = std::max(0, prompt_tokens - evaluated_prompt_tokens);
        const double visible_first_token_ms = std::max(first_token_ms, context->prefill_us / 1000.0);
        os << ",\"tokens\":{\"prompt\":" << prompt_tokens
           << ",\"generated\":" << context->gen_seq_len
           << ",\"cached\":" << cached_tokens
           << ",\"evaluatedPrompt\":" << evaluated_prompt_tokens << "}"
           << ",\"firstTokenMs\":" << visible_first_token_ms
           << ",\"prefillUs\":" << context->prefill_us
           << ",\"decodeUs\":" << context->decode_us;
    }
    os << "}";
    return os.str();
}

std::string MnnRuntimeNative::chat_image(JNIEnv* env, jobject bitmap, const std::string& prompt, int max_tokens, const std::string& config_json) {
    const double start = now_ms();
    std::lock_guard<std::mutex> lock(mutex_);
    cancel_requested_.store(false);
    if (!loaded_ || !llm_) {
        return "{\"ok\":false,\"message\":\"model not loaded\"}";
    }

    int width = 0;
    int height = 0;
    auto image = bitmap_to_rgb_var(env, bitmap, width, height);
    if (!image.get()) {
        return "{\"ok\":false,\"message\":\"invalid bitmap\"}";
    }

    const auto runtime_config = runtime_config_json(config_json);
    const int tokens = clamp_max_tokens(max_tokens, runtime_config);
    MNN::Transformer::MultimodalPrompt input;
    input.prompt_template =
        "<img>image_0</img>\n" +
        (prompt.empty()
            ? std::string("Describe the image and answer the user's question.")
            : prompt);

    MNN::Transformer::PromptImagePart part;
    part.image_data = image;
    part.width = width;
    part.height = height;
    input.images["image_0"] = part;

    reset_text_session_cache(llm_, active_runtime_config_, active_cache_session_id_);
    LOCIANT_LOGI("chat_image stepped enter image=%dx%d promptLen=%zu maxTokens=%d", width, height, prompt.size(), tokens);
    if (!runtime_config.empty()) {
        llm_->set_config(runtime_config);
    }
    double first_token_ms = -1.0;
    const auto text = run_image_generation(llm_, input, tokens, cancel_requested_, nullptr, &first_token_ms);
    LOCIANT_LOGI("chat_image stepped exit elapsed=%.2f cancelled=%d", now_ms() - start, cancel_requested_.load() ? 1 : 0);
    const auto* context = llm_->getContext();

    std::ostringstream os;
    os << "{"
       << "\"ok\":true,"
       << "\"message\":\"chat completed\","
       << "\"cancelled\":" << bool_json(cancel_requested_.load()) << ","
       << "\"elapsedMs\":" << (now_ms() - start) << ","
       << "\"image\":{\"width\":" << width << ",\"height\":" << height << "},"
       << "\"text\":\"" << escape_json(text) << "\"";
    if (context) {
        const double visible_first_token_ms = std::max(first_token_ms, context->prefill_us / 1000.0);
        os << ",\"tokens\":{\"prompt\":" << context->prompt_len
           << ",\"generated\":" << context->gen_seq_len << "}"
           << ",\"firstTokenMs\":" << visible_first_token_ms
           << ",\"prefillUs\":" << context->prefill_us
           << ",\"decodeUs\":" << context->decode_us;
    }
    os << "}";
    return os.str();
}

std::string MnnRuntimeNative::chat_image_stream(
    JNIEnv* env,
    jobject bitmap,
    const std::string& prompt,
    int max_tokens,
    const std::string& config_json,
    const std::function<void(const std::string&, bool)>& on_chunk) {

    const double start = now_ms();
    std::lock_guard<std::mutex> lock(mutex_);
    cancel_requested_.store(false);
    if (!loaded_ || !llm_) {
        return "{\"ok\":false,\"message\":\"model not loaded\"}";
    }

    int width = 0;
    int height = 0;
    auto image = bitmap_to_rgb_var(env, bitmap, width, height);
    if (!image.get()) {
        return "{\"ok\":false,\"message\":\"invalid bitmap\"}";
    }

    const auto runtime_config = runtime_config_json(config_json);
    const int tokens = clamp_max_tokens(max_tokens, runtime_config);
    MNN::Transformer::MultimodalPrompt input;
    input.prompt_template =
        "<img>image_0</img>\n" +
        (prompt.empty()
            ? std::string("Describe the image and answer the user's question.")
            : prompt);

    MNN::Transformer::PromptImagePart part;
    part.image_data = image;
    part.width = width;
    part.height = height;
    input.images["image_0"] = part;

    reset_text_session_cache(llm_, active_runtime_config_, active_cache_session_id_);
    LOCIANT_LOGI("chat_image_stream stepped enter image=%dx%d promptLen=%zu maxTokens=%d", width, height, prompt.size(), tokens);
    if (!runtime_config.empty()) {
        llm_->set_config(runtime_config);
    }
    double first_token_ms = -1.0;
    const auto text = run_image_generation(llm_, input, tokens, cancel_requested_, on_chunk, &first_token_ms);
    LOCIANT_LOGI("chat_image_stream stepped exit elapsed=%.2f cancelled=%d", now_ms() - start, cancel_requested_.load() ? 1 : 0);

    const auto* context = llm_->getContext();
    std::ostringstream os;
    os << "{"
       << "\"ok\":true,"
       << "\"message\":\"chat stream completed\","
       << "\"cancelled\":" << bool_json(cancel_requested_.load()) << ","
       << "\"elapsedMs\":" << (now_ms() - start) << ","
       << "\"image\":{\"width\":" << width << ",\"height\":" << height << "}";
    if (context) {
        const double visible_first_token_ms = std::max(first_token_ms, context->prefill_us / 1000.0);
        os << ",\"tokens\":{\"prompt\":" << context->prompt_len
           << ",\"generated\":" << context->gen_seq_len << "}"
           << ",\"firstTokenMs\":" << visible_first_token_ms
           << ",\"prefillUs\":" << context->prefill_us
           << ",\"decodeUs\":" << context->decode_us;
    }
    os << "}";
    return os.str();
}

void MnnRuntimeNative::cancel() {
    cancel_requested_.store(true);
}

void MnnRuntimeNative::reset_session_cache() {
    std::lock_guard<std::mutex> lock(mutex_);
    reset_text_session_cache(llm_, active_runtime_config_, active_cache_session_id_);
    LOCIANT_LOGI("session cache reset");
}

std::string MnnRuntimeNative::state_json() const {
    std::lock_guard<std::mutex> lock(mutex_);
    std::ostringstream os;
    os << "{"
       << "\"loaded\":" << bool_json(loaded_) << ","
       << "\"configPath\":\"" << escape_json(config_path_) << "\","
       << "\"message\":\"" << escape_json(last_error_) << "\""
       << "}";
    return os.str();
}
