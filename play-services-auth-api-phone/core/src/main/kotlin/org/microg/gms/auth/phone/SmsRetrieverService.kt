/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth.phone

import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.phone.SmsRetrieverStatusCodes
import com.google.android.gms.auth.api.phone.internal.IAutofillPermissionStateCallback
import com.google.android.gms.auth.api.phone.internal.IOngoingSmsRequestCallback
import com.google.android.gms.auth.api.phone.internal.ISmsRetrieverApiService
import com.google.android.gms.auth.api.phone.internal.ISmsRetrieverResultCallback
import com.google.android.gms.common.Feature
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.api.internal.IStatusCallback
import com.google.android.gms.common.internal.ConnectionInfo
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService
import org.microg.gms.common.PackageUtils


private const val TAG = "SmsRetrieverService"
private val FEATURES = arrayOf(
    Feature("sms_retrieve", 1),
    Feature("user_consent", 3)
)

class SmsRetrieverService : BaseService(TAG, GmsService.SMS_RETRIEVER) {
    private val smsRetriever = SmsRetrieverCore(this, lifecycle)

    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        val packageName = PackageUtils.getAndCheckCallingPackage(this, request.packageName)
            ?: throw IllegalArgumentException("Missing package name")
        callback.onPostInitCompleteWithConnectionInfo(
            CommonStatusCodes.SUCCESS,
            SmsRetrieverServiceImpl(smsRetriever, packageName, lifecycle),
            ConnectionInfo().apply { features = FEATURES }
        )
    }
}


class SmsRetrieverServiceImpl(private val smsRetriever: SmsRetrieverCore, private val packageName: String, override val lifecycle: Lifecycle) :
    ISmsRetrieverApiService.Stub(), LifecycleOwner {

    override fun startSmsRetriever(callback: ISmsRetrieverResultCallback) {
        Log.d(TAG, "startSmsRetriever()")
        lifecycleScope.launchWhenStarted {
            val status = try {
                smsRetriever.startSmsRetriever(packageName)
                Status.SUCCESS
            } catch (e: Exception) {
                Status(CommonStatusCodes.INTERNAL_ERROR, e.message)
            }
            try {
                callback.onResult(status)
            } catch (e: Exception) {
                Log.w(TAG, "Failed delivering $status for startSmsRetriever()", e)
            }
        }
    }

    override fun startWithConsentPrompt(senderPhoneNumber: String?, callback: ISmsRetrieverResultCallback) {
        Log.d(TAG, "startWithConsentPrompt($senderPhoneNumber)")
        lifecycleScope.launchWhenStarted {
            val status = try {
                smsRetriever.startWithConsentPrompt(packageName, senderPhoneNumber)
                Status.SUCCESS
            } catch (e: Exception) {
                Status(CommonStatusCodes.INTERNAL_ERROR, e.message)
            }
            try {
                callback.onResult(status)
            } catch (e: Exception) {
                Log.w(TAG, "Failed delivering $status for startWithConsentPrompt()", e)
            }
        }
    }

    override fun startSmsCodeAutofill(callback: IStatusCallback) {
        Log.d(TAG, "startSmsCodeAutofill()")
        try {
            callback.onResult(Status(SmsRetrieverStatusCodes.API_NOT_AVAILABLE))
        } catch (e: Exception) {
            Log.w(TAG, "Failed delivering result for startSmsCodeAutofill()", e)
        }
    }

    override fun checkAutofillPermissionState(callback: IAutofillPermissionStateCallback) {
        Log.d(TAG, "checkAutofillPermissionState()")
        try {
            callback.onCheckPermissionStateResult(Status.SUCCESS, 1)
        } catch (e: Exception) {
            Log.w(TAG, "Failed delivering result for checkAutofillPermissionState()", e)
        }
    }

    override fun checkOngoingSmsRequest(packageName: String?, callback: IOngoingSmsRequestCallback) {
        Log.d(TAG, "checkOngoingSmsRequest($packageName)")
        lifecycleScope.launchWhenStarted {
            val result = try {
                smsRetriever.hasOngoingUserConsentRequest()
            } catch (e: Exception) {
                true
            }

            try {
                callback.onHasOngoingSmsRequestResult(Status.SUCCESS, result)
            } catch (e: Exception) {
                Log.w(TAG, "Failed delivering $result for checkOngoingSmsRequest()", e)
            }
        }
    }


    override fun startSmsCodeBrowser(callback: IStatusCallback) {
        Log.d(TAG, "startSmsCodeBrowser()")
        try {
            callback.onResult(Status(SmsRetrieverStatusCodes.API_NOT_AVAILABLE))
        } catch (e: Exception) {
            Log.w(TAG, "Failed delivering result for startSmsCodeBrowser()", e)
        }
    }
}