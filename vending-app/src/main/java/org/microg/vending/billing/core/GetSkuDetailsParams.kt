package org.microg.vending.billing.core

data class GetSkuDetailsParams(
    val apiVersion: Int,
    val skuType: String,
    val skuIdList: List<String>,
    val skuPkgName: String = "",
    val sdkVersion: String = "",
    val multiOfferSkuDetail: Map<String, Any> = emptyMap()
)