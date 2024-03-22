/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.feedback

import android.content.Context
import android.content.Intent
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
import org.microg.gms.profile.ProfileManager

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
        showFeedbackDisabledToast()
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
        showFeedbackDisabledToast()
    }

    override fun isValidConfiguration(options: FeedbackOptions): Boolean {
        Log.d(TAG, "Not impl isValidConfiguration: $options")
        return false
    }

    private fun showFeedbackDisabledToast() {
        lifecycleScope.launchWhenStarted {
            withContext(Dispatchers.Main){
                val intent: Intent = Intent().apply {
                    setClassName(context, "com.google.android.gms.feedback.FeedbackActivity")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }
        }
    }

}
