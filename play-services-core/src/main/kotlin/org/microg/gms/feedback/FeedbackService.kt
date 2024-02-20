/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.feedback

import android.app.ApplicationErrorReport
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.common.Feature
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.internal.ConnectionInfo
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import com.google.android.gms.feedback.ErrorReport
import com.google.android.gms.feedback.FeedbackOptions
import com.google.android.gms.feedback.internal.IFeedbackService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService
import org.microg.gms.feedback.ui.FeedbackAlohaActivity
import org.microg.gms.feedback.ui.KEY_ERROR_REPORT
import org.microg.gms.feedback.ui.KEY_SCREENSHOT_FILEPATH
import org.microg.gms.profile.ProfileManager
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Arrays


private const val TAG = "FeedbackService"

class FeedbackService : BaseService(TAG, GmsService.FEEDBACK) {

    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        Log.d(TAG, "handleServiceRequest start ")
        ProfileManager.ensureInitialized(this)
        callback.onPostInitCompleteWithConnectionInfo(
            CommonStatusCodes.SUCCESS,
            FeedbackServiceImpl(this, lifecycleScope),
            ConnectionInfo().apply {
                features = arrayOf(Feature("new_send_silent_feedback", 1))
            })
    }

}

class FeedbackServiceImpl(private val context: Context, private val lifecycleScope: LifecycleCoroutineScope) :
    IFeedbackService.Stub() {

    override fun startFeedbackFlow(errorReport: ErrorReport): Boolean {
        Log.d(TAG, "startFeedbackFlow: ")
        startFeedbackActivity(errorReport)
        return false
    }

    override fun silentSendFeedback(errorReport: ErrorReport): Boolean {
        Log.d(TAG, "Not impl silentSendFeedback: ")
        return false
    }

    override fun saveFeedbackDataAsync(bundle: Bundle, id: Long) {
        Log.d(TAG, "Not impl saveFeedbackDataAsync: ")
    }

    override fun saveFeedbackDataAsyncWithOption(options: FeedbackOptions, bundle: Bundle, id: Long) {
        Log.d(TAG, "Not impl saveFeedbackDataAsyncWithOption: $options")
    }

    override fun startFeedbackFlowAsync(errorReport: ErrorReport, id: Long) {
        Log.d(TAG, "startFeedbackFlowAsync errorReport:$errorReport")
        startFeedbackActivity(errorReport)
    }

    override fun isValidConfiguration(options: FeedbackOptions): Boolean {
        Log.d(TAG, "Not impl isValidConfiguration: $options")
        return false
    }

    private fun startFeedbackActivity(errorReport: ErrorReport) {
        Log.d(TAG, "startFeedbackActivity start ")
        lifecycleScope.launchWhenStarted {
            val intent = Intent(context, FeedbackAlohaActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                handleApplicationErrorReport(errorReport)
                val bitmapFilePath = handleErrorReportBitmap(errorReport)
                putExtra(KEY_SCREENSHOT_FILEPATH, bitmapFilePath)
                putExtra(KEY_ERROR_REPORT, errorReport)
                Log.d(TAG, "errorReport: $errorReport")
            }
            context.startActivity(intent)
        }
    }

    private suspend fun handleErrorReportBitmap(errorReport: ErrorReport): String? {
        if (errorReport.bitmap == null) {
            return null
        }
        return withContext(Dispatchers.IO) {
            var filePath: String? = null
            try {
                val imageFile = File(context.cacheDir, "screenshot.jpg")
                val outputStream = FileOutputStream(imageFile)
                errorReport.bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                outputStream.close()
                filePath = imageFile.absolutePath
            } catch (e: IOException) {
                Log.d(TAG, "handleErrorReportBitmap: ", e)
            } finally {
                errorReport.bitmap = null
            }
            filePath
        }
    }

    private fun handleApplicationErrorReport(errorReport: ErrorReport) {
        val uid = getCallingUid()
        val packages = context.packageManager.getPackagesForUid(uid)
        Log.d(TAG, "handleApplicationErrorReport: " + uid + " packages:" + Arrays.toString(packages))
        if (!packages.isNullOrEmpty()) {
            val applicationErrorReport = ApplicationErrorReport()
            applicationErrorReport.packageName = packages[0]
            applicationErrorReport.processName = packages[0]
            applicationErrorReport.time = System.currentTimeMillis()
            applicationErrorReport.type = 11
            val packageManager = context.packageManager
            try {
                val applicationInfo = packageManager.getApplicationInfo(packages[0], 0)
                applicationErrorReport.systemApp = applicationInfo.flags and 1 == 1
            } catch (e: PackageManager.NameNotFoundException) {
                Log.w(TAG, "handleApplicationErrorReport: ", e)
            }
            errorReport.applicationErrorReport = applicationErrorReport
        }
    }

}
