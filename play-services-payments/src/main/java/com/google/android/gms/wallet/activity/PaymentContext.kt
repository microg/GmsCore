/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.wallet.activity

import android.accounts.AccountManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.util.Base64
import android.util.Log
import androidx.core.net.toUri
import com.google.android.gms.wallet.OAUTH_SCOPE_SIERRA
import com.google.android.gms.wallet.shared.BuyFlowConfig
import com.google.common.io.BaseEncoding
import com.google.crypto.tink.subtle.EllipticCurves
import com.google.crypto.tink.subtle.EllipticCurves.CurveType
import com.google.crypto.tink.subtle.EllipticCurves.PointFormatType
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.ByteString
import okio.ByteString.Companion.toByteString
import org.json.JSONException
import org.json.JSONObject
import org.microg.gms.checkin.LastCheckinInfo.Companion.read
import org.microg.gms.common.Constants
import org.microg.gms.profile.Build
import org.microg.vending.billing.proto.ClientToken
import org.microg.vending.billing.proto.DataStateInfo
import org.microg.vending.billing.proto.DataValue
import org.microg.vending.billing.proto.FinishActionParams
import org.microg.vending.billing.proto.FunctionalDataExecutionState
import org.microg.vending.billing.proto.IapCommonResponse
import org.microg.vending.billing.proto.InitializeRequest
import org.microg.vending.billing.proto.LayoutModeProto
import org.microg.vending.billing.proto.PageElement
import org.microg.vending.billing.proto.ResultingActionType
import org.microg.vending.billing.proto.SubmitRequest
import org.microg.vending.billing.proto.ValidityState
import java.security.GeneralSecurityException
import java.security.PrivateKey
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class PaymentContext(private val context: Context) {

    companion object {
        private const val TAG = "PaymentContext"
        private const val UI_THEME_BASE64 = "CogCCP3//////////wESHBoKGggSBgj/////DxoOCgIIAhoIEgYIlKbM+A8ox+aLfrq03vAH0wEK0AEI/v//////////ARIKGggaBgoEMgIIAhj+//////////8BKJHpl2aKyb6xBqIBCp0BCP///////////wESdho3GjUKIBoLCAESBwgBFQAAQEIyCwgBEgcIARUAAEBCcgQgAygDIhEKDwjGjpH6DxIHCAEVAACAQBo7CgIIAho1CiAaCwgBEgcIARUAAEBCMgsIARIHCAEVAABAQnIEIAMoAyIRCg8IxY+T/g8SBwgBFQAAgEAY////////////ASjbv9l+2v3L9QcECgIIARABInMIARD9//////////8BIP7//////////wEqIwoLCP7//////////wE4q++/atr6/tMGCwj///////////8BMP///////////wE6KQoYCP///////////wEo7ZPOfuqe8fQHAggCOIa/zn6y+PP0BwQKAhAB"

        private fun dumpBase64(marker: String, bytes: ByteArray) {
            Log.d(TAG, "===== $marker raw base64 (${bytes.size} bytes) BEGIN =====")
            Base64.encodeToString(bytes, Base64.NO_WRAP)
                .chunked(200)
                .forEach { Log.d(TAG, "[$marker] $it") }
            Log.d(TAG, "===== $marker END =====")
        }
    }

    private val _paymentState = MutableStateFlow<PaymentState>(PaymentState.Idle)
    val paymentState: StateFlow<PaymentState> = _paymentState.asStateFlow()

    private var _oauthToken: String = ""
    val oauthToken: String get() = _oauthToken

    private var _clientToken: ClientToken.Info1? = null
    val clientToken: ClientToken.Info1? get() = _clientToken

    private var _ephemeralPrivateKey: ByteArray = ByteArray(0)
    val ephemeralPrivateKey: ByteArray get() = _ephemeralPrivateKey

    // field 290848975 for encryption
    private var _mcReqSessionKey: ByteArray? = null
    val mcReqSessionKey: ByteArray? get() = _mcReqSessionKey

    private var _serverAnalyticsToken: ByteArray = ByteArray(0)
    val serverAnalyticsToken: ByteArray get() = _serverAnalyticsToken

    private val _treeManager = ComponentTreeManager()
    val componentTreeManager: ComponentTreeManager get() = _treeManager

    private val _eventEngine = EventEngine()
    val eventEngine: EventEngine get() = _eventEngine

    // Full snapshot — visibility controlled by the condition tree
    private var _currentPageElements: List<PageElement> = emptyList()
    val currentPageElements: List<PageElement> get() = _currentPageElements

    private var _componentElementMap = mutableMapOf<Long, PageElement>()
    val componentElementMap: Map<Long, PageElement> get() = _componentElementMap

    private val _inputStateFlow = MutableStateFlow<Map<Long, String>>(emptyMap())
    val inputTextStateFlow: StateFlow<Map<Long, String>> get() = _inputStateFlow

    fun getInputText(cid: Long): String {
        val text = _inputStateFlow.value[cid] ?: ""
        Log.d(TAG, "getInputText: cid=$cid, text.len=${text.length}, text=${if (text.length > 20) text.take(20) + "..." else text}")
        return text
    }

    fun updateInputText(cid: Long, text: String) {
        _inputStateFlow.value = _inputStateFlow.value.toMutableMap().apply { put(cid, text) }
        Log.d(TAG, "updateInputText: cid=$cid, text.len=${text.length}")
    }

    private fun refreshCurrentPageElements() {
        _currentPageElements = _componentElementMap.values.toList()
        Log.d(TAG, "refreshCurrentPageElements: ${_currentPageElements.size} elements")
    }

    private val httpClient by lazy { OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .callTimeout(150, TimeUnit.SECONDS)
        .build() }
    
    fun updateState(newState: PaymentState) {
        val detail = when (newState) {
            is PaymentState.Error -> " (${newState.message})"
            is PaymentState.Completed -> " (resultCode=${newState.resultCode})"
            is PaymentState.Submitting -> " Submitting"
            is PaymentState.Submitted -> " Submitted"
            else -> ""
        }
        Log.d(TAG, "State transition: ${_paymentState.value::class.simpleName} -> ${newState::class.simpleName}$detail")
        if (newState is PaymentState.Error && newState.exception != null) {
            Log.w(TAG, "  Error cause:", newState.exception)
        }
        _paymentState.value = newState
    }
    
    /**
     * Initialize the payment flow
     * 1. Get OAuth token
     * 2. Create device env info and client token
     * 3. Send initialize request
     * 4. Return initialize response
     */
    suspend fun initialize(buyFlowConfig: BuyFlowConfig, encryptedParams: ByteArray?, unencryptedParams: ByteArray?): IapCommonResponse? {
        updateState(PaymentState.Initializing)
        try {
            val account = buyFlowConfig.applicationParameters?.buyerAccount
            requireNotNull(account) {
                "unable to initialize widget. buyFlowConfig:$buyFlowConfig account:$account"
            }

            _oauthToken = withContext(Dispatchers.IO) {
                runCatching {
                    AccountManager.get(context).blockingGetAuthToken(account, OAUTH_SCOPE_SIERRA, false)
                }
                    .onFailure { Log.w(TAG, "Failed to acquire OAuth token for ${account.name}", it) }
                    .getOrNull() ?: ""
            }
            if (_oauthToken.isEmpty()) {
                updateState(PaymentState.Error("Failed to get OAuth token"))
                return null
            }
            val deviceEnvInfo = createDeviceEnvInfo(context)
            if (deviceEnvInfo == null) {
                updateState(PaymentState.Error("Failed to create deviceEnvInfo"))
                return null
            }

            val gsfId = try { read(context).androidId.toString(16) } catch (e: Exception) { "1" }
            _clientToken = createClientTokenInfo1(context, deviceEnvInfo, gsfId, false)

            // Build initialize request
            val uiThemeBytes = Base64.decode(UI_THEME_BASE64, Base64.DEFAULT).toByteString()

            val bodyBytes = InitializeRequest.Builder()
                .clientToken(clientToken)
                .uiTheme(uiThemeBytes)
                .encryptedPayload(encryptedParams?.toByteString())
                .unencryptedPayload(unencryptedParams?.toByteString())
                .build()
                .encode()

            dumpBase64("initializeRequest", bodyBytes)

            // Send initialize request
            val response = initializeNetwork(_oauthToken, bodyBytes)

            val responseBytes = response?.body?.bytes()

            if (responseBytes == null) {
                updateState(PaymentState.Error("Initialize request failed - no response"))
                return null
            }

            dumpBase64("initializeResponse", responseBytes)

            // Decode response
            val initializeResponse = try {
                IapCommonResponse.ADAPTER.decode(responseBytes)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to decode response", e)
                updateState(PaymentState.Error("Failed to decode response", e))
                return null
            }

            val oldUnknown2 = clientToken?.unknown2
            val responseClientToken = initializeResponse.clientToken
            val responseUnknown2 = responseClientToken?.unknown2
            if (responseUnknown2 != null) {
                _clientToken = createClientTokenInfo1(context, deviceEnvInfo, gsfId, true).newBuilder()
                    .unknown2(responseUnknown2).build()
            } else {
                Log.w(TAG, "response.clientToken.unknown2 is null - keeping original")
            }

            // Store component tree
            updateComponentTree(initializeResponse, isInitial = true)
            Log.d(TAG, "Component tree root nodeId=${_treeManager.getTree()?.nodeId}")

            updatePartialPageManagers(initializeResponse, isInitial = true)
            refreshCurrentPageElements()
            Log.d(TAG, "componentElementMap initialized with ${_componentElementMap.size} entries")

            return initializeResponse
        } catch (e: Exception) {
            updateState(PaymentState.Error("Initialize failed: ${e.message}", e))
            return null
        }
    }

    private suspend fun initializeNetwork(oauthToken: String, bodyBytes: ByteArray): Response? = withContext(Dispatchers.IO) {
        Log.d(TAG, "initializeNetwork: sending request...")
        Log.d(TAG, "Request body size: ${bodyBytes.size} bytes")

        val mediaType = "application/x-protobuf".toMediaType()
        val requestBody = bodyBytes.toRequestBody(mediaType)

        val request = Request.Builder()
            .url("https://payments-pa.googleapis.com/payments/apis-secure/ui2/purchasemanagerservice/initialize")
            .post(requestBody)
            .header("Content-Type", "application/x-protobuf")
            .header("Authorization", "Bearer $oauthToken")
            .header("X-Modality", "ANDROID_NATIVE")
            .header(
                "User-Agent",
                "${Constants.GMS_PACKAGE_NAME}/${Constants.GMS_VERSION_CODE} " +
                        "(Linux; Android ${Build.VERSION.RELEASE}; ${Build.MODEL} Build/${Build.ID})"
            )
            .build()

        runCatching {
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w(TAG, "HTTP response not successful: ${response.code}, message: ${response.message}")
                throw RuntimeException("HTTP ${response.code}")
            }
            response
        }.onFailure { e ->
            Log.w(TAG, "initializeNetwork failed", e)
        }.getOrNull()
    }

    /**
     * Submit data to Google API
     * @param pageElements The page elements to process
     * @param heqc Deprecated: heqc is now managed internally via _heqcTree
     * @param tokenization Tokenization header data
     * @return The response from Google API
     */
    suspend fun submit(tokenization: List<String>): IapCommonResponse? = withContext(Dispatchers.IO) {
        try {
            val token = _oauthToken

            if (token.isEmpty()) {
                return@withContext null
            }
            if (clientToken == null) {
                return@withContext null
            }

            val processedDataValues = _componentElementMap.values.mapNotNull { it.dataValue }
            Log.d(TAG, "Using componentElementMap with ${_componentElementMap.size} entries, ${processedDataValues.size} dataValues")
            val submitRequest = SubmitRequest.Builder()
                .clientToken(clientToken)
                .dataValue(processedDataValues.sortedWith(compareBy { it.componentId ?: 0 }))
                .heqc(_treeManager.getTree())
                .build()

            val requestBytes = submitRequest.encode()
            dumpBase64("submitRequest", requestBytes)

            // Send submit request
            val response = submitNetwork(token, requestBytes, tokenization)

            val responseBytes = response?.body?.bytes()
            if (responseBytes == null) {
                Log.w(TAG, "responseBytes is null")
                return@withContext null
            }

            dumpBase64("submitResponse", responseBytes)

            // Decode response
            val submitResponse = try {
                IapCommonResponse.ADAPTER.decode(responseBytes)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to decode response", e)
                return@withContext null
            }

            val responseClientToken = submitResponse.clientToken
            val responseUnknown2 = responseClientToken?.unknown2
            if (responseUnknown2 != null) {
                _clientToken = clientToken?.newBuilder()?.unknown2(responseUnknown2)?.build()
            }
            // Update heqc from submit response
            updateComponentTree(submitResponse, isInitial = false)

            updatePartialPageManagers(submitResponse, isInitial = false)
            refreshCurrentPageElements()
            _eventEngine.rebuildGraphs(_componentElementMap)

            submitResponse

        } catch (e: Exception) {
            Log.w(TAG, "EXCEPTION: ${e::class.simpleName}: ${e.message}")
            null
        }
    }

    private suspend fun submitNetwork(
        oauthToken: String,
        bodyBytes: ByteArray,
        tokenization: List<String>
    ): Response? = withContext(Dispatchers.IO) {
        val mediaType = "application/x-protobuf".toMediaType()
        val requestBody = bodyBytes.toRequestBody(mediaType)
        val requestHeader = Request.Builder()
            .url("https://payments-pa.googleapis.com/payments/apis-secure/ui2/purchasemanagerservice/submit")
            .post(requestBody)
            .header("Content-Type", "application/x-protobuf")
            .header("Authorization", "Bearer $oauthToken")
            .header("X-Modality", "ANDROID_NATIVE")
            .header("User-Agent", "${Constants.GMS_PACKAGE_NAME}/${Constants.GMS_VERSION_CODE} (Linux; U; Android ${Build.VERSION.RELEASE}; ${localeToString(Locale.getDefault())}; ${Build.MODEL}; Build/${Build.ID})")
            .header("Priority", "u=1, i")

        if (tokenization.isNotEmpty()) {
            requestHeader.header("ees-s7e-mode", "proto")
            if (tokenization.size % 2 == 0) {
                for (i in tokenization.indices step 2) {
                    requestHeader.header(tokenization[i], tokenization[i + 1])
                }
            }
        } else {
            Log.d(TAG, "[Header] tokenization is empty - no extra headers")
        }

        runCatching {
            val response = httpClient.newCall(requestHeader.build()).execute()
            if (!response.isSuccessful) {
                val errorBody = response.body.string()
                Log.w(TAG, "[Response] HTTP error body = $errorBody")
                null  // body has been consumed; the response is no longer returned to the caller
            } else {
                response
            }
        }.onFailure { e ->
            Log.w(TAG, "submitNetwork failed", e)
        }.getOrNull()
    }

    /**
     * Process PageElement list - handle ECDH key generation, encryption, etc.
     */
    private fun processDataValueList(list: List<PageElement>): List<DataValue> {
        val result = ArrayList<DataValue>()

        for ((index, item) in list.withIndex()) {
            val extensionValue = item.extensionFieldNumber ?: continue
            val oldDataValue = item.dataValue ?: continue

            var dataValueBuilder = oldDataValue.newBuilder()
            val dataStateInfoBuilder = oldDataValue.dataState?.newBuilder() ?: DataStateInfo.Builder()

            Log.d(TAG, ">>> processDataValueList extensionValue:${extensionValue}")
            when (extensionValue) {
                217440216 -> {
                    dataStateInfoBuilder.dataRole(1)
                        .validityState(ValidityState.VALIDITY_VALID)
                    dataValueBuilder.dataState(dataStateInfoBuilder.build())
                }

                223344552 -> {
                    dataStateInfoBuilder.dataRole(1)
                        .validityState(ValidityState.VALIDITY_VALID)
                    dataValueBuilder.dataState(dataStateInfoBuilder.build())
                }

                223344553 -> {
                    dataStateInfoBuilder.dataRole(1)
                        .validityState(ValidityState.VALIDITY_VALID)
                        .executionState(FunctionalDataExecutionState.FUNCTIONAL_DATA_EXECUTION_STATE_NOT_STARTED)

                    val existingCondValueExt = oldDataValue.conditionValueExt
                    if (existingCondValueExt != null) {
                        dataValueBuilder.conditionValueExt(existingCondValueExt)
                    }
                    dataValueBuilder.dataState(dataStateInfoBuilder.build())
                    Log.d(TAG, "  [$index] 223344553 (Conditional Container) - id=${oldDataValue.componentId}, conditionValueExt.conditionValue=${existingCondValueExt?.conditionValue}")
                }

                223344555 -> {
                    dataStateInfoBuilder.dataRole(1).validityState(ValidityState.VALIDITY_VALID)
                    dataValueBuilder.dataState(dataStateInfoBuilder.build())
                    Log.d(TAG, "  [$index] 223344555 (Generic Container)")
                }

                232057536 -> {
                    dataStateInfoBuilder.dataRole(1)
                        .validityState(ValidityState.VALIDITY_VALID)
                        .executionState(FunctionalDataExecutionState.FUNCTIONAL_DATA_EXECUTION_STATE_NOT_STARTED)
                    val existMessage = dataValueBuilder.message204201689 ?: DataValue.Message204201689()
                    val existMessageText = existMessage.text ?: ""
                    dataValueBuilder.dataState(dataStateInfoBuilder.build())
                        .message204201689(existMessage.newBuilder().text(existMessageText).unknown2(0).build())
                }

                233780159 -> {
                    dataStateInfoBuilder.dataRole(1)
                        .validityState(ValidityState.VALIDITY_VALID)
                        .executionState(FunctionalDataExecutionState.FUNCTIONAL_DATA_EXECUTION_STATE_RUNNING)

                    dataValueBuilder
                        .dataState(dataStateInfoBuilder.build())
                        .unknown1(DataValue.Message239872231.Builder().unknown2(3).build())
                    Log.d(TAG, "  [$index] 233780159 (Card Container)")
                }

                264984587 -> {
                    dataStateInfoBuilder.dataRole(1)
                        .validityState(ValidityState.VALIDITY_VALID)
                        .executionState(FunctionalDataExecutionState.FUNCTIONAL_DATA_EXECUTION_STATE_NOT_STARTED)
                    dataValueBuilder.dataState(dataStateInfoBuilder.build())
                    Log.d(TAG, "  [$index] 264984587 (Spacing)")
                }

                265527174 -> {
                    dataStateInfoBuilder.executionState(FunctionalDataExecutionState.FUNCTIONAL_DATA_EXECUTION_STATE_NOT_STARTED)
                    dataValueBuilder.dataState(dataStateInfoBuilder.build())

                    val animated = DataValue.AnimatedImageDataValueExtension.Builder()
                        .state(DataValue.AnimatedImageState.ANIMATED_IMAGE_STATE_NOT_STARTED)
                        .progress(0)
                        .build()
                    dataValueBuilder.animatedImageDataValueExtension(animated)
                    Log.d(TAG, "  [$index] 265527174 (Animated Image)")
                }

                217437962 -> {
                    val cid = oldDataValue.componentId ?: 0L
                    val currentText = _inputStateFlow.value[cid] ?: ""
                    val submitConfig = oldDataValue.submitConfig ?: 0
                    dataStateInfoBuilder.dataRole(1)
                        .validityState(ValidityState.VALIDITY_VALID)
                        .executionState(
                            if (currentText.isEmpty())
                                FunctionalDataExecutionState.FUNCTIONAL_DATA_EXECUTION_STATE_NOT_STARTED
                            else
                                FunctionalDataExecutionState.FUNCTIONAL_DATA_EXECUTION_STATE_COMPLETED
                        )
                    dataValueBuilder.dataState(dataStateInfoBuilder.build())

                    if (submitConfig == 1) {
                        dataValueBuilder.textInputDataValueExtension(
                            DataValue.TextInputDataValueExtension.Builder().text(currentText).build()
                        )
                    }
                    Log.d(TAG, "  [$index] 217437962 (TextInput) - cid=$cid, submitConfig=$submitConfig, text.len=${currentText.length}")
                }

                228971049 -> {
                    dataStateInfoBuilder.dataRole(1).validityState(ValidityState.VALIDITY_VALID)
                    dataValueBuilder.dataState(dataStateInfoBuilder.build())
                    Log.d(TAG, "  [$index] 228971049 (TemplateText) - cid=${oldDataValue.componentId}")
                }

                232946268 -> {
                    // SMS auto-reader — functionality not yet implemented
                    dataStateInfoBuilder.dataRole(1).validityState(ValidityState.VALIDITY_VALID)
                        .executionState(FunctionalDataExecutionState.FUNCTIONAL_DATA_EXECUTION_STATE_NOT_STARTED)
                    dataValueBuilder.dataState(dataStateInfoBuilder.build())
                    Log.d(TAG, "  [$index] 232946268 (SmsAutoReader) - cid=${oldDataValue.componentId}, not wired")
                }

                290848973 -> {
                    try {
                        val keyPair = EllipticCurves.generateKeyPair(CurveType.NIST_P256)
                        val publicKey = keyPair.public as ECPublicKey
                        val publicKeyHex = EllipticCurves.pointEncode(publicKey.params.curve, PointFormatType.UNCOMPRESSED, publicKey.w)

                        dataStateInfoBuilder.dataRole(1)
                            .validityState(ValidityState.VALIDITY_VALID)
                            .executionState(FunctionalDataExecutionState.FUNCTIONAL_DATA_EXECUTION_STATE_COMPLETED)

                        val ecPub = DataValue.EphemeralECPublicKey.Builder()
                            .ecdhPublicKey(BaseEncoding.base64().encode(publicKeyHex))
                            .build()

                        dataValueBuilder = DataValue.Builder()
                            .componentId(oldDataValue.componentId)
                            .unknown2(ByteString.EMPTY)
                            .dataState(dataStateInfoBuilder.build())
                            .unknown5(oldDataValue.unknown5)
                            .ephemeralECPublicKey(ecPub)

                        _ephemeralPrivateKey = (keyPair.private as ECPrivateKey).s.toByteArray()

                        Log.d(TAG, "  [$index] 290848973 (ECDH Key Gen) - publicKeyHex length=${publicKeyHex.size}")

                        val cid = oldDataValue.componentId ?: 0L
                        _eventEngine.onComponentCompleted(cid)
                    } catch (e: GeneralSecurityException) {
                        throw IllegalStateException("Error generating ephemeral key pair.", e)
                    }
                }

                290848974 -> {
                    Log.d(TAG, "  [$index] 290848974 (ECDH Key Agreement)")
                    val context = item.ecdhKeyAgreementContext
                    if (!ephemeralPrivateKey.isEmpty() && context != null) {
                        try {
                            val decodePublicKeyBytes = BaseEncoding.base64().decode(context.ecdhPublicKey!!)

                            val publicKeyEcPoint = EllipticCurves.pointDecode(CurveType.NIST_P256, PointFormatType.UNCOMPRESSED, decodePublicKeyBytes)
                            val decodePrivateKey: PrivateKey = EllipticCurves.getEcPrivateKey(CurveType.NIST_P256, ephemeralPrivateKey)
                            val sharedSecret = EllipticCurves.computeSharedSecret(decodePrivateKey as ECPrivateKey, publicKeyEcPoint)

                            val agreementPartyVInfo = BaseEncoding.base64().decode(context.agreementPartyVInfo!!)
                            val a = concatenateByteArrays(
                                byteArrayOf(0, 0, 0, 1), sharedSecret,
                                byteArrayOf(0, 0, 0, 0),
                                byteArrayOf(0, 0, 0, 0), intTo4BytesBE(agreementPartyVInfo.size), agreementPartyVInfo,
                                byteArrayOf(0, 0, 1, 0)
                            ).toByteString().sha256().toByteArray()

                            val cReqSessionKey = ByteArray(16)
                            val cResSessionKey = ByteArray(16)
                            System.arraycopy(a, 0, cReqSessionKey, 0, 16)
                            System.arraycopy(a, 16, cResSessionKey, 0, 16)

                            _mcReqSessionKey = cReqSessionKey

                            dataStateInfoBuilder.dataRole(1)
                                .validityState(ValidityState.VALIDITY_VALID)
                                .executionState(FunctionalDataExecutionState.FUNCTIONAL_DATA_EXECUTION_STATE_COMPLETED)

                            dataValueBuilder.dataState(dataStateInfoBuilder.build())
                                .sessionKeyExtension(DataValue.SessionKeyExtension.Builder()
                                    .reqSessionKey(BaseEncoding.base64().encode(cResSessionKey)).build()
                                )
                            Log.d(TAG, "  [$index] 290848974 (ECDH Key Agreement) - sessionKey generated")

                            val cid = oldDataValue.componentId ?: 0L
                            val actions = _eventEngine.onComponentCompleted(cid)
                            Log.d(TAG, "  [$index] 290848974 event engine results: $actions")
                        } catch (e: GeneralSecurityException) {
                            Log.w(TAG, "    ERROR - GeneralSecurityException: ${e.message}")
                            throw IllegalStateException("Error computing shared keys.", e)
                        }
                    }
                }

                290848975 -> {
                    val componentId = oldDataValue.componentId ?: 0L
                    val engineState = _eventEngine.getExecutionState(componentId)
                    val shouldEncrypt = engineState == FunctionalDataExecutionState.FUNCTIONAL_DATA_EXECUTION_STATE_RUNNING ||
                        engineState == FunctionalDataExecutionState.FUNCTIONAL_DATA_EXECUTION_STATE_COMPLETED
                    Log.d(TAG, "  [$index] 290848975 (AES) - id=$componentId, engineState=$engineState, shouldEncrypt=$shouldEncrypt")

                    if (shouldEncrypt) {
                        val ext = item.encryptionActionExtension ?: PageElement.EncryptionActionExtension()

                        val plainText = resolvePlainText(ext)
                        when {
                            ext.fieldNumber != null ->
                                Log.d(TAG, "    [$index] AES plainText from cid=${ext.fieldNumber} reference, text.len=${plainText.size}")
                            ext.displayText == null ->
                                Log.w(TAG, "    [$index] AES plainText source missing — encrypting empty")
                        }
                        val iv = BaseEncoding.base64().decode(ext.initializationVector ?: "")
                        val encryptionValue = try {
                            encryptJwe(ext.keyId, iv, plainText)
                        } catch (e: GeneralSecurityException) {
                            throw IllegalArgumentException("Error performing A128GCM encryption.", e)
                        } catch (e: JSONException) {
                            throw IllegalArgumentException("Error creating JWE protected header.", e)
                        }

                        dataStateInfoBuilder.dataRole(1)
                            .validityState(ValidityState.VALIDITY_VALID)
                            .executionState(FunctionalDataExecutionState.FUNCTIONAL_DATA_EXECUTION_STATE_COMPLETED)

                        dataValueBuilder.dataState(dataStateInfoBuilder.build())
                            .encryptionActionExtension(DataValue.EncryptionActionExtension.Builder()
                                .encryptionValue(encryptionValue).build()
                            )

                        _eventEngine.setExecutionState(componentId, FunctionalDataExecutionState.FUNCTIONAL_DATA_EXECUTION_STATE_COMPLETED)
                        val broadcastResults = _eventEngine.onComponentCompleted(componentId)
                        Log.d(TAG, "  [$index] 290848975 completed, broadcast results: $broadcastResults")
                    } else {
                        dataStateInfoBuilder.dataRole(1)
                            .validityState(ValidityState.VALIDITY_VALID)
                            .executionState(FunctionalDataExecutionState.FUNCTIONAL_DATA_EXECUTION_STATE_NOT_STARTED)

                        dataValueBuilder.dataState(dataStateInfoBuilder.build())
                            .encryptionActionExtension(DataValue.EncryptionActionExtension.Builder()
                                .encryptionValue("").build()
                            )
                    }
                }

                else -> {
                    Log.w(TAG, "  [$index] Unhandled extensionFieldNumber=$extensionValue, componentId=${oldDataValue.componentId}")
                }
            }

            result.add(dataValueBuilder.build())
        }

        return result
    }

    /**
     * Updates PageManager data (initialization or incremental update)
     * init response: extract PageElement → processDataValueList → store into map
     * submit response: toRemove → delete; toAddOrReplaceData → add → processDataValueList → store into map
     * Performs AES encryption on the 290848975 component for the specified componentId, updating componentElementMap
     * Called by PaymentController.triggerButtonAction() after the event engine has been triggered
     */
    fun executeEncryptionForComponent(componentId: Long) {
        val pe = _componentElementMap[componentId]
        if (pe == null || pe.extensionFieldNumber != 290848975) {
            Log.w(TAG, "executeEncryptionForComponent: id=$componentId not found or not 290848975")
            return
        }
        val oldDv = pe.dataValue ?: return

        // Perform AES encryption
        val ext = pe.encryptionActionExtension ?: PageElement.EncryptionActionExtension()

        val plainText = resolvePlainText(ext)
        ext.fieldNumber?.let { sourceCid ->
            val text = String(plainText)
            Log.d(TAG, "executeEncryptionForComponent: id=$componentId fieldNumber=$sourceCid, inputText.len=${plainText.size}, inputText=${if (text.length > 50) text.take(50) + "..." else text}")

            if (sourceCid == 31L && text.isNotEmpty()) {
                Log.d(TAG, "executeEncryptionForComponent: id=$componentId CReq template will be formatted with inputText")
            }
        }

        if (plainText.isEmpty()) {
            Log.w(TAG, "executeEncryptionForComponent: id=$componentId skipped — plainText empty")
            return
        }
        if (_mcReqSessionKey?.size != 16) {
            Log.w(TAG, "executeEncryptionForComponent: invalid session key")
            return
        }

        val iv = BaseEncoding.base64().decode(ext.initializationVector ?: "")
        val encryptionValue = encryptJwe(ext.keyId, iv, plainText)

        val dataStateInfoBuilder = (oldDv.dataState?.newBuilder() ?: DataStateInfo.Builder())
            .dataRole(1)
            .validityState(ValidityState.VALIDITY_VALID)
            .executionState(FunctionalDataExecutionState.FUNCTIONAL_DATA_EXECUTION_STATE_COMPLETED)

        val updatedDv = oldDv.newBuilder()
            .dataState(dataStateInfoBuilder.build())
            .encryptionActionExtension(DataValue.EncryptionActionExtension.Builder()
                .encryptionValue(encryptionValue).build()
            )
            .build()

        _componentElementMap[componentId] = pe.newBuilder().dataValue(updatedDv).build()

        _eventEngine.setExecutionState(componentId, FunctionalDataExecutionState.FUNCTIONAL_DATA_EXECUTION_STATE_COMPLETED)
        val broadcastResults = _eventEngine.onComponentCompleted(componentId)

        Log.d(TAG, "executeEncryptionForComponent: id=$componentId encrypted, JWE length=${encryptionValue.length}, broadcast=$broadcastResults")
    }

    private fun updatePartialPageManagers(
        response: IapCommonResponse,
        isInitial: Boolean
    ): List<DataValue> {
        if (isInitial) {

            val pageElements = response.responseBody?.initializePartialPageProtoWrapper?.partialPage?.pageElements ?: emptyList()

            // Temporarily write into the map first, then build the event engine index, to ensure the event chain in processDataValueList can work
            pageElements.forEach { pe ->
                val cid = pe.dataValue?.componentId ?: return@forEach
                _componentElementMap[cid] = pe
            }
            _eventEngine.rebuildGraphs(_componentElementMap)

            val processedDataValues = processDataValueList(pageElements)

            processedDataValues.forEach { processedDataValue ->
                val componentId = processedDataValue.componentId ?: return@forEach
                val originalPageElement = pageElements.find { it.dataValue?.componentId == componentId } ?: return@forEach
                _componentElementMap[componentId] = originalPageElement.newBuilder()
                    .dataValue(processedDataValue)
                    .build()
            }
            _eventEngine.rebuildGraphs(_componentElementMap)

            return processedDataValues
        } else {
            // submit response
            val updateProto = response.responseBody?.updatePartialPageProtoWrapper?.updatePartialPageProto

            val toRemoveIds = updateProto?.toRemove ?: emptyList()
            toRemoveIds.forEach { componentId ->
                _componentElementMap.remove(componentId)
            }

            // toAddOrReplaceData
            val toAddOrReplace = updateProto?.toAddOrReplaceData ?: emptyList()

            toAddOrReplace.forEach { pe ->
                val cid = pe.dataValue?.componentId ?: return@forEach
                _componentElementMap[cid] = pe
            }
            _eventEngine.rebuildGraphs(_componentElementMap)

            val processedDataValues = processDataValueList(toAddOrReplace)
            processedDataValues.forEach { processedDataValue ->
                val componentId = processedDataValue.componentId ?: return@forEach
                val originalPageElement = toAddOrReplace.find { it.dataValue?.componentId == componentId } ?: return@forEach
                _componentElementMap[componentId] = originalPageElement.newBuilder()
                    .dataValue(processedDataValue)
                    .build()
            }

            val toReplace = updateProto?.toReplaceDataValue ?: emptyList()
            toReplace.forEach { newDataValue ->
                val componentId = newDataValue.componentId ?: return@forEach
                val existing = _componentElementMap[componentId]
                if (existing != null) {
                    _componentElementMap[componentId] = existing.newBuilder()
                        .dataValue(newDataValue).build()
                }
            }

            val toReplacePreserving = updateProto?.toReplaceDataValuePreservingExtension ?: emptyList()
            toReplacePreserving.forEach { newDataValue ->
                val componentId = newDataValue.componentId ?: return@forEach
                val existing = _componentElementMap[componentId]
                if (existing != null) {
                    val oldDv = existing.dataValue
                    val mergedDataValue = newDataValue.newBuilder()
                        .ephemeralECPublicKey(oldDv?.ephemeralECPublicKey ?: newDataValue.ephemeralECPublicKey)
                        .sessionKeyExtension(oldDv?.sessionKeyExtension ?: newDataValue.sessionKeyExtension)
                        .encryptionActionExtension(oldDv?.encryptionActionExtension ?: newDataValue.encryptionActionExtension)
                        .animatedImageDataValueExtension(oldDv?.animatedImageDataValueExtension ?: newDataValue.animatedImageDataValueExtension)
                        .build()
                    _componentElementMap[componentId] = existing.newBuilder()
                        .dataValue(mergedDataValue).build()
                }
            }

            return _componentElementMap.values.mapNotNull { it.dataValue }
        }
    }

    /**
     * - init response: uses initializePartialPageProtoWrapper.partialPage.componentTree
     * - submit response: uses updatePartialPageProtoWrapper.updatePartialPageProto.treeFragments
     */
    private fun updateComponentTree(response: IapCommonResponse, isInitial: Boolean) {
        if (isInitial) {
            // initResponse: Store the complete component tree
            val tree = response.responseBody?.initializePartialPageProtoWrapper?.partialPage?.componentTree
            _treeManager.initTree(tree)
        } else {
            // submitResponse: Merge subtree fragments into the current tree
            val fragments = response.responseBody?.updatePartialPageProtoWrapper?.updatePartialPageProto?.treeFragments ?: emptyList()
            _treeManager.mergeFragments(fragments)
        }
    }

    /**
     * Concatenate multiple byte arrays
     */
    private fun concatenateByteArrays(vararg arrays: ByteArray): ByteArray {
        val totalLength = arrays.sumOf { it.size }
        val result = ByteArray(totalLength)
        var offset = 0
        for (arr in arrays) {
            System.arraycopy(arr, 0, result, offset, arr.size)
            offset += arr.size
        }
        return result
    }
    
    /**
     * Convert int to 4 bytes (big-endian)
     */
    private fun intTo4BytesBE(value: Int): ByteArray {
        return byteArrayOf(
            ((value shr 24) and 0xFF).toByte(),
            ((value shr 16) and 0xFF).toByte(),
            ((value shr 8) and 0xFF).toByte(),
            (value and 0xFF).toByte()
        )
    }

    private fun resolvePlainText(ext: PageElement.EncryptionActionExtension): ByteArray {
        if (ext.displayText != null) return ext.displayText!!.toByteArray()
        val refCid = ext.fieldNumber ?: return ByteArray(0)
        return resolveStringValue(refCid).toByteArray()
    }

    private fun resolveStringValue(cid: Long, visited: MutableSet<Long> = mutableSetOf()): String {
        if (cid in visited) {
            return ""
        }
        visited.add(cid)

        val pe = _componentElementMap[cid] ?: return ""
        return when (pe.extensionFieldNumber) {
            217437962 -> _inputStateFlow.value[cid] ?: ""
            223344552 -> pe.textInfoDataExtension?.text
                ?: pe.textInfoDataExtension?.displayText?.text
                ?: ""
            232057536 -> pe.dataValue?.message204201689?.text
                ?: pe.message232057536?.messageExtension?.unknown1
                ?: ""
            228971049 -> {
                val tpl = pe.templateTextExtension ?: return ""
                val template = tpl.formatTemplate ?: return ""
                val args: Array<Any> = tpl.childComponentIds
                    .map { resolveStringValue(it, visited) }
                    .toTypedArray()
                String.format(template, *args)
            }
            217440216 -> ""
            else -> {
                Log.w(TAG, "resolveStringValue: unhandled cid=$cid ext=${pe.extensionFieldNumber}")
                ""
            }
        }
    }

    /**
     * AES-128-GCM + JWE Compact Serialization with the current session key.
     * Format: `<base64UrlHeader>..<base64UrlIv>.<base64UrlPayload>.<base64UrlTag>`
     * where header = `{"alg":"dir","kid":<keyId>,"enc":"A128GCM"}`.
     *
     * @throws IllegalArgumentException if [_mcReqSessionKey] is null or not 128-bit
     * @throws GeneralSecurityException on cipher init / doFinal failure
     * @throws JSONException on header construction failure (rare)
     */
    private fun encryptJwe(keyId: String?, iv: ByteArray, plainText: ByteArray): String {
        val keyBytes = requireNotNull(_mcReqSessionKey) {
            "AES encryption key (_mcReqSessionKey) is null - ECDH key agreement must run first"
        }
        require(keyBytes.size == 16) {
            "Invalid key size ${keyBytes.size * 8}; only 128-bit AES keys are supported"
        }

        val header = JSONObject().apply {
            put("alg", "dir")
            put("kid", keyId)
            put("enc", "A128GCM")
        }
        val jweHeader = BaseEncoding.base64Url().encode(header.toString().toByteArray())

        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE, SecretKeySpec(keyBytes, "AES"), IvParameterSpec(iv))
            updateAAD(jweHeader.toByteArray())
        }
        val ciphertext = cipher.doFinal(plainText)
        val payload = ciphertext.copyOf(ciphertext.size - 16)
        val tag = ciphertext.copyOfRange(ciphertext.size - 16, ciphertext.size)

        return String.format(
            Locale.US, "%s..%s.%s.%s",
            jweHeader,
            BaseEncoding.base64Url().encode(iv),
            BaseEncoding.base64Url().encode(payload),
            BaseEncoding.base64Url().encode(tag)
        )
    }

    fun detectDirectFinish(): FinishActionParams? {
        for ((_, pe) in _componentElementMap) {
            if (pe.extensionFieldNumber != 233780159) continue
            val infraDataExt = pe.infrastructureDataExtension ?: continue
            for (entry in infraDataExt.extension) {
                val inner = entry.infrastructureAction ?: continue
                when (val actionType = inner.resultActionType) {
                    ResultingActionType.RESULTING_ACTION_TYPE_FINISH -> {
                        val finishParams = inner.finishParams
                        Log.d(TAG, "detectDirectFinish: FINISH in componentId=${pe.dataValue?.componentId}, resultCode=${finishParams?.resultCode}, hasIntegratorData=${finishParams?.integratorCallbackData != null}")
                        return finishParams
                    }
                    ResultingActionType.RESULTING_ACTION_TYPE_LOAD_URL -> {
                        context.startActivity(Intent(Intent.ACTION_VIEW, inner.urlWrapper?.url?.url?.toUri()))
                    }
                    ResultingActionType.RESULTING_ACTION_TYPE_SUBMIT,
                    ResultingActionType.RESULTING_ACTION_TYPE_ANNOUNCE_FOR_ACCESSIBILITY -> {

                    }

                    ResultingActionType.RESULTING_ACTION_TYPE_COPY_TO_CLIPBOARD -> {
                        inner.copyToClipboard?.let {
                            val clipData = ClipData.newPlainText("", it.text)
                            val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            if (clipboardManager != null) {
                                clipboardManager.setPrimaryClip(clipData)
                            }
                        }
                    }
                    ResultingActionType.RESULTING_ACTION_TYPE_TRIGGER_FULL_SCREEN_SPINNER,
                    ResultingActionType.RESULTING_ACTION_TYPE_SEND_PAYMENT_EVENT_CALLBACK_DATA,
                    ResultingActionType.RESULTING_ACTION_TYPE_FINISH_WITH_REDIRECT -> {
                        Log.d(TAG, "Unsupported infrastructure resulting action type=${actionType}")
                    }
                    else -> {
                        Log.w(TAG, "detectDirectFinish: unhandled actionType=${actionType}, componentId=${pe.dataValue?.componentId}")
                    }
                }
            }
        }
        return null
    }

    private val _conditionValues = mutableMapOf<Long, Int>()

    fun getComponentVisibility(): Pair<Set<Long>, Set<Long>> {
        val managed = mutableSetOf<Long>()
        val visible = mutableSetOf<Long>()
        for ((_, pe) in _componentElementMap) {
            if (pe.extensionFieldNumber != 223344553) continue
            val options = pe.verticalContainerExtension?.options ?: continue
            val currentValue = pe.dataValue?.conditionValueExt?.conditionValue ?: 0
            for (option in options) {
                managed.addAll(option.children)
            }
            val matched = options.firstOrNull { it.conditionValue == currentValue }
            if (matched != null) {
                visible.addAll(matched.children)
            }
        }
        return managed to visible
    }

    /**
     * Dynamically update the conditionValue of the condition container after a button click to trigger a UI branch switch
     */
    fun updateConditionValue(componentId: Long, newValue: Int) {
        val pe = _componentElementMap[componentId] ?: run {
            Log.w(TAG, "updateConditionValue: componentId=$componentId not found")
            return
        }
        if (pe.extensionFieldNumber != 223344553) {
            Log.w(TAG, "updateConditionValue: componentId=$componentId is not a conditional container (ext=${pe.extensionFieldNumber})")
            return
        }
        val dv = pe.dataValue ?: return
        val oldCondValueExt = dv.conditionValueExt ?: DataValue.ConditionValueExtension()
        val updatedDv = dv.newBuilder()
            .conditionValueExt(oldCondValueExt.newBuilder().conditionValue(newValue).build())
            .build()
        _componentElementMap[componentId] = pe.newBuilder().dataValue(updatedDv).build()
        Log.d(TAG, "updateConditionValue: componentId=$componentId → condValue=$newValue")
        refreshCurrentPageElements()
    }

    /**
     * 1=RELATIVE(Box), 2=FLEX(Column)
     */
    fun getLayoutModes(): Map<Long, LayoutModeProto> = _treeManager.getLayoutModes()

    /**
     * Get the state of the AnimatedImage
     * 2=RUNNING → show; otherwise → hide
     */
    fun getAnimatedImageState(componentId: Long): Int = _eventEngine.getAnimatedImageState(componentId)

    fun reset() {
        _paymentState.value = PaymentState.Idle
        _oauthToken = ""
        _clientToken = null
        _ephemeralPrivateKey = ByteArray(0)
        _treeManager.reset()
        _eventEngine.reset()
        _conditionValues.clear()
        _currentPageElements = emptyList()
        _componentElementMap = mutableMapOf()
    }

    @Volatile private var resourcesClosed = false

    @OptIn(DelicateCoroutinesApi::class)
    fun closeResources() {
        if (resourcesClosed) return
        resourcesClosed = true
        val client = httpClient
        GlobalScope.launch(Dispatchers.IO) {
            try {
                client.dispatcher.executorService.shutdownNow()
                client.connectionPool.evictAll()
            } catch (e: Exception) {
                Log.w(TAG, "closeResources: ", e)
            }
        }
    }
}
