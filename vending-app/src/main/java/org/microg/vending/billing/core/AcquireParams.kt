package org.microg.vending.billing.core

import org.microg.vending.billing.proto.SecurePayloadData

data class AcquireParams(
    val buyFlowParams: BuyFlowParams,
    val actionContext: List<ByteArray> = emptyList(),
    val droidGuardResult: String? = null,
    val authToken: String? = null,
    var lastAcquireResult: AcquireResult? = null,
    val integratorCallbackData: String? = null,
    val securePayload: SecurePayloadData? = null
)