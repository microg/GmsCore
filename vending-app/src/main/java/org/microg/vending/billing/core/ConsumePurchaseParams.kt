package org.microg.vending.billing.core

data class ConsumePurchaseParams(
    val apiVersion: Int,
    val purchaseToken: String,
    val extraParams: Map<String, Any> = emptyMap()
)