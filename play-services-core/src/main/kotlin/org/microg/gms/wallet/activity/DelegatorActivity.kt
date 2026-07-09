/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.wallet.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.wallet.shared.BuyFlowConfig
import org.microg.gms.wallet.activity.PaymentState.*
import kotlinx.coroutines.launch
import org.microg.gms.wallet.ACTION_BENDER3
import org.microg.gms.wallet.EXTRA_BENDER3_BUYFLOW_CONFIG
import org.microg.gms.wallet.EXTRA_BENDER3_ENCRYPTED_PARAMS
import org.microg.gms.wallet.EXTRA_BENDER3_O2_ACTION_TOKEN
import org.microg.gms.wallet.EXTRA_BENDER3_UNENCRYPTED_PARAMS
import org.microg.vending.billing.proto.FinishActionParams
import org.microg.vending.billing.proto.IapResponseWrapper
import org.microg.vending.billing.proto.ResultCode

/**
 * Bender3 Payment Activity — handles the ACTION_BENDER3 Intent and carries the IAP 3DS2 verification flow.
 */
class DelegatorActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "GenericDelegatorX"
    }
    
    private lateinit var paymentContext: PaymentContext
    private lateinit var paymentController: PaymentController
    private lateinit var resultBuilder: WidgetResultIntentBuilder
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val action = requireNotNull(intent.action) { "Intent action must not be null" }
        if (action != ACTION_BENDER3) {
            throw SecurityException("Unsupported intent action: $action")
        }

        paymentContext = PaymentContext(this)
        paymentController = PaymentController(paymentContext)
        resultBuilder = WidgetResultIntentBuilder(callingPackage = callingPackage)

        onBackPressedDispatcher.addCallback(this) {
            Log.d(TAG, "back pressed — triggering CANCEL path")
            paymentContext.updateState(Cancelled)
            isEnabled = false
            onBackPressedDispatcher.onBackPressed()
        }

        handleIntent()
    }

    private fun handleIntent() {
        val o2ActionToken = intent.getByteArrayExtra(EXTRA_BENDER3_O2_ACTION_TOKEN)
        val encryptedParams = intent.getByteArrayExtra(EXTRA_BENDER3_ENCRYPTED_PARAMS)
        val unencryptedParams = intent.getByteArrayExtra(EXTRA_BENDER3_UNENCRYPTED_PARAMS)

        Log.d(TAG, "o2ActionToken: ${o2ActionToken?.size ?: "null"} bytes")

        if (o2ActionToken != null) {
            val encryptedData = try {
                val wrapper = IapResponseWrapper.ADAPTER.decode(o2ActionToken)
                wrapper.encryptedData?.toByteArray()
            } catch (e: Exception) {
                Log.w(TAG, "unable to decode o2ActionToken", e)
                null
            }

            if (encryptedData == null) {
                Log.e(TAG, "Unable to initialize bender3 widget with token")
                setResult(RESULT_FIRST_USER, Intent())
                finish()
                return
            }

            lifecycleScope.launch {
                startPaymentFlow(encryptedData, unencryptedParams)
            }
            return
        }

        if (encryptedParams == null && unencryptedParams == null) {
            Log.w(TAG, "unable to initialize widget: both encryptedParams and unencryptedParams are null")
            setResult(RESULT_FIRST_USER, Intent())
            finish()
            return
        }

        lifecycleScope.launch {
            startPaymentFlow(encryptedParams, unencryptedParams)
        }
    }
    
    private fun startPaymentFlow(encryptedParams: ByteArray?, unencryptedParams: ByteArray?) {
        val buyFlowConfig = intent.getParcelableExtra<BuyFlowConfig>(EXTRA_BENDER3_BUYFLOW_CONFIG)
        if (buyFlowConfig == null) {
            Log.w(TAG, "BuyFlowConfig is null, returning ERROR")
            setResult(RESULT_FIRST_USER, Intent())
            finish()
            return
        }

        setContent {
            val paymentState by paymentContext.paymentState.collectAsState()
            
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                    when (val state = paymentState) {
                        is Idle, is Initializing,
                        is Initialized, is Submitted, is Submitting -> {
                            val (condManaged, condVisible) = paymentContext.getComponentVisibility()
                            val inputTextState by paymentContext.inputTextStateFlow.collectAsState()
                            PaymentsScreen(
                                pageElements = paymentContext.currentPageElements,
                                elementMap = paymentContext.componentElementMap,
                                layoutModes = paymentContext.getLayoutModes(),
                                condManaged = condManaged,
                                condVisible = condVisible,
                                animatedImageStateProvider = { cid ->
                                    paymentContext.getAnimatedImageState(cid)
                                },
                                inputTextProvider = { cid ->
                                    inputTextState[cid] ?: ""
                                },
                                onTextChange = { cid, text ->
                                    paymentContext.updateInputText(cid, text)
                                },
                                onButtonClick = { componentId ->
                                    handleButtonClick(componentId)
                                }
                            )
                        }

                        is Completed -> {
                            LaunchedEffect(Unit) {
                                resultBuilder.serverAnalyticsToken = paymentContext.serverAnalyticsToken
                                val (resultCode, resultIntent) = when (state.resultCode) {
                                    ResultCode.RESULT_CODE_SUCCESS -> {
                                        val fp = state.finishParams ?: run {
                                            Log.w(TAG, "SUCCESS but finishParams is null, using default-empty finishParams")
                                            FinishActionParams()
                                        }
                                        RESULT_OK to resultBuilder.buildSuccessIntent(fp)
                                    }
                                    ResultCode.RESULT_CODE_CANCEL -> {
                                        RESULT_CANCELED to resultBuilder.buildCancelIntent(state.finishParams)
                                    }
                                    ResultCode.RESULT_CODE_UNKNOWN -> {
                                        RESULT_FIRST_USER to resultBuilder.buildErrorIntent(null)
                                    }
                                    ResultCode.RESULT_CODE_ERROR -> {
                                        RESULT_FIRST_USER to resultBuilder.buildErrorIntent(state.finishParams)
                                    }
                                }
                                setResult(resultCode, resultIntent)
                                finish()
                            }
                        }
                        
                        is Error -> {
                            LaunchedEffect(Unit) {
                                setResult(RESULT_FIRST_USER, resultBuilder.buildErrorIntent())
                                finish()
                            }
                        }

                        is Cancelled -> {
                            LaunchedEffect(Unit) {
                                setResult(RESULT_CANCELED, resultBuilder.buildCancelIntent())
                                finish()
                            }
                        }
                    }

                    IconButton(
                        onClick = { onBackPressedDispatcher.onBackPressed() },
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                    }
                }
            }
        }

        lifecycleScope.launch {
            try {
                paymentController.startPaymentFlow(buyFlowConfig, encryptedParams, unencryptedParams)
            } catch (e: Exception) {
                paymentContext.updateState(Error("Payment flow failed: ${e.message}", e))
            }
        }
    }
    
    private fun handleButtonClick(componentId: Long) {
        lifecycleScope.launch {
            paymentController.onUserAction(componentId)
        }
    }

    override fun onDestroy() {
        if (::paymentController.isInitialized) {
            paymentController.destroy()
        }
        super.onDestroy()
    }
}


