/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.vending.billing

import android.accounts.Account
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_CANCEL_CURRENT
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.os.bundleOf
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.android.vending.VendingPreferences
import com.android.vending.billing.IInAppBillingService
import org.microg.vending.billing.ui.InAppBillingHostActivity
import org.microg.vending.billing.ui.logic.BuyFlowResult
import com.google.android.gms.droidguard.DroidGuardClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.microg.gms.utils.toHexString
import org.microg.vending.billing.core.*

private class BuyFlowCacheEntry(
    var packageName: String,
    var account: Account,
    var buyFlowParams: BuyFlowParams? = null,
    var lastAcquireResult: AcquireResult? = null,
    var droidGuardResult: String = ""
)

private const val EXPIRE_MS = 1 * 60 * 1000

private data class IAPCoreCacheEntry(
    val iapCore: IAPCore,
    val expiredAt: Long
)


private const val requestCode = 10001
@RequiresApi(21)
class InAppBillingServiceImpl(private val context: Context) : IInAppBillingService.Stub() {


    companion object {
        private val buyFlowCacheMap = mutableMapOf<String, BuyFlowCacheEntry>()
        private val iapCoreCacheMap = mutableMapOf<String, IAPCoreCacheEntry>()
        private val typeList = listOf(
            ProductType.SUBS,
            ProductType.INAPP,
            "first_party",
            "audio_book",
            "book",
            "book_subs",
            "nest_subs",
            "play_pass_subs",
            "stadia_item",
            "stadia_subs",
            "movie",
            "tv_show",
            "tv_episode",
            "tv_season"
        )
        fun acquireRequest(
            context: Context,
            cacheKey: String,
            actionContexts: List<ByteArray> = emptyList(),
            authToken: String? = null,
            firstRequest: Boolean = false
        ): BuyFlowResult {
            if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "acquireRequest(cacheKey=$cacheKey, actionContexts=${actionContexts.map { it.toHexString() }}, authToken=$authToken)")
            val buyFlowCacheEntry = buyFlowCacheMap[cacheKey] ?: return BuyFlowResult(
                null, null, resultBundle(BillingResponseCode.DEVELOPER_ERROR, "Parameter check error.")
            )
            val buyFlowParams = buyFlowCacheEntry.buyFlowParams ?: return BuyFlowResult(
                null, buyFlowCacheEntry.account, resultBundle(BillingResponseCode.DEVELOPER_ERROR, "Parameter check error.")
            )
            val params = AcquireParams(
                buyFlowParams = buyFlowParams,
                actionContext = actionContexts,
                authToken = authToken,
                droidGuardResult = buyFlowCacheEntry.droidGuardResult.takeIf { !firstRequest },
                lastAcquireResult = buyFlowCacheEntry.lastAcquireResult.takeIf { !firstRequest }
            )

            val coreResult = try {
                val deferred = CoroutineScope(Dispatchers.IO).async {
                    createIAPCore(
                        context,
                        buyFlowCacheEntry.account,
                        buyFlowCacheEntry.packageName
                    ).doAcquireRequest(
                        params
                    )
                }
                runBlocking { deferred.await() }
            } catch (e: RuntimeException) {
                if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "acquireRequest", e)
                return BuyFlowResult(null, buyFlowCacheEntry.account, resultBundle(BillingResponseCode.DEVELOPER_ERROR, e.message))
            } catch (e: Exception) {
                if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "acquireRequest", e)
                return BuyFlowResult(null, buyFlowCacheEntry.account, resultBundle(BillingResponseCode.DEVELOPER_ERROR, "Internal error."))
            }
            if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "acquireRequest acquireParsedResult: ${coreResult.acquireParsedResult}")
            buyFlowCacheEntry.lastAcquireResult = coreResult
            if (coreResult.acquireParsedResult.action?.droidGuardMap?.isNotEmpty() == true) {
                DroidGuardClient.getResults(context, "phonesky_acquire_flow", coreResult.acquireParsedResult.action.droidGuardMap).addOnCompleteListener { task ->
                    buyFlowCacheEntry.droidGuardResult = task.result
                }
            }
            coreResult.acquireParsedResult.purchaseItems.forEach {
                PurchaseManager.addPurchase(buyFlowCacheEntry.account, buyFlowCacheEntry.packageName, it)
            }
            return BuyFlowResult(
                coreResult.acquireParsedResult,
                buyFlowCacheEntry.account,
                coreResult.acquireParsedResult.result.toBundle()
            )
        }

        fun requestAuthProofToken(context: Context, cacheKey: String, password: String): String {
            val buyFlowCacheEntry = buyFlowCacheMap[cacheKey]
                ?: throw IllegalStateException("Nothing cached: $cacheKey")
            val deferred = CoroutineScope(Dispatchers.IO).async {
                createIAPCore(
                    context,
                    buyFlowCacheEntry.account,
                    buyFlowCacheEntry.packageName
                ).requestAuthProofToken(password)
            }
            return runBlocking { deferred.await() }
        }

        private fun createIAPCore(context: Context, account: Account, pkgName: String): IAPCore {
            val key = "$pkgName:$account"
            val cacheEntry = iapCoreCacheMap[key]
            if (cacheEntry != null) {
                if (cacheEntry.expiredAt > System.currentTimeMillis())
                    return cacheEntry.iapCore
                iapCoreCacheMap.remove(key)
            }
            val authData = AuthManager.getAuthData(context, account)
                ?: throw RuntimeException("Failed to obtain login token.")
            val deviceEnvInfo = createDeviceEnvInfo(context)
                ?: throw RuntimeException("Failed to retrieve device information.")
            val clientInfo = createClient(context, pkgName)
                ?: throw RuntimeException("Failed to retrieve client information.")
            val iapCore = IAPCore(context.applicationContext, deviceEnvInfo, clientInfo, authData)
            iapCoreCacheMap[key] =
                IAPCoreCacheEntry(iapCore, System.currentTimeMillis() + EXPIRE_MS)
            return iapCore
        }
    }

    private fun getPreferredAccount(extraParams: Bundle?): Account {
        val name = extraParams?.getString("accountName")
        name?.let {
            extraParams.remove("accountName")
        }
        return getGoogleAccount(context, name)
            ?: throw RuntimeException("No Google account found.")
    }

    private fun isBillingSupported(
        apiVersion: Int,
        type: String?,
        packageName: String,
        extraParams: Bundle?
    ): Bundle {
        if (!VendingPreferences.isBillingEnabled(context)) {
            Log.w(TAG, "isBillingSupported: Billing is disabled")
            return resultBundle(BillingResponseCode.BILLING_UNAVAILABLE, "Billing is disabled")
        }
        if (apiVersion < 3 || apiVersion > 17) {
            return resultBundle(BillingResponseCode.BILLING_UNAVAILABLE, "Client does not support the requesting billing API.")
        }
        if (extraParams != null && apiVersion < 7) {
            return resultBundle(BillingResponseCode.DEVELOPER_ERROR, "ExtraParams was introduced in API version 7.")
        }
        if (type.isNullOrBlank()) {
            return resultBundle(BillingResponseCode.DEVELOPER_ERROR, "SKU type can't be empty.")
        }
        if (!typeList.contains(type)) {
            return resultBundle(BillingResponseCode.DEVELOPER_ERROR, "Invalid SKU type: $type")
        }
        if (extraParams != null && !extraParams.isEmpty && extraParams.getBoolean("vr") && type == "subs") {
            return resultBundle(BillingResponseCode.BILLING_UNAVAILABLE, "subscription is not supported in VR Mode.")
        }
        return resultBundle(BillingResponseCode.OK, "")
    }

    override fun isBillingSupported(apiVersion: Int, packageName: String?, type: String?): Int {
        val result = isBillingSupported(apiVersion, type, packageName!!, null)
        Log.d(TAG, "isBillingSupported(apiVersion=$apiVersion, packageName=$packageName, type=$type)=$result")
        return result.getInt("RESPONSE_CODE")
    }

    override fun getSkuDetails(
        apiVersion: Int,
        packageName: String?,
        type: String?,
        skusBundle: Bundle?
    ): Bundle {
        Log.d(TAG, "getSkuDetails(apiVersion=$apiVersion, packageName=$packageName, type=$type, skusBundle=$skusBundle)")
        return resultBundle(BillingResponseCode.BILLING_UNAVAILABLE, "Not yet implemented")
    }

    override fun getBuyIntent(
        apiVersion: Int,
        packageName: String?,
        sku: String?,
        type: String?,
        developerPayload: String?
    ): Bundle {
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "getBuyIntent(apiVersion=$apiVersion, packageName=$packageName, sku=$sku, type=$type, developerPayload=$developerPayload)")
        return resultBundle(BillingResponseCode.BILLING_UNAVAILABLE, "Not yet implemented")
    }

    override fun getPurchases(
        apiVersion: Int,
        packageName: String?,
        type: String?,
        continuationToken: String?
    ): Bundle {
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "getPurchases(apiVersion=$apiVersion, packageName=$packageName, type=$type, continuationToken=$continuationToken)")
        return resultBundle(BillingResponseCode.BILLING_UNAVAILABLE, "Not yet implemented")
    }

    override fun consumePurchase(
        apiVersion: Int,
        packageName: String?,
        purchaseToken: String?
    ): Int {
        Log.d(TAG, "consumePurchase(apiVersion=$apiVersion, packageName=$packageName, purchaseToken=$purchaseToken)")
        return BillingResponseCode.BILLING_UNAVAILABLE
    }

    override fun isPromoEligible(apiVersion: Int, packageName: String?, type: String?): Int {
        Log.d(TAG, "isPromoEligible(apiVersion=$apiVersion, packageName=$packageName, type=$type)")
        return BillingResponseCode.BILLING_UNAVAILABLE
    }

    override fun getBuyIntentToReplaceSkus(
        apiVersion: Int,
        packageName: String?,
        oldSkus: MutableList<String>?,
        newSku: String?,
        type: String?,
        developerPayload: String?
    ): Bundle {
        Log.d(TAG, "getBuyIntentToReplaceSkus(apiVersion=$apiVersion, packageName=$packageName, oldSkus=$oldSkus, newSku=$newSku, type=$type, developerPayload=$developerPayload)")
        return resultBundle(BillingResponseCode.BILLING_UNAVAILABLE, "Not yet implemented")
    }

    override fun getBuyIntentExtraParams(
        apiVersion: Int,
        packageName: String,
        sku: String,
        type: String,
        developerPayload: String?,
        extraParams: Bundle?
    ): Bundle {
        if (!VendingPreferences.isBillingEnabled(context)) {
            Log.w(TAG, "getBuyIntentExtraParams: Billing is disabled")
            return resultBundle(BillingResponseCode.BILLING_UNAVAILABLE, "Billing is disabled")
        }
        extraParams?.size()
        Log.d(TAG, "getBuyIntentExtraParams(apiVersion=$apiVersion, packageName=$packageName, sku=$sku, type=$type, developerPayload=$developerPayload, extraParams=$extraParams)")


        val skuSerializedDocIdList =
            extraParams?.getStringArrayList("SKU_SERIALIZED_DOCID_LIST")
        skuSerializedDocIdList?.forEach {
            if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "serializedDocId=$it")
        }
        val skuOfferTypeList = extraParams?.getIntegerArrayList("SKU_OFFER_TYPE_LIST")
        skuOfferTypeList?.forEach {
            if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "skuOfferType=$it")
        }
        val skuOfferIdTokenList = extraParams?.getStringArrayList("SKU_OFFER_ID_TOKEN_LIST")
        skuOfferIdTokenList?.forEach {
            if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "skuOfferIdToken=$it")
        }
        val accountName = extraParams?.getString("accountName")?.also {
            if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "accountName=$it")
        }
        val oldSkuPurchaseToken = extraParams?.getString("oldSkuPurchaseToken")?.also {
            if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "oldSkuPurchaseToken=$it")
        }
        val oldSkuPurchaseId = extraParams?.getString("oldSkuPurchaseId")?.also {
            if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "oldSkuPurchaseId=$it")
        }

        extraParams?.let {
            it.remove("skusToReplace")
            it.remove("oldSkuPurchaseToken")
            it.remove("vr")
            it.remove("isDynamicSku")
            it.remove("rewardToken")
            it.remove("childDirected")
            it.remove("underAgeOfConsent")
            it.remove("additionalSkus")
            it.remove("additionalSkuTypes")
            it.remove("SKU_OFFER_ID_TOKEN_LIST")
            it.remove("SKU_OFFER_ID_LIST")
            it.remove("SKU_OFFER_TYPE_LIST")
            it.remove("SKU_SERIALIZED_DOCID_LIST")
            it.remove("oldSkuPurchaseId")
        }

        val account = try {
            getPreferredAccount(extraParams)
        } catch (e: RuntimeException) {
            return resultBundle(BillingResponseCode.BILLING_UNAVAILABLE, e.message)
        }
        val params = BuyFlowParams(
            apiVersion = apiVersion,
            sku = sku,
            skuType = type,
            developerPayload = developerPayload ?: "",
            skuParams = bundleToMap(extraParams),
            needAuth = SettingsManager(context).getAuthStatus(),
            skuSerializedDockIdList = skuSerializedDocIdList,
            skuOfferIdTokenList = skuOfferIdTokenList,
            oldSkuPurchaseId = oldSkuPurchaseId,
            oldSkuPurchaseToken = oldSkuPurchaseToken
        )
        val cacheEntryKey = "${packageName}:${account.name}"
        buyFlowCacheMap[cacheEntryKey] =
            BuyFlowCacheEntry(packageName, account, buyFlowParams = params)
        val intent = Intent(context, InAppBillingHostActivity::class.java)
        intent.putExtra(KEY_IAP_SHEET_UI_PARAM, cacheEntryKey)
        val buyFlowPendingIntent = PendingIntent.getActivity(context, requestCode, intent, FLAG_CANCEL_CURRENT or FLAG_IMMUTABLE)
        return resultBundle(BillingResponseCode.OK, "", bundleOf("BUY_INTENT" to buyFlowPendingIntent))
    }

    override fun getPurchaseHistory(
        apiVersion: Int,
        packageName: String?,
        type: String?,
        continuationToken: String?,
        extraParams: Bundle?
    ): Bundle {
        if (!VendingPreferences.isBillingEnabled(context)) {
            Log.w(TAG, "getPurchaseHistory: Billing is disabled")
            return resultBundle(BillingResponseCode.BILLING_UNAVAILABLE, "Billing is disabled")
        }
        extraParams?.size()
        Log.d(TAG, "getPurchaseHistory(apiVersion=$apiVersion, packageName=$packageName, type=$type, continuationToken=$continuationToken, extraParams=$extraParams)")
        val account = try {
            getPreferredAccount(extraParams)
        } catch (e: RuntimeException) {
            return resultBundle(BillingResponseCode.BILLING_UNAVAILABLE, e.message)
        }
        val params = GetPurchaseHistoryParams(
            apiVersion = apiVersion,
            type = type!!,
            continuationToken = continuationToken,
            extraParams = bundleToMap(extraParams)
        )
        val coreResult = try {
            val deferred = CoroutineScope(Dispatchers.IO).async {
                createIAPCore(context, account, packageName!!).getPurchaseHistory(params)
            }
            runBlocking {
                deferred.await()
            }
        } catch (e: RuntimeException) {
            return resultBundle(BillingResponseCode.DEVELOPER_ERROR, e.message)
        } catch (e: Exception) {
            Log.e(TAG, "getPurchaseHistory", e)
            return resultBundle(BillingResponseCode.DEVELOPER_ERROR, "Internal error.")
        }
        if (coreResult.getCode() == BillingResponseCode.OK) {
            val itemList = ArrayList<String>()
            val dataList = ArrayList<String>()
            val signatureList = ArrayList<String>()
            coreResult.purchaseHistoryList?.forEach {
                itemList.add(it.sku)
                dataList.add(it.jsonData)
                signatureList.add(it.signature)
            }
            val result = Bundle()
            result.putStringArrayList("INAPP_PURCHASE_ITEM_LIST", itemList)
            result.putStringArrayList("INAPP_PURCHASE_DATA_LIST", dataList)
            result.putStringArrayList("INAPP_DATA_SIGNATURE_LIST", signatureList)
            if (!coreResult.continuationToken.isNullOrEmpty()) {
                result.putString("INAPP_CONTINUATION_TOKEN", coreResult.continuationToken)
            }
            return resultBundle(BillingResponseCode.OK, "", result)
        }
        return coreResult.resultMap.toBundle()
    }

    override fun isBillingSupportedExtraParams(
        apiVersion: Int,
        packageName: String?,
        type: String?,
        extraParams: Bundle?
    ): Int {
        extraParams?.size()
        val result = isBillingSupported(apiVersion, type, packageName!!, extraParams)
        Log.d(TAG, "isBillingSupportedExtraParams(apiVersion=$apiVersion, packageName=$packageName, type=$type, extraParams=$extraParams)=$result")
        return result.getInt("RESPONSE_CODE")
    }

    override fun getPurchasesExtraParams(
        apiVersion: Int,
        packageName: String?,
        type: String?,
        continuationToken: String?,
        extraParams: Bundle?
    ): Bundle {
        if (!VendingPreferences.isBillingEnabled(context)) {
            Log.w(TAG, "getPurchasesExtraParams: Billing is disabled")
            return resultBundle(BillingResponseCode.BILLING_UNAVAILABLE, "Billing is disabled")
        }
        extraParams?.size()
        Log.d(TAG, "getPurchasesExtraParams(apiVersion=$apiVersion, packageName=$packageName, type=$type, continuationToken=$continuationToken, extraParams=$extraParams)")
        if (apiVersion < 7 && extraParams != null) {
            return resultBundle(BillingResponseCode.DEVELOPER_ERROR, "Parameter check error.")
        }
        val account = try {
            getPreferredAccount(extraParams)
        } catch (e: RuntimeException) {
            return resultBundle(BillingResponseCode.BILLING_UNAVAILABLE, e.message)
        }
        val enablePendingPurchases = extraParams?.getBoolean("enablePendingPurchases", false) ?: false
        val itemList = ArrayList<String>()
        val dataList = ArrayList<String>()
        val signatureList = ArrayList<String>()
        PurchaseManager.queryPurchases(account, packageName!!, type!!).filter {
            if (it.type == "subs" && it.expireAt < System.currentTimeMillis()) return@filter false
            true
        }.forEach {
            if (enablePendingPurchases || it.purchaseState != 4) {
                itemList.add(it.sku)
                dataList.add(it.jsonData)
                signatureList.add(it.signature)
            }
        }
        val result = Bundle()
        result.putStringArrayList("INAPP_PURCHASE_ITEM_LIST", itemList)
        result.putStringArrayList("INAPP_PURCHASE_DATA_LIST", dataList)
        result.putStringArrayList("INAPP_DATA_SIGNATURE_LIST", signatureList)
        return resultBundle(BillingResponseCode.OK, "", result)
    }

    override fun consumePurchaseExtraParams(
        apiVersion: Int,
        packageName: String?,
        purchaseToken: String,
        extraParams: Bundle?
    ): Bundle {
        if (!VendingPreferences.isBillingEnabled(context)) {
            Log.w(TAG, "consumePurchaseExtraParams: Billing is disabled")
            return resultBundle(BillingResponseCode.BILLING_UNAVAILABLE, "Billing is disabled")
        }
        extraParams?.size()
        Log.d(TAG, "consumePurchaseExtraParams(apiVersion=$apiVersion, packageName=$packageName, purchaseToken=$purchaseToken, extraParams=$extraParams)")
        val account = try {
            getPreferredAccount(extraParams)
        } catch (e: RuntimeException) {
            return resultBundle(BillingResponseCode.BILLING_UNAVAILABLE, e.message)
        }
        val params = ConsumePurchaseParams(
            apiVersion = apiVersion,
            purchaseToken = purchaseToken,
            extraParams = bundleToMap(extraParams)
        )
        val coreResult = try {
            val deferred = CoroutineScope(Dispatchers.IO).async {
                val coreResult = createIAPCore(context, account, packageName!!).consumePurchase(params)
                if (coreResult.getCode() == BillingResponseCode.OK) {
                    PurchaseManager.removePurchase(purchaseToken)
                }
                coreResult
            }
            runBlocking { deferred.await() }
        } catch (e: RuntimeException) {
            return resultBundle(BillingResponseCode.DEVELOPER_ERROR, e.message)
        } catch (e: Exception) {
            Log.e(TAG, "consumePurchaseExtraParams", e)
            return resultBundle(BillingResponseCode.DEVELOPER_ERROR, "Internal error.")
        }

        return coreResult.resultMap.toBundle()
    }

    override fun getPriceChangeConfirmationIntent(
        apiVersion: Int,
        packageName: String?,
        sku: String?,
        type: String?,
        extraParams: Bundle?
    ): Bundle {
        extraParams?.size()
        Log.d(TAG, "getPriceChangeConfirmationIntent(apiVersion=$apiVersion, packageName=$packageName, sku=$sku, type=$type, extraParams=$extraParams)")
        return resultBundle(BillingResponseCode.BILLING_UNAVAILABLE, "Not yet implemented")
    }

    override fun getSkuDetailsExtraParams(
        apiVersion: Int,
        packageName: String?,
        type: String?,
        skuBundle: Bundle?,
        extraParams: Bundle?
    ): Bundle {
        if (!VendingPreferences.isBillingEnabled(context)) {
            Log.w(TAG, "getSkuDetailsExtraParams: Billing is disabled")
            return resultBundle(BillingResponseCode.BILLING_UNAVAILABLE, "Billing is disabled")
        }
        extraParams?.size()
        skuBundle?.size()
        Log.d(TAG, "getSkuDetailsExtraParams(apiVersion=$apiVersion, packageName=$packageName, type=$type, skusBundle=$skuBundle, extraParams=$extraParams)")
        val account = try {
            getPreferredAccount(extraParams)
        } catch (e: RuntimeException) {
            return resultBundle(BillingResponseCode.BILLING_UNAVAILABLE, e.message)
        }
        val idList = skuBundle?.getStringArrayList("ITEM_ID_LIST")
        val dynamicPriceTokensList = skuBundle?.getStringArrayList("DYNAMIC_PRICE_TOKENS_LIST")
        if (idList.isNullOrEmpty()) {
            Log.e(TAG, "Input Error: skusBundle must contain an array associated with key ITEM_ID_LIST.")
            return resultBundle(BillingResponseCode.DEVELOPER_ERROR, "SKU bundle must contain sku list")
        }
        idList.sort()
        if (dynamicPriceTokensList != null && dynamicPriceTokensList.isEmpty()) {
            Log.e(TAG, "Input Error: skusBundle array associated with key ITEM_ID_LIST or key DYNAMIC_PRICE_TOKENS_LIST cannot be empty.")
            return resultBundle(BillingResponseCode.DEVELOPER_ERROR, "SKU bundle must contain sku list")
        }
        if (apiVersion < 9 && extraParams?.isEmpty == false) {
            return resultBundle(BillingResponseCode.DEVELOPER_ERROR, "Must specify an API version >= 9 to use this API.")
        }
        val params = GetSkuDetailsParams(
            apiVersion = apiVersion,
            skuType = type!!,
            skuIdList = idList,
            skuPkgName = extraParams?.getString("SKU_PACKAGE_NAME")?.also {
                extraParams.remove("SKU_PACKAGE_NAME")
            } ?: "",
            sdkVersion = extraParams?.getString("playBillingLibraryVersion") ?: "",
            multiOfferSkuDetail = extraParams?.let { bundleToMap(it) } ?: emptyMap()
        )

        val coreResult = try {
            val deferred = CoroutineScope(Dispatchers.IO).async {
                createIAPCore(context, account, packageName!!).getSkuDetails(params)
            }
            runBlocking { deferred.await() }
        } catch (e: RuntimeException) {
            Log.e(TAG, "getSkuDetailsExtraParams", e)
            return resultBundle(BillingResponseCode.DEVELOPER_ERROR, e.message)
        } catch (e: Exception) {
            Log.e(TAG, "getSkuDetailsExtraParams", e)
            return resultBundle(BillingResponseCode.DEVELOPER_ERROR, "Internal error.")
        }

        coreResult.let { detailsResult ->
            val details = ArrayList(detailsResult.skuDetailsList.map { it.jsonDetails })
            if (detailsResult.getCode() == BillingResponseCode.OK) {
                return resultBundle(BillingResponseCode.OK, "", bundleOf("DETAILS_LIST" to details))
            } else {
                return resultBundle(detailsResult.getCode(), detailsResult.getMessage())
            }
        }
    }

    override fun acknowledgePurchase(
        apiVersion: Int,
        packageName: String?,
        purchaseToken: String?,
        extraParams: Bundle?
    ): Bundle {
        if (!VendingPreferences.isBillingEnabled(context)) {
            Log.w(TAG, "acknowledgePurchase: Billing is disabled")
            return resultBundle(BillingResponseCode.BILLING_UNAVAILABLE, "Billing is disabled")
        }
        extraParams?.size()
        Log.d(TAG, "acknowledgePurchase(apiVersion=$apiVersion, packageName=$packageName, purchaseToken=$purchaseToken, extraParams=$extraParams)")
        val account = try {
            getPreferredAccount(extraParams)
        } catch (e: RuntimeException) {
            return resultBundle(BillingResponseCode.BILLING_UNAVAILABLE, e.message)
        }
        val params = AcknowledgePurchaseParams(
            apiVersion = apiVersion,
            purchaseToken = purchaseToken!!,
            extraParams = bundleToMap(extraParams)
        )
        val coreResult = try {
            val deferred = CoroutineScope(Dispatchers.IO).async {
                val coreResult = createIAPCore(context, account, packageName!!).acknowledgePurchase(params)
                if (coreResult.getCode() == BillingResponseCode.OK && coreResult.purchaseItem != null) {
                    PurchaseManager.updatePurchase(coreResult.purchaseItem)
                }
                coreResult
            }
            runBlocking { deferred.await() }
        } catch (e: RuntimeException) {
            return resultBundle(BillingResponseCode.DEVELOPER_ERROR, e.message)
        } catch (e: Exception) {
            Log.e(TAG, "acknowledgePurchase", e)
            return resultBundle(BillingResponseCode.DEVELOPER_ERROR, "Internal error.")
        }

        return coreResult.resultMap.toBundle()
    }

    override fun o(
        apiVersion: Int,
        packageName: String?,
        arg3: String?,
        extraParams: Bundle?
    ): Bundle {
        extraParams?.size()
        Log.d(TAG, "o(apiVersion=$apiVersion, packageName=$packageName, arg3=$arg3, extraParams=$extraParams)")
        return resultBundle(BillingResponseCode.BILLING_UNAVAILABLE, "Not yet implemented")
    }

}