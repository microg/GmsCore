package org.microg.gms.constellation.core

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

internal interface IidCredentialProvider {
    fun getIidToken(projectNumber: String?): String
    fun getFid(): String
    fun signIidToken(iidToken: String): Pair<ByteArray, Long>
}

private class AuthManagerIidCredentialProvider(
    private val authManager: AuthManager
) : IidCredentialProvider {
    override fun getIidToken(projectNumber: String?): String =
        authManager.getIidToken(projectNumber)

    override fun getFid(): String = authManager.getFid()

    override fun signIidToken(iidToken: String): Pair<ByteArray, Long> =
        authManager.signIidTokenCompat(iidToken)
}

suspend fun handleGetIidToken(
    context: Context,
    callbacks: IConstellationCallbacks,
    request: GetIidTokenRequest
) = handleGetIidToken(
    callbacks,
    request,
    AuthManagerIidCredentialProvider(context.authManager)
)

internal suspend fun handleGetIidToken(
    callbacks: IConstellationCallbacks,
    request: GetIidTokenRequest,
    credentialProvider: IidCredentialProvider
) = withContext(Dispatchers.IO) {
    try {
        val iidToken = credentialProvider.getIidToken(request.projectNumber?.toString())
        require(iidToken.isNotEmpty()) { "Instance ID token is empty" }
        val fid = credentialProvider.getFid()
        val (signature, timestamp) = credentialProvider.signIidToken(iidToken)

        callbacks.onIidTokenGenerated(
            Status.SUCCESS,
            GetIidTokenResponse(iidToken, fid, signature, timestamp),
            ApiMetadata.DEFAULT
        )
    } catch (e: Exception) {
        Log.e(TAG, "getIidToken failed", e)
        callbacks.onIidTokenGenerated(Status.INTERNAL_ERROR, null, ApiMetadata.DEFAULT)
    }
}
