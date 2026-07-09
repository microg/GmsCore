/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.wallet.activity

import android.content.Intent
import android.util.Base64
import org.microg.gms.wallet.firstparty.ExitResult
import org.microg.gms.common.Constants
import org.microg.vending.billing.proto.FinishActionParams

class WidgetResultIntentBuilder(private val callingPackage: String? = null) {
    companion object {
        private const val TAG = "WidgetResultIntent"

        const val WIDGET_TYPE_SECURE_PAYMENTS = 3

        private const val EXTRA_PREFIX = "com.google.android.gms.wallet.firstparty."
        private const val EXTRA_INTEGRATOR_CALLBACK_DATA_TOKEN = EXTRA_PREFIX + "EXTRA_INTEGRATOR_CALLBACK_DATA_TOKEN"
        private const val EXTRA_ORDER_ID = EXTRA_PREFIX + "EXTRA_ORDER_ID"
        private const val EXTRA_CLIENT_CALLBACK_DATA_TOKEN = EXTRA_PREFIX + "EXTRA_CLIENT_CALLBACK_DATA_TOKEN"
        private const val EXTRA_SERVER_ANALYTICS_TOKEN = EXTRA_PREFIX + "EXTRA_SERVER_ANALYTICS_TOKEN"
        private const val EXTRA_ANALYTICS_PROTO = EXTRA_PREFIX + "EXTRA_ANALYTICS_PROTO"
        private const val EXTRA_INTERNAL_CLIENT_CALLBACK_DATA = EXTRA_PREFIX + "EXTRA_INTERNAL_CLIENT_CALLBACK_DATA"
    }

    var serverAnalyticsToken: ByteArray = ByteArray(0)

    fun buildSuccessIntent(finishParams: FinishActionParams): Intent {
        val intent = buildBaseIntent(finishParams)

        val integratorData = finishParams.integratorCallbackData
        val tokenBytes = integratorData?.primaryData?.toByteArray()
            ?: integratorData?.secondaryData?.toByteArray()
            ?: ByteArray(0)
        if (tokenBytes.isNotEmpty()) {
            intent.putExtra(EXTRA_INTEGRATOR_CALLBACK_DATA_TOKEN, tokenBytes)
            if (callingPackage != Constants.GMS_PACKAGE_NAME) {
                intent.putExtra(EXTRA_ORDER_ID, Base64.encodeToString(tokenBytes, Base64.NO_WRAP))
            }
        }

        val clientBytes = extractClientCallbackData(finishParams)
        if (clientBytes.isNotEmpty()) {
            intent.putExtra(EXTRA_CLIENT_CALLBACK_DATA_TOKEN, clientBytes)
        }

        intent.putExtra(EXTRA_SERVER_ANALYTICS_TOKEN, serverAnalyticsToken)
        return intent
    }

    /**
     * CANCEL → setResult(RESULT_CANCELED)
     */
    fun buildCancelIntent(finishParams: FinishActionParams? = null): Intent {
        val intent = buildBaseIntent(finishParams)

        if (finishParams != null) {
            val clientBytes = extractClientCallbackData(finishParams)
            if (clientBytes.isNotEmpty()) {
                intent.putExtra(EXTRA_CLIENT_CALLBACK_DATA_TOKEN, clientBytes)
            }
        }

        return intent
    }

    /**
     * ERROR → setResult(1)
     */
    fun buildErrorIntent(finishParams: FinishActionParams? = null): Intent {
        val intent = buildBaseIntent(finishParams)

        if (finishParams != null) {
            val clientBytes = extractClientCallbackData(finishParams)
            if (clientBytes.isNotEmpty()) {
                intent.putExtra(EXTRA_CLIENT_CALLBACK_DATA_TOKEN, clientBytes)
            }

            val apiErrorData = finishParams.apiErrorData
            if (apiErrorData?.debugMessage != null) {
                val exitResult = ExitResult()
                exitResult.paymentsExitCode = 404
                exitResult.debugMessage = apiErrorData.debugMessage
                exitResult.apiErrorReason = apiErrorData.apiErrorReason ?: 0
                exitResult.writeToIntent(intent)
            }
        }
        return intent
    }

    fun buildBaseIntent(finishParams: FinishActionParams? = null): Intent {
        val intent = Intent()

        intent.putExtra(EXTRA_ANALYTICS_PROTO, ByteArray(0))

        finishParams?.additionalData?.let { ad ->
            intent.putExtra(EXTRA_INTERNAL_CLIENT_CALLBACK_DATA, ad.encode())
        }

        return intent
    }

    private fun extractClientCallbackData(finishParams: FinishActionParams): ByteArray {
        return finishParams.clientCallbackData?.dataBytes?.toByteArray() ?: ByteArray(0)
    }
}
