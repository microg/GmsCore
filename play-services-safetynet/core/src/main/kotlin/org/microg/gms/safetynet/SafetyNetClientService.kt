/*
 * SPDX-FileCopyrightText: 2021 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package org.microg.gms.safetynet

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Parcel
import android.os.ResultReceiver
import android.util.Base64
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import com.google.android.gms.safetynet.AttestationData
import com.google.android.gms.safetynet.RecaptchaResultData
import com.google.android.gms.safetynet.SafeBrowsingData
import com.google.android.gms.safetynet.SafetyNetStatusCodes
import com.google.android.gms.safetynet.internal.ISafetyNetCallbacks
import com.google.android.gms.safetynet.internal.ISafetyNetService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService
import org.microg.gms.common.PackageUtils
import org.microg.gms.droidguard.core.DroidGuardPreferences
import org.microg.gms.droidguard.core.DroidGuardResultCreator
import org.microg.gms.settings.SettingsContract
import org.microg.gms.settings.SettingsContract.CheckIn.getContentUri
import org.microg.gms.settings.SettingsContract.getSettings
import java.io.IOException
import java.net.URLEncoder
import java.util.*

private const val TAG = "GmsSafetyNet"
private const val DEFAULT_API_KEY = "AIzaSyDqVnJBjE5ymo--oBJt3On7HQx9xNm1RHA"

class SafetyNetClientService : BaseService(TAG, GmsService.SAFETY_NET_CLIENT) {
    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        callback.onPostInitComplete(0, SafetyNetClientServiceImpl(this, request.packageName, lifecycle), null)
    }
}

private fun StringBuilder.appendUrlEncodedParam(key: String, value: String?) = append("&")
    .append(URLEncoder.encode(key, "UTF-8"))
    .append("=")
    .append(value?.let { URLEncoder.encode(it, "UTF-8") } ?: "")

class SafetyNetClientServiceImpl(
    private val context: Context,
    private val packageName: String,
    private val lifecycle: Lifecycle
) : ISafetyNetService.Stub(), LifecycleOwner {
    override fun getLifecycle(): Lifecycle = lifecycle

    override fun attest(callbacks: ISafetyNetCallbacks, nonce: ByteArray) {
        attestWithApiKey(callbacks, nonce, DEFAULT_API_KEY)
    }

    override fun attestWithApiKey(callbacks: ISafetyNetCallbacks, nonce: ByteArray?, apiKey: String) {
        if (nonce == null) {
            callbacks.onAttestationData(Status(SafetyNetStatusCodes.DEVELOPER_ERROR, "Nonce missing"), null)
            return
        }

        if (!SafetyNetPreferences.isEnabled(context)) {
            Log.d(TAG, "ignoring SafetyNet request, SafetyNet is disabled")
            callbacks.onAttestationData(Status(SafetyNetStatusCodes.ERROR, "Disabled"), null)
            return
        }

        if (!DroidGuardPreferences.isEnabled(context)) {
            Log.d(TAG, "ignoring SafetyNet request, DroidGuard is disabled")
            callbacks.onAttestationData(Status(SafetyNetStatusCodes.ERROR, "Disabled"), null)
            return
        }

        lifecycleScope.launchWhenStarted {
            val db = SafetyNetDatabase(context)
            var requestID: Long = -1
            try {
                val attestation = Attestation(context, packageName)
                val safetyNetData = attestation.buildPayload(nonce)

                requestID = db.insertRecentRequestStart(
                    SafetyNetRequestType.ATTESTATION,
                    safetyNetData.packageName,
                    safetyNetData.nonce?.toByteArray(),
                    safetyNetData.currentTimeMs ?: 0
                )

                val data = mapOf("contentBinding" to attestation.payloadHashBase64)
                val dg = withContext(Dispatchers.IO) { DroidGuardResultCreator.getResults(context, "attest", data) }
                attestation.setDroidGuardResult(dg)
                val jwsResult = withContext(Dispatchers.IO) { attestation.attest(apiKey) }


                val jsonData = try {
                    requireNotNull(jwsResult)
                    jwsResult.split(".").let {
                        assert(it.size == 3)
                        return@let Base64.decode(it[1], Base64.URL_SAFE).decodeToString()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.w(TAG, "An exception occurred when parsing the JWS token.")
                    null
                }

                db.insertRecentRequestEnd(requestID, Status.SUCCESS, jsonData)
                callbacks.onAttestationData(Status.SUCCESS, AttestationData(jwsResult))
            } catch (e: Exception) {
                Log.w(TAG, "Exception during attest: ${e.javaClass.name}", e)
                val code = when (e) {
                    is IOException -> SafetyNetStatusCodes.NETWORK_ERROR
                    else -> SafetyNetStatusCodes.ERROR
                }
                val status = Status(code, e.localizedMessage)

                // This shouldn't happen, but do not update the database if it didn't insert the start of the request
                if (requestID != -1L) db.insertRecentRequestEnd(requestID, status, null)
                try {
                    callbacks.onAttestationData(Status(code, e.localizedMessage), null)
                } catch (e: Exception) {
                    Log.w(TAG, "Exception while sending error", e)
                }
            }
            db.close()
        }
    }

    override fun getSharedUuid(callbacks: ISafetyNetCallbacks) {
        PackageUtils.checkPackageUid(context, packageName, getCallingUid())
        PackageUtils.assertExtendedAccess(context)

        // TODO
        Log.d(TAG, "dummy Method: getSharedUuid")
        callbacks.onString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
    }

    override fun lookupUri(callbacks: ISafetyNetCallbacks, apiKey: String, threatTypes: IntArray, i: Int, s2: String) {
        Log.d(TAG, "unimplemented Method: lookupUri")
        callbacks.onSafeBrowsingData(Status.SUCCESS, SafeBrowsingData())
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
            callbacks.onAttestationData(Status(SafetyNetStatusCodes.RECAPTCHA_INVALID_SITEKEY, "SiteKey missing"), null)
            return
        }

        if (!SafetyNetPreferences.isEnabled(context)) {
            Log.d(TAG, "ignoring SafetyNet request, SafetyNet is disabled")
            callbacks.onRecaptchaResult(Status(SafetyNetStatusCodes.ERROR, "Disabled"), null)
            return
        }

        if (!DroidGuardPreferences.isEnabled(context)) {
            Log.d(TAG, "ignoring SafetyNet request, DroidGuard is disabled")
            callbacks.onRecaptchaResult(Status(SafetyNetStatusCodes.ERROR, "Disabled"), null)
            return
        }

        val db = SafetyNetDatabase(context)
        val requestID = db.insertRecentRequestStart(
            SafetyNetRequestType.RECAPTCHA,
            context.packageName,
            null,
            System.currentTimeMillis()
        )

        val intent = Intent("org.microg.gms.safetynet.RECAPTCHA_ACTIVITY")
        intent.`package` = context.packageName
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        val androidId = getSettings(
            context,
            getContentUri(context),
            arrayOf(SettingsContract.CheckIn.ANDROID_ID)
        ) { cursor: Cursor -> cursor.getLong(0) }
        val params = StringBuilder()

        val (packageFileDigest, packageSignatures) = try {
            Pair(
                Base64.encodeToString(
                    Attestation.getPackageFileDigest(context, packageName),
                    Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
                ),
                Attestation.getPackageSignatures(context, packageName)
                    .map { Base64.encodeToString(it, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING) }
            )
        } catch (e: Exception) {
            db.insertRecentRequestEnd(requestID, Status(SafetyNetStatusCodes.ERROR, e.localizedMessage), null)
            db.close()
            callbacks.onRecaptchaResult(Status(SafetyNetStatusCodes.ERROR, e.localizedMessage), null)
            return
        }

        params.appendUrlEncodedParam("k", siteKey)
            .appendUrlEncodedParam("di", androidId.toString())
            .appendUrlEncodedParam("pk", packageName)
            .appendUrlEncodedParam("sv", SDK_INT.toString())
            .appendUrlEncodedParam("gv", "20.47.14 (040306-{{cl}})")
            .appendUrlEncodedParam("gm", "260")
            .appendUrlEncodedParam("as", packageFileDigest)
        for (signature in packageSignatures) {
            Log.d(TAG, "Sig: $signature")
            params.appendUrlEncodedParam("ac", signature)
        }
        params.appendUrlEncodedParam("ip", "com.android.vending")
            .appendUrlEncodedParam("av", false.toString())
            .appendUrlEncodedParam("si", null)
        intent.putExtra("params", params.toString())
        intent.putExtra("result", object : ResultReceiver(null) {
            override fun onReceiveResult(resultCode: Int, resultData: Bundle) {
                if (resultCode != 0) {
                    db.insertRecentRequestEnd(
                        requestID,
                        Status(resultData.getInt("errorCode"), resultData.getString("error")),
                        null
                    )
                    db.close()
                    callbacks.onRecaptchaResult(
                        Status(resultData.getInt("errorCode"), resultData.getString("error")),
                        null
                    )
                } else {
                    db.insertRecentRequestEnd(requestID, Status.SUCCESS, resultData.getString("token"))
                    db.close()
                    callbacks.onRecaptchaResult(
                        Status.SUCCESS,
                        RecaptchaResultData().apply { token = resultData.getString("token") })
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
