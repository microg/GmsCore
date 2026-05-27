/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth.credentials.provider

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CreateCredentialResponse
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.CreateCredentialUnknownException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.provider.PendingIntentHandler
import androidx.credentials.provider.ProviderCreateCredentialRequest
import androidx.credentials.provider.ProviderGetCredentialRequest

private const val TAG = "CredentialProviderActivity"

abstract class CredentialProviderActivity : AppCompatActivity() {
    var isCreateRequest = false
        private set
    var isGetRequest = false
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: intent: ${intent?.extras?.keySet()}")
        runCatching {
            val createRequest = PendingIntentHandler.retrieveProviderCreateCredentialRequest(intent)
            if (createRequest != null) {
                isCreateRequest = true
                return onProviderCreateCredentialRequest(createRequest)
            }
            val getRequest = PendingIntentHandler.retrieveProviderGetCredentialRequest(intent)
            if (getRequest != null) {
                isGetRequest = true
                return onProviderGetCredentialRequest(getRequest)
            }
            finishWithException("Unknown request")
        }.onFailure { e ->
            Log.e(TAG, "Error handling passkey", e)
            finishWithException(e.localizedMessage)
        }
    }

    // TODO: Turn these into suspendable and make sure that returning the correct result type becomes mandatory
    abstract fun onProviderCreateCredentialRequest(request: ProviderCreateCredentialRequest)
    abstract fun onProviderGetCredentialRequest(request: ProviderGetCredentialRequest)

    fun finishWithSuccess(response: CreateCredentialResponse) {
        if (!isCreateRequest) return finishWithException()
        setResult(
            RESULT_OK,
            Intent().also { PendingIntentHandler.setCreateCredentialResponse(it, response) }
        )
        finish()
    }

    fun finishWithSuccess(response: GetCredentialResponse) {
        if (!isGetRequest) return finishWithException()
        setResult(
            RESULT_OK,
            Intent().also { PendingIntentHandler.setGetCredentialResponse(it, response) }
        )
        finish()
    }

    fun finishWithException(
        message: String? = null,
        createExceptionCreator: (String?) -> CreateCredentialException = { CreateCredentialUnknownException(it) },
        getExceptionCreator: (String?) -> GetCredentialException = { GetCredentialUnknownException(it) }
    ) {
        when {
            isCreateRequest -> {
                setResult(
                    RESULT_OK,
                    Intent().also {
                        PendingIntentHandler.setCreateCredentialException(
                            it,
                            createExceptionCreator(message)
                        )
                    }
                )
            }

            isGetRequest -> {
                setResult(
                    RESULT_OK,
                    Intent().also { PendingIntentHandler.setGetCredentialException(it, getExceptionCreator(message)) }
                )
            }

            else -> {
                if (message != null) Log.w(TAG, message)
                setResult(RESULT_CANCELED)
            }
        }
        finish()
    }
}
