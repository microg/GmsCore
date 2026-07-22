package org.microg.vending.billing.core

import org.microg.vending.billing.core.ui.AcquireParsedResult
import org.microg.vending.billing.core.ui.parseAcquireResponse
import org.microg.vending.billing.proto.AcquireRequest
import org.microg.vending.billing.proto.AcquireResponse

data class AcquireResult(
    val acquireParsedResult: AcquireParsedResult,
    val acquireRequest: AcquireRequest,
    val acquireResponse: AcquireResponse,
) {
    companion object {
        fun parseFrom(
            acquireParams: AcquireParams,
            acquireRequest: AcquireRequest,
            acquireResponse: AcquireResponse?
        ): AcquireResult {
            if (acquireResponse == null) {
                throw NullPointerException("AcquireResponse is null")
            }
            return AcquireResult(
                parseAcquireResponse(acquireParams, acquireResponse),
                acquireRequest,
                acquireResponse
            )
        }
    }
}