/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.carrierauth

import android.content.Context
import android.telephony.TelephonyManager
import android.util.Log

class IccAuthenticator(context: Context) {
    private val telephonyManager =
        context.applicationContext.getSystemService(TelephonyManager::class.java)

    fun authenticate(
        subscriptionId: Int,
        appType: Int,
        authType: Int,
        challenge: String
    ): String? {
        if (subscriptionId < 0 || challenge.isEmpty()) {
            return null
        }

        return try {
            telephonyManager
                .createForSubscriptionId(subscriptionId)
                .getIccAuthentication(appType, authType, challenge)
        } catch (exception: SecurityException) {
            Log.w(TAG, "ICC authentication is not permitted", exception)
            null
        } catch (exception: IllegalArgumentException) {
            Log.w(TAG, "ICC authentication request is invalid", exception)
            null
        } catch (exception: UnsupportedOperationException) {
            Log.w(TAG, "ICC authentication is unsupported", exception)
            null
        } catch (exception: RuntimeException) {
            Log.w(TAG, "ICC authentication failed", exception)
            null
        }
    }

    companion object {
        private const val TAG = "GmsCarrierAuth"
    }
}