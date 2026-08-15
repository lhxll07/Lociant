#include "ncnn_runtime.h"

#include <algorithm>
#include <chrono>
#include <cmath>
#include <sstream>
#include <vector>

#include "gpu.h"

namespace {

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

struct Detection {
    float x1;
    float y1;
    float x2;
    float y2;
    float score;
    int cls;
};

// CameraX leaves the YUV buffer in sensor orientation while the preview is
// rotated into display orientation. Detection coordinates are normalized, so
// rotating the four box edges is enough to keep them aligned with that preview.
void rotate_detection_to_display(Detection& detection, int rotation) {
    int normalized = rotation % 360;
    if (normalized < 0) normalized += 360;

    const float x1 = detection.x1;
    const float y1 = detection.y1;
    const float x2 = detection.x2;
    const float y2 = detection.y2;

    switch (normalized) {
        case 90:
            detection.x1 = 1.f - y2;
            detection.y1 = x1;
            detection.x2 = 1.f - y1;
            detection.y2 = x2;
            break;
        case 180:
            detection.x1 = 1.f - x2;
            detection.y1 = 1.f - y2;
            detection.x2 = 1.f - x1;
            detection.y2 = 1.f - y1;
            break;
        case 270:
            detection.x1 = y1;
            detection.y1 = 1.f - x2;
            detection.x2 = y2;
            detection.y2 = 1.f - x1;
            break;
        default:
            break;
    }
}

const char* coco_label(int cls) {
    static const char* labels[] = {
        "person","bicycle","car","motorcycle","airplane","bus","train","truck","boat","traffic light",
        "fire hydrant","stop sign","parking meter","bench","bird","cat","dog","horse","sheep","cow",
        "elephant","bear","zebra","giraffe","backpack","umbrella","handbag","tie","suitcase","frisbee",
        "skis","snowboard","sports ball","kite","baseball bat","baseball glove","skateboard","surfboard","tennis racket","bottle",
        "wine glass","cup","fork","knife","spoon","bowl","banana","apple","sandwich","orange",
        "broccoli","carrot","hot dog","pizza","donut","cake","chair","couch","potted plant","bed",
        "dining table","toilet","tv","laptop","mouse","remote","keyboard","cell phone","microwave","oven",
        "toaster","sink","refrigerator","book","clock","vase","scissors","teddy bear","hair drier","toothbrush"
    };
    if (cls < 0 || cls >= 80) return "object";
    return labels[cls];
}

float intersection_area(const Detection& a, const Detection& b) {
    const float x1 = std::max(a.x1, b.x1);
    const float y1 = std::max(a.y1, b.y1);
    const float x2 = std::min(a.x2, b.x2);
    const float y2 = std::min(a.y2, b.y2);
    return std::max(0.f, x2 - x1) * std::max(0.f, y2 - y1);
}

void nms(std::vector<Detection>& detections, float threshold) {
    std::sort(detections.begin(), detections.end(), [](const Detection& a, const Detection& b) {
        return a.score > b.score;
    });

    std::vector<Detection> kept;
    for (const auto& det : detections) {
        bool keep = true;
        const float area_det = std::max(0.f, det.x2 - det.x1) * std::max(0.f, det.y2 - det.y1);
        for (const auto& prev : kept) {
            if (det.cls != prev.cls) continue;
            const float area_prev = std::max(0.f, prev.x2 - prev.x1) * std::max(0.f, prev.y2 - prev.y1);
            const float inter = intersection_area(det, prev);
            const float iou = inter / std::max(0.0001f, area_det + area_prev - inter);
            if (iou > threshold) {
                keep = false;
                break;
            }
        }
        if (keep) kept.push_back(det);
        if (kept.size() >= 20) break;
    }
    detections.swap(kept);
}

inline unsigned char clamp_u8(int value) {
    return static_cast<unsigned char>(std::max(0, std::min(255, value)));
}

void yuv_to_letterbox_rgb(
    int width,
    int height,
    const unsigned char* y,
    const unsigned char* u,
    const unsigned char* v,
    int y_row_stride,
    int u_row_stride,
    int v_row_stride,
    int y_pixel_stride,
    int u_pixel_stride,
    int v_pixel_stride,
    int input_size,
    std::vector<unsigned char>& rgb,
    float& scale,
    float& pad_x,
    float& pad_y) {

    scale = std::min(input_size / static_cast<float>(width), input_size / static_cast<float>(height));
    const int resized_w = static_cast<int>(std::round(width * scale));
    const int resized_h = static_cast<int>(std::round(height * scale));
    pad_x = (input_size - resized_w) * 0.5f;
    pad_y = (input_size - resized_h) * 0.5f;

    rgb.assign(input_size * input_size * 3, 114);

    for (int dy = 0; dy < resized_h; ++dy) {
        const int sy = std::min(height - 1, static_cast<int>(dy / scale));
        for (int dx = 0; dx < resized_w; ++dx) {
            const int sx = std::min(width - 1, static_cast<int>(dx / scale));
            const int yv = y[sy * y_row_stride + sx * y_pixel_stride] & 0xff;
            const int uv_index = (sy / 2) * u_row_stride + (sx / 2) * u_pixel_stride;
            const int vv_index = (sy / 2) * v_row_stride + (sx / 2) * v_pixel_stride;
            const int uu = (u[uv_index] & 0xff) - 128;
            const int vv = (v[vv_index] & 0xff) - 128;

            const int r = static_cast<int>(yv + 1.402f * vv);
            const int g = static_cast<int>(yv - 0.344136f * uu - 0.714136f * vv);
            const int b = static_cast<int>(yv + 1.772f * uu);

            const int out_x = static_cast<int>(pad_x) + dx;
            const int out_y = static_cast<int>(pad_y) + dy;
            const int out = (out_y * input_size + out_x) * 3;
            rgb[out] = clamp_u8(r);
            rgb[out + 1] = clamp_u8(g);
            rgb[out + 2] = clamp_u8(b);
        }
    }
}

bool resolve_yolov8_layout(const ncnn::Mat& out, int& channels, int& anchors, bool& channel_first) {
    channels = 0;
    anchors = 0;
    channel_first = true;

    if (out.dims == 2) {
        if (out.h >= 5 && out.h <= 512 && out.w > out.h) {
            channels = out.h;
            anchors = out.w;
            channel_first = true;
            return true;
        }
        if (out.w >= 5 && out.w <= 512 && out.h > out.w) {
            channels = out.w;
            anchors = out.h;
            channel_first = false;
            return true;
        }
    }

    if (out.dims == 3 && out.c == 1) {
        if (out.h >= 5 && out.h <= 512 && out.w > out.h) {
            channels = out.h;
            anchors = out.w;
            channel_first = true;
            return true;
        }
        if (out.w >= 5 && out.w <= 512 && out.h > out.w) {
            channels = out.w;
            anchors = out.h;
            channel_first = false;
            return true;
        }
    }

    return false;
}

float yolov8_value(const ncnn::Mat& out, int channel, int anchor, bool channel_first) {
    if (out.dims == 2) {
        return channel_first ? out.row(channel)[anchor] : out.row(anchor)[channel];
    }
    if (out.dims == 3 && out.c == 1) {
        return channel_first ? out.channel(0).row(channel)[anchor] : out.channel(0).row(anchor)[channel];
    }
    return 0.f;
}

std::string detections_to_json(
    bool ok,
    const std::string& message,
    const std::string& backend,
    int output_w,
    int output_h,
    int output_c,
    double elapsed_ms,
    const std::vector<Detection>& detections) {

    std::ostringstream os;
    os << "{"
       << "\"ok\":" << (ok ? "true" : "false") << ","
       << "\"message\":\"" << escape_json(message) << "\","
       << "\"backend\":\"" << escape_json(backend) << "\","
       << "\"elapsedMs\":" << elapsed_ms << ","
       << "\"output\":{\"w\":" << output_w << ",\"h\":" << output_h << ",\"c\":" << output_c << "},"
       << "\"detections\":[";
    for (size_t i = 0; i < detections.size(); ++i) {
        const auto& d = detections[i];
        if (i) os << ",";
        os << "{"
           << "\"classId\":" << d.cls << ","
           << "\"label\":\"" << coco_label(d.cls) << "\","
           << "\"score\":" << d.score << ","
           << "\"x\":" << d.x1 << ","
           << "\"y\":" << d.y1 << ","
           << "\"width\":" << std::max(0.f, d.x2 - d.x1) << ","
           << "\"height\":" << std::max(0.f, d.y2 - d.y1)
           << "}";
    }
    os << "]}";
    return os.str();
}

} // namespace

NcnnRuntimeNative::NcnnRuntimeNative(AAssetManager* asset_manager)
    : asset_manager_(asset_manager) {
    gpu_count_ = ncnn::get_gpu_count();
}

NcnnRuntimeNative::~NcnnRuntimeNative() {
    std::lock_guard<std::mutex> lock(mutex_);
    net_.reset();
}

NcnnLoadResult NcnnRuntimeNative::load_model(
    const std::string& model_id,
    const std::string& param_asset,
    const std::string& bin_asset,
    const std::string& backend,
    int num_threads) {

    std::lock_guard<std::mutex> lock(mutex_);

    NcnnLoadResult result;
    result.model_id = model_id;
    result.requested_backend = backend.empty() ? "auto" : backend;
    result.gpu_count = ncnn::get_gpu_count();

    const std::string selected_backend = choose_backend(result.requested_backend, result.gpu_count);
    if (selected_backend == "unavailable") {
        loaded_ = false;
        result.ok = false;
        result.actual_backend = "none";
        result.message = "vulkan unavailable";
        return result;
    }

    auto next = std::make_unique<ncnn::Net>();
    next->opt.num_threads = num_threads > 0 ? num_threads : 2;
    const bool use_vulkan = selected_backend == "vulkan";
    next->opt.use_vulkan_compute = use_vulkan;
    next->opt.use_fp16_packed = false;
    next->opt.use_fp16_storage = false;
    next->opt.use_fp16_arithmetic = false;

    int status = next->load_param(asset_manager_, param_asset.c_str());
    if (status != 0) {
        loaded_ = false;
        result.ok = false;
        result.actual_backend = selected_backend;
        result.message = "load param failed: " + std::to_string(status);
        return result;
    }

    status = next->load_model(asset_manager_, bin_asset.c_str());
    if (status != 0) {
        loaded_ = false;
        result.ok = false;
        result.actual_backend = selected_backend;
        result.message = "load model failed: " + std::to_string(status);
        return result;
    }

    net_ = std::move(next);
    loaded_ = true;
    model_id_ = model_id;
    requested_backend_ = result.requested_backend;
    actual_backend_ = selected_backend;
    gpu_count_ = result.gpu_count;

    result.ok = true;
    result.actual_backend = actual_backend_;
    result.message = "loaded";
    return result;
}

std::string NcnnRuntimeNative::state_json() const {
    std::lock_guard<std::mutex> lock(mutex_);
    std::ostringstream os;
    os << "{"
       << "\"loaded\":" << (loaded_ ? "true" : "false") << ","
       << "\"modelId\":\"" << escape_json(model_id_) << "\","
       << "\"requestedBackend\":\"" << escape_json(requested_backend_) << "\","
       << "\"actualBackend\":\"" << escape_json(actual_backend_) << "\","
       << "\"gpuCount\":" << gpu_count_
       << "}";
    return os.str();
}

std::string NcnnRuntimeNative::detect_yuv420(
    int width,
    int height,
    int rotation,
    const unsigned char* y,
    const unsigned char* u,
    const unsigned char* v,
    int y_row_stride,
    int u_row_stride,
    int v_row_stride,
    int y_pixel_stride,
    int u_pixel_stride,
    int v_pixel_stride,
    const std::string& input_name,
    const std::string& output_name,
    int input_size,
    float confidence_threshold) {

    const double start = now_ms();
    std::lock_guard<std::mutex> lock(mutex_);
    if (!loaded_ || !net_) {
        return detections_to_json(false, "model not loaded", actual_backend_, 0, 0, 0, 0.0, {});
    }
    if (!y || !u || !v || width <= 0 || height <= 0) {
        return detections_to_json(false, "invalid image", actual_backend_, 0, 0, 0, 0.0, {});
    }

    input_size = input_size > 0 ? input_size : 640;
    std::vector<unsigned char> rgb;
    float scale = 1.f;
    float pad_x = 0.f;
    float pad_y = 0.f;
    yuv_to_letterbox_rgb(
        width, height, y, u, v,
        y_row_stride, u_row_stride, v_row_stride,
        y_pixel_stride, u_pixel_stride, v_pixel_stride,
        input_size, rgb, scale, pad_x, pad_y);

    ncnn::Mat input = ncnn::Mat::from_pixels(rgb.data(), ncnn::Mat::PIXEL_RGB, input_size, input_size);
    const float norm[3] = {1.f / 255.f, 1.f / 255.f, 1.f / 255.f};
    input.substract_mean_normalize(nullptr, norm);

    ncnn::Extractor ex = net_->create_extractor();
    ex.input(input_name.c_str(), input);

    ncnn::Mat output;
    const int status = ex.extract(output_name.c_str(), output);
    if (status != 0 || output.empty()) {
        return detections_to_json(false, "extract failed: " + std::to_string(status), actual_backend_, output.w, output.h, output.c, now_ms() - start, {});
    }

    int channels = 0;
    int anchors = 0;
    bool channel_first = true;
    if (!resolve_yolov8_layout(output, channels, anchors, channel_first)) {
        return detections_to_json(false, "unsupported output shape", actual_backend_, output.w, output.h, output.c, now_ms() - start, {});
    }

    const int class_count = channels - 4;
    if (class_count <= 0 || class_count > 1000 || anchors <= 0) {
        return detections_to_json(false, "invalid yolov8 layout", actual_backend_, output.w, output.h, output.c, now_ms() - start, {});
    }

    const float conf_threshold = std::max(0.05f, std::min(0.95f, confidence_threshold));
    std::vector<Detection> detections;
    detections.reserve(64);

    for (int i = 0; i < anchors; ++i) {
        float best_score = 0.f;
        int best_cls = -1;
        for (int c = 0; c < class_count; ++c) {
            const float score = yolov8_value(output, 4 + c, i, channel_first);
            if (!std::isfinite(score) || score < 0.f || score > 1.0001f) continue;
            if (score > best_score) {
                best_score = score;
                best_cls = c;
            }
        }
        if (best_score < conf_threshold) continue;

        const float cx = yolov8_value(output, 0, i, channel_first);
        const float cy = yolov8_value(output, 1, i, channel_first);
        const float bw = yolov8_value(output, 2, i, channel_first);
        const float bh = yolov8_value(output, 3, i, channel_first);

        if (!std::isfinite(cx) || !std::isfinite(cy) || !std::isfinite(bw) || !std::isfinite(bh)) continue;
        if (bw <= 1.f || bh <= 1.f || bw > input_size * 1.5f || bh > input_size * 1.5f) continue;

        float x1 = (cx - bw * 0.5f - pad_x) / scale;
        float y1 = (cy - bh * 0.5f - pad_y) / scale;
        float x2 = (cx + bw * 0.5f - pad_x) / scale;
        float y2 = (cy + bh * 0.5f - pad_y) / scale;

        x1 = std::max(0.f, std::min(width * 1.f, x1)) / width;
        y1 = std::max(0.f, std::min(height * 1.f, y1)) / height;
        x2 = std::max(0.f, std::min(width * 1.f, x2)) / width;
        y2 = std::max(0.f, std::min(height * 1.f, y2)) / height;

        if (x2 <= x1 || y2 <= y1) continue;
        detections.push_back({x1, y1, x2, y2, best_score, best_cls});
    }

    nms(detections, 0.45f);
    for (auto& detection : detections) {
        rotate_detection_to_display(detection, rotation);
    }
    return detections_to_json(true, "ok", actual_backend_, output.w, output.h, output.c, now_ms() - start, detections);
}

std::string NcnnRuntimeNative::choose_backend(const std::string& requested, int gpu_count) const {
    if (requested == "cpu") return "cpu";
    if (requested == "vulkan") return gpu_count > 0 ? "vulkan" : "unavailable";
    return gpu_count > 0 ? "vulkan" : "cpu";
}

std::string to_json(const NcnnLoadResult& result) {
    std::ostringstream os;
    os << "{"
       << "\"ok\":" << (result.ok ? "true" : "false") << ","
       << "\"message\":\"" << escape_json(result.message) << "\","
       << "\"modelId\":\"" << escape_json(result.model_id) << "\","
       << "\"requestedBackend\":\"" << escape_json(result.requested_backend) << "\","
       << "\"actualBackend\":\"" << escape_json(result.actual_backend) << "\","
       << "\"gpuCount\":" << result.gpu_count
       << "}";
    return os.str();
}
