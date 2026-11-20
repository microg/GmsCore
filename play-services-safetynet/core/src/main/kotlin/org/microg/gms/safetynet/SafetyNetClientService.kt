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
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import com.google.android.gms.safetynet.HarmfulAppsInfo
import com.google.android.gms.safetynet.RecaptchaResultData
import com.google.android.gms.safetynet.SafeBrowsingData
import com.google.android.gms.safetynet.SafetyNetStatusCodes
import com.google.android.gms.safetynet.internal.ISafetyNetCallbacks
import com.google.android.gms.safetynet.internal.ISafetyNetService
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService
import org.microg.gms.common.GooglePackagePermission
import org.microg.gms.common.PackageUtils
import org.microg.gms.settings.SettingsContract
import org.microg.gms.settings.SettingsContract.CheckIn.getContentUri
import org.microg.gms.settings.SettingsContract.getSettings
import org.microg.gms.utils.digest
import org.microg.gms.utils.getCertificates
import org.microg.gms.utils.toBase64
import org.microg.gms.utils.warnOnTransactionIssues
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.net.URLEncoder
import java.security.MessageDigest

private const val TAG = "GmsSafetyNet"
private const val DEFAULT_API_KEY = "AIzaSyDqVnJBjE5ymo--oBJt3On7HQx9xNm1RHA"

class SafetyNetClientService : BaseService(TAG, GmsService.SAFETY_NET) {
    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        PackageUtils.getAndCheckCallingPackage(this, request.packageName)
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
    override val lifecycle: Lifecycle
) : ISafetyNetService.Stub(), LifecycleOwner {

    override fun attest(callbacks: ISafetyNetCallbacks, nonce: ByteArray) {
        attestWithApiKey(callbacks, nonce, DEFAULT_API_KEY)
    }

    override fun attestWithApiKey(callbacks: ISafetyNetCallbacks, nonce: ByteArray?, apiKey: String) {
        runCatching {
            callbacks.onAttestationResult(Status(SafetyNetStatusCodes.API_NOT_CONNECTED, "The SafetyNet Attestation API is deprecated and no longer functional."), null)
        }
    }

    override fun getSharedUuid(callbacks: ISafetyNetCallbacks) {
        PackageUtils.checkPackageUid(context, packageName, getCallingUid())
        PackageUtils.assertGooglePackagePermission(context, GooglePackagePermission.SAFETYNET)

        // TODO
        Log.d(TAG, "dummy Method: getSharedUuid")
        callbacks.onSharedUuid("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
    }

    override fun lookupUri(callbacks: ISafetyNetCallbacks, apiKey: String, threatTypes: IntArray, i: Int, s2: String) {
        Log.d(TAG, "unimplemented Method: lookupUri")
        callbacks.onSafeBrowsingData(Status.SUCCESS, SafeBrowsingData())
    }

    override fun enableVerifyApps(callbacks: ISafetyNetCallbacks) {
        Log.d(TAG, "dummy Method: enableVerifyApps")
        callbacks.onVerifyAppsUserResult(Status.SUCCESS, true)
    }

    override fun listHarmfulApps(callbacks: ISafetyNetCallbacks) {
        Log.d(TAG, "dummy Method: listHarmfulApps")
        callbacks.onHarmfulAppsInfo(Status.SUCCESS, HarmfulAppsInfo().apply {
            lastScanTime = ((System.currentTimeMillis() - VERIFY_APPS_LAST_SCAN_DELAY) / VERIFY_APPS_LAST_SCAN_TIME_ROUNDING) * VERIFY_APPS_LAST_SCAN_TIME_ROUNDING + VERIFY_APPS_LAST_SCAN_OFFSET
        })
    }

    private fun InputStream.digest(algorithm: String): ByteArray {
        val digest = MessageDigest.getInstance(algorithm)
        val data = ByteArray(4096)
        while (true) {
            val read = read(data)
            if (read < 0) break
            digest.update(data, 0, read)
        }
        return digest.digest()
    }

    private fun File.digest(algorithm: String): ByteArray {
        return FileInputStream(this).use { it.digest(algorithm) }
    }

    override fun verifyWithRecaptcha(callbacks: ISafetyNetCallbacks, siteKey: String?) {
        if (siteKey == null) {
            callbacks.onRecaptchaResult(Status(SafetyNetStatusCodes.RECAPTCHA_INVALID_SITEKEY, "SiteKey missing"), null)
            return
        }

        if (!SafetyNetPreferences.isEnabled(context)) {
            Log.d(TAG, "ignoring SafetyNet request, SafetyNet is disabled")
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
                    File(context.packageManager.getApplicationInfo(packageName, 0).sourceDir).digest("SHA-256"),
                    Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
                ),
                context.packageManager.getCertificates(packageName)
                    .map { it.digest("SHA-256").toBase64(Base64.URL_SAFE, Base64.NO_WRAP, Base64.NO_PADDING) }
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

    override fun initSafeBrowsing(callbacks: ISafetyNetCallbacks) {
        Log.d(TAG, "dummy: initSafeBrowsing")
        callbacks.onInitSafeBrowsingResult(Status.SUCCESS)
    }

    override fun shutdownSafeBrowsing() {
        Log.d(TAG, "dummy: shutdownSafeBrowsing")
    }

    override fun isVerifyAppsEnabled(callbacks: ISafetyNetCallbacks) {
        Log.d(TAG, "dummy: isVerifyAppsEnabled")
        callbacks.onVerifyAppsUserResult(Status.SUCCESS, true)
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean = warnOnTransactionIssues(code, reply, flags, TAG) { super.onTransact(code, data, reply, flags) }

    companion object {
        // We simulate one scan every day, which will happen at 03:12:02.121 and will be available 32 seconds later
        const val VERIFY_APPS_LAST_SCAN_DELAY = 32 * 1000L
        const val VERIFY_APPS_LAST_SCAN_OFFSET = ((3 * 60 + 12) * 60 + 2) * 1000L + 121
        const val VERIFY_APPS_LAST_SCAN_TIME_ROUNDING = 24 * 60 * 60 * 1000L
    }
}
