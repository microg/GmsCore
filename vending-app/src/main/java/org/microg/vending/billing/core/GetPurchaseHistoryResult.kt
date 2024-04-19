package org.microg.vending.billing.core

import org.microg.vending.billing.proto.PurchaseHistoryResponse

class GetPurchaseHistoryResult(
    val purchaseHistoryList: List<PurchaseHistoryItem>?,
    val continuationToken: String?,
    resultMap: Map<String, Any> = mapOf(
        "RESPONSE_CODE" to 0,
        "DEBUG_MESSAGE" to ""
    )
) : IAPResult(resultMap) {
    companion object {
        fun parseFrom(
            response: PurchaseHistoryResponse?
        ): GetPurchaseHistoryResult {
            if (response == null) {
                throw NullPointerException("PurchaseHistoryResponse is null")
            }
            if (response.failedResponse != null) {
                return GetPurchaseHistoryResult(
                    null,
                    null,
                    mapOf(
                        "RESPONSE_CODE" to response.failedResponse.statusCode,
                        "DEBUG_MESSAGE" to response.failedResponse.msg
                    )
                )
            }
            if (response.productId.size != response.purchaseJson.size || response.purchaseJson.size != response.signature.size) {
                throw IllegalStateException("GetPurchaseHistoryResult item count error")
            }
            val purchaseHistoryList = mutableListOf<PurchaseHistoryItem>()
            var continuationToken: String? = null
            for (cnt in 0 until response.productId.size) {
                purchaseHistoryList.add(
                    PurchaseHistoryItem(
                        response.productId[cnt],
                        response.purchaseJson[cnt],
                        response.signature[cnt]
                    )
                )
            }
            if (!response.continuationToken.isNullOrEmpty()) {
                continuationToken = response.continuationToken
            }

            return GetPurchaseHistoryResult(purchaseHistoryList, continuationToken)
        }
    }

    class PurchaseHistoryItem(val sku: String, val jsonData: String, val signature: String)
}