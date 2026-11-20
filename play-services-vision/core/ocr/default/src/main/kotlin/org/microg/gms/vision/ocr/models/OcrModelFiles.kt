/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.vision.ocr.models

import android.content.Context
import androidx.core.content.ContextCompat
import org.microg.gms.vision.text.paddle.model.RecognitionModel
import java.io.File

internal const val MODEL_VERSION = 1L

private val detector = ModelSource(
    "ocr/ppocrv6_tiny/det", "PP-OCRv6_tiny_det_onnx", "2ba1506c0380b8f0b03dd142459aac66d4421f6c"
).file("inference.onnx", 1_780_590, "193bab7a04fca699a6c82e6abb5b81bdb28177f0abd4062552b04908dafb19f8")

private val orientation = ModelSource(
    "ocr/pp_lcnet_x0_25_textline_ori", "PP-LCNet_x0_25_textline_ori_onnx", "aea1f18d97338aaf84b463f90192f2820524a00a"
).file("inference.onnx", 1_019_454, "94a6a0a0425f2b5f08b5df72086f2d72abe40f1d22f6d12d2cd83674f11f2ff3")

internal val ocrModelFiles = mapOf(
    RecognitionModel.LATIN.pack to ModelSource(
        RecognitionModel.LATIN.assetDirectory, "latin_PP-OCRv5_mobile_rec_onnx", "89d3a50e2c27e2e7cceeab0e944c25c807d5db4f"
    ).run {
        listOf(
            detector,
            file("inference.onnx", 8_042_023, "7888113072263cb471b93f66dd5e2ad70548dc526fa1ace760d0d973dd121498"),
            file("inference.yml", 6_817, "0bbe984570f597af3638e50bdf2e8276f3ab26a61966096538b3b0d1849f5c84"),
            orientation
        )
    },
    RecognitionModel.CJK.pack to ModelSource(
        RecognitionModel.CJK.assetDirectory, "PP-OCRv6_small_rec_onnx", "b8f84f0b80c529de40b4fbb3544b84fa7233a513"
    ).run {
        listOf(
            detector,
            file("inference.onnx", 21_159_378, "5435fd747c9e0efe15a96d0b378d5bd157e9492ed8fd80edf08f30d02fa24634"),
            file("inference.yml", 150_579, "ab078671bb49f06228eadccd34f1bb501e157f7a047095ffb943ba81512c77d1"),
            orientation
        )
    },
    RecognitionModel.KOREAN.pack to ModelSource(
        RecognitionModel.KOREAN.assetDirectory, "korean_PP-OCRv5_mobile_rec_onnx", "5c6f574b8e2230adf4287b33e736d71b9fabd28e"
    ).run {
        listOf(
            detector,
            file("inference.onnx", 13_418_787, "92f0b7785e64fc9090106a241cf4c1eb97472824558272751b88a2a4476d3a08"),
            file("inference.yml", 96_039, "f757fa1c40e99edcf27e9cce879b93eb2a51fa46f5ef39095689b8c37dd75998")
        )
    },
    RecognitionModel.DEVANAGARI.pack to ModelSource(
        RecognitionModel.DEVANAGARI.assetDirectory, "devanagari_PP-OCRv5_mobile_rec_onnx", "251aec19e36739540d35e2cc943f6aa7503b98e5"
    ).run {
        listOf(
            detector,
            file("inference.onnx", 7_912_311, "cb789212ce96c69d3e74728ae4309d179281d68cb3945d0616b67cafab41c986"),
            file("inference.yml", 5_027, "9bd172dd26440c8ce94d1cde5d5baea6aefdc7cf3c5c8492e0beedef656d4e54")
        )
    }
)

private class ModelSource(private val directory: String, repository: String, revision: String) {
    // PaddlePaddle publishes these models under the Apache License 2.0.
    private val url = "https://huggingface.co/PaddlePaddle/$repository/resolve/$revision"

    fun file(name: String, bytes: Long, sha256: String) = OcrModelFile("$directory/$name", "$url/$name", bytes, sha256)
}

internal data class OcrModelFile(val assetPath: String, val url: String, val bytes: Long, val sha256: String) {
    fun blob(context: Context) = File(directory(context), sha256)
    fun partial(context: Context) = File(directory(context), "$sha256.part")
    fun exists(context: Context) = blob(context).let { it.isFile && it.length() == bytes }

    private fun directory(context: Context): File {
        val noBackupDirectory = checkNotNull(ContextCompat.getNoBackupFilesDir(context)) { "OCR model directory is unavailable" }
        return File(noBackupDirectory, "ocr-models")
    }
}
