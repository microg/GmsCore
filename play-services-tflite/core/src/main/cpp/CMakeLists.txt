# SPDX-FileCopyrightText: 2026 microG Project Team
# SPDX-License-Identifier: Apache-2.0

cmake_minimum_required(VERSION 3.22.1)
project(microg_tflite LANGUAGES CXX)

set(litert_library "${LITERT_RUNTIME_DIR}/jni/${ANDROID_ABI}/libLiteRt.so")
# IDE sync configures CMake before Gradle extracts the AAR for the native build.
if(NOT LITERT_RUNTIME_DIR)
    message(FATAL_ERROR "Set LITERT_RUNTIME_DIR to the extracted LiteRT AAR directory")
endif()

add_library(litert_runtime SHARED IMPORTED)
set_target_properties(litert_runtime PROPERTIES IMPORTED_LOCATION "${litert_library}")

add_library(microg_tflite SHARED tflite_jni.cpp)
target_compile_features(microg_tflite PRIVATE cxx_std_17)
target_compile_options(microg_tflite PRIVATE -O2 -fno-exceptions -fno-rtti -fvisibility=hidden -Wall -Wextra)
target_link_options(microg_tflite PRIVATE -Wl,--exclude-libs,ALL
    -Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384)
target_link_libraries(microg_tflite PRIVATE litert_runtime)
