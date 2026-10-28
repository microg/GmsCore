/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

#include <jni.h>
#include <stddef.h>
#include <stdarg.h>
#include <stdint.h>

// Public C declarations adapted from Google LiteRT v2.3.0 (Apache-2.0)
struct TfLiteModel;
struct TfLiteInterpreter;
struct TfLiteInterpreterOptions;
struct TfLiteTensor;
struct TfLiteOpaqueTensor;
struct TfLiteDelegate;
using TfLiteOpaqueDelegate = TfLiteDelegate;
enum TfLiteStatus : int32_t { kTfLiteOk = 0 };
enum TfLiteType : int32_t;

struct TfLiteQuantizationParams {
    float scale;
    int32_t zero_point;
};
static_assert(sizeof(TfLiteStatus) == 4 && sizeof(TfLiteType) == 4);
static_assert(sizeof(TfLiteQuantizationParams) == 8 && alignof(TfLiteQuantizationParams) == 4);
static_assert(offsetof(TfLiteQuantizationParams, zero_point) == 4);

#pragma GCC visibility push(default)
extern "C" {
const char* TfLiteVersion(void);
TfLiteModel* TfLiteModelCreate(const void* model_data, size_t model_size);
TfLiteModel* TfLiteModelCreateFromFile(const char* model_path);
void TfLiteModelDelete(TfLiteModel* model);
TfLiteInterpreterOptions* TfLiteInterpreterOptionsCreate();
void TfLiteInterpreterOptionsDelete(TfLiteInterpreterOptions* options);
void TfLiteInterpreterOptionsSetNumThreads(TfLiteInterpreterOptions* options, int32_t num_threads);
void TfLiteInterpreterOptionsSetErrorReporter(TfLiteInterpreterOptions* options,
        void (*reporter)(void* user_data, const char* format, va_list args), void* user_data);
TfLiteInterpreter* TfLiteInterpreterCreate(const TfLiteModel* model,
        const TfLiteInterpreterOptions* optional_options);
void TfLiteInterpreterDelete(TfLiteInterpreter* interpreter);
int32_t TfLiteInterpreterGetInputTensorCount(const TfLiteInterpreter* interpreter);
TfLiteTensor* TfLiteInterpreterGetInputTensor(const TfLiteInterpreter* interpreter, int32_t input_index);
TfLiteStatus TfLiteInterpreterResizeInputTensor(TfLiteInterpreter* interpreter, int32_t input_index,
        const int* input_dims, int32_t input_dims_size);
TfLiteStatus TfLiteInterpreterAllocateTensors(TfLiteInterpreter* interpreter);
TfLiteStatus TfLiteInterpreterInvoke(TfLiteInterpreter* interpreter);
int32_t TfLiteInterpreterGetOutputTensorCount(const TfLiteInterpreter* interpreter);
const TfLiteTensor* TfLiteInterpreterGetOutputTensor(const TfLiteInterpreter* interpreter, int32_t output_index);
TfLiteType TfLiteTensorType(const TfLiteTensor* tensor);
int32_t TfLiteTensorNumDims(const TfLiteTensor* tensor);
int32_t TfLiteTensorDim(const TfLiteTensor* tensor, int32_t dim_index);
size_t TfLiteTensorByteSize(const TfLiteTensor* tensor);
void* TfLiteTensorData(const TfLiteTensor* tensor);
const char* TfLiteTensorName(const TfLiteTensor* tensor);
TfLiteStatus TfLiteTensorCopyFromBuffer(TfLiteTensor* tensor, const void* input_data, size_t input_data_size);
TfLiteStatus TfLiteTensorCopyToBuffer(const TfLiteTensor* output_tensor, void* output_data, size_t output_data_size);
void TfLiteInterpreterOptionsAddDelegate(TfLiteInterpreterOptions* options, TfLiteOpaqueDelegate* delegate);
TfLiteQuantizationParams TfLiteTensorQuantizationParams(const TfLiteTensor* tensor);
TfLiteStatus TfLiteOpaqueTensorGetNumDimsSignature(const TfLiteOpaqueTensor* opaque_tensor, int32_t* num_dims);
TfLiteStatus TfLiteOpaqueTensorGetDimSignature(const TfLiteOpaqueTensor* opaque_tensor, int32_t dim_index,
        int32_t* dim_length);
}
#pragma GCC visibility pop

namespace {

// This CPU API has process-scoped state; its opaque token has no heap ownership.
char cpuContext;
void* Initialize(const void*) { return &cpuContext; }
void Destroy(void*) { }

template <auto Callback, typename Signature = decltype(Callback)>
struct Forward;

template <auto Callback, typename Result, typename... Args>
struct Forward<Callback, Result (*)(Args...)> {
    static Result Invoke(void*, Args... args) { return Callback(args...); }
};

float QuantizationScale(void*, const TfLiteTensor* tensor) {
    return tensor ? TfLiteTensorQuantizationParams(tensor).scale : 0;
}

int32_t QuantizationZeroPoint(void*, const TfLiteTensor* tensor) {
    return tensor ? TfLiteTensorQuantizationParams(tensor).zero_point : 0;
}

int32_t DimensionSignature(void*, const TfLiteTensor* tensor, int32_t index) {
    if (!tensor || index < 0) return -1;
    const auto* opaque = reinterpret_cast<const TfLiteOpaqueTensor*>(tensor);
    int32_t count = -1;
    if (TfLiteOpaqueTensorGetNumDimsSignature(opaque, &count) != kTfLiteOk || count <= 0) {
        count = TfLiteTensorNumDims(tensor);
    }
    if (index >= count) return -1;
    int32_t value = -1;
    return TfLiteOpaqueTensorGetDimSignature(opaque, index, &value) == kTfLiteOk ? value : -1;
}

bool VerifyModel(void*, const void* data, size_t size) {
    if (!data || size == 0) return false;
    TfLiteModel* model = TfLiteModelCreate(data, size);
    if (!model) return false;
    TfLiteModelDelete(model);
    return true;
}

using Function = void (*)();
#define ENTRY(name) reinterpret_cast<Function>(name)
#define C_API_ENTRY(name) ENTRY(Forward<&TfLite##name>::Invoke)
const Function api[40] = {
    ENTRY(Initialize), ENTRY(Destroy), C_API_ENTRY(Version), C_API_ENTRY(ModelCreate),
    C_API_ENTRY(ModelCreateFromFile), C_API_ENTRY(ModelDelete), C_API_ENTRY(InterpreterOptionsCreate),
    C_API_ENTRY(InterpreterOptionsDelete), C_API_ENTRY(InterpreterOptionsSetNumThreads),
    C_API_ENTRY(InterpreterOptionsSetErrorReporter), C_API_ENTRY(InterpreterCreate),
    C_API_ENTRY(InterpreterDelete), C_API_ENTRY(InterpreterGetInputTensorCount),
    C_API_ENTRY(InterpreterGetInputTensor), C_API_ENTRY(InterpreterResizeInputTensor),
    C_API_ENTRY(InterpreterAllocateTensors), C_API_ENTRY(InterpreterInvoke),
    C_API_ENTRY(InterpreterGetOutputTensorCount), C_API_ENTRY(InterpreterGetOutputTensor), C_API_ENTRY(TensorType),
    C_API_ENTRY(TensorNumDims), C_API_ENTRY(TensorDim), C_API_ENTRY(TensorByteSize),
    C_API_ENTRY(TensorData), C_API_ENTRY(TensorName), C_API_ENTRY(TensorCopyFromBuffer),
    C_API_ENTRY(TensorCopyToBuffer), nullptr, ENTRY(QuantizationScale), ENTRY(QuantizationZeroPoint),
    ENTRY(DimensionSignature), C_API_ENTRY(InterpreterOptionsAddDelegate), ENTRY(Initialize),
    nullptr, nullptr, nullptr, nullptr, nullptr, nullptr, ENTRY(VerifyModel)
};
#undef C_API_ENTRY
#undef ENTRY

struct Descriptor {
    const char* version;
    const Function* functions;
    const char* experimentalVersion;
    const void* experimental;
    const char* mostlyStableVersion;
    const void* mostlyStable;
    const char* compiledModelVersion;
    const void* compiledModel;
};
static_assert(sizeof(Descriptor) == 8 * sizeof(void*));
const Descriptor descriptor{"1.4.0", api, "0.0.0", nullptr, "0.0.0", nullptr, nullptr, nullptr};

} // namespace

extern "C" JNIEXPORT jlong JNICALL
Java_com_google_android_gms_tflite_dynamite_TfLiteNativeApi_getApiNative(JNIEnv*, jclass) {
    return static_cast<jlong>(reinterpret_cast<intptr_t>(&descriptor));
}
