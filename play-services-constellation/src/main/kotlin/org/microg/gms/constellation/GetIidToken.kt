package org.microg.gms.constellation

import android.content.Context
import android.util.Log
import com.google.android.gms.common.api.ApiMetadata
import com.google.android.gms.common.api.Status
import com.google.android.gms.constellation.GetIidTokenRequest
import com.google.android.gms.constellation.GetIidTokenResponse
import com.google.android.gms.constellation.internal.IConstellationCallbacks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "GetIidToken"

suspend fun handleGetIidToken(
    context: Context,
    callbacks: IConstellationCallbacks,
    request: GetIidTokenRequest
) = withContext(Dispatchers.IO) {
    try {
        val authManager = AuthManager.get(context)
        val iidToken = authManager.getIidToken(request.projectNumber?.toString())
        val fid = authManager.getFid()
        val (signature, timestamp) = authManager.signIidToken(iidToken)

        callbacks.onIidTokenGenerated(
            Status.SUCCESS,
            GetIidTokenResponse(iidToken, fid, signature, timestamp.toEpochMilli()),
            ApiMetadata.DEFAULT
        )
    } catch (e: Exception) {
        Log.e(TAG, "getIidToken failed", e)
        callbacks.onIidTokenGenerated(Status.INTERNAL_ERROR, null, ApiMetadata.DEFAULT)
    }
}
