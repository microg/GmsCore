/*
 * SPDX-FileCopyrightText: 2021, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package org.microg.gms.safetynet

import android.content.Context
import android.content.Intent
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Parcel
import android.os.ResultReceiver
import android.util.Base64
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import com.google.android.gms.safetynet.AttestationData
import com.google.android.gms.safetynet.RecaptchaResultData
import com.google.android.gms.safetynet.internal.ISafetyNetCallbacks
import com.google.android.gms.safetynet.internal.ISafetyNetService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.microg.gms.BaseService
import org.microg.gms.checkin.LastCheckinInfo
import org.microg.gms.common.GmsService
import org.microg.gms.common.PackageUtils
import org.microg.gms.droidguard.DroidGuardResultCreator
import org.microg.gms.recaptcha.ReCaptchaActivity
import org.microg.gms.recaptcha.appendUrlEncodedParam
import java.io.IOException
import java.util.*

private const val TAG = "GmsSafetyNet"
private const val DEFAULT_API_KEY = "AIzaSyDqVnJBjE5ymo--oBJt3On7HQx9xNm1RHA"

class SafetyNetClientService : BaseService(TAG, GmsService.SAFETY_NET_CLIENT) {
    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        callback.onPostInitComplete(0, SafetyNetClientServiceImpl(this, request.packageName, lifecycle), null)
    }
}


class SafetyNetClientServiceImpl(private val context: Context, private val packageName: String, private val lifecycle: Lifecycle) : ISafetyNetService.Stub(), LifecycleOwner {
    override fun getLifecycle(): Lifecycle = lifecycle

    override fun attest(callbacks: ISafetyNetCallbacks, nonce: ByteArray) {
        attestWithApiKey(callbacks, nonce, DEFAULT_API_KEY)
    }

    override fun attestWithApiKey(callbacks: ISafetyNetCallbacks, nonce: ByteArray?, apiKey: String) {
        if (nonce == null) {
            callbacks.onAttestationData(Status(CommonStatusCodes.DEVELOPER_ERROR), null)
            return
        }
        if (!SafetyNetPrefs.get(context).isEnabled) {
            Log.d(TAG, "ignoring SafetyNet request, it's disabled")
            callbacks.onAttestationData(Status.CANCELED, null)
            return
        }

        lifecycleScope.launchWhenStarted {
            try {
                val attestation = Attestation(context, packageName)
                attestation.buildPayload(nonce)
                try {
                    val dg = DroidGuardResultCreator.getResult(context, "attest", mapOf("contentBinding" to attestation.payloadHashBase64))
                    attestation.setDroidGaurdResult(Base64.encodeToString(dg, Base64.NO_WRAP + Base64.NO_PADDING + Base64.URL_SAFE))
                } catch (e: Exception) {
                    if (SafetyNetPrefs.get(context).isOfficial) throw e
                    Log.w(TAG, e)
                    null
                }
                val data = withContext(Dispatchers.IO) { AttestationData(attestation.attest(apiKey)) }
                callbacks.onAttestationData(Status.SUCCESS, data)
            } catch (e: IOException) {
                Log.w(TAG, e)
                callbacks.onAttestationData(Status.INTERNAL_ERROR, null)
            }
        }
    }

    override fun getSharedUuid(callbacks: ISafetyNetCallbacks) {
        PackageUtils.checkPackageUid(context, packageName, getCallingUid())
        PackageUtils.assertExtendedAccess(context)

        // TODO
        Log.d(TAG, "dummy Method: getSharedUuid")
        callbacks.onString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
    }

    override fun lookupUri(callbacks: ISafetyNetCallbacks, s1: String, threatTypes: IntArray, i: Int, s2: String) {
        Log.d(TAG, "unimplemented Method: lookupUri")
    }

    override fun init(callbacks: ISafetyNetCallbacks) {
        Log.d(TAG, "dummy Method: init")
        callbacks.onBoolean(Status.SUCCESS, true)
    }

    override fun getHarmfulAppsList(callbacks: ISafetyNetCallbacks) {
        Log.d(TAG, "dummy Method: unknown4")
        callbacks.onHarmfulAppsData(Status.SUCCESS, ArrayList())
    }

    override fun verifyWithRecaptcha(callbacks: ISafetyNetCallbacks, siteKey: String?) {
        if (siteKey == null) {
            callbacks.onAttestationData(Status(CommonStatusCodes.DEVELOPER_ERROR), null)
            return
        }
        if (!SafetyNetPrefs.get(context).isEnabled) {
            Log.d(TAG, "ignoring SafetyNet request, it's disabled")
            callbacks.onAttestationData(Status.CANCELED, null)
            return
        }
        val intent = Intent(context, ReCaptchaActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        val params = StringBuilder()
        params.appendUrlEncodedParam("k", siteKey)
                .appendUrlEncodedParam("di", LastCheckinInfo.read(context).androidId.toString())
                .appendUrlEncodedParam("pk", packageName)
                .appendUrlEncodedParam("sv", SDK_INT.toString())
                .appendUrlEncodedParam("gv", "20.47.14 (040306-{{cl}})")
                .appendUrlEncodedParam("gm", "260")
                .appendUrlEncodedParam("as", Base64.encodeToString(Attestation.getPackageFileDigest(context, packageName), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING))
        for (signature in Attestation.getPackageSignatures(context, packageName)) {
            params.appendUrlEncodedParam("ac", Base64.encodeToString(signature, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING))
        }
        params.appendUrlEncodedParam("ip", "com.android.vending")
                .appendUrlEncodedParam("av", false.toString())
                .appendUrlEncodedParam("si", null)
        intent.putExtra("params", params.toString())
        intent.putExtra("result", object : ResultReceiver(null) {
            override fun onReceiveResult(resultCode: Int, resultData: Bundle) {
                if (resultCode != 0) {
                    callbacks.onRecaptchaResult(Status(resultData.getInt("errorCode"), resultData.getString("error")), null)
                } else {
                    callbacks.onRecaptchaResult(Status.SUCCESS, RecaptchaResultData().apply { token = resultData.getString("token") })
                }
            }
        })
        context.startActivity(intent)
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
        if (super.onTransact(code, data, reply, flags)) return true
        Log.d(TAG, "onTransact [unknown]: $code, $data, $flags")
        return false
    }
}
