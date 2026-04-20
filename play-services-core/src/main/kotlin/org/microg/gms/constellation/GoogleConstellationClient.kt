/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation

import android.content.Context
import android.accounts.AccountManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import org.microg.gms.checkin.LastCheckinInfo
import org.microg.gms.gcm.RegisterRequest
import org.microg.gms.gcm.RegisterResponse
import android.telephony.TelephonyManager
import android.util.Log
import com.google.android.gms.BuildConfig
import org.microg.gms.common.Constants
import kotlinx.coroutines.runBlocking
import okio.ByteString
import org.microg.gms.common.PackageUtils
import org.microg.gms.auth.AuthConstants
import org.microg.gms.auth.AuthManager
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import google.internal.communications.phonedeviceverification.v1.ClientInfo
import google.internal.communications.phonedeviceverification.v1.CountryInfo
import google.internal.communications.phonedeviceverification.v1.DeviceId
import google.internal.communications.phonedeviceverification.v1.SIMAssociation
import google.internal.communications.phonedeviceverification.v1.StringId
import google.internal.communications.phonedeviceverification.v1.IMSIRequest
import google.internal.communications.phonedeviceverification.v1.Param
import google.internal.communications.phonedeviceverification.v1.SyncRequest
import google.internal.communications.phonedeviceverification.v1.Verification
import google.internal.communications.phonedeviceverification.v1.VerificationMethodInfo
import google.internal.communications.phonedeviceverification.v1.VerificationMethodData
import google.internal.communications.phonedeviceverification.v1.TelephonyInfo
import google.internal.communications.phonedeviceverification.v1.TelephonyInfoContainer
import google.internal.communications.phonedeviceverification.v1.ClientCredentials
import google.internal.communications.phonedeviceverification.v1.CredentialMetadata
import google.internal.communications.phonedeviceverification.v1.CarrierInfo
import google.internal.communications.phonedeviceverification.v1.IdTokenRequest
import com.google.android.gms.constellation.VerifyPhoneNumberRequest as AidlVerifyPhoneNumberRequest

class GoogleConstellationClient(private val context: Context) {
    private data class ConstellationKeyMaterial(
        val publicKeyBytes: ByteString,
        val privateKey: java.security.PrivateKey?,
        val isPublicKeyAcked: Boolean
    )

    private data class ResolvedPhoneIdentity(
        val imsi: String,
        val msisdn: String,
        val phoneNumber: String
    )

    private data class ResolvedDeviceIdentity(
        val deviceAndroidId: Long,
        val userAndroidId: Long
    )

    private data class ResolvedIdTokenCarrierInfo(
        val idTokenCertificateHash: String,
        val idTokenNonce: String,
        val idTokenCallingPackage: String,
        val carrierInfo: CarrierInfo
    )

    private data class ConstellationCallContext(
        val rpc: ConstellationRpcClient,
        val keyPrefs: android.content.SharedPreferences,
        val iidToken: String,
        val sessionId: String,
        val subId: Int,
        val imsi: String,
        val phoneNumber: String,
        val deviceAndroidId: Long,
        val userAndroidId: Long,
        val registeredAppIds: List<StringId>,
        val params: List<Param>,
        val protoCtx: RequestProtoContext,
        val gpnvRequestContext: GpnvRequestContext,
        val syncRequest: SyncRequest,
        val proceedClientCredentials: ClientCredentials?,
    )

    companion object {
        private const val TAG = "GmsConstellationClient"
        private const val API_KEY = "AIzaSyAP-gfH3qvi6vgHZbSYwQ_XHqV_mXHhzIk"
        private const val GAIA_TOKEN_SCOPE = "oauth2:https://www.googleapis.com/auth/numberer"

        private const val FORCE_ONE_TIME_VERIFICATION_KEY = "microg_constellation_force_one_time_verification"


        @JvmStatic
        @JvmOverloads
        fun getOrRegisterIidToken(
            context: Context,
            packageName: String,
            senderId: String = ConstellationConstants.SENDER_CONSTELLATION
        ): Pair<String, String> {
            val prefs = context.getSharedPreferences(ConstellationConstants.PREFS_CONSTELLATION_IID, Context.MODE_PRIVATE)

            val hasKeyPair = prefs.getString("key_private", null) != null
            val cachedToken = prefs.getString("iid_token_$senderId", null)
            val cachedSource = prefs.getString("iid_source_$senderId", null)
            val isSeededFromStock = cachedSource?.startsWith("seeded-from-stock") == true
            if (cachedToken != null && (hasKeyPair || isSeededFromStock)) {
                val reason = if (isSeededFromStock) "seeded-from-stock (skip pub2/sig check)" else "cached-$senderId"
                Log.d(TAG, "Using cached IID token for sender=$senderId ($reason)")
                return Pair(cachedToken, cachedSource ?: "cached-$senderId")
            }
            if (cachedToken != null && !hasKeyPair) {
                Log.w(TAG, "Invalidating cached token (registered without pub2/sig)")
                prefs.edit().remove("iid_token_$senderId").remove("iid_source_$senderId").apply()
            }

            val appIdPrefs = context.getSharedPreferences("com.google.android.gms.appid", Context.MODE_PRIVATE)
            val appIdToken = appIdPrefs.getString("|T|$senderId|GCM", null)
            if (!appIdToken.isNullOrEmpty()) {
                Log.i(TAG, "Using preserved stock GMS IID token for sender $senderId (from appid.xml): ${appIdToken.take(20)}...")
                prefs.edit().putString("iid_token_$senderId", appIdToken).putString("iid_source_$senderId", "seeded-from-stock-gms-appid").apply()
                return Pair(appIdToken, "seeded-from-stock-gms-appid")
            }

            if (senderId == ConstellationConstants.SENDER_CONSTELLATION) {
                val stockPrefs = context.getSharedPreferences(ConstellationConstants.PREFS_CONSTELLATION, Context.MODE_PRIVATE)
                val stockGcmToken = stockPrefs.getString("gcm_token", null)
                if (!stockGcmToken.isNullOrEmpty()) {
                    Log.i(TAG, "Using preserved stock GMS IID token for sender $senderId (from constellation_prefs gcm_token): ${stockGcmToken.take(20)}...")
                    prefs.edit().putString("iid_token_$senderId", stockGcmToken).putString("iid_source_$senderId", "seeded-from-stock-gms").apply()
                    return Pair(stockGcmToken, "seeded-from-stock-gms")
                }
            }

            Log.i(TAG, "No preserved IID token found for sender $senderId, registering new")

            try {
                var instanceId = prefs.getString("instance_id", null)
                val keyPair: java.security.KeyPair

                if (instanceId == null) {
                    val rsaGenerator = java.security.KeyPairGenerator.getInstance("RSA")
                    rsaGenerator.initialize(2048)
                    keyPair = rsaGenerator.generateKeyPair()

                    val digest = MessageDigest.getInstance("SHA1").digest(keyPair.public.encoded)
                    digest[0] = ((112 + (0xF and digest[0].toInt())) and 0xFF).toByte()
                    instanceId = android.util.Base64.encodeToString(digest, 0, 8,
                        android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING)
                    prefs.edit()
                        .putString("instance_id", instanceId)
                        .putString("key_public", android.util.Base64.encodeToString(keyPair.public.encoded, android.util.Base64.NO_WRAP))
                        .putString("key_private", android.util.Base64.encodeToString(keyPair.private.encoded, android.util.Base64.NO_WRAP))
                        .apply()
                    Log.d(TAG, "Generated new Instance ID + key pair: $instanceId")
                } else {
                    val pubBytes = prefs.getString("key_public", null)?.let { android.util.Base64.decode(it, android.util.Base64.NO_WRAP) }
                    val privBytes = prefs.getString("key_private", null)?.let { android.util.Base64.decode(it, android.util.Base64.NO_WRAP) }
                    if (pubBytes != null && privBytes != null) {
                        val kf = java.security.KeyFactory.getInstance("RSA")
                        keyPair = java.security.KeyPair(
                            kf.generatePublic(java.security.spec.X509EncodedKeySpec(pubBytes)),
                            kf.generatePrivate(java.security.spec.PKCS8EncodedKeySpec(privBytes))
                        )
                    } else {
                        Log.w(TAG, "Key pair missing for existing instance ID, regenerating")
                        prefs.edit().remove("instance_id").apply()
                        return getOrRegisterIidToken(context, packageName, senderId)
                    }
                }

                val pubKeyBase64 = android.util.Base64.encodeToString(keyPair.public.encoded,
                    android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING)
                val signaturePayload = (packageName + "\n" + pubKeyBase64).toByteArray(Charsets.UTF_8)
                val sig = java.security.Signature.getInstance("SHA256withRSA")
                sig.initSign(keyPair.private)
                sig.update(signaturePayload)
                val signatureBase64 = android.util.Base64.encodeToString(sig.sign(),
                    android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING)

                Log.d(TAG, "pub2 length=${pubKeyBase64.length}, sig length=${signatureBase64.length}")

                val checkinInfo = LastCheckinInfo.read(context)
                if (checkinInfo.androidId != 0L && checkinInfo.securityToken != 0L) {
                    try {
                        @Suppress("DEPRECATION")
                        val certSha1 = PackageUtils.firstSignatureDigest(context, packageName)
                        val versionCode = org.microg.gms.common.Constants.GMS_VERSION_CODE
                        val versionName = "%09d".format(versionCode).let {
                            "${it.substring(0, 2)}.${it.substring(2, 4)}.${it.substring(4, 6)} (190400-858744110)"
                        }
                        val clientLibVersion = "iid-${(versionCode / 1000) * 1000}"

                        val response: RegisterResponse = RegisterRequest()
                            .build(context)
                            .checkin(checkinInfo)
                            .app(packageName, certSha1, versionCode)
                            .sender(senderId)
                            .extraParam("subscription", senderId)
                            .extraParam("X-subscription", senderId)
                            .extraParam("subtype", senderId)
                            .extraParam("X-subtype", senderId)
                            .extraParam("scope", "GCM")
                            .extraParam("gmsv", versionCode.toString())
                            .extraParam("osv", Build.VERSION.SDK_INT.toString())
                            .extraParam("app_ver", versionCode.toString())
                            .extraParam("app_ver_name", versionName)
                            .extraParam("cliv", clientLibVersion)
                            .extraParam("appid", instanceId!!)
                            .extraParam("pub2", pubKeyBase64)
                            .extraParam("sig", signatureBase64)
                            .getResponse()

                        if (response.token != null) {
                            Log.i(TAG, "Got IID registration token from Google for sender=$senderId (with pub2/sig): ${response.token.take(20)}...")
                            prefs.edit().putString("iid_token_$senderId", response.token).apply()
                            return Pair(response.token, "registered-$senderId")
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Registration failed: ${e.message}, using Instance ID")
                    }
                } else {
                    Log.w(TAG, "Device not checked in yet, cannot register FCM token")
                }

                Log.d(TAG, "Using Instance ID as fallback: $instanceId")
                return Pair(instanceId!!, "instance-id")

            } catch (e: Exception) {
                Log.e(TAG, "Failed to get IID token", e)
                val randomId = java.util.UUID.randomUUID().toString().take(11).replace("-", "")
                Log.w(TAG, "Using random ID as last resort: $randomId")
                return Pair(randomId, "random-fallback")
            }
        }

    }

    private suspend fun getGaiaTokens(packageName: String): List<String> {
        val accountManager = AccountManager.get(context)
        val accounts = accountManager.getAccountsByType(AuthConstants.DEFAULT_ACCOUNT_TYPE)
        if (accounts.isEmpty()) {
            Log.d(TAG, "No Google accounts available for gaia tokens")
            return emptyList()
        }
        Log.d(TAG, "Gaia token scope: $GAIA_TOKEN_SCOPE")
        Log.d(TAG, "Google accounts found: ${accounts.size}")
        val tokens = ArrayList<String>(accounts.size)
        for (account in accounts) {
            val authManager = AuthManager(context, account.name, packageName, GAIA_TOKEN_SCOPE)
            authManager.isGmsApp = true
            authManager.setPermitted(true)
            authManager.forceRefreshToken = true  // Skip cache to get fresh token
            val token = authManager.getAuthToken() ?: try {
                Log.d(TAG, "No cached token, requesting from Google for ${account.name}...")
                val response = withContext(Dispatchers.IO) {
                    authManager.requestAuthWithBackgroundResolution(false)
                }
                val effectiveToken = response.auths ?: response.auth
                Log.d(TAG, "Auth response for ${account.name}: ${effectiveToken?.length ?: 0} chars (${if (response.auths != null) "IT" else "auth"})")
                effectiveToken
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get gaia token for ${account.name}: ${e.javaClass.simpleName}: ${e.message}")
                e.printStackTrace()
                null
            }
            if (!token.isNullOrEmpty()) {
                tokens.add(token)
                Log.d(TAG, "Gaia token for ${account.name}: ${token.take(20)}... (${token.length} chars)")
            } else {
                Log.w(TAG, "No gaia token available for ${account.name}")
            }
        }
        Log.d(TAG, "Gaia tokens acquired: ${tokens.size}")
        return tokens
    }

    private fun getGaiaIds(): List<String> {
        val accountManager = AccountManager.get(context)
        val accounts = accountManager.getAccountsByType(AuthConstants.DEFAULT_ACCOUNT_TYPE)
        if (accounts.isEmpty()) {
            Log.d(TAG, "No Google accounts available for Gaia IDs")
            return emptyList()
        }
        val gaiaIds = ArrayList<String>(accounts.size)
        for (account in accounts) {
            val gaiaId = accountManager.getUserData(account, "GoogleUserId")
            if (!gaiaId.isNullOrEmpty()) {
                gaiaIds.add(gaiaId)
                Log.d(TAG, "Gaia ID for ${account.name}: $gaiaId")
            } else {
                Log.w(TAG, "No Gaia ID available for ${account.name}")
            }
        }
        Log.d(TAG, "Gaia IDs acquired: ${gaiaIds.size}")
        return gaiaIds
    }

    fun verifyPhoneNumber(request: AidlVerifyPhoneNumberRequest?, callingPackage: String?, imsiOverride: String?, msisdnOverride: String?): Ts43Client.EntitlementResult {
        val requestedNumber = extractRequestedPhoneNumber(request, msisdnOverride)
        Log.i(TAG, "verifyPhoneNumber: phone=$requestedNumber")

        return (try {
            val packageName = context.packageName
            @Suppress("DEPRECATION")
            val certSha1 = PackageUtils.firstSignatureDigest(context, packageName)
            Log.d(TAG, "Using API key auth with package=$packageName, cert=$certSha1")

            runBlocking {
                var callContext: ConstellationCallContext? = null
                try {
                val (iidToken, iidSource) = getOrRegisterIidToken(context, packageName, ConstellationConstants.SENDER_CONSTELLATION)
                Log.d(TAG, "IID token source: $iidSource, token prefix: ${iidToken.take(20)}...")

                val (readOnlyIidToken, readOnlyIidSource) = getOrRegisterIidToken(context, packageName, ConstellationConstants.SENDER_READ_ONLY)
                Log.d(TAG, "Read-only IID token source: $readOnlyIidSource, token prefix: ${readOnlyIidToken.take(20)}...")

                val iidHashDigest = MessageDigest.getInstance("SHA-256").digest(iidToken.toByteArray(Charsets.UTF_8))
                val iidHashPadded = iidHashDigest.copyOf(64)  // Pad to 64 bytes with zeros
                val iidHashFull = android.util.Base64.encodeToString(iidHashPadded,
                    android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP)  // Flag 3 = NO_PADDING | NO_WRAP
                val iidHash = iidHashFull.substring(0, 32)  // Truncate to 32 chars like GMS

                val gaiaTokens = getGaiaTokens(packageName)
                if (gaiaTokens.isNotEmpty()) {
                    Log.i(TAG, "OAuth tokens for proto gaia_ids: ${gaiaTokens.first().take(15)}... (${gaiaTokens.first().length} chars)")
                }

                val spatulaHeader = try {
                    Log.d(TAG, "Fetching Spatula header (10s timeout)...")
                    val spatula = runBlocking {
                        kotlinx.coroutines.withTimeoutOrNull(10_000L) {
                            org.microg.gms.auth.appcert.AppCertManager(context).getSpatulaHeader(packageName)
                        }
                    }
                    if (spatula != null) {
                        Log.d(TAG, "Spatula header obtained (${spatula.length} chars)")
                    } else {
                        Log.w(TAG, "Spatula header returned null (timeout or device key not available)")
                    }
                    spatula
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to get Spatula header: ${e.message}")
                    null
                }

                val rpc = ConstellationRpcClient(
                    context = context,
                    apiKey = API_KEY,
                    packageName = packageName,
                    certSha1 = certSha1,
                    spatulaHeader = spatulaHeader,
                    iidHash = iidHash
                )

                val keyPrefs = context.getSharedPreferences(ConstellationConstants.PREFS_CONSTELLATION, Context.MODE_PRIVATE)
                val keyMaterial = loadOrCreateKeyMaterial(keyPrefs)
                val publicKeyBytes = keyMaterial.publicKeyBytes
                val privateKey = keyMaterial.privateKey

                val isPublicKeyAcked = keyMaterial.isPublicKeyAcked
                Log.d(TAG, "is_public_key_acked: $isPublicKeyAcked")

                val targetImsi = request?.imsiRequests?.firstOrNull()?.imsi
                val targetMsisdn = request?.imsiRequests?.firstOrNull()?.msisdn
                val td = gatherTelephonyData(context, targetImsi, targetMsisdn)
                val subscriptionInfo = td.subscriptionInfo
                val telephonyManagerSub = td.telephonyManagerSub
                val subId = td.subId
                val simCountry = td.simCountry
                val networkCountry = td.networkCountry
                val iccId = td.iccId
                val simSlotIndex = td.simSlotIndex
                Log.d(TAG, "CountryInfo: simCountry=$simCountry, networkCountry=$networkCountry")

                val countryInfo = buildCountryInfo(td)
                val connectivityInfos = gatherConnectivityInfos(context)
                val telephonyInfo = buildTelephonyInfo(td)
                Log.d(TAG, "TelephonyInfo: phoneType=${td.phoneTypeInt}, gid1=${td.groupIdLevel1}, simState=${td.simStateEnum}, serviceState=${td.serviceStateEnum}, subs=${td.activeSubCount}/${td.maxSubCount}")

                val sessionId = UUID.randomUUID().toString()
                val localeStr = Locale.getDefault().toString()
                Log.d(TAG, "Locale: '$localeStr'")

                Log.i(TAG, "OAuth tokens for proto body: ${gaiaTokens.size} available, format: ${gaiaTokens.firstOrNull()?.take(10) ?: "none"}...")

                if (gaiaTokens.isEmpty()) {
                    Log.e(TAG, "No Google account / Gaia token available - aborting Constellation verification")
                    return@runBlocking Ts43Client.EntitlementResult.error("no-gaia-token")
                }

                val registeredAppIds = gaiaTokens.map { StringId(value_ = it) }

                val simAssociationIdentifiers = registeredAppIds

                val gaiaIdsList = getGaiaIds()
                val telephonyInfoContainer = buildTelephonyInfoContainer(gaiaIdsList)
                Log.d(TAG, "TelephonyInfoContainer: ${gaiaIdsList.size} Gaia IDs")

                val phoneIdentity = resolvePhoneIdentity(
                    request = request,
                    requestedNumber = requestedNumber,
                    imsiOverride = imsiOverride,
                    msisdnOverride = msisdnOverride,
                    telephonyManagerSub = telephonyManagerSub,
                    subscriptionInfo = subscriptionInfo,
                    subId = subId
                )
                val imsi = phoneIdentity.imsi
                val msisdn = phoneIdentity.msisdn
                val phoneNumber = phoneIdentity.phoneNumber

                val mergedBundle = if (request?.extras != null) android.os.Bundle(request.extras) else android.os.Bundle()
                mergedBundle.putString("calling_api", "verifyPhoneNumber")
                if (!phoneNumber.isNullOrEmpty() && !mergedBundle.containsKey("force_provisioning")) {
                    mergedBundle.putString("force_provisioning", "true")
                    Log.i(TAG, "Added force_provisioning=true (MSISDN present, not in extras)")
                }
                val forceOneTimeVerificationSetting = Settings.Global.getString(
                    context.contentResolver,
                    FORCE_ONE_TIME_VERIFICATION_KEY
                )?.trim()?.lowercase(Locale.US)
                val shouldAutoAddOneTimeVerification = when (forceOneTimeVerificationSetting) {
                    null, "", "on", "true", "1" -> true
                    "off", "false", "0" -> false
                    else -> {
                        Log.w(TAG, "Unknown $FORCE_ONE_TIME_VERIFICATION_KEY='$forceOneTimeVerificationSetting' - defaulting to auto-add")
                        true
                    }
                }
                if (!mergedBundle.containsKey("one_time_verification")) {
                    if (shouldAutoAddOneTimeVerification) {
                        mergedBundle.putString("one_time_verification", "True")
                        Log.i(TAG, "Added one_time_verification=True (not in extras, key=$FORCE_ONE_TIME_VERIFICATION_KEY setting=${forceOneTimeVerificationSetting ?: "<unset>"})")
                    } else {
                        Log.w(TAG, "Skipping one_time_verification auto-add due to $FORCE_ONE_TIME_VERIFICATION_KEY=${forceOneTimeVerificationSetting}")
                    }
                } else {
                    Log.i(TAG, "Caller already supplied one_time_verification=${mergedBundle.getString("one_time_verification")}")
                }
                val params = bundleToParams(mergedBundle)
                Log.d(TAG, "Verification params: ${params.joinToString { "${it.name}=${it.value_}" }}")

                val imsiRequests = if (request?.imsiRequests != null && request.imsiRequests.isNotEmpty()) {
                    request.imsiRequests.map {
                        IMSIRequest(imsi = it.imsi ?: "", phone_number_hint = it.msisdn ?: "")
                    }
                } else if (imsi.isNotEmpty()) {
                    listOf(IMSIRequest(imsi = imsi, phone_number_hint = msisdn))
                } else {
                    emptyList()
                }

                val idTokenCarrierInfo = resolveIdTokenCarrierInfo(
                    request = request,
                    callingPackage = callingPackage,
                    phoneNumber = phoneNumber,
                    imsiRequests = imsiRequests
                )
                val idTokenCertificateHash = idTokenCarrierInfo.idTokenCertificateHash
                val idTokenNonce = idTokenCarrierInfo.idTokenNonce
                val idTokenCallingPackage = idTokenCarrierInfo.idTokenCallingPackage
                val carrierInfo = idTokenCarrierInfo.carrierInfo
                val gpnvRequestContext = GpnvRequestContext(
                    sessionId = sessionId,
                    privateKey = privateKey,
                    readOnlyIidToken = readOnlyIidToken,
                    idTokenCertificateHash = idTokenCertificateHash,
                    idTokenCallingPackage = idTokenCallingPackage,
                    idTokenNonce = idTokenNonce
                )

                val deviceIdentity = resolveDeviceIdentity()
                val deviceAndroidId = deviceIdentity.deviceAndroidId
                val userAndroidId = deviceIdentity.userAndroidId

                val protoCtx = RequestProtoContext(
                    iidToken = iidToken,
                    deviceAndroidId = deviceAndroidId,
                    userAndroidId = userAndroidId,
                    publicKeyBytes = publicKeyBytes,
                    localeStr = localeStr,
                    gmscoreVersionNumber = constellationGmscoreVersionNumber(),
                    gmscoreVersion = constellationGmscoreVersionString(),
                    registeredAppIds = registeredAppIds,
                    countryInfo = countryInfo,
                    connectivityInfos = connectivityInfos,
                    telephonyInfoContainer = telephonyInfoContainer
                )

                val syncTokenRaw = rpc.getDroidGuardToken("sync", iidToken)
                val (cachedArfb, _, _) = rpc.getCachedDroidGuardToken(rpc.resolveDroidGuardFlow("sync"))
                val syncToken = cachedArfb ?: syncTokenRaw  // Prefer ARfb, fall back to raw DG
                Log.i(TAG, "Sync DG: using ${if (cachedArfb != null) "cached ARfb" else if (syncToken != null) "raw DG" else "NONE (will retry)"} (${syncToken?.length ?: 0} chars)")

                val syncDeviceId = DeviceId(
                    iid_token = iidToken,
                    device_android_id = deviceAndroidId,
                    user_android_id = userAndroidId
                )

                val syncClientCredentials = createClientCredentials(
                    iidTokenForSig = iidToken,
                    deviceIdForCreds = syncDeviceId,
                    privateKey = privateKey,
                    isPublicKeyAcked = isPublicKeyAcked,
                )
                if (syncClientCredentials != null) {
                    Log.d(TAG, "Including client_credentials in Sync request (forced)")
                }

                val smsToken = try {
                    val smsSubId = subscriptionInfo?.subscriptionId ?: -1
                    val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        context.getSystemService(android.telephony.SmsManager::class.java)
                            ?.let { if (smsSubId > 0) it.createForSubscriptionId(smsSubId) else it }
                    } else if (smsSubId != -1 && Build.VERSION.SDK_INT >= 22) {
                        @Suppress("DEPRECATION")
                        android.telephony.SmsManager.getSmsManagerForSubscriptionId(smsSubId)
                    } else {
                        @Suppress("DEPRECATION")
                        android.telephony.SmsManager.getDefault()
                    }
                    if (smsManager == null) {
                        Log.w(TAG, "createAppSpecificSmsToken failed: SmsManager unavailable for subId=$smsSubId")
                        ""
                    } else {
                    val intent = android.content.Intent(ConstellationConstants.ACTION_SILENT_SMS_RECEIVED)
                        .setPackage(context.packageName)
                    // FLAG_MUTABLE is REQUIRED: createAppSpecificSmsToken fires PendingIntent via
                    // send(ctx, code, fillInIntent) where fillInIntent carries PDU extras.
                    // FLAG_IMMUTABLE causes fillIn extras to be ignored → receiver gets empty intent.
                    val pendingIntentFlags = android.app.PendingIntent.FLAG_UPDATE_CURRENT or
                        if (Build.VERSION.SDK_INT >= 31) android.app.PendingIntent.FLAG_MUTABLE else 0
                    val pendingIntent = android.app.PendingIntent.getBroadcast(
                        context, 0, intent, pendingIntentFlags
                    )
                    smsManager.createAppSpecificSmsToken(pendingIntent) ?: ""
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "createAppSpecificSmsToken failed: ${e.message}")
                    ""
                }
                Log.d(TAG, "SMS app-specific token: '${smsToken}' (${smsToken.length} chars)")

                val verificationMethodInfo = buildVerificationMethodInfo(smsToken)

                val loadedVerificationTokens = loadVerificationTokens(keyPrefs)

                val simInfo = buildSIMInfo(imsi, msisdn, iccId)
                val verification = buildVerification(
                    simInfo = simInfo,
                    simAssociationIdentifiers = simAssociationIdentifiers,
                    simSlotIndex = simSlotIndex,
                    subId = subId,
                    telephonyInfo = telephonyInfo,
                    params = params,
                    verificationMethodInfo = verificationMethodInfo,
                    carrierInfo = carrierInfo
                )
                val syncRequest = buildSyncRequest(
                    sessionId = sessionId,
                    ctx = protoCtx,
                    syncToken = syncToken,
                    syncClientCredentials = syncClientCredentials,
                    verification = verification,
                    verificationTokens = loadedVerificationTokens
                )

                val proceedDeviceId = DeviceId(
                    iid_token = iidToken,
                    device_android_id = deviceAndroidId,
                    user_android_id = userAndroidId,
                )
                val proceedClientCredentials = createClientCredentials(
                    iidTokenForSig = iidToken,
                    deviceIdForCreds = proceedDeviceId,
                    privateKey = privateKey,
                    isPublicKeyAcked = isPublicKeyAcked,
                )
                callContext = prepareConstellationCall(
                    rpc = rpc,
                    keyPrefs = keyPrefs,
                    iidToken = iidToken,
                    sessionId = sessionId,
                    subId = subId,
                    imsi = imsi,
                    phoneNumber = phoneNumber,
                    deviceAndroidId = deviceAndroidId,
                    userAndroidId = userAndroidId,
                    registeredAppIds = registeredAppIds,
                    params = params,
                    protoCtx = protoCtx,
                    gpnvRequestContext = gpnvRequestContext,
                    syncRequest = syncRequest,
                    proceedClientCredentials = proceedClientCredentials,
                )
                val call = checkNotNull(callContext)

                val consentOutcome = runConsentFlow(
                    rpc = call.rpc,
                    requestContext = ConsentRequestContext(
                        sessionId = call.sessionId,
                        protoCtx = call.protoCtx,
                        registeredAppIds = call.registeredAppIds,
                        params = call.params,
                        iidToken = call.iidToken
                    )
                )
                Log.d(
                    TAG,
                    "ConsentFlow outcome: consented=${consentOutcome.consented}, setConsentAttempted=${consentOutcome.setConsentAttempted}, setConsentSucceeded=${consentOutcome.setConsentSucceeded}, arfbCached=${consentOutcome.arfbCached}"
                )

                SmsInbox.prepare(context)

                try {
                val syncOutcome = try {
                    runSyncFlow(
                        rpc = call.rpc,
                        requestContext = SyncRequestContext(
                            context = context,
                            keyPrefs = call.keyPrefs,
                            initialRequest = call.syncRequest,
                            iidToken = call.iidToken,
                            imsi = call.imsi,
                            phoneNumber = call.phoneNumber
                        )
                    )
                } catch (e: SyncNoResponsesException) {
                    return@runBlocking Ts43Client.EntitlementResult.error("sync-no-responses")
                }

                if (syncOutcome.hasVerified) {
                    Log.i(TAG, "Calling GetVerifiedPhoneNumbers to retrieve JWT token...")

                    try {
                        val verifiedToken = fetchVerifiedPhoneToken(
                            rpc = call.rpc,
                            requestContext = call.gpnvRequestContext,
                            targetPhone = call.phoneNumber,
                            marker = "GPNV_POST_SYNC"
                        )
                        if (verifiedToken != null) {
                            return@runBlocking Ts43Client.EntitlementResult.success(verifiedToken.jwt)
                        } else {
                            Log.w(TAG, "GetVerifiedPhoneNumbers returned empty token")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "GetVerifiedPhoneNumbers failed: ${e.message}", e)
                        // Fall through to stub
                    }
                }

                // NONE state - best-effort GPNV for stock->microG swap, then check reason
                // Stock GMS handles NONE separately: stores record with state=NONE, calls GPNV as
                // independent read-only query (bevv.java). If GPNV returns nothing, no token is provided.
                if (syncOutcome.noneReason != null && !syncOutcome.hasVerified && syncOutcome.pendingVerification == null) {
                    Log.i(TAG, "NONE state: trying GPNV as best-effort (stock→microG swap scenario)...")
                    try {
                        val verifiedToken = fetchVerifiedPhoneToken(
                            rpc = call.rpc,
                            requestContext = call.gpnvRequestContext,
                            targetPhone = call.phoneNumber,
                            marker = "GPNV_NONE_BEST_EFFORT"
                        )
                        if (verifiedToken != null) {
                            return@runBlocking Ts43Client.EntitlementResult.success(verifiedToken.jwt)
                        } else {
                            Log.w(TAG, "GPNV returned nothing for NONE state - no cached verification exists")
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "GPNV failed for NONE state: ${e.message}")
                    }

                    val unverifiedReason = syncOutcome.noneReason

                    if (unverifiedReason == 5) {
                        Log.i(TAG, "Server requests manual phone number entry (reason=5) - returning status 7")
                        return@runBlocking Ts43Client.EntitlementResult.phoneNumberEntryRequired("none-phone-number-entry-required")
                    }

                    if (unverifiedReason in listOf(0, 1, 2)) {
                        Log.w(TAG, "NONE state retryable reason=$unverifiedReason - returning Status(5002) for retry")
                        return@runBlocking Ts43Client.EntitlementResult.error("5002:none-state-reason-$unverifiedReason")
                    }

                    Log.w(TAG, "NONE state non-retryable reason=$unverifiedReason - returning Status(5001)")
                    return@runBlocking Ts43Client.EntitlementResult.error("5001:none-state-reason-$unverifiedReason")
                }

                // Challenge dispatch loop (multi-type, multi-round)
                if (syncOutcome.pendingVerification != null) {
                    when (val proceedOutcome = runProceedFlow(
                        ProceedRequestContext(
                            context = context,
                            rpc = call.rpc,
                            protoCtx = call.protoCtx,
                            gpnvRequestContext = call.gpnvRequestContext,
                            sessionId = call.sessionId,
                            iidToken = call.iidToken,
                            subId = call.subId,
                            phoneNumber = call.phoneNumber,
                            deviceAndroidId = call.deviceAndroidId,
                            userAndroidId = call.userAndroidId,
                            proceedClientCredentials = call.proceedClientCredentials,
                            initialVerification = syncOutcome.pendingVerification,
                        )
                    )) {
                        is ProceedFlowOutcome.Verified -> {
                            return@runBlocking Ts43Client.EntitlementResult.success(proceedOutcome.jwt)
                        }
                        is ProceedFlowOutcome.Error -> {
                            return@runBlocking Ts43Client.EntitlementResult.error(proceedOutcome.reason, proceedOutcome.cause)
                        }
                        ProceedFlowOutcome.Incomplete -> {
                            Log.w(TAG, "Proceed flow ended without terminal verification")
                        }
                    }
                }

                // If we got here, no token was found
                Log.w(TAG, "No token extracted from sync flow")
                return@runBlocking Ts43Client.EntitlementResult.error("sync-success-no-token")

                } catch (e: Exception) {
                    if (e is com.squareup.wire.GrpcException) {
                        Log.e(TAG, "Sync gRPC error: code=${e.grpcStatus.code} name=${e.grpcStatus.name}")
                        Log.e(TAG, "Sync gRPC message: ${e.grpcMessage}")
                        val details = e.grpcStatusDetails
                        if (details != null) {
                            val b64 = android.util.Base64.encodeToString(details, android.util.Base64.NO_WRAP)
                            Log.e(TAG, "Sync gRPC statusDetails (${details.size} bytes): $b64")
                        } else {
                            Log.e(TAG, "Sync gRPC statusDetails: null")
                        }
                        // Clear DroidGuard cache on auth errors (GMS bewt.java:180-191)
                        if (e.grpcStatus.code == 7 || e.grpcStatus.code == 16) {
                            call.rpc.clearDroidGuardTokenCache(call.rpc.resolveDroidGuardFlow("sync"), "Sync auth error (grpc-status=${e.grpcStatus.code})")
                            call.keyPrefs.edit().putBoolean("is_public_key_acked", false).apply()
                            Log.w(TAG, "Cleared is_public_key_acked due to grpc-status=${e.grpcStatus.code}")
                        }
                    } else {
                        Log.e(TAG, "Sync failed: ${e.javaClass.simpleName}: ${e.message}")
                        if (e.cause != null) {
                            Log.e(TAG, "  Cause: ${e.cause?.javaClass?.simpleName}: ${e.cause?.message}")
                        }
                    }
                    Log.d(TAG, "Full exception trace:", e)

                    // ============================================================
                    // GPNV Fallback: after GetConsent/Sync fail, try to retrieve a
                    // cached JWT via PhoneNumber/GetVerifiedPhoneNumbers.
                    //
                    // This covers the stock GMS -> microG swap scenario: if a prior
                    // stock GMS run left a VERIFIED phone number on the server, we
                    // can retrieve a fresh JWT without needing GetConsent/Sync to
                    // succeed. This is intentionally AFTER GetConsent/Sync so that
                    // we follow stock GMS call ordering and only fall back to GPNV
                    // when the primary flow fails.
                    //
                    // Logging uses a stable marker so we can grep for success:
                    //   - "GPNV_FALLBACK: SUCCESS" means this approach worked.
                    // ============================================================
                    Log.i(TAG, "GPNV_FALLBACK: START - GetConsent/Sync failed, checking for cached verification")
                    try {
                        Log.i(TAG, "GPNV_FALLBACK: Calling GetVerifiedPhoneNumbers after verification state: sync-failed")
                        val verifiedToken = fetchVerifiedPhoneToken(
                            rpc = call.rpc,
                            requestContext = call.gpnvRequestContext,
                            targetPhone = call.phoneNumber,
                            marker = "GPNV_FALLBACK"
                        )
                        val jwt = verifiedToken?.jwt
                        if (!jwt.isNullOrEmpty()) {
                            Log.i(TAG, "GPNV_FALLBACK: returned JWT token (${jwt.length} chars)")
                            // Persist a minimal breadcrumb so success is visible even if logcat rolls.
                            try {
                                val shaPrefix = jwtSha256HexPrefix(jwt)
                                context.getSharedPreferences(ConstellationConstants.PREFS_CONSTELLATION, Context.MODE_PRIVATE)
                                    .edit()
                                    .putString("last_fallback_jwt_sha256_8", shaPrefix)
                                    .putLong("last_fallback_jwt_time_ms", System.currentTimeMillis())
                                    .apply()
                            } catch (_: Throwable) {
                            }

                            Log.i(TAG, "GPNV_FALLBACK: SUCCESS - returning JWT from cached verification")
                            return@runBlocking Ts43Client.EntitlementResult.success(jwt)
                        }

                        Log.i(TAG, "GPNV_FALLBACK: MISS (token_empty=${jwt.isNullOrEmpty()}) - no cached verification found")
                    } catch (gpnvEx: Exception) {
                        val gpnvMsg = gpnvEx.message ?: ""
                        val gpnvStatus = Regex("grpc-status=(\\d+)").find(gpnvMsg)?.groupValues?.get(1)
                        val gpnvGrpcMessage = Regex("grpc-message=([^,]+)").find(gpnvMsg)?.groupValues?.get(1)
                        Log.w(TAG, "GPNV_FALLBACK: ERROR (grpc-status=${gpnvStatus ?: "?"} grpc-message=${gpnvGrpcMessage ?: "?"}) - no cached verification available")
                        Log.d(TAG, "GPNV_FALLBACK exception trace:", gpnvEx)
                    }

                    return@runBlocking Ts43Client.EntitlementResult.error("sync-failed", e)
                }
                } finally {
                    // Dispose SMS receivers (pre-registered before Sync)
                    SmsInbox.dispose(context)
                    // Close RPC client (closes DG handle + gRPC resources)
                    callContext?.rpc?.close()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "verifyPhoneNumber failed with exception", e)
            Ts43Client.EntitlementResult.error("exception-${e.javaClass.simpleName}", e)
        }).also { result ->
            val s = if (!result.token.isNullOrEmpty()) "VERIFIED" else if (result.isError()) "ERROR" else if (result.needsManualMsisdn) "MANUAL_MSISDN" else if (result.ineligible) "INELIGIBLE" else "UNKNOWN"
            Log.i("MicroGRcs", "provision status=$s reason=${result.reason ?: "none"}")
        }
    }

    private fun loadVerificationTokens(
        prefs: android.content.SharedPreferences
    ): List<google.internal.communications.phonedeviceverification.v1.VerificationToken> {
        val stored = prefs.getStringSet("verification_tokens", null) ?: return emptyList()
        return stored.mapNotNull { b64 ->
            try {
                google.internal.communications.phonedeviceverification.v1.VerificationToken.ADAPTER.decode(
                    android.util.Base64.decode(b64, android.util.Base64.NO_WRAP)
                )
            } catch (e: Exception) {
                null
            }
        }.also {
            if (it.isNotEmpty()) Log.d(TAG, "Loaded ${it.size} verification_tokens from storage")
        }
    }

    private fun loadOrCreateKeyMaterial(
        keyPrefs: android.content.SharedPreferences
    ): ConstellationKeyMaterial {
        val storedPublicKeyBase64 = keyPrefs.getString("public_key", null)
        val storedPrivateKeyBase64 = keyPrefs.getString("private_key", null)

        val (publicKeyBytes, privateKey) = if (!storedPublicKeyBase64.isNullOrEmpty() && !storedPrivateKeyBase64.isNullOrEmpty()) {
            try {
                val publicDecoded = android.util.Base64.decode(storedPublicKeyBase64, android.util.Base64.DEFAULT)
                val privateDecoded = android.util.Base64.decode(storedPrivateKeyBase64, android.util.Base64.DEFAULT)
                val keyFactory = java.security.KeyFactory.getInstance("EC")
                val privKeySpec = java.security.spec.PKCS8EncodedKeySpec(privateDecoded)
                val privKey = keyFactory.generatePrivate(privKeySpec)
                Log.d(TAG, "Using stored key pair (public: ${publicDecoded.size} bytes, private: ${privateDecoded.size} bytes)")
                Pair(ByteString.of(*publicDecoded), privKey)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to decode stored keys, generating new", e)
                keyPrefs.edit().remove("public_key").remove("private_key").apply()
                generateAndStoreKeyMaterial(keyPrefs)
            }
        } else {
            generateAndStoreKeyMaterial(keyPrefs)
        }

        return ConstellationKeyMaterial(
            publicKeyBytes = publicKeyBytes,
            privateKey = privateKey,
            isPublicKeyAcked = keyPrefs.getBoolean("is_public_key_acked", false)
        )
    }

    private fun generateAndStoreKeyMaterial(
        keyPrefs: android.content.SharedPreferences
    ): Pair<ByteString, java.security.PrivateKey> {
        val keyGen = KeyPairGenerator.getInstance("EC")
        keyGen.initialize(256)
        val keyPair = keyGen.generateKeyPair()
        val publicEncoded = keyPair.public.encoded
        val privateEncoded = keyPair.private.encoded
        keyPrefs.edit()
            .putString("public_key", android.util.Base64.encodeToString(publicEncoded, android.util.Base64.DEFAULT))
            .putString("private_key", android.util.Base64.encodeToString(privateEncoded, android.util.Base64.DEFAULT))
            .apply()
        Log.d(TAG, "Generated and stored new key pair (public: ${publicEncoded.size} bytes, private: ${privateEncoded.size} bytes)")
        return Pair(ByteString.of(*publicEncoded), keyPair.private)
    }

    private fun resolvePhoneIdentity(
        request: AidlVerifyPhoneNumberRequest?,
        requestedNumber: String?,
        imsiOverride: String?,
        msisdnOverride: String?,
        telephonyManagerSub: TelephonyManager?,
        subscriptionInfo: android.telephony.SubscriptionInfo?,
        subId: Int
    ): ResolvedPhoneIdentity {
        val requestImsi = request?.imsiRequests?.firstOrNull()?.imsi
        val requestMsisdn = request?.imsiRequests?.firstOrNull()?.msisdn
        val telephonyImsi = try {
            telephonyManagerSub?.subscriberId ?: ""
        } catch (e: SecurityException) {
            Log.w(TAG, "Cannot read IMSI (no READ_PHONE_STATE permission): ${e.message}")
            ""
        }
        val imsi = requestImsi ?: imsiOverride ?: telephonyImsi

        val subscriptionMsisdn = try {
            @Suppress("DEPRECATION")
            subscriptionInfo?.number?.takeIf { it.isNotEmpty() } ?: ""
        } catch (e: Exception) {
            ""
        }
        val telephonyMsisdn = if (subscriptionMsisdn.isNotEmpty()) {
            subscriptionMsisdn
        } else {
            Log.d(TAG, "SIM subId=$subId has no stored number (SubscriptionInfo.number empty)")
            ""
        }

        val msisdn = requestMsisdn?.takeIf { it.isNotEmpty() }
            ?: msisdnOverride?.takeIf { it.isNotEmpty() && it.startsWith("+") }
            ?: requestedNumber?.takeIf { it.isNotEmpty() && it.startsWith("+") }
            ?: telephonyMsisdn.takeIf { it.isNotEmpty() }
            ?: ""
        val phoneNumber = msisdn

        Log.i(TAG, "IMSI/MSISDN resolution: imsi=${if (imsi.isNotEmpty()) "${imsi.take(5)}..." else "EMPTY"} (src=${when { requestImsi != null -> "AIDL"; imsiOverride != null -> "override"; telephonyImsi.isNotEmpty() -> "TelephonyManager(sub=$subId)"; else -> "NONE" }}), msisdn=${if (msisdn.isNotEmpty()) "${msisdn.take(5)}..." else "EMPTY"}")

        val allMsisdnSources = mutableMapOf<String, String>()
        if (!requestMsisdn.isNullOrEmpty()) allMsisdnSources["AIDL"] = requestMsisdn
        if (!msisdnOverride.isNullOrEmpty()) allMsisdnSources["override"] = msisdnOverride
        if (telephonyMsisdn.isNotEmpty()) allMsisdnSources["SIM"] = telephonyMsisdn
        if (allMsisdnSources.values.toSet().size > 1) {
            Log.w(TAG, "MSISDN MISMATCH: ${allMsisdnSources.entries.joinToString { "${it.key}=${it.value}" }}, using: $msisdn")
        }
        if (msisdn.isNotEmpty() && !msisdn.startsWith("+")) {
            Log.w(TAG, "MSISDN '$msisdn' missing + prefix (not E.164)")
        }

        return ResolvedPhoneIdentity(imsi = imsi, msisdn = msisdn, phoneNumber = phoneNumber)
    }

    private fun resolveDeviceIdentity(): ResolvedDeviceIdentity {
        val checkinAndroidId = LastCheckinInfo.read(context).androidId
        val androidIdStr = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        val androidIdFromSettings = if (checkinAndroidId != 0L) {
            Log.i(TAG, "Using checkin androidId ($checkinAndroidId) instead of Settings.Secure.ANDROID_ID for DG consistency")
            checkinAndroidId
        } else {
            Log.w(TAG, "Checkin androidId=0, falling back to Settings.Secure.ANDROID_ID")
            try {
                java.lang.Long.parseUnsignedLong(androidIdStr, 16)
            } catch (e: Exception) {
                0L
            }
        }

        val userManager = context.getSystemService(Context.USER_SERVICE) as? android.os.UserManager
        val deviceUserId = try {
            val serial = userManager?.getSerialNumberForUser(android.os.Process.myUserHandle())
            Log.d(TAG, "UserManager.getSerialNumberForUser() = $serial")
            serial ?: 0L
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get user serial number", e)
            0L
        }

        val devicePrefs = context.getSharedPreferences(ConstellationConstants.PREFS_CONSTELLATION, Context.MODE_PRIVATE)
        val primaryDeviceId = devicePrefs.getLong("primary_device_id", 0L)
        val userAndroidId = if (primaryDeviceId != 0L) {
            Log.d(TAG, "Using primary_device_id ($primaryDeviceId) for user_android_id (Settings.Secure.ANDROID_ID=$androidIdFromSettings)")
            primaryDeviceId
        } else {
            androidIdFromSettings
        }

        var deviceAndroidId = primaryDeviceId
        if (deviceAndroidId == 0L) {
            val isSystemUser = userManager?.isSystemUser ?: true
            if (isSystemUser) {
                deviceAndroidId = userAndroidId
            }
        }

        Log.d(TAG, "Android IDs: device=$deviceAndroidId, deviceUser=$deviceUserId, userAndroid=$userAndroidId (settings=$androidIdFromSettings)")

        return ResolvedDeviceIdentity(deviceAndroidId = deviceAndroidId, userAndroidId = userAndroidId)
    }

    private fun resolveIdTokenCarrierInfo(
        request: AidlVerifyPhoneNumberRequest?,
        callingPackage: String?,
        phoneNumber: String,
        imsiRequests: List<IMSIRequest>
    ): ResolvedIdTokenCarrierInfo {
        val audienceOverride = try {
            Settings.Global.getString(context.contentResolver, "microg_constellation_id_token_audience") ?: ""
        } catch (e: Exception) {
            ""
        }
        val nonceOverride = try {
            Settings.Global.getString(context.contentResolver, "microg_constellation_id_token_nonce") ?: ""
        } catch (e: Exception) {
            ""
        }
        val idTokenCertificateHash = request?.idTokenRequest?.audience
            ?: audienceOverride.ifEmpty { null }
            ?: ""
        val idTokenNonce = request?.idTokenRequest?.nonce
            ?: nonceOverride.ifEmpty { null }
            ?: ""
        val idTokenCallingPackage = callingPackage ?: ""
        Log.i(TAG, "IdTokenRequest: certificate_hash_len=${idTokenCertificateHash.length}, calling_package=$idTokenCallingPackage, token_nonce_len=${idTokenNonce.length}, source=${if (request?.idTokenRequest != null) "aidl" else if (audienceOverride.isNotEmpty()) "override" else "empty"}")

        val carrierInfo = buildCarrierInfo(
            phoneNumber = phoneNumber,
            subscriptionId = request?.timeout ?: 0L,
            idTokenCertificateHash = idTokenCertificateHash,
            idTokenNonce = idTokenNonce,
            callingPackage = idTokenCallingPackage,
            imsiRequests = imsiRequests
        )
        Log.d(TAG, "CarrierInfo: phone_number=$phoneNumber, sub_id=${request?.timeout}, calling_package=$idTokenCallingPackage, imsi_requests=${imsiRequests.size}")

        return ResolvedIdTokenCarrierInfo(
            idTokenCertificateHash = idTokenCertificateHash,
            idTokenNonce = idTokenNonce,
            idTokenCallingPackage = idTokenCallingPackage,
            carrierInfo = carrierInfo
        )
    }

    private fun prepareConstellationCall(
        rpc: ConstellationRpcClient,
        keyPrefs: android.content.SharedPreferences,
        iidToken: String,
        sessionId: String,
        subId: Int,
        imsi: String,
        phoneNumber: String,
        deviceAndroidId: Long,
        userAndroidId: Long,
        registeredAppIds: List<StringId>,
        params: List<Param>,
        protoCtx: RequestProtoContext,
        gpnvRequestContext: GpnvRequestContext,
        syncRequest: SyncRequest,
        proceedClientCredentials: ClientCredentials?,
    ): ConstellationCallContext {
        return ConstellationCallContext(
            rpc = rpc,
            keyPrefs = keyPrefs,
            iidToken = iidToken,
            sessionId = sessionId,
            subId = subId,
            imsi = imsi,
            phoneNumber = phoneNumber,
            deviceAndroidId = deviceAndroidId,
            userAndroidId = userAndroidId,
            registeredAppIds = registeredAppIds,
            params = params,
            protoCtx = protoCtx,
            gpnvRequestContext = gpnvRequestContext,
            syncRequest = syncRequest,
            proceedClientCredentials = proceedClientCredentials,
        )
    }

    private fun createClientCredentials(
        iidTokenForSig: String,
        deviceIdForCreds: DeviceId,
        privateKey: java.security.PrivateKey?,
        isPublicKeyAcked: Boolean,
        force: Boolean = false,
    ): ClientCredentials? {
        if ((!isPublicKeyAcked && !force) || privateKey == null) {
            return null
        }
        return try {
            val nowMillis = System.currentTimeMillis()
            val seconds = nowMillis / 1000
            val nanos = ((nowMillis % 1000) * 1000000).toInt()
            val signingString = "$iidTokenForSig:$seconds:$nanos"
            Log.d(TAG, "Signing string: $signingString")

            val signature = java.security.Signature.getInstance("SHA256withECDSA")
            signature.initSign(privateKey)
            signature.update(signingString.toByteArray(Charsets.UTF_8))
            val signatureBytes = signature.sign()
            Log.d(TAG, "Generated signature (${signatureBytes.size} bytes)")

            ClientCredentials(
                device_id = deviceIdForCreds,
                client_signature = ByteString.of(*signatureBytes),
                metadata = CredentialMetadata(
                    timestamp_nanos = seconds,
                    nonce = nanos,
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create client credentials", e)
            null
        }
    }

    private fun constellationGmscoreVersionNumber(): Int = BuildConfig.VERSION_CODE / 1000

    private fun constellationGmscoreVersionString(): String {
        val versionNumber = constellationGmscoreVersionNumber()
        val major = versionNumber / 10000
        val minor = (versionNumber / 100) % 100
        val patch = versionNumber % 100
        return "$major.$minor.$patch"
    }

}
