package org.microg.vending.billing.core

data class BuyFlowParams(
    val apiVersion: Int,
    val sku: String,
    val skuType: String,
    val developerPayload: String = "",
    val sdkVersion: String = "",
    val needAuth: Boolean = false,
    val skuParams: Map<String, Any> = emptyMap(),
    val skuSerializedDockIdList: List<String>? = null,
    val skuOfferIdTokenList: List<String>? = null,
    val oldSkuPurchaseToken: String? = null,
    val oldSkuPurchaseId: String? = null
)