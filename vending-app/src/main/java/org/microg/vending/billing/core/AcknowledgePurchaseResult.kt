package org.microg.vending.billing.core

import org.microg.vending.billing.proto.AcknowledgePurchaseResponse

class AcknowledgePurchaseResult(
    val purchaseItem: PurchaseItem? = null,
    resultMap: Map<String, Any> = mapOf(
        "RESPONSE_CODE" to 0,
        "DEBUG_MESSAGE" to ""
    )
) : IAPResult(resultMap) {
    companion object {
        fun parseFrom(
            response: AcknowledgePurchaseResponse?
        ): AcknowledgePurchaseResult {
            if (response == null) {
                throw NullPointerException("response is null")
            }
            if (response.failedResponse != null) {
                return AcknowledgePurchaseResult(
                    null,
                    mapOf(
                        "RESPONSE_CODE" to response.failedResponse.statusCode,
                        "DEBUG_MESSAGE" to response.failedResponse.msg
                    )
                )
            }
            if (response.purchaseItem == null) {
                throw NullPointerException("AcknowledgePurchaseResponse PurchaseItem is null")
            }
            if (response.purchaseItem.purchaseItemData.size != 1)
                throw IllegalStateException("AcknowledgePurchaseResult purchase item count != 1")
            return AcknowledgePurchaseResult(parsePurchaseItem(response.purchaseItem).getOrNull(0))
        }
    }
}