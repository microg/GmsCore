/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.recaptcha.modac.signals

import android.content.Context
import android.util.Log
import com.google.android.gms.droidguard.DroidGuardClient
import com.google.android.gms.tasks.await
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.microg.gms.recaptcha.qr.RecaptchaSignals

private const val TAG = "RecaptchaQr"
private const val DROID_GUARD_FLOW_NAME = "recaptchabase-modac"
private const val MAX_DROID_GUARD_TIMEOUT_MS = 5_000L

internal class DefaultSignalCollectionManager(context: Context) : SignalCollectionManager {
    private val context = context.applicationContext

    override suspend fun collectSignals(request: SignalCollectionRequest): RecaptchaSignals {
        val droidGuardNonce = request.cachedCredential?.droidGuardNonce?.utf8().orEmpty()
        val droidGuardEnabled = request.initializationResponse.droidGuardConfig?.payload?.size?.let { it > 0 } == true
        if (!droidGuardEnabled || droidGuardNonce.isEmpty()) {
            return SignalEnvelopeBuilder.build(action = request.action)
        }
        val result = try {
            withTimeout(request.timeoutMs.coerceAtMost(MAX_DROID_GUARD_TIMEOUT_MS)) {
                DroidGuardClient.getResults(
                    context,
                    DROID_GUARD_FLOW_NAME,
                    mapOf("token" to droidGuardNonce),
                ).await()
            }
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "DroidGuard signal collection timed out")
            null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "DroidGuard signal collection failed", e)
            null
        }
        return SignalEnvelopeBuilder.build(action = request.action, droidGuardResult = result)
    }
}
