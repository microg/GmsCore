package org.microg.vending.billing.core

// TODO: Use class from billing client instead
data class PurchaseItem(
    val type: String,
    val sku: String,
    val pkgName: String,
    val purchaseToken: String,
    val purchaseState: Int,
    val jsonData: String,
    val signature: String,
    val startAt: Long = 0L,
    val expireAt: Long = 0L,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PurchaseItem) return false
        if (purchaseToken != other.purchaseToken) return false
        return true
    }

    override fun hashCode(): Int {
        return purchaseToken.hashCode()
    }
}
