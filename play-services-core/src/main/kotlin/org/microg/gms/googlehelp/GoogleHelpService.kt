/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.googlehelp

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import com.google.android.gms.common.Feature
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.internal.ConnectionInfo
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import com.google.android.gms.feedback.FeedbackOptions
import com.google.android.gms.googlehelp.GoogleHelp
import com.google.android.gms.googlehelp.InProductHelp
import com.google.android.gms.googlehelp.SupportRequestHelp
import com.google.android.gms.googlehelp.internal.common.IGoogleHelpCallbacks
import com.google.android.gms.googlehelp.internal.common.IGoogleHelpService
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService
import org.microg.gms.common.PackageUtils

private const val TAG = "GoogleHelp"

val FEATURES = arrayOf(
    Feature("user_service_support", 1)
)

class GoogleHelpService : BaseService(TAG, GmsService.HELP) {
    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        val packageName = PackageUtils.getAndCheckCallingPackage(this, request.packageName)
            ?: throw IllegalArgumentException("Missing package name")
        val binder = GoogleHelpServiceImpl(packageName).asBinder()
        callback.onPostInitCompleteWithConnectionInfo(CommonStatusCodes.SUCCESS, binder, ConnectionInfo().apply { features = FEATURES })
    }
}

class GoogleHelpServiceImpl(val packageName: String) : IGoogleHelpService.Stub() {
    override fun processGoogleHelpAndPip(googleHelp: GoogleHelp?, callbacks: IGoogleHelpCallbacks?) {
        Log.d(TAG, "Not yet implemented: processGoogleHelpAndPip($googleHelp)")
        // Strip error report, can be too big
        googleHelp?.errorReport = null
        callbacks?.onProcessGoogleHelpFinished(googleHelp)
    }

    override fun processGoogleHelpAndPipWithBitmap(googleHelp: GoogleHelp?, bitmap: Bitmap?, callbacks: IGoogleHelpCallbacks?) {
        processGoogleHelpAndPip(googleHelp, callbacks)
    }

    override fun saveAsyncHelpPsd(bundle: Bundle?, timestamp: Long, googleHelp: GoogleHelp?, callbacks: IGoogleHelpCallbacks?) {
        Log.d(TAG, "Not yet implemented: saveAsyncHelpPsd")
        callbacks?.onSaveAsyncPsdFinished()
    }

    override fun saveAsyncFeedbackPsd(bundle: Bundle?, timestamp: Long, googleHelp: GoogleHelp?, callbacks: IGoogleHelpCallbacks?) {
        Log.d(TAG, "Not yet implemented: saveAsyncFeedbackPsd")
        callbacks?.onSaveAsyncPsdFinished()
    }

    override fun saveAsyncFeedbackPsbd(options: FeedbackOptions?, bundle: Bundle?, timestamp: Long, googleHelp: GoogleHelp?, callbacks: IGoogleHelpCallbacks?) {
        Log.d(TAG, "Not yet implemented: saveAsyncFeedbackPsbd")
        callbacks?.onSaveAsyncPsbdFinished()
    }

    override fun requestChatSupport(googleHelp: GoogleHelp?, phoneNumber: String?, s2: String?, callbacks: IGoogleHelpCallbacks?) {
        requestChatSupportWithSupportRequest(SupportRequestHelp().also { it.googleHelp = googleHelp; it.phoneNumber = phoneNumber }, callbacks)
    }

    override fun requestC2cSupport(googleHelp: GoogleHelp?, phoneNumber: String?, s2: String?, callbacks: IGoogleHelpCallbacks?) {
        requestC2cSupportWithSupportRequest(SupportRequestHelp().also { it.googleHelp = googleHelp; it.phoneNumber = phoneNumber }, callbacks)
    }

    override fun getSuggestions(googleHelp: GoogleHelp?, callbacks: IGoogleHelpCallbacks?) {
        Log.d(TAG, "Not yet implemented: getSuggestions")
        callbacks?.onNoSuggestions()
    }

    override fun getEscalationOptions(googleHelp: GoogleHelp?, callbacks: IGoogleHelpCallbacks?) {
        Log.d(TAG, "Not yet implemented: getEscalationOptions")
        callbacks?.onNoEscalationOptions()
    }

    override fun requestChatSupportWithSupportRequest(supportRequestHelp: SupportRequestHelp?, callbacks: IGoogleHelpCallbacks?) {
        Log.d(TAG, "Not yet implemented: requestChatSupport")
        callbacks?.onRequestChatSupportFailed()
    }

    override fun requestC2cSupportWithSupportRequest(supportRequestHelp: SupportRequestHelp?, callbacks: IGoogleHelpCallbacks?) {
        Log.d(TAG, "Not yet implemented: requestC2cSupport")
        callbacks?.onRequestC2cSupportFailed()
    }

    override fun processInProductHelpAndPip(inProductHelp: InProductHelp?, bitmap: Bitmap?, callbacks: IGoogleHelpCallbacks?) {
        Log.d(TAG, "Not yet implemented: processInProductHelpAndPip")
        callbacks?.onProcessInProductHelpFinished(inProductHelp)
    }

    override fun getRealtimeSupportStatus(googleHelp: GoogleHelp?, callbacks: IGoogleHelpCallbacks?) {
        Log.d(TAG, "Not yet implemented: getRealtimeSupportStatus")
        callbacks?.onNoRealtimeSupportStatus()
    }

}