package org.microg.vending.billing.core

data class GetPurchaseHistoryParams(
    val apiVersion: Int,
    val type: String,
    val continuationToken: String? = null,
    val extraParams: Map<String, Any> = emptyMap()
)