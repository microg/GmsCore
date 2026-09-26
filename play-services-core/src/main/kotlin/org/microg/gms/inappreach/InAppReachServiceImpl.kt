/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.inappreach

import android.accounts.AccountManager
import android.content.Context
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.R
import com.google.android.gms.common.api.ApiMetadata
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.api.internal.IStatusCallback
import com.google.android.gms.inappreach.internal.IInAppReachService
import com.google.android.gms.inappreach.internal.IOnAccountDataResponseListener
import com.google.android.gms.inappreach.internal.IOnAccountHealthAlertsListener
import com.google.android.gms.inappreach.internal.IOnAccountMessagesListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import org.microg.gms.auth.AuthConstants
import org.microg.gms.auth.AuthManager
import org.microg.gms.common.Constants
import org.microg.gms.inappreach.proto.AccountData
import org.microg.gms.inappreach.proto.AccountDataMetadata
import org.microg.gms.inappreach.proto.AccountDataPayload
import org.microg.gms.inappreach.proto.AccountDataResponse
import org.microg.gms.inappreach.proto.CallToAction
import org.microg.gms.inappreach.proto.CallToActionDetails
import org.microg.gms.inappreach.proto.CallToActionParameters
import org.microg.gms.inappreach.proto.StorageActions
import org.microg.gms.inappreach.proto.StorageCard
import org.microg.gms.inappreach.proto.StoragePayloadType
import org.microg.gms.inappreach.proto.StorageProgress
import org.microg.gms.utils.singleInstanceOf
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val TAG = "InAppReachService"
private const val DRIVE_SCOPE = "oauth2:https://www.googleapis.com/auth/drive.metadata.readonly"
private const val DRIVE_QUOTA_URL = "https://www.googleapis.com/drive/v3/about?fields=storageQuota"
private const val CALL_TO_ACTION_TYPE = "type.googleapis.com/google.internal.subscriptions.firstparty.v1.CallToAction"
private const val CALL_TO_ACTION_GET_STORAGE = 1
private const val CALL_TO_ACTION_CLEAN_UP_STORAGE = 3
private const val CALL_TO_ACTION_SOURCE = 465

class InAppReachServiceImpl(
    private val context: Context,
    private val packageName: String,
    override val lifecycle: Lifecycle
) : IInAppReachService.Stub(), LifecycleOwner {
    private var responseJob: Job? = null

    override fun registerAccountHealthAlerts(
        callback: IStatusCallback?,
        clientPackageName: String?,
        listener: IOnAccountHealthAlertsListener?,
        apiMetadata: ApiMetadata?
    ) = acknowledge(clientPackageName, callback)

    override fun unregisterAccountHealthAlerts(
        callback: IStatusCallback?,
        clientPackageName: String?,
        apiMetadata: ApiMetadata?
    ) = acknowledge(clientPackageName, callback)

    override fun registerAccountMessages(
        callback: IStatusCallback?,
        clientPackageName: String?,
        listener: IOnAccountMessagesListener?,
        apiMetadata: ApiMetadata?
    ) = acknowledge(clientPackageName, callback)

    override fun unregisterAccountMessages(
        callback: IStatusCallback?,
        clientPackageName: String?,
        apiMetadata: ApiMetadata?
    ) = acknowledge(clientPackageName, callback)

    override fun registerAccountDataResponse(
        callback: IStatusCallback?,
        clientPackageName: String?,
        listener: IOnAccountDataResponseListener?,
        apiMetadata: ApiMetadata?
    ) {
        require(clientPackageName == packageName) { "Package name mismatch" }
        responseJob?.cancel()
        responseJob = listener?.let {
            lifecycleScope.launch(Dispatchers.IO) {
                val response = buildAccountDataResponse(context)
                currentCoroutineContext().ensureActive()
                notifyAccountDataResponse(it, response)
            }
        }
        notifySuccess(callback)
    }

    override fun unregisterAccountDataResponse(
        callback: IStatusCallback?,
        clientPackageName: String?,
        apiMetadata: ApiMetadata?
    ) {
        unregister(clientPackageName, callback)
    }

    override fun registerAccountDataResponseV2(
        callback: IStatusCallback?,
        clientPackageName: String?,
        listener: IOnAccountDataResponseListener?,
        apiMetadata: ApiMetadata?
    ) {
        registerAccountDataResponse(callback, clientPackageName, listener, apiMetadata)
    }

    override fun unregisterAccountDataResponseV2(
        callback: IStatusCallback?,
        clientPackageName: String?,
        apiMetadata: ApiMetadata?
    ) {
        unregister(clientPackageName, callback)
    }

    private fun unregister(clientPackageName: String?, callback: IStatusCallback?) {
        require(clientPackageName == packageName) { "Package name mismatch" }
        responseJob?.cancel()
        responseJob = null
        notifySuccess(callback)
    }

    private fun acknowledge(clientPackageName: String?, callback: IStatusCallback?) {
        require(clientPackageName == packageName) { "Package name mismatch" }
        notifySuccess(callback)
    }
}

private suspend fun buildAccountDataResponse(context: Context): ByteArray {
    val accounts = linkedMapOf<String, AccountData>()
    for (account in AccountManager.get(context).getAccountsByType(AuthConstants.DEFAULT_ACCOUNT_TYPE)) {
        currentCoroutineContext().ensureActive()
        try {
            val (usage, limit) = getStorageQuota(context, account.name) ?: continue
            if (limit <= 0) continue
            accounts[account.name] = AccountData(
                accountName = account.name,
                payload = AccountDataPayload(
                    metadata = AccountDataMetadata(type = 1),
                    cards = listOf(storageCard(context, usage, limit))
                )
            )
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Unable to load storage quota", e)
        }
    }
    return AccountDataResponse.ADAPTER.encode(AccountDataResponse(accounts))
}

private suspend fun getStorageQuota(
    context: Context,
    accountName: String,
    queue: RequestQueue = singleInstanceOf { Volley.newRequestQueue(context.applicationContext) }
): Pair<Long, Long>? {
    val auth = AuthManager(context, accountName, Constants.GMS_PACKAGE_NAME, DRIVE_SCOPE).apply {
        isGmsApp = true
    }.requestAuth(false).auth ?: return null
    val response = suspendCancellableCoroutine<JSONObject> { continuation ->
        val request = object : JsonObjectRequest(
            DRIVE_QUOTA_URL,
            { if (continuation.isActive) continuation.resume(it) },
            { if (continuation.isActive) continuation.resumeWithException(it) }
        ) {
            override fun getHeaders(): MutableMap<String, String> = mutableMapOf(
                "Authorization" to "Bearer $auth"
            )
        }
        continuation.invokeOnCancellation { request.cancel() }
        queue.add(request)
    }
    val quota = response.getJSONObject("storageQuota")
    return quota.getLong("usage") to quota.getLong("limit")
}

private fun storageCard(context: Context, usage: Long, limit: Long): StorageCard {
    val percent = usage * 100 / limit
    val total = formatBytes(limit)
    return StorageCard(
        title = context.getString(R.string.in_app_reach_storage_usage, percent, total),
        type = 1,
        progress = StorageProgress(
            fraction = (usage.toFloat() / limit).coerceIn(0f, 1f),
            subtitle = "${formatBytes(usage)}/$total"
        ),
        actions = StorageActions(
            primary = callToAction(CALL_TO_ACTION_GET_STORAGE, context.getString(R.string.in_app_reach_get_storage)),
            secondary = callToAction(CALL_TO_ACTION_CLEAN_UP_STORAGE, context.getString(R.string.in_app_reach_clean_up_storage)),
            alignment = 0,
            arrangement = 1
        ),
        payloadType = StoragePayloadType(typeUrl = CALL_TO_ACTION_TYPE),
        style = 1,
        layout = 2,
        emphasis = 1,
        size = 2
    )
}

private fun callToAction(kind: Int, label: String) = CallToAction(
    typeUrl = CALL_TO_ACTION_TYPE,
    details = CallToActionDetails(
        parameters = CallToActionParameters(
            action = kind,
            source = CALL_TO_ACTION_SOURCE
        ),
        label = label,
        enabled = 1
    )
)

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024 * 1024 -> formatUnit(bytes, 1024L * 1024 * 1024, "GB")
    bytes >= 1024L * 1024 -> formatUnit(bytes, 1024L * 1024, "MB")
    else -> formatUnit(bytes, 1024L, "KB")
}

private fun formatUnit(bytes: Long, unit: Long, suffix: String): String {
    val value = bytes.toDouble() / unit
    return if (value == value.toLong().toDouble()) "${value.toLong()} $suffix"
    else String.format(Locale.US, "%.2f %s", value, suffix)
}

private fun notifySuccess(callback: IStatusCallback?) {
    try {
        callback?.onResult(Status.SUCCESS)
    } catch (e: Exception) {
        Log.w(TAG, "Unable to notify status callback", e)
    }
}

private fun notifyAccountDataResponse(listener: IOnAccountDataResponseListener, response: ByteArray) {
    try {
        listener.onAccountDataResponse(response)
    } catch (e: Exception) {
        Log.w(TAG, "Unable to notify account data response listener", e)
    }
}
