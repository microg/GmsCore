/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.asterism.core

import android.content.Context
import android.os.RemoteException
import android.util.Log
import com.google.android.gms.asterism.GetAsterismConsentResponse
import com.google.android.gms.asterism.SetAsterismConsentRequest
import com.google.android.gms.asterism.SetAsterismConsentResponse
import com.google.android.gms.asterism.internal.IAsterismApiService
import com.google.android.gms.asterism.internal.IAsterismCallbacks
import org.microg.gms.common.PackageUtils
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Core Asterism API service implementation.
 * Bridges AIDL interface to local consent store and manages
 * callback registration for consent state change notifications.
 */
class AsterismApiService(private val context: Context) : IAsterismApiService.Stub() {

    companion object {
        private const val TAG = "AsterismApiService"
    }

    private val consentStore = AsterismConsentStore(context)
    private val callbacks = CopyOnWriteArrayList<IAsterismCallbacks>()

    override fun getAsterismConsent(request: com.google.android.gms.asterism.GetAsterismConsentRequest?): GetAsterismConsentResponse {
        if (request == null) {
            Log.w(TAG, "getAsterismConsent called with null request")
            return GetAsterismConsentResponse(
                GetAsterismConsentResponse.CONSENT_UNKNOWN, 0L, 0L, null,
                SetAsterismConsentResponse.RESULT_ERROR_INVALID_ARGUMENT, "Request is null"
            )
        }

        val callingPackage = request.packageName ?: PackageUtils.getCallingPackage(context)
        Log.d(TAG, "getAsterismConsent from package=$callingPackage, requestId=${request.requestId}")

        return try {
            consentStore.getCurrentState()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting consent state", e)
            GetAsterismConsentResponse(
                GetAsterismConsentResponse.CONSENT_UNKNOWN, 0L, 0L, null,
                SetAsterismConsentResponse.RESULT_ERROR_UNKNOWN, e.message
            )
        }
    }

    override fun setAsterismConsent(request: com.google.android.gms.asterism.SetAsterismConsentRequest?): SetAsterismConsentResponse {
        if (request == null) {
            Log.w(TAG, "setAsterismConsent called with null request")
            return SetAsterismConsentResponse(
                SetAsterismConsentResponse.RESULT_ERROR_INVALID_ARGUMENT,
                GetAsterismConsentResponse.CONSENT_UNKNOWN,
                "Request is null", null, 0L
            )
        }

        val callingPackage = request.callingPackage ?: PackageUtils.getCallingPackage(context)
        Log.d(TAG, "setAsterismConsent action=${request.action} from package=$callingPackage")

        return try {
            val response = when (request.action) {
                SetAsterismConsentRequest.ACTION_GRANT -> {
                    if (request.requiresDeviceIntegrity) {
                        Log.i(TAG, "Device integrity check requested but not enforced in microG")
                    }
                    val consent = consentStore.grantConsent(null, request.ttlMillis)
                    SetAsterismConsentResponse(
                        SetAsterismConsentResponse.RESULT_OK,
                        consent.consentState, null,
                        consent.consentToken, consent.expiryTimestamp
                    )
                }
                SetAsterismConsentRequest.ACTION_REVOKE -> {
                    val consent = consentStore.revokeConsent()
                    SetAsterismConsentResponse(
                        SetAsterismConsentResponse.RESULT_OK,
                        consent.consentState, null, null, 0L
                    )
                }
                SetAsterismConsentRequest.ACTION_REFRESH -> {
                    val consent = consentStore.refreshConsent(request.ttlMillis)
                    SetAsterismConsentResponse(
                        SetAsterismConsentResponse.RESULT_OK,
                        consent.consentState, null,
                        consent.consentToken, consent.expiryTimestamp
                    )
                }
                SetAsterismConsentRequest.ACTION_CHECK_STATUS -> {
                    val consent = consentStore.getCurrentState()
                    SetAsterismConsentResponse(
                        if (consent.isConsentGranted && !consent.isExpired) SetAsterismConsentResponse.RESULT_OK
                        else SetAsterismConsentResponse.RESULT_ERROR_CONSENT_EXPIRED,
                        consent.consentState,
                        if (consent.isExpired) "Consent has expired" else null,
                        consent.consentToken, consent.expiryTimestamp
                    )
                }
                else -> {
                    SetAsterismConsentResponse(
                        SetAsterismConsentResponse.RESULT_ERROR_INVALID_ARGUMENT,
                        GetAsterismConsentResponse.CONSENT_UNKNOWN,
                        "Unknown action: ${request.action}", null, 0L
                    )
                }
            }

            notifyCallbacks(response.consentState)
            response
        } catch (e: Exception) {
            Log.e(TAG, "Error setting consent state", e)
            SetAsterismConsentResponse(
                SetAsterismConsentResponse.RESULT_ERROR_UNKNOWN,
                GetAsterismConsentResponse.CONSENT_UNKNOWN,
                e.message, null, 0L
            )
        }
    }

    override fun registerCallbacks(callbacks: IAsterismCallbacks?) {
        if (callbacks != null && !this.callbacks.contains(callbacks)) {
            this.callbacks.add(callbacks)
            Log.d(TAG, "Registered callbacks, total: ${this.callbacks.size}")
        }
    }

    override fun unregisterCallbacks(callbacks: IAsterismCallbacks?) {
        if (callbacks != null) {
            this.callbacks.remove(callbacks)
            Log.d(TAG, "Unregistered callbacks, total: ${this.callbacks.size}")
        }
    }

    private fun notifyCallbacks(consentState: Int) {
        val timestamp = System.currentTimeMillis()
        for (cb in callbacks) {
            try {
                cb.onConsentStateChanged(consentState, timestamp)
            } catch (e: RemoteException) {
                Log.w(TAG, "Failed to notify callback, removing", e)
                callbacks.remove(cb)
            }
        }
    }

    fun getConsentStore(): AsterismConsentStore = consentStore
}
