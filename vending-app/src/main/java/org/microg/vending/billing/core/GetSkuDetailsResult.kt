package org.microg.vending.billing.core

import org.microg.vending.billing.proto.DocId
import org.microg.vending.billing.proto.SkuDetailsResponse


class GetSkuDetailsResult private constructor(
    val skuDetailsList: List<SkuDetailsItem>,
    resultMap: Map<String, Any> = mapOf("RESPONSE_CODE" to 0, "DEBUG_MESSAGE" to "")
) : IAPResult(resultMap) {
    companion object {
        fun parseFrom(skuDetailsResponse: SkuDetailsResponse?): GetSkuDetailsResult {
            if (skuDetailsResponse == null) {
                throw NullPointerException("SkuDetailsResponse is null")
            }
            if (skuDetailsResponse.failedResponse != null) {
                return GetSkuDetailsResult(
                    emptyList(),
                    mapOf(
                        "RESPONSE_CODE" to skuDetailsResponse.failedResponse.statusCode,
                        "DEBUG_MESSAGE" to skuDetailsResponse.failedResponse.msg
                    )
                )
            }
            val skuDetailsList =
                skuDetailsResponse.details.filter { it.skuDetails.isNotBlank() && it.skuInfo != null}
                    .map { skuDetails ->
                        SkuDetailsItem(
                            skuDetails.skuDetails,
                            skuDetails.skuInfo!!.skuItem.associate { it.token to it.docId }
                        )
                    }
            return GetSkuDetailsResult(skuDetailsList)
        }
    }


    data class SkuDetailsItem(
        val jsonDetails: String,
        val docIdMap: Map<String, DocId?>
    )
}