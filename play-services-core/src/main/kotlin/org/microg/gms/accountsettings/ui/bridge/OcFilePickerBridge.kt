/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.accountsettings.ui.bridge

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import org.microg.gms.accountsettings.ui.MainActivity
import org.microg.gms.accountsettings.ui.evaluateJavascriptCallback
import org.microg.gms.accountsettings.ui.runOnMainLooper
import java.io.File
import java.lang.RuntimeException
import java.util.concurrent.ExecutorService

enum class ResultStatus(val value: Int) {
    USER_CANCEL(1), FAILED(2), SUCCESS(3), NO_OP(4)
}

class OcFilePickerBridge(val activity: MainActivity, val webView: WebView, val executor: ExecutorService) {

    companion object {
        const val NAME = "ocFilePicker"
        private const val TAG = "JS_$NAME"
        private const val CAMERA_TEMP_DIR = "octa_camera_temp"
    }

    private var currentRequestId: Int = 0
    private var pendingRequestId: Int? = null
    private var lastResult: Triple<Int, String?, String?>? = null
    private var pendingMimeType: String? = null

    private lateinit var fileChooserLauncher: ActivityResultLauncher<Intent>
    private lateinit var cameraPermissionLauncher: ActivityResultLauncher<String>
    private var currentPhotoUri: Uri? = null

    init {
        initializeChooserLauncher()
    }

    private fun initializeChooserLauncher() {
        fileChooserLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri = result.data?.data
                if (uri != null) {
                    handleResult(uri)
                } else if (currentPhotoUri != null) {
                    handleResult(currentPhotoUri)
                } else {
                    notifyJavascript(currentRequestId, ResultStatus.FAILED.value, "", "")
                }
            } else {
                notifyJavascript(currentRequestId, ResultStatus.USER_CANCEL.value, "", "")
            }
        }
        cameraPermissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                pendingMimeType?.let {
                    launchChooserInternal(it)
                    pendingMimeType = null
                }
            } else {
                pendingMimeType?.let {
                    launchFilePickerOnly(it)
                    pendingMimeType = null
                }
            }
        }
    }

    @JavascriptInterface
    fun pick(requestId: Int, mimeType: String?) {
        Log.d(TAG, "pick: requestId = $requestId, mimeType = $mimeType")
        currentRequestId = requestId
        val type = mimeType ?: "*/*"

        runOnMainLooper {
            try {
                launchChooser(type)
            } catch (e: Exception) {
                Log.w(TAG, "pick: launchChooser error", e)
                notifyJavascript(requestId, ResultStatus.FAILED.value, "", "")
            }
        }
    }

    @JavascriptInterface
    fun resume(requestId: Int) {
        Log.d(TAG, "resume: requestId: $requestId lastResult:$lastResult")
        val lastResult = this.lastResult

        runOnMainLooper {
            if (lastResult != null) {
                val (status, mimeType, data) = lastResult
                notifyJavascript(requestId, status, mimeType ?: "", data ?: "")
                this.lastResult = null
            } else if (pendingRequestId != null) {
                pendingRequestId = requestId
            } else {
                notifyJavascript(requestId, ResultStatus.NO_OP.value, "", "")
            }
        }
    }

    private fun launchChooser(mimeType: String) {
        if (mimeType.startsWith("image/") || mimeType == "image/*" || mimeType == "*/*") {
            when {
                ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                    launchChooserInternal(mimeType)
                }

                else -> {
                    pendingMimeType = mimeType
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }
        } else {
            launchFilePickerOnly(mimeType)
        }
    }

    private fun launchFilePickerOnly(mimeType: String) {
        val getContentIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = mimeType
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        fileChooserLauncher.launch(getContentIntent)
    }

    private fun launchChooserInternal(mimeType: String) {
        val getContentIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = mimeType
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(activity.packageManager) != null) {
            currentPhotoUri = createImageUri()
            if (currentPhotoUri != null) {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri)
                val chooserIntent = Intent.createChooser(getContentIntent, "Choose")
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(takePictureIntent))
                fileChooserLauncher.launch(chooserIntent)
            } else {
                fileChooserLauncher.launch(getContentIntent)
            }
        } else {
            fileChooserLauncher.launch(getContentIntent)
        }
    }

    private fun createImageUri(): Uri? {
        try {
            val cacheDir = activity.cacheDir
            val cameraDir = File(cacheDir, CAMERA_TEMP_DIR)
            if (!cameraDir.exists()) {
                cameraDir.mkdirs()
            }
            val photoFile = File(cameraDir, "camera_temp.jpg")
            if (photoFile.exists()) {
                photoFile.delete()
            }
            photoFile.createNewFile()
            return FileProvider.getUriForFile(activity, "${activity.packageName}.fileprovider", photoFile)
        } catch (e: Exception) {
            Log.w(TAG, "createImageUri: ", e)
            return null
        }
    }

    private fun handleResult(uri: Uri?) {
        if (uri == null) {
            notifyJavascript(currentRequestId, ResultStatus.USER_CANCEL.value, "", "")
            return
        }
        pendingRequestId = currentRequestId
        executor.submit {
            try {
                val contentResolver = activity.contentResolver
                val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
                val inputStream = contentResolver.openInputStream(uri)

                if (inputStream != null) {
                    val bytes = inputStream.readBytes()
                    val encodedData = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    inputStream.close()

                    runOnMainLooper {
                        val pendingId = pendingRequestId
                        if (pendingId != null) {
                            notifyJavascript(pendingId, ResultStatus.SUCCESS.value, mimeType, encodedData)
                            pendingRequestId = null
                        } else {
                            lastResult = Triple(ResultStatus.SUCCESS.value, mimeType, encodedData)
                        }
                    }
                } else {
                    throw RuntimeException("Failed to open input stream")
                }
            } catch (e: Exception) {
                Log.w(TAG, "handleResult: ", e)
                runOnMainLooper {
                    val pendingId = pendingRequestId
                    if (pendingId != null) {
                        notifyJavascript(pendingId, ResultStatus.FAILED.value, "", "")
                        pendingRequestId = null
                    }
                }
            }
        }
    }

    private fun notifyJavascript(requestId: Int, status: Int, mimeType: String, data: String) {
        Log.d(TAG, "notifyJavascript: requestId: $requestId status: $status mimeType: $mimeType, data: $data")
        val escapedData = data.replace("\\", "\\\\").replace("'", "\\'")
        val script = "window.ocFilePickerCallback($requestId, $status, '$mimeType', '$escapedData')"
        evaluateJavascriptCallback(webView, script)
    }

}