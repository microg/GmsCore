/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.vending.billing

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

internal fun normalizeProductDetails(
    jsonDetails: String,
    productType: String?,
    responseFormat: String?,
    multipleOfferTypes: List<String>?
): String {
    if (productType != "inapp" || responseFormat != "PRODUCT_DETAILS" || multipleOfferTypes?.contains("inapp") != true) {
        return jsonDetails
    }
    return try {
        val details = JSONObject(jsonDetails)
        if (details.has("oneTimePurchaseOfferDetailsList")) return jsonDetails
        val offer = details.optJSONObject("oneTimePurchaseOfferDetails") ?: return jsonDetails
        if (offer.length() == 0) return jsonDetails

        // Multi-offer clients can require the array even when the backend returns a legacy offer.
        details.put("oneTimePurchaseOfferDetailsList", JSONArray().put(offer)).toString()
    } catch (_: JSONException) {
        jsonDetails
    }
}
