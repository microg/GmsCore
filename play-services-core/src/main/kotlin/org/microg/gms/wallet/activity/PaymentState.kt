/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.wallet.activity

import org.microg.vending.billing.proto.FinishActionParams
import org.microg.vending.billing.proto.IapCommonResponse
import org.microg.vending.billing.proto.ResultCode

/**
 * Payment flow state definitions
 * Represents all possible states in the payment authentication flow
 */
sealed class PaymentState {
    /**
     * Initial state - no payment in progress
     */
    object Idle : PaymentState()
    
    /**
     * Starting the payment flow - initializing
     */
    object Initializing : PaymentState()
    
    /**
     * Payment initialized, ready for first submission
     */
    object Initialized : PaymentState()
    
    /**
     * Submit in progress
     */
    object Submitting: PaymentState()
    
    /**
     * Submit completed, waiting for user interaction or next submit
     * @param response The response from Google API
     */
    data class Submitted(val response: IapCommonResponse) : PaymentState()
    
    /**
     * Payment flow completed with a result from the server
     * @param resultCode 1=SUCCESS, 2=CANCEL, 3=ERROR (from InfrastructureAction.finishParams)
     */
    data class Completed(
        val resultCode: ResultCode = ResultCode.RESULT_CODE_UNKNOWN,
        val finishParams: FinishActionParams? = null
    ) : PaymentState()
    
    /**
     * Payment flow failed with error
     * @param message Error message
     * @param exception Optional exception details
     */
    data class Error(val message: String, val exception: Throwable? = null) : PaymentState()
    
    /**
     * Payment was cancelled by user
     */
    object Cancelled : PaymentState()
}
