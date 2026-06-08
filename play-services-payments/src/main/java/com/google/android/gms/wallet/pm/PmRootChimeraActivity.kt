/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.wallet.pm

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.wallet.ACTION_BENDER3
import com.google.android.gms.wallet.EXTRA_BENDER3_BUYFLOW_CONFIG
import com.google.android.gms.wallet.EXTRA_BENDER3_ENCRYPTED_PARAMS
import com.google.android.gms.wallet.EXTRA_BENDER3_O2_ACTION_TOKEN
import com.google.android.gms.wallet.EXTRA_BENDER3_UNENCRYPTED_PARAMS
import com.google.android.gms.wallet.activity.WidgetResultIntentBuilder
import com.google.android.gms.wallet.bender3.Bender3RedirectExtras
import com.google.android.gms.wallet.firstparty.pm.SecurePaymentsPayload
import com.google.android.gms.wallet.shared.BuyFlowConfig
import com.google.android.wallet.bender3.framework.client.ParcelableKeyValue
import org.microg.vending.billing.proto.PaymentManagerConfig
import java.util.UUID

class PmRootChimeraActivity : ComponentActivity() {

    companion object {
        private const val TAG = "PmRootChimera"

        private const val EXTRA_SECURE_PAYLOAD = "com.google.android.gms.wallet.firstparty.SECURE_PAYMENTS_PAYLOAD"
        private const val EXTRA_PARAMS = "com.google.android.gms.wallet.firstparty.EXTRA_PARAMS"
        private const val EXTRA_UNENCRYPTED_PARAMS = "com.google.android.gms.wallet.firstparty.EXTRA_UNENCRYPTED_PARAMS"
        private const val EXTRA_AUTH_TOKEN = "com.google.android.gms.wallet.firstparty.EXTRA_AUTH_TOKEN"
        private const val EXTRA_BUILD_TIME = "com.google.android.gms.wallet.intentBuildTimeMs"
        private const val EXTRA_SUPPORTS_PROTO = "com.google.android.gms.wallet.firstparty.SUPPORTS_SECURE_PAYMENTS_PAYLOAD_PROTO"
    }

    private val bender3Launcher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        setResult(result.resultCode, result.data)
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onBackPressedDispatcher.addCallback(this) {
            setResult(RESULT_CANCELED)
            finish()
        }

        if (savedInstanceState != null) return

        val params = resolveParamsFromIntent() ?: run {
            Log.e(TAG, "No o2ActionToken, encryptedParams, or unencryptedParams — full PM UI not supported")
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        bender3Launcher.launch(buildBender3Intent(params))
    }

    private data class PmParams(
        val buyFlowConfig: BuyFlowConfig?,
        val o2ActionToken: ByteArray?,
        val encryptedParams: ByteArray?,
        val unencryptedParams: ByteArray?,
        val secureDataList: List<ParcelableKeyValue>?,
    )

    private fun resolveParamsFromIntent(): PmParams? {
        val buyFlowConfig = intent.getParcelableExtra<BuyFlowConfig>("com.google.android.gms.wallet.buyFlowConfig")
        buyFlowConfig?.applicationParameters?.buyerAccount = intent.getParcelableExtra("com.google.android.gms.wallet.account")

        val securePaymentsPayload = intent.getParcelableExtra<SecurePaymentsPayload>(EXTRA_SECURE_PAYLOAD)

        val secureDataList = securePaymentsPayload?.securePayments?.map {
            ParcelableKeyValue(it.key, it.value)
        }

        val o2ActionToken = securePaymentsPayload?.securePayload?.let { decodeO2ActionToken(it) }
        val encryptedParams = intent.getByteArrayExtra(EXTRA_PARAMS)
        val unencryptedParams = intent.getByteArrayExtra(EXTRA_UNENCRYPTED_PARAMS)

        if (o2ActionToken == null && encryptedParams == null && unencryptedParams == null) return null

        return PmParams(buyFlowConfig, o2ActionToken, encryptedParams, unencryptedParams, secureDataList)
    }

    private fun decodeO2ActionToken(payload: ByteArray): ByteArray? {
        return try {
            PaymentManagerConfig.ADAPTER.decode(payload).o2ActionToken?.toByteArray()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to decode PaymentManagerConfig", e)
            null
        }
    }

    private fun buildBender3Intent(params: PmParams): Intent {
        return Intent(ACTION_BENDER3).apply {
            putExtra("widgetType", WidgetResultIntentBuilder.WIDGET_TYPE_SECURE_PAYMENTS)
            putExtra(EXTRA_BENDER3_BUYFLOW_CONFIG, params.buyFlowConfig)
            putExtra(EXTRA_BUILD_TIME, intent.getLongExtra(EXTRA_BUILD_TIME, 0))

            params.encryptedParams?.let { putExtra(EXTRA_BENDER3_ENCRYPTED_PARAMS, it) }
            params.unencryptedParams?.let { putExtra(EXTRA_BENDER3_UNENCRYPTED_PARAMS, it) }
            params.o2ActionToken?.let { putExtra(EXTRA_BENDER3_O2_ACTION_TOKEN, it) }
            intent.getByteArrayExtra(EXTRA_AUTH_TOKEN)?.let { putExtra("productAuthToken", it) }

            if (!params.secureDataList.isNullOrEmpty()) {
                putParcelableArrayListExtra("secureDataArray", ArrayList(params.secureDataList))
            }

            putExtra(EXTRA_SUPPORTS_PROTO, intent.getBooleanExtra(EXTRA_SUPPORTS_PROTO, false))
            putExtra("bender3RedirectExtras", Bender3RedirectExtras(UUID.randomUUID().toString(), -1, -1))
        }
    }
}
