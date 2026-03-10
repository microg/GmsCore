package org.microg.vending.billing.core

data class AcquireParams(
    val buyFlowParams: BuyFlowParams,
    val actionContext: List<ByteArray> = emptyList(),
    val droidGuardResult: String? = null,
    val authToken: String? = null,
    var lastAcquireResult: AcquireResult? = null
)