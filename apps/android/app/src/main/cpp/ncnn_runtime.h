#pragma once

#include <android/asset_manager.h>
#include <memory>
#include <mutex>
#include <string>

#include "net.h"

struct NcnnLoadResult {
    bool ok = false;
    std::string message;
    std::string model_id;
    std::string requested_backend;
    std::string actual_backend;
    int gpu_count = 0;
};

class NcnnRuntimeNative {
public:
    explicit NcnnRuntimeNative(AAssetManager* asset_manager);
    ~NcnnRuntimeNative();

    NcnnLoadResult load_model(
        const std::string& model_id,
        const std::string& param_asset,
        const std::string& bin_asset,
        const std::string& backend,
        int num_threads);

    std::string state_json() const;

    std::string detect_yuv420(
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
        float confidence_threshold);

private:
    std::string choose_backend(const std::string& requested, int gpu_count) const;

    AAssetManager* asset_manager_ = nullptr;
    std::unique_ptr<ncnn::Net> net_;
    mutable std::mutex mutex_;
    bool loaded_ = false;
    std::string model_id_;
    std::string requested_backend_ = "auto";
    std::string actual_backend_ = "cpu";
    int gpu_count_ = 0;
};

std::string to_json(const NcnnLoadResult& result);
