/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.accountsettings.ui.bridge

import android.net.Uri
import android.util.Base64
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import org.microg.gms.accountsettings.ui.MainActivity
import org.microg.gms.accountsettings.ui.PicturePickerUtils
import org.microg.gms.accountsettings.ui.ResultStatus
import org.microg.gms.accountsettings.ui.evaluateJavascriptCallback
import org.microg.gms.accountsettings.ui.runOnMainLooper
import java.lang.RuntimeException
import java.util.concurrent.ExecutorService

class OcFilePickerBridge(val activity: MainActivity, val webView: WebView, val executor: ExecutorService) {

    companion object {
        const val NAME = "ocFilePicker"
        private const val TAG = "JS_$NAME"
    }

    private var currentRequestId: Int = 0
    private var pendingRequestId: Int? = null
    private var lastResult: Triple<Int, String?, String?>? = null

    private val pickerUtils = PicturePickerUtils(activity, ::handleResult, ::handleError)

    @JavascriptInterface
    fun pick(requestId: Int, mimeType: String?) {
        Log.d(TAG, "pick: requestId = $requestId, mimeType = $mimeType")
        currentRequestId = requestId
        val type = mimeType ?: "*/*"

        runOnMainLooper {
            try {
                pickerUtils.launchChooser(type)
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

    private fun handleError(status: ResultStatus) {
        notifyJavascript(currentRequestId, status.value, "", "")
    }

    private fun notifyJavascript(requestId: Int, status: Int, mimeType: String, data: String) {
        Log.d(TAG, "notifyJavascript: requestId: $requestId status: $status mimeType: $mimeType, data: $data")
        val escapedData = data.replace("\\", "\\\\").replace("'", "\\'")
        val script = "window.ocFilePickerCallback($requestId, $status, '$mimeType', '$escapedData')"
        evaluateJavascriptCallback(webView, script)
    }

}