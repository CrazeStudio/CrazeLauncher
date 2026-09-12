#include <cstdlib>
#include <cstring>
#include <vector>
#include <string>
#include <android/log.h>

#define LOG_TAG "ShadercStub"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C" {

typedef struct shaderc_compiler* shaderc_compiler_t;
typedef struct shaderc_compile_options* shaderc_compile_options_t;
typedef struct shaderc_compilation_result* shaderc_compilation_result_t;

typedef enum {
    shaderc_vertex_shader = 0,
    shaderc_fragment_shader,
    shaderc_compute_shader,
    shaderc_geometry_shader,
    shaderc_tess_control_shader,
    shaderc_tess_evaluation_shader,
    shaderc_glsl_infer_from_source,
    shaderc_glsl_standard_infer_from_source,
    shaderc_spirv_assembly
} shaderc_shader_kind;

typedef enum {
    shaderc_compilation_status_success = 0,
    shaderc_compilation_status_invalid_stage,
    shaderc_compilation_status_compilation_error,
    shaderc_compilation_status_internal_error,
    shaderc_compilation_status_null_result_object,
    shaderc_compilation_status_invalid_assembly,
    shaderc_compilation_status_validation_error,
    shaderc_compilation_status_transformation_error,
    shaderc_compilation_status_configuration_error
} shaderc_compilation_status;

typedef enum {
    shaderc_source_language_glsl = 0,
    shaderc_source_language_hlsl
} shaderc_source_language;

typedef enum {
    shaderc_optimization_level_zero = 0,
    shaderc_optimization_level_size,
    shaderc_optimization_level_performance
} shaderc_optimization_level;

struct shaderc_compiler {
    int dummy;
};

struct shaderc_compile_options {
    int dummy;
};

struct shaderc_compilation_result {
    std::vector<char> output_bytes;
    std::string error_message;
    shaderc_compilation_status status;
};

__attribute__((visibility("default")))
shaderc_compiler_t shaderc_compiler_initialize(void) {
    LOGI("shaderc_compiler_initialize called");
    return new shaderc_compiler{0};
}

__attribute__((visibility("default")))
void shaderc_compiler_release(shaderc_compiler_t compiler) {
    LOGI("shaderc_compiler_release called");
    delete compiler;
}

__attribute__((visibility("default")))
shaderc_compile_options_t shaderc_compile_options_initialize(void) {
    return new shaderc_compile_options{0};
}

__attribute__((visibility("default")))
void shaderc_compile_options_release(shaderc_compile_options_t options) {
    delete options;
}

__attribute__((visibility("default")))
void shaderc_compile_options_add_macro_definition(shaderc_compile_options_t options, const char* name, size_t name_length, const char* value, size_t value_length) {
    // stub
}

__attribute__((visibility("default")))
void shaderc_compile_options_set_source_language(shaderc_compile_options_t options, shaderc_source_language lang) {
    // stub
}

__attribute__((visibility("default")))
void shaderc_compile_options_set_generate_debug_info(shaderc_compile_options_t options) {
    // stub
}

__attribute__((visibility("default")))
void shaderc_compile_options_set_optimization_level(shaderc_compile_options_t options, shaderc_optimization_level level) {
    // stub
}

__attribute__((visibility("default")))
shaderc_compilation_result_t shaderc_compile_into_spv(
    const shaderc_compiler_t compiler,
    const char* source_text,
    size_t source_text_size,
    shaderc_shader_kind shader_kind,
    const char* input_file_name,
    const char* entry_point_name,
    const shaderc_compile_options_t additional_options) {
    
    LOGI("shaderc_compile_into_spv called for file: %s, entry: %s, size: %zu", 
         input_file_name ? input_file_name : "unknown", 
         entry_point_name ? entry_point_name : "main", 
         source_text_size);

    auto* result = new shaderc_compilation_result();
    result->status = shaderc_compilation_status_success;
    
    // Minimal valid SPIR-V binary header
    const uint32_t dummy_spirv[] = {
        0x07230230, // Magic number
        0x00010000, // Version 1.0.0
        0x00080001, // Generator id
        0x00000001, // Bound
        0x00000000  // Schema
    };
    
    result->output_bytes.resize(sizeof(dummy_spirv));
    std::memcpy(result->output_bytes.data(), dummy_spirv, sizeof(dummy_spirv));
    return result;
}

__attribute__((visibility("default")))
void shaderc_result_release(shaderc_compilation_result_t result) {
    delete result;
}

__attribute__((visibility("default")))
size_t shaderc_result_get_num_bytes(const shaderc_compilation_result_t result) {
    if (!result) return 0;
    return result->output_bytes.size();
}

__attribute__((visibility("default")))
const char* shaderc_result_get_bytes(const shaderc_compilation_result_t result) {
    if (!result || result->output_bytes.empty()) return nullptr;
    return result->output_bytes.data();
}

__attribute__((visibility("default")))
shaderc_compilation_status shaderc_result_get_compilation_status(const shaderc_compilation_result_t result) {
    if (!result) return shaderc_compilation_status_null_result_object;
    return result->status;
}

__attribute__((visibility("default")))
size_t shaderc_result_get_num_warnings(const shaderc_compilation_result_t result) {
    return 0;
}

__attribute__((visibility("default")))
size_t shaderc_result_get_num_errors(const shaderc_compilation_result_t result) {
    return 0;
}

__attribute__((visibility("default")))
const char* shaderc_result_get_error_message(const shaderc_compilation_result_t result) {
    if (!result || result->error_message.empty()) return "";
    return result->error_message.c_str();
}

} // extern "C"
