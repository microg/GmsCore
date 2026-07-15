package org.microg.vending.billing.core

import android.accounts.AccountManager
import android.app.KeyguardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Base64
import android.util.Log
import com.android.vending.makeTimestamp
import org.json.JSONObject
import org.microg.gms.auth.AuthConstants
import org.microg.gms.deviceinfo.DeviceEnvInfo
import org.microg.gms.utils.ExtendedPackageInfo
import org.microg.gms.utils.toBase64
import org.microg.vending.billing.proto.*
import java.io.IOException

private val skuDetailsCache = IAPCacheManager(2048)

private fun dumpAcquireBase64(marker: String, bytes: ByteArray) {
    Log.d("IAPCore", "===== $marker raw base64 (${bytes.size} bytes) BEGIN =====")
    Base64.encodeToString(bytes, 11)
        .chunked(200)
        .forEach { Log.d("IAPCore", "[$marker] $it") }
    Log.d("IAPCore", "===== $marker END =====")
}

class IAPCore(
    private val context: Context,
    private val deviceInfo: DeviceEnvInfo,
    private val clientInfo: ClientInfo,
    private val authData: AuthData
) {
    suspend fun requestAuthProofToken(password: String): String {
        return HttpClient().post(
            GooglePlayApi.URL_AUTH_PROOF_TOKENS,
            headers = HeaderProvider.getBaseHeaders(authData, deviceInfo),
            payload = JSONObject().apply {
                put("credentialType", "password")
                put("credential", password)
            }
        ).getString("encodedRapt")
    }

    suspend fun getSkuDetails(params: GetSkuDetailsParams): GetSkuDetailsResult {
        val builder = SkuDetailsRequest.Builder()
        builder.apply {
            apiVersion = params.apiVersion
            type = params.skuType
            package_ = clientInfo.pkgName
            isWifi = true
            skuPackage = params.skuPkgName
            skuId = params.skuIdList
            val skuDetailsExtraBuilder = SkuDetailsExtra.Builder()
            skuDetailsExtraBuilder.apply {
                version = params.sdkVersion
            }
            skuDetailsExtra = skuDetailsExtraBuilder.build()

            val multiOfferSkuDetailTemp: MutableList<MultiOfferSkuDetail> = mutableListOf()
            params.multiOfferSkuDetail.forEach {
                multiOfferSkuDetailTemp.add(
                    if (it.key == "SKU_SERIALIZED_DOCID_LIST") {
                        val multiOfferSkuDetailBuilder = MultiOfferSkuDetail.Builder()
                        val skuSerializedDocIdList = SkuSerializedDocIds.Builder()
                        val docIdList = params.multiOfferSkuDetail["SKU_SERIALIZED_DOCID_LIST"]
                        if (docIdList != null) {
                            skuSerializedDocIdList.docIds(docIdList as List<String>)
                            multiOfferSkuDetailBuilder.apply {
                                key = it.key
                                skuSerializedDocIds = skuSerializedDocIdList.build()
                            }
                        }
                        multiOfferSkuDetailBuilder.build()
                    } else {
                        when (val value = it.value) {
                            is Boolean -> {
                                val multiOfferSkuDetailBuilder = MultiOfferSkuDetail.Builder()
                                multiOfferSkuDetailBuilder.apply {
                                    key = it.key
                                    bv = value
                                }
                                multiOfferSkuDetailBuilder.build()
                            }

                            is Long -> {
                                val multiOfferSkuDetailBuilder = MultiOfferSkuDetail.Builder()
                                multiOfferSkuDetailBuilder.apply {
                                    key = it.key
                                    iv = value
                                }
                                multiOfferSkuDetailBuilder.build()
                            }

                            is Int -> {
                                val multiOfferSkuDetailBuilder = MultiOfferSkuDetail.Builder()
                                multiOfferSkuDetailBuilder.apply {
                                    key = it.key
                                    iv = value.toLong()
                                }
                                multiOfferSkuDetailBuilder.build()
                            }

                            else -> {
                                val multiOfferSkuDetailBuilder = MultiOfferSkuDetail.Builder()
                                multiOfferSkuDetailBuilder.apply {
                                    key = it.key
                                    sv = value.toString()
                                }
                                multiOfferSkuDetailBuilder.build()
                            }
                        }
                    }
                )
            }
            this.multiOfferSkuDetail = multiOfferSkuDetailTemp
        }
        val skuDetailsRequest = builder.build()
        return try {
            val requestBody = skuDetailsRequest.encode()
            val cacheEntry = skuDetailsCache.get(requestBody)
            if (cacheEntry != null) {
                val getSkuDetailsResult = GetSkuDetailsResult.parseFrom(GoogleApiResponse.ADAPTER.decode(cacheEntry).payload?.skuDetailsResponse)
                if (getSkuDetailsResult.skuDetailsList != null && getSkuDetailsResult.skuDetailsList.isNotEmpty()) {
                    Log.d("IAPCore", "getSkuDetails from cache ")
                    return getSkuDetailsResult
                }
            }
            Log.d("IAPCore", "getSkuDetails: ")
            val response = HttpClient().post(
                GooglePlayApi.URL_SKU_DETAILS,
                headers = HeaderProvider.getDefaultHeaders(authData, deviceInfo),
                payload = skuDetailsRequest,
                adapter = GoogleApiResponse.ADAPTER
            )
            skuDetailsCache.put(requestBody, response.encode())
            GetSkuDetailsResult.parseFrom(response.payload?.skuDetailsResponse)
        } catch (e: Exception) {
            throw RuntimeException("Network request failed. message=${e.message}")
        }
    }

    private fun createAcquireRequest(params: AcquireParams): AcquireRequest {
        val theme = 2

        val skuPackageName = params.buyFlowParams.skuParams["skuPackageName"] ?: clientInfo.pkgName
        val extendedPackageInfo = ExtendedPackageInfo(context, skuPackageName as String)
        val docId = if (params.buyFlowParams.skuSerializedDockIdList?.isNotEmpty() == true) {
            val sDocIdBytes = Base64.decode(params.buyFlowParams.skuSerializedDockIdList[0], Base64.URL_SAFE + Base64.NO_WRAP)
            DocId.ADAPTER.decode(sDocIdBytes)
        } else {
            val docIdBuilder = DocId.Builder()
            docIdBuilder.apply {
                backendDocId =
                    "${params.buyFlowParams.skuType}:$skuPackageName:${params.buyFlowParams.sku}"
                type = getSkuType(params.buyFlowParams.skuType)
                backend = 3
            }
            docIdBuilder.build()
        }
        val documentInfo = DocumentInfo.Builder().apply {
            this.docId = docId
            this.unknown2 = 1
            if (params.buyFlowParams.skuOfferIdTokenList?.isNotEmpty() == true) {
                if (params.buyFlowParams.skuOfferIdTokenList[0].isNotBlank())
                    this.token14 = params.buyFlowParams.skuOfferIdTokenList[0]
            }
        }.build()

        val authFrequency = if (params.buyFlowParams.needAuth) 0 else 3
        return AcquireRequest.Builder().apply {
            this.documentInfo = documentInfo
            this.clientInfo = org.microg.vending.billing.proto.ClientInfo.Builder().apply {
                this.apiVersion = params.buyFlowParams.apiVersion
                this.package_ = this@IAPCore.clientInfo.pkgName
                this.versionCode = this@IAPCore.clientInfo.versionCode
                this.signatureMD5 = this@IAPCore.clientInfo.signatureMD5
                this.skuParamList = mapToSkuParamList(params.buyFlowParams.skuParams)
                this.unknown8 = 1
                this.installerPackage = deviceInfo.gpPkgName
                this.unknown10 = 0
                this.unknown11 = 1
                this.unknown15 = UnkMessage1.Builder().apply {
                    this.unknown1 = UnkMessage2.Builder().apply {
                        this.unknown1 = 1
                    }.build()
                }.build()
                this.versionCode1 = this@IAPCore.clientInfo.versionCode
                if (params.buyFlowParams.oldSkuPurchaseToken?.isNotBlank() == true)
                    this.oldSkuPurchaseToken = params.buyFlowParams.oldSkuPurchaseToken
                if (params.buyFlowParams.oldSkuPurchaseId?.isNotBlank() == true) {
                    this.oldSkuPurchaseToken = null
                    this.oldSkuPurchaseId = params.buyFlowParams.oldSkuPurchaseId
                }
                unKnownMessage21 = UnKnownMessage21.Builder().apply {
                    val pkg = skuPackageName as? String ?: return@apply
                    this.unknown1 = runCatching {
                        context.packageManager
                            .getApplicationInfo(pkg, PackageManager.GET_META_DATA)
                            .metaData
                            ?.getInt("com.android.vending.derived.apk.id", 0)
                            ?.takeIf { it != 0 }
                    }.getOrNull()
                }.build()
                this.skuPackageSignatureSha256 = extendedPackageInfo.firstCertificateSha256?.toBase64(11)
                this.secondaryAccount = AccountNameMessage.Builder().apply {
                    this.accountName = params.buyFlowParams.accountName
                }.build()
            }.build()
            this.clientTokenB64 =
                createClientToken(this@IAPCore.deviceInfo, this@IAPCore.authData)
            this.deviceAuthInfo = DeviceAuthInfo.Builder().apply {
                this.canAuthenticate = true
                this.isBiometricStrong = true
                this.fingerprintValid = true
                this.desiredAuthMethod = 0
                this.unknown5 = 1
                this.lastGaiaAuthTimestamp = System.currentTimeMillis()
                this.unknown9 = true
                this.authFrequency = authFrequency
                this.authParams = mutableMapOf<String, String>().apply {
                    put("prc", "true")
                    put("adca", "true")
                    val km = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
                    if (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            km.isDeviceSecure
                        } else {
                            false
                        }
                    ) put("dle", "true")
                }
                this.unknown20 = false
                this.itemColor = ItemColor.Builder().apply {
                    this.androidAppsColor = -16735885
                    this.booksColor = -11488012
                    this.musicColor = -45771
                    this.moviesColor = -52375
                    this.newsStandColor = -7686920
                }.build()
                this.verificationMethodSelectionMode = 2
                this.allowedGoogleAccounts = AccountManager.get(context).getAccountsByType(AuthConstants.DEFAULT_ACCOUNT_TYPE)
                    .map { account ->
                        AccountNameMessage.Builder().accountName(account.name).build()
                    }
                this.isAccessibilityServiceEnabled = false
                this.isAccessibilityEnabledInConfig = false
                this.hasSeenPurchaseSessionAuthRequirementPrompt = false
                this.isAuthRationalizationFinished = true
            }.build()
            this.newAcquireCacheKey = getAcquireCacheKey(
                this@IAPCore.deviceInfo,
                this@IAPCore.authData.email,
                listOf(
                    CKDocument.Builder().apply {
                        this.docId = docId
                        this.token3 = documentInfo.token3
                        this.token14 = documentInfo.token14
                        this.unknown3 = 1
                    }.build()
                ),
                this@IAPCore.clientInfo.pkgName,
                mapOf(
                    "enablePendingPurchases" to (params.buyFlowParams.skuParams["enablePendingPurchases"]
                        ?: false).toString()
                ),
                authFrequency
            )
            this.nonce = createNonce()
            this.theme = theme
            this.createTimestamp = makeTimestamp(System.currentTimeMillis())
        }.build()
    }

    suspend fun doAcquireRequest(params: AcquireParams): AcquireResult {
        val acquireRequest =
            if (params.lastAcquireResult == null) {
                createAcquireRequest(params)
            } else {
                params.lastAcquireResult!!.acquireRequest.copy().newBuilder().apply {
                    this.serverContextToken =
                        params.lastAcquireResult!!.acquireResponse.serverContextToken
                    this.actionContext = (params.actionContext.toByteStringList())
                    if (params.lastAcquireResult!!.acquireRequest.deviceAuthInfo != null) {
                        this.deviceAuthInfo = params.lastAcquireResult!!.acquireRequest.deviceAuthInfo!!.copy().newBuilder().apply {
                            if (params.droidGuardResult?.isNotBlank() == true) {
                                this.droidGuardPayload = params.droidGuardResult
                            }
                        }.build()
                    }
                    val authTokensTemp = mutableMapOf<String, String>()
                    params.authToken?.let {
                        authTokensTemp["rpt"] = it
                    }
                    params.integratorCallbackData?.let {
                        authTokensTemp["imeicd"] = it
                    }
                    authTokensTemp["spei"] = "false"
                    this.authTokens = authTokensTemp
                    params.securePayload?.let {
                        this.securePayload = it
                    }
                }.build()
            }

        dumpAcquireBase64("acquireRequest", acquireRequest.encode())

        return try {
            val response = HttpClient().post(
                GooglePlayApi.URL_EES_ACQUIRE,
                headers = HeaderProvider.getDefaultHeaders(authData, deviceInfo),
                params = mapOf("theme" to (acquireRequest.theme ?: 2).toString()),
                payload = acquireRequest,
                GoogleApiResponse.ADAPTER
            )
            response.payload?.acquireResponse?.let {
                dumpAcquireBase64("acquireResponse", it.encode())
            }
            AcquireResult.parseFrom(params, acquireRequest, response.payload?.acquireResponse)
        } catch (e: Exception) {
            Log.e("IAPCore", "acquireRequest failed: ${e.message}", e)
            throw RuntimeException("Network request failed. message=${e.message}")
        }
    }

    suspend fun consumePurchase(params: ConsumePurchaseParams): ConsumePurchaseResult {
        val iabx = IABX.Builder().apply {
            this.skuParam = mapToSkuParamList(params.extraParams)
        }.build().encode().toBase64(Base64.URL_SAFE + Base64.NO_WRAP)
        val request = mapOf(
            "pt" to params.purchaseToken,
            "ot" to "1",
            "shpn" to clientInfo.pkgName,
            "iabx" to iabx
        )

        return try {
            val response = HttpClient().post(
                GooglePlayApi.URL_CONSUME_PURCHASE,
                headers = HeaderProvider.getDefaultHeaders(authData, deviceInfo),
                form = request,
                adapter = GoogleApiResponse.ADAPTER
            )
            ConsumePurchaseResult.parseFrom(response.payload?.consumePurchaseResponse)
        } catch (e: Exception) {
            throw RuntimeException("Network request failed. message=${e.message}")
        }
    }

    suspend fun acknowledgePurchase(params: AcknowledgePurchaseParams): AcknowledgePurchaseResult {
        val acknowledgePurchaseRequest = AcknowledgePurchaseRequest.Builder().apply {
            this.purchaseToken = params.purchaseToken
            params.extraParams["developerPayload"]?.let {
                this.developerPayload = it as String
            }
        }.build()

        return try {
            val response = HttpClient().post(
                GooglePlayApi.URL_ACKNOWLEDGE_PURCHASE,
                headers = HeaderProvider.getDefaultHeaders(authData, deviceInfo),
                payload = acknowledgePurchaseRequest,
                adapter = GoogleApiResponse.ADAPTER
            )
            AcknowledgePurchaseResult.parseFrom(response.payload?.acknowledgePurchaseResponse)
        } catch (e: Exception) {
            throw RuntimeException("Network request failed. message=${e.message}")
        }
    }

    suspend fun getPurchaseHistory(params: GetPurchaseHistoryParams): GetPurchaseHistoryResult {
        val reqParams = mutableMapOf(
            "bav" to params.apiVersion.toString(),
            "shpn" to clientInfo.pkgName,
            "iabt" to params.type
        )
        if (!params.continuationToken.isNullOrEmpty()) {
            reqParams["ctntkn"] = params.continuationToken
        }
        if (params.extraParams.isNotEmpty()) {
            reqParams["iabx"] = IABX.Builder().apply {
                this.skuParam = mapToSkuParamList(params.extraParams)
            }.build().encode().toBase64(Base64.URL_SAFE + Base64.NO_WRAP)
        }

        return try {
            val response = HttpClient().get(
                GooglePlayApi.URL_GET_PURCHASE_HISTORY,
                HeaderProvider.getDefaultHeaders(authData, deviceInfo),
                reqParams,
                GoogleApiResponse.ADAPTER
            )
            GetPurchaseHistoryResult.parseFrom(response.payload?.purchaseHistoryResponse)
        } catch (e: IOException) {
            throw RuntimeException("Network request failed. message=${e.message}")
        }
    }
}