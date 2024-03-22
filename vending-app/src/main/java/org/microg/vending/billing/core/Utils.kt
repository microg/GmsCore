package org.microg.vending.billing.core

import android.util.Base64
import android.util.Log
import com.android.billingclient.api.BillingClient.ProductType
import okio.ByteString
import okio.ByteString.Companion.toByteString
import org.json.JSONObject
import org.microg.vending.billing.TAG
import org.microg.vending.billing.proto.*
import org.microg.vending.billing.proto.PurchaseItem
import java.security.InvalidParameterException
import java.security.SecureRandom
import java.util.*

fun List<ByteArray>.toByteStringList(): List<ByteString> {
    return this.map { it.toByteString() }
}

fun mapToSkuParamList(map: Map<String, Any>?): List<SkuParam> {
    val result = mutableListOf<SkuParam>()
    if (map == null)
        return result
    map.forEach { entry ->
        result.add(
            when (val value = entry.value) {
                is Boolean -> SkuParam.Builder().apply {
                    name = entry.key
                    bv = value
                }.build()

                is Long -> SkuParam.Builder().apply {
                    name = entry.key
                    i64v = value
                }.build()

                is Int -> SkuParam.Builder().apply {
                    name = entry.key
                    i64v = value.toLong()
                }.build()

                is ArrayList<*> -> SkuParam.Builder().apply {
                    name = entry.key
                    svList = value.map { it as String }
                }.build()

                is String -> SkuParam.Builder().apply {
                    name = entry.key
                    sv = value
                }.build()

                else -> SkuParam.Builder().apply {
                    name = entry.key
                    sv = value.toString()
                }.build()
            }
        )
    }

    return result
}

fun localeToString(locale: Locale): String {
    val result = StringBuilder()
    result.append(locale.language)
    locale.country?.let {
        if (it.isNotEmpty())
            result.append("-$it")
    }
    locale.variant?.let {
        if (it.isNotEmpty())
            result.append("-$it")
    }
    return result.toString()
}

fun createClientToken(deviceInfo: DeviceEnvInfo, authData: AuthData): String {
    val clientToken = ClientToken.Builder().apply {
        this.info1 = ClientToken.Info1.Builder().apply {
            this.locale = localeToString(deviceInfo.locale)
            this.unknown8 = 2
            this.gpVersionCode = deviceInfo.gpVersionCode
            this.deviceInfo = ClientToken.DeviceInfo.Builder().apply {
                this.unknown3 = "33"
                this.device = deviceInfo.device
                deviceInfo.displayMetrics?.let {
                    this.widthPixels = it.widthPixels
                    this.heightPixels = it.heightPixels
                    this.xdpi = it.xdpi
                    this.ydpi = it.ydpi
                    this.densityDpi = it.densityDpi
                }
                this.gpPackage = deviceInfo.gpPkgName
                this.gpVersionCode = deviceInfo.gpVersionCode.toString()
                this.gpVersionName = deviceInfo.gpVersionName
                this.envInfo = ClientToken.EnvInfo.Builder().apply {
                    this.deviceData = ClientToken.DeviceData.Builder().apply {
                        this.unknown1 = 0
                        deviceInfo.telephonyData?.let {
                            this.simOperatorName = it.simOperatorName
                            this.phoneDeviceId = it.phoneDeviceId
                            this.phoneDeviceId1 = it.phoneDeviceId
                        }
                        this.gsfId = authData.gsfId.toLong(16)
                        this.device = deviceInfo.device
                        this.product = deviceInfo.product
                        this.model = deviceInfo.model
                        this.manufacturer = deviceInfo.manufacturer
                        this.fingerprint = deviceInfo.fingerprint
                        this.release = deviceInfo.release
                        this.brand = deviceInfo.brand
                        this.serial = deviceInfo.serialNo
                        this.isEmulator = false
                    }.build()
                    this.otherInfo = ClientToken.OtherInfo.Builder().apply {
                        this.gpInfo = mutableListOf((
                            ClientToken.GPInfo.Builder().apply {
                                this.package_ = deviceInfo.gpPkgName
                                this.versionCode = deviceInfo.gpVersionCode.toString()
                                this.lastUpdateTime = deviceInfo.gpLastUpdateTime
                                this.firstInstallTime = deviceInfo.gpFirstInstallTime
                                this.sourceDir = deviceInfo.gpSourceDir
                            }).build())
                        this.batteryLevel = deviceInfo.batteryLevel
                        this.timeZoneOffset = deviceInfo.timeZoneOffset
                        this.location = ClientToken.Location.Builder().apply {
                            deviceInfo.locationData?.let {
                                this.altitude = it.altitude
                                this.latitude = it.latitude
                                this.longitude = it.longitude
                                this.accuracy = it.accuracy
                                this.time = it.time
                            }
                            this.isMock = false
                        }.build()
                        this.isAdbEnabled = deviceInfo.isAdbEnabled
                        this.installNonMarketApps = deviceInfo.installNonMarketApps
                        this.iso3Language = deviceInfo.locale.isO3Language
                        this.netAddress = deviceInfo.networkData?.netAddressList ?: emptyList()
                        this.locale = deviceInfo.locale.toString()
                        deviceInfo.telephonyData?.let {
                            this.networkOperator = it.networkOperator
                            this.simOperator = it.simOperator
                            this.phoneType = it.phoneType
                        }
                        this.language = deviceInfo.locale.language
                        this.country = deviceInfo.locale.country
                        this.uptimeMillis = deviceInfo.uptimeMillis
                        this.timeZoneDisplayName = deviceInfo.timeZoneDisplayName
                        this.googleAccountCount = deviceInfo.googleAccounts.size
                    }.build()
                }.build()
                this.marketClientId = "am-google"
                this.unknown15 = 1
                this.unknown16 = 2
                this.unknown22 = 2
                deviceInfo.networkData?.let {
                    this.linkDownstreamBandwidth = it.linkDownstreamBandwidth
                    this.linkUpstreamBandwidth = it.linkUpstreamBandwidth
                    this.isActiveNetworkMetered = it.isActiveNetworkMetered
                }
                this.unknown34 = 2
                this.uptimeMillis = deviceInfo.uptimeMillis
                this.timeZoneDisplayName = deviceInfo.timeZoneDisplayName
                this.unknown40 = 1
            }.build()
            this.unknown11 = "-5228872483831680725"
            this.googleAccounts = deviceInfo.googleAccounts
        }.build()
        this.info2 = ClientToken.Info2.Builder().apply {
            this.unknown1 =
                "https://play.app.goo.gl/?link=http%3A%2F%2Funused.google.com&apn=com.android.vending&al=google-orchestration%3A%2F%2Freturn"
            this.unknown3 = 1
            this.unknown4 = mutableListOf(2)
            this.unknown5 = 1
        }.build()
    }.build()
    return Base64.encodeToString(
        clientToken.encode(),
        Base64.URL_SAFE + Base64.NO_WRAP
    )
}

fun getAcquireCacheKey(
    deviceInfo: DeviceEnvInfo,
    accountName: String,
    docList: List<CKDocument>,
    callingPackage: String,
    extras: Map<String, String>,
    authFrequency: Int
): String {
    val stringBuilder = StringBuilder()
    stringBuilder.append(accountName)
    for (item in docList) {
        stringBuilder.append("#")
        stringBuilder.append(Base64.encodeToString(item.encode(), Base64.NO_WRAP))
    }
    stringBuilder.append("#simId=${deviceInfo.deviceId}")
    stringBuilder.append("#clientTheme=2")
    stringBuilder.append("#fingerprintValid=false")
    stringBuilder.append("#desiredAuthMethod=0")
    stringBuilder.append("#authFrequency=$authFrequency")
    stringBuilder.append("#userHasFop=false")
    stringBuilder.append("#callingAppPackageName=$callingPackage")
    for (item in extras) {
        stringBuilder.append("#${item.key}=${item.value}")
    }
    return stringBuilder.toString()
}

fun createNonce(): String {
    val secureRandom = SecureRandom.getInstance("SHA1PRNG")
        ?: throw RuntimeException("Uninitialized SecureRandom.")
    val result = ByteArray(0x100)
    secureRandom.nextBytes(result)
    return "nonce=" + Base64.encodeToString(result, Base64.URL_SAFE + Base64.NO_WRAP + Base64.NO_PADDING)
}

fun responseBundleToMap(responseBundle: ResponseBundle?): Map<String, Any> {
    val result = mutableMapOf<String, Any>()
    if (responseBundle != null) {
        for (bundleItem in responseBundle.bundleItem) {
            if (bundleItem.bv != null) {
                result[bundleItem.key] = bundleItem.bv
            } else if (bundleItem.i32v != null) {
                result[bundleItem.key] = bundleItem.i32v
            } else if (bundleItem.i64v != null) {
                result[bundleItem.key] = bundleItem.i64v
            } else if (bundleItem.sv != null) {
                result[bundleItem.key] = bundleItem.sv
            } else if (bundleItem.sList != null) {
                result[bundleItem.key] =
                    ArrayList(bundleItem.sList.value_)
            } else {

            }
        }
    }
    return result
}

fun getSkuType(skuType: String): Int {
    return when (skuType) {
        ProductType.SUBS -> 15
        ProductType.INAPP -> 11
        "first_party" -> 15
        else -> throw InvalidParameterException("unknown skuType: $skuType")
    }
}

fun splitDocId(docId: DocId): List<String> {
    return docId.backendDocId.split(":")
}

fun parsePurchaseItem(purchaseItem: PurchaseItem): List<org.microg.vending.billing.core.PurchaseItem> {
    val result = mutableListOf<org.microg.vending.billing.core.PurchaseItem>()
    for (it in purchaseItem.purchaseItemData) {
        if (it == null)
            continue
        val spr = if (it.docId != null) {
            splitDocId(it.docId)
        } else {
            emptyList()
        }
        if (spr.size < 3)
            continue
        val (type, _, sku) = spr
        var startAt = 0L
        var expireAt = 0L
        val (jsonData, signature) = when (type) {
            ProductType.INAPP -> {
                if (it.inAppPurchase == null)
                    continue
                it.inAppPurchase.jsonData to it.inAppPurchase.signature
            }

            ProductType.SUBS -> {
                if (it.subsPurchase == null)
                    continue
                startAt = it.subsPurchase.startAt
                expireAt = it.subsPurchase.expireAt
                it.subsPurchase.jsonData to it.subsPurchase.signature
            }

            else -> {
                Log.e(TAG, "unknown sku type $type")
                continue
            }
        }
        val jdo = JSONObject(jsonData)
        val pkgName = jdo.optString("packageName").takeIf { it.isNotBlank() } ?: continue
        val purchaseToken = jdo.optString("purchaseToken").takeIf { it.isNotBlank() } ?: continue
        val purchaseState = jdo.optInt("purchaseState", -1).takeIf { it != -1 } ?: continue
        result.add(
            PurchaseItem(
                type,
                sku,
                pkgName,
                purchaseToken,
                purchaseState,
                jsonData,
                signature,
                startAt,
                expireAt
            )
        )
    }
    return result
}