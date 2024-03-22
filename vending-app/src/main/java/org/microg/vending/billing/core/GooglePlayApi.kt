package org.microg.vending.billing.core

class GooglePlayApi {
    companion object {
        const val URL_BASE = "https://play-fe.googleapis.com"
        const val URL_FDFE = "$URL_BASE/fdfe"
        const val URL_SKU_DETAILS = "$URL_FDFE/skuDetails"
        const val URL_EES_ACQUIRE = "$URL_FDFE/ees/acquire"
        const val URL_ACKNOWLEDGE_PURCHASE = "$URL_FDFE/acknowledgePurchase"
        const val URL_CONSUME_PURCHASE = "$URL_FDFE/consumePurchase"
        const val URL_GET_PURCHASE_HISTORY = "$URL_FDFE/inAppPurchaseHistory"
        const val URL_AUTH_PROOF_TOKENS = "https://www.googleapis.com/reauth/v1beta/users/me/reauthProofTokens"
    }
}