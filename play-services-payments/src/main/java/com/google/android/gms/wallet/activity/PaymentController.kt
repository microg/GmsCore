/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.wallet.activity

import android.util.Log
import com.google.android.gms.wallet.shared.BuyFlowConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.microg.vending.billing.proto.ComponentTreeNode
import org.microg.vending.billing.proto.FunctionalDataExecutionState
import org.microg.vending.billing.proto.PageElement
import org.microg.vending.billing.proto.IapCommonResponse
import org.microg.vending.billing.proto.ResultCode

/**
 * Payment Controller
 * Orchestrates the entire payment flow: init → submit1 → submit2 → ... → submitN
 * Manages state transitions and coordinates between PaymentContext and UI
 */
class PaymentController(private val paymentContext: PaymentContext) {
    private val scopeJob = Job()
    private val scope = CoroutineScope(Dispatchers.Main + scopeJob)

    companion object {
        private const val TAG = "PaymentController"
    }

    private var isProcessingAction = false
    private var submitJob: Job? = null

    suspend fun startPaymentFlow(buyFlowConfig: BuyFlowConfig, encryptedParams: ByteArray?, unencryptedParams: ByteArray?) {
        val initResponse = paymentContext.initialize(
            buyFlowConfig,
            encryptedParams,
            unencryptedParams
        )
        if (initResponse == null) {
            Log.d(TAG, "startPaymentFlow, init response is null")
            return
        }

        paymentContext.updateState(PaymentState.Initialized)

        val firstSubmitResponse = awaitSubmit(
            tokenization = initResponse.secureDataHeader?.tokenization ?: emptyList()
        )

        if (firstSubmitResponse == null) {
            Log.d(TAG, "startPaymentFlow, first submit response")
            paymentContext.updateState(PaymentState.Error("First submit failed"))
            return
        }

        val delta = getDeltaPageElements(firstSubmitResponse, isInitial = false)
        handleResponseDecision(firstSubmitResponse, delta)
    }
    
    /**
     * Perform a single submit operation
     */
    private suspend fun awaitSubmit(
        tokenization: List<String>
    ): IapCommonResponse? {
        val result = paymentContext.submit(tokenization)
        return result
    }

    /**
     * Extract incremental PageElements from the response (used for decision-making: presence of buttons/encryption, etc.)
     * Note: UI rendering uses paymentContext.currentPageElements (full set)
     */
    private fun getDeltaPageElements(
        response: IapCommonResponse,
        isInitial: Boolean
    ): List<PageElement> {
        return if (isInitial) {
            response.responseBody?.initializePartialPageProtoWrapper?.partialPage?.pageElements ?: emptyList()
        } else {
            response.responseBody?.updatePartialPageProtoWrapper?.updatePartialPageProto?.toAddOrReplaceData ?: emptyList()
        }
    }
    
    /**
     * Called when the user clicks a button
     */
    fun onUserAction(buttonComponentId: Long) {
        Log.d(TAG, "onUserAction - buttonId=$buttonComponentId")
        if (isProcessingAction) {
            Log.d(TAG, "onUserAction - already processing, ignoring")
            return
        }
        when (val currentState = paymentContext.paymentState.value) {
            is PaymentState.Submitted -> {
                isProcessingAction = true
                triggerButtonAction(buttonComponentId)
                // If triggerButtonAction triggered FINISH, do not continue with submit
                if (paymentContext.paymentState.value is PaymentState.Completed) {
                    Log.d(TAG, "onUserAction - FINISH triggered by button, not continuing")
                    isProcessingAction = false
                    return
                }
                Log.d(TAG, "onUserAction - continuing to next submit after button trigger")
                launchSubmit {
                    continueToNextSubmit(currentState.response)
                }
            }
            is PaymentState.Completed -> {
                Log.d(TAG, "onUserAction - already completed(resultCode=${currentState.resultCode})")
            }
            else -> {
                Log.d(TAG, "onUserAction - unexpected state ${currentState::class.simpleName}")
            }
        }
    }

    private fun triggerButtonAction(buttonComponentId: Long) {
        val tree = paymentContext.componentTreeManager.getTree() ?: return

        val dataId = findDataIdByCondRef(tree, buttonComponentId)
        if (dataId == null) {
            Log.d(TAG, "triggerButtonAction: no tree node for button id=$buttonComponentId")
            return
        }

        Log.d(TAG, "triggerButtonAction: button=$buttonComponentId → dataId=$dataId → event engine")
        val results = paymentContext.eventEngine.onButtonClick(listOf(dataId))
        Log.d(TAG, "triggerButtonAction: results=$results")

        for (result in results) {
            when (result) {
                is ActionResult.StateChange -> {
                    if (result.newState == FunctionalDataExecutionState.FUNCTIONAL_DATA_EXECUTION_STATE_RUNNING) {
                        Log.d(TAG, "triggerButtonAction: component ${result.componentId} → RUNNING, executing encryption now")
                        paymentContext.executeEncryptionForComponent(result.componentId)
                    }
                }
                is ActionResult.Finish -> {
                    Log.d(TAG, "triggerButtonAction: FINISH from event engine, resultCode=${result.resultCode}")
                    paymentContext.updateState(PaymentState.Completed(result.resultCode, result.finishParams))
                    return
                }
                is ActionResult.Submit -> {
                    Log.d(TAG, "triggerButtonAction: SUBMIT from event engine")
                }
                is ActionResult.AnimatedImageStateChange -> {
                    Log.d(TAG, "triggerButtonAction: AnimatedImage ${result.componentId} → state=${result.newState}")
                }
                is ActionResult.EnablementChange -> {
                    Log.d(TAG, "triggerButtonAction: Enablement ${result.componentId} → ${result.enablementState}")
                }
                is ActionResult.ConditionValueChange -> {
                    Log.d(TAG, "triggerButtonAction: ConditionValue ${result.componentId} → ${result.newConditionValue}")
                    paymentContext.updateConditionValue(result.componentId, result.newConditionValue)
                }
                is ActionResult.ValidationPassed -> {
                    Log.d(TAG, "triggerButtonAction: ValidationPassed for cid=${result.componentId}")
                }
            }
        }
    }

    private fun findDataIdByCondRef(node: ComponentTreeNode, targetCondRef: Long): Long? {
        if (node.conditionRef == targetCondRef) {
            return node.nodeDataFieldRefs.firstOrNull()?.dataIds?.firstOrNull()
        }
        val children = when (node.nodeTypeId) {
            214299793 -> (node.containerExt?.children ?: emptyList()) + listOfNotNull(node.containerExt?.footer)
            231420908 -> node.conditionalExt?.children ?: emptyList()
            264434503 -> listOfNotNull(node.fullSheetExt?.child)
            229613734 -> listOfNotNull(node.scrollExt?.child)
            else -> emptyList()
        }
        for (child in children) {
            val result = findDataIdByCondRef(child, targetCondRef)
            if (result != null) return result
        }
        return null
    }

    /**
     * Unified decision-making after each submit response is processed
     * Priority: FINISH > button (full-set visible) > auto-continue (incremental has new data) > complete
     */
    private fun handleResponseDecision(
        response: IapCommonResponse,
        delta: List<PageElement>
    ) {
        // 1. Check whether the server has directly sent a FINISH signal
        val finishParams = paymentContext.detectDirectFinish()
        if (finishParams != null) {
            val resultCode = finishParams.resultCode ?: ResultCode.RESULT_CODE_UNKNOWN
            Log.d(TAG, "Server sent FINISH signal, resultCode=$resultCode")
            isProcessingAction = false
            paymentContext.updateState(PaymentState.Completed(resultCode, finishParams))
            return
        }

        // 2. A button is present in the fully visible components → wait for user interaction
        if (hasVisibleButton()) {
            Log.d(TAG, "Has visible button - waiting for user action")
            isProcessingAction = false
            paymentContext.updateState(PaymentState.Submitted(response))
            return
        }

        // 3. Incremental contains new encryption/key fields → must continue automatically
        if (hasCryptoFields(delta)) {
            Log.d(TAG, "Has crypto fields in delta - auto submit next")
            launchSubmit {
                continueToNextSubmit(response)
            }
            return
        }

        // 4. Incremental has other new data but no button → continue automatically (UI-only update)
        if (delta.isNotEmpty()) {
            Log.d(TAG, "Delta has new data but no button - auto submit next")
            launchSubmit {
                continueToNextSubmit(response)
            }
            return
        }

        // 5. Incremental is empty with no button and no FINISH → terminate abnormally
        Log.e(TAG, "No button, no new data, no FINISH - flow ended abnormally")
        isProcessingAction = false
        paymentContext.updateState(PaymentState.Error("Unexpected: no button, no data, no FINISH after submit "))
    }

    private fun hasVisibleButton(): Boolean {
        val (managed, visible) = paymentContext.getComponentVisibility()
        return paymentContext.componentElementMap.any { (cid, pe) ->
            pe.extensionFieldNumber == 232057536 &&
                (cid !in managed || cid in visible)
        }
    }

    private fun hasCryptoFields(delta: List<PageElement>): Boolean {
        return delta.any {
            it.extensionFieldNumber == 290848975 ||  // AES-GCM encryption action
                it.extensionFieldNumber == 290848973 ||  // P256 key generation
                it.extensionFieldNumber == 290848974     // ECDH key exchange
        }
    }
    
    private suspend fun continueToNextSubmit(
        stepResponse: IapCommonResponse
    ) {
        try {

            paymentContext.updateState(PaymentState.Submitting)

            val nextResponse = awaitSubmit(
                tokenization = stepResponse.secureDataHeader?.tokenization ?: emptyList()
            )

            if (nextResponse == null) {
                Log.e(TAG, "Submit failed - response is null")
                paymentContext.updateState(PaymentState.Error("Submit failed"))
                return
            }

            val delta = getDeltaPageElements(nextResponse, isInitial = false)

            handleResponseDecision(nextResponse, delta)
        } catch (e: Exception) {
            Log.e(TAG, "continueToNextSubmit failed", e)
            paymentContext.updateState(PaymentState.Error("Submit failed: ${e.message}", e))
        } finally {
            val state = paymentContext.paymentState.value
            if (state is PaymentState.Error || state is PaymentState.Completed) {
                isProcessingAction = false
            }
        }
    }

    private fun launchSubmit(block: suspend () -> Unit) {
        submitJob?.cancel()
        submitJob = scope.launch { block() }
    }

    fun reset() {
        Log.d(TAG, "reset() - clearing state")
        submitJob?.cancel()
        isProcessingAction = false
        paymentContext.reset()
    }

    fun destroy() {
        Log.d(TAG, "destroy() - cancelling scope")
        scopeJob.cancel()
        paymentContext.closeResources()
    }
}
