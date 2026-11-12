/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.cryptauth

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.android.gms.BuildConfig
import com.google.android.gms.common.Scopes
import com.google.android.gms.iid.InstanceID
import cryptauthv2.ClientMetadata
import cryptauthv2.KeyType
import cryptauthv2.SyncKeysResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.microg.gms.auth.AuthConstants
import org.microg.gms.auth.AuthManager
import org.microg.gms.auth.cryptauth.CryptAuthKeyOperations
import org.microg.gms.auth.cryptauth.CryptAuthKeyRecord
import org.microg.gms.auth.cryptauth.CryptAuthKeyStore
import org.microg.gms.auth.cryptauth.CryptAuthKeyType
import org.microg.gms.checkin.LastCheckinInfo
import org.microg.gms.common.Constants
import org.microg.gms.gcm.GcmConstants
import org.microg.gms.gcm.GcmDatabase
import org.microg.gms.gcm.KEY_GCM_REG_ID
import org.microg.gms.gcm.RegisterRequest
import org.microg.gms.gcm.completeRegisterRequest
import org.microg.gms.gcm.ensureAppRegistrationAllowed
import org.microg.gms.profile.Build
import org.microg.gms.smartdevice.d2d.AuthZenKeyStore
import org.microg.gms.smartdevice.d2d.AuthZenWire
import java.security.interfaces.ECPublicKey
import javax.crypto.KeyAgreement

private const val TAG = "EnrollKeyName"

private const val GCM_REGISTER_SENDER = "16502139086"
private const val GCM_REGISTER_SUBTYPE = "16502139086"
private const val GCM_REGISTER_SUBSCRIPTION = "16502139086"
private const val GCM_REGISTER_SCOPE = "GCM"
private const val GCM_REGISTER_CLIV = "iid-202414000"
private const val GCM_REGISTER_INFO = "0wqs6iYsl_URQEb1aBJ6XhzCbHSr-hg"

private const val GMS_GCM_REGISTER_SENDER = "745476177629"
private const val GMS_GCM_REGISTER_SUBTYPE = "745476177629"
private const val GMS_GCM_REGISTER_SUBSCRIPTION = "745476177629"
private const val GMS_GCM_REGISTER_SCOPE = "GCM"
private const val GMS_GCM_PREFERENCES = "com.google.android.gcm"

private const val AFTER_REQUEST_DELAY = 5000L
internal const val PUBLIC_KEY_NAME = "PublicKey"
private const val AUTHZEN_KEY_NAME = "authzen"
private val SUPPORTED_KEY_NAMES = setOf(PUBLIC_KEY_NAME, AUTHZEN_KEY_NAME) +
    FidoKeyEnrollment.KEY_NAME

internal suspend fun enrollKeyNames(
    context: Context,
    accountName: String,
    invocationReason: ClientMetadata.InvocationReason,
    keyNames: List<String>,
): Boolean {
    val requestedKeyNames = keyNames.distinct()
    if (requestedKeyNames.isEmpty() || requestedKeyNames.any { it !in SUPPORTED_KEY_NAMES }) {
        Log.w(TAG, "Unsupported CryptAuth key request keys=${requestedKeyNames.joinToString()}")
        return false
    }
    val wantsPublicKey = PUBLIC_KEY_NAME in requestedKeyNames
    val wantsAuthZen = AUTHZEN_KEY_NAME in requestedKeyNames
    val fidoEnrollment = if (FidoKeyEnrollment.KEY_NAME in requestedKeyNames) {
        FidoKeyEnrollment(context, accountName)
    } else {
        null
    }

    if (android.os.Build.VERSION.SDK_INT < 23) {
        Log.w(TAG, "CryptAuth key enrollment requires Android 6.0 or newer")
        return false
    }

    // Ensure that device is checked in
    val checkinInfo = LastCheckinInfo.read(context)
    if (checkinInfo.androidId == 0L) {
        Log.w(TAG, "Device is not checked in, as it doesn't have an Android ID. Cannot perform cryptauth flow.")
        return false
    }

    // CryptAuth uses GmsCore's persistent Instance ID. Generating a fresh key here makes
    // every DirectTransfer attempt look like a new client to the enrollment service.
    val instanceId = runCatching { InstanceID.getInstance(context).id }.getOrElse {
        Log.e(TAG, "Unable to load the persistent CryptAuth Instance ID", it)
        return false
    }
    val instanceToken = registerForCryptAuth(context, checkinInfo, instanceId)
        .getString(GcmConstants.EXTRA_REGISTRATION_ID)
    if (instanceToken == null) {
        Log.w(TAG, "No instance ID was gathered. Is GCM enabled, has there been a checkin?")
        return false
    }

    // Application metadata uses the package-wide GCM registration, not the
    // CryptAuth Instance ID token.
    val gcmRegistrationId = try {
        getOrRegisterGmsGcmId(context)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "Unable to obtain GmsCore's general GCM registration", e)
        null
    }
    if (gcmRegistrationId == null) {
        Log.w(TAG, "CryptAuth metadata requires the general GmsCore GCM registration")
        return false
    }

    // Auth token for use in CryptAuth query
    val authToken = authenticateForCryptAuth(context, accountName)
    if (authToken == null) {
        Log.w(TAG, "Authentication failed. Cannot perform cryptauth flow.")
        return false
    }

    val authZenKeyStore = AuthZenKeyStore(context)
    val hadPublicKeyRecord = authZenKeyStore.hasPublicKeyRecord(accountName)
    val wasPublicKeyActive = hadPublicKeyRecord && authZenKeyStore.isEnrolled(accountName)
    val authZenHandles = authZenKeyStore.getAuthZenKeyHandles(accountName)
    val authZenActiveStates = authZenHandles.map { handle ->
        authZenKeyStore.isAuthZenKeyActive(accountName, handle)
    }
    val syncKeyRequests = requestedKeyNames.map { keyName ->
        when (keyName) {
            PUBLIC_KEY_NAME -> CryptAuthSyncKeyRequest(
                name = keyName,
                handles = if (hadPublicKeyRecord) listOf(AuthZenKeyStore.KEY_HANDLE) else emptyList(),
            )
            AUTHZEN_KEY_NAME -> CryptAuthSyncKeyRequest(
                name = keyName,
                handles = authZenHandles,
            )
            FidoKeyEnrollment.KEY_NAME -> requireNotNull(fidoEnrollment).syncRequest()
            else -> error("Unsupported CryptAuth key name: $keyName")
        }
    }
    val cryptAuthSyncKeysResult = cryptAuthSyncKeys(
        context,
        authToken,
        instanceId,
        instanceToken,
        gcmRegistrationId,
        checkinInfo.androidId,
        invocationReason,
        syncKeyRequests,
    )
    if (cryptAuthSyncKeysResult == null) {
        Log.w(TAG, "CryptAuth syncKeys failed. Cannot complete flow.")
        return false
    }
    when (cryptAuthSyncKeysResult.opt("serverStatus").asServerStatus()) {
        SyncKeysResponse.ServerStatus.SERVER_OK -> Unit
        SyncKeysResponse.ServerStatus.SERVER_OVERLOADED -> {
            Log.w(TAG, "CryptAuth server is overloaded")
            return false
        }
        null -> {
            Log.w(TAG, "CryptAuth returned an unknown server status")
            return false
        }
    }
    if (!cryptAuthSyncKeysResult.has(CRYPTAUTH_FIELD_SESSION_ID)
        || cryptAuthSyncKeysResult.get(CRYPTAUTH_FIELD_SESSION_ID) !is String
    ) {
        Log.w(TAG, "CryptAuth syncKeys omitted the random session identifier")
        return false
    }
    val clientDirective = cryptAuthSyncKeysResult.optJSONObject("clientDirective")
    val checkinDelayMillis = clientDirective?.opt("checkinDelayMillis").asLongOrNull()
    if (checkinDelayMillis == null || checkinDelayMillis <= 0L) {
        Log.w(TAG, "CryptAuth syncKeys omitted a valid client directive")
        return false
    }
    val creationTime = System.currentTimeMillis()
    if (checkinDelayMillis > Long.MAX_VALUE - creationTime) {
        Log.w(TAG, "CryptAuth client directive exceeds the supported expiration range")
        return false
    }
    val expirationTime = creationTime + checkinDelayMillis

    val session: String = cryptAuthSyncKeysResult.get(CRYPTAUTH_FIELD_SESSION_ID) as String
    val keyResponses = cryptAuthSyncKeysResult.optJSONArray("syncSingleKeyResponses")
    if (keyResponses == null || keyResponses.length() != syncKeyRequests.size) {
        Log.w(
            TAG,
            "CryptAuth syncKeys response count mismatch actual=${keyResponses?.length() ?: 0} " +
                "expected=${syncKeyRequests.size}",
        )
        return false
    }
    val responsesByName = requestedKeyNames.mapIndexed { index, keyName ->
        val response = keyResponses.optJSONObject(index)
        if (response == null) {
            Log.w(TAG, "CryptAuth syncKeys returned an invalid response for key=$keyName")
            return false
        }
        keyName to response
    }.toMap()
    syncKeyRequests.forEachIndexed { index, request ->
        val actions = keyResponses.optJSONObject(index)?.optJSONArray("keyActions")
        val actionCount = actions?.length() ?: 0
        if (actionCount != request.handles.size) {
            Log.w(
                TAG,
                "CryptAuth key action count mismatch key=${request.name} " +
                    "actual=$actionCount expected=${request.handles.size}",
            )
            return false
        }
        for (actionIndex in 0 until actionCount) {
            if (parseKeyAction(actions?.opt(actionIndex)) == null) {
                Log.w(TAG, "CryptAuth returned an invalid key action for key=${request.name}")
                return false
            }
        }
    }
    val publicKeyResponse = responsesByName[PUBLIC_KEY_NAME]
    val authZenResponse = responsesByName[AUTHZEN_KEY_NAME]
    val publicKeyCreationRequested =
        publicKeyResponse?.opt("keyCreation").isKeyCreationRequested()
    val publicKeyAction = publicKeyResponse?.optJSONArray("keyActions").firstKeyAction()
    val authZenCreationRequested =
        authZenResponse?.opt("keyCreation").isKeyCreationRequested()
    val authZenKeyActions = parseKeyActions(authZenResponse)
    fidoEnrollment?.applySyncResponse(responsesByName[FidoKeyEnrollment.KEY_NAME])

    var publicKeyRecordPresent = hadPublicKeyRecord
    var publicKeyActive = wasPublicKeyActive
    when (publicKeyAction) {
        SyncKeysResponse.SyncSingleKeyResponse.KeyAction.ACTIVATE -> {
            publicKeyRecordPresent = true
            publicKeyActive = true
        }
        SyncKeysResponse.SyncSingleKeyResponse.KeyAction.DEACTIVATE -> {
            publicKeyRecordPresent = true
            publicKeyActive = false
        }
        SyncKeysResponse.SyncSingleKeyResponse.KeyAction.DELETE -> {
            publicKeyRecordPresent = false
            publicKeyActive = false
        }
        else -> Unit
    }
    val authZenKeyRecordPresent = authZenHandles.indices.any { index ->
        authZenKeyActions.getOrNull(index) != SyncKeysResponse.SyncSingleKeyResponse.KeyAction.DELETE
    }
    val authZenActive = authZenHandles.indices.any { index ->
        when (authZenKeyActions.getOrNull(index)) {
            SyncKeysResponse.SyncSingleKeyResponse.KeyAction.ACTIVATE -> true
            SyncKeysResponse.SyncSingleKeyResponse.KeyAction.DEACTIVATE,
            SyncKeysResponse.SyncSingleKeyResponse.KeyAction.DELETE -> false
            else -> authZenActiveStates[index]
        }
    }

    applyPublicKeyAction(authZenKeyStore, accountName, publicKeyAction)
    applyAuthZenKeyActions(authZenKeyStore, accountName, authZenHandles, authZenKeyActions)

    if (!publicKeyCreationRequested && !authZenCreationRequested && fidoEnrollment?.creationRequested != true) {
        val usablePublicKey = !wantsPublicKey ||
            publicKeyRecordPresent && publicKeyActive
        val usableAuthZen = !wantsAuthZen ||
            authZenKeyRecordPresent && authZenActive
        delay(AFTER_REQUEST_DELAY)
        return usablePublicKey && usableAuthZen && fidoEnrollment?.isUsable != false
    }
    if (publicKeyCreationRequested && !publicKeyResponse?.opt("keyType").isP256KeyType()) {
        Log.w(TAG, "CryptAuth requested an unsupported PublicKey type")
        return false
    }
    if (fidoEnrollment?.hasSupportedCreationType() == false) return false

    val randomSessionId = runCatching { AuthZenWire.decodeBase64(session) }.getOrElse {
        Log.w(TAG, "CryptAuth returned an invalid random session identifier", it)
        return false
    }
    if (randomSessionId.isEmpty()) {
        Log.w(TAG, "CryptAuth returned an empty random session identifier")
        return false
    }
    val enrollmentKeys = ArrayList<CryptAuthEnrollmentKey>(requestedKeyNames.size)
    var publicKeyWillBeActive = !wantsPublicKey ||
        publicKeyRecordPresent && publicKeyActive
    if (publicKeyCreationRequested) {
        // Proto3 omits enum fields carrying their zero value.
        val keyStorageLevel = publicKeyResponse?.opt("keyStorageLevel").asKeyStorageLevel()
            ?: SyncKeysResponse.SyncSingleKeyResponse.KeyStorageLevel.KEY_STORAGE_LEVEL_UNSPECIFIED
        val publicKeyExists = authZenKeyStore.hasKey()
        val softwareBacked = keyStorageLevel ==
            SyncKeysResponse.SyncSingleKeyResponse.KeyStorageLevel.KEY_STORAGE_LEVEL_UNSPECIFIED ||
            keyStorageLevel == SyncKeysResponse.SyncSingleKeyResponse.KeyStorageLevel.SOFTWARE
        val hardwareUserPresenceRequired =
            publicKeyResponse?.opt("hardwareUserPresenceRequired").asBooleanOrFalse()
        val userVerificationRequired =
            publicKeyResponse?.opt("userVerificationRequired").asBooleanOrFalse()
        val userAuthenticationValidityDurationSeconds =
            publicKeyResponse?.opt("userAuthenticationValidityDurationSeconds").asIntOrZero()
        val publicKey = runCatching {
            authZenKeyStore.getOrCreatePublicKey(
                attestationChallenge = randomSessionId,
                strongBoxBacked = keyStorageLevel ==
                    SyncKeysResponse.SyncSingleKeyResponse.KeyStorageLevel.DEDICATED_SECURE_ELEMENT,
                softwareBacked = !publicKeyExists && softwareBacked,
                hardwareUserPresenceRequired = hardwareUserPresenceRequired,
                userVerificationRequired = userVerificationRequired,
                userAuthenticationValidityDurationSeconds = userAuthenticationValidityDurationSeconds,
            )
        }.getOrElse {
            Log.w(TAG, "Unable to create the requested CryptAuth PublicKey", it)
            return false
        }
        enrollmentKeys += CryptAuthEnrollmentKey(
            name = PUBLIC_KEY_NAME,
            handle = AuthZenKeyStore.KEY_HANDLE,
            material = AuthZenWire.encodeGenericPublicKey(publicKey),
            proof = authZenKeyStore.createKeyProof(randomSessionId),
        )
        publicKeyWillBeActive = publicKeyResponse?.opt("keyCreation").isActiveKeyCreation()
    }

    var clientEphemeralDh: ByteArray? = null
    var pendingAuthZenKey: PendingAuthZenKey? = null
    var authZenWillBeActive = !wantsAuthZen ||
        authZenKeyRecordPresent && authZenActive
    if (authZenCreationRequested) {
        val keyType = parseCryptAuthKeyType(authZenResponse?.opt("keyType"))
            ?.takeIf { it.symmetricKeyLength != null }
        if (keyType == null) {
            Log.w(TAG, "CryptAuth requested an unsupported authzen key type")
            return false
        }
        val serverDhEncoded = cryptAuthSyncKeysResult.optString("serverEphemeralDh")
            .takeIf(String::isNotEmpty)
            ?.let(AuthZenWire::decodeBase64)
        if (serverDhEncoded == null) {
            Log.w(TAG, "CryptAuth omitted serverEphemeralDh for authzen key creation")
            return false
        }
        val ephemeralKeyPair = CryptAuthKeyOperations.generateKeyPair(CryptAuthKeyType.P256)
        val sharedSecret = KeyAgreement.getInstance("ECDH").run {
            init(ephemeralKeyPair.private)
            doPhase(AuthZenWire.decodeGenericPublicKey(serverDhEncoded), true)
            generateSecret()
        }
        val material = CryptAuthKeyOperations.deriveSymmetricKey(keyType, sharedSecret, AUTHZEN_KEY_NAME)
        val handle = authZenKeyStore.createAuthZenKeyHandle()
        enrollmentKeys += CryptAuthEnrollmentKey(
            name = AUTHZEN_KEY_NAME,
            handle = handle,
            material = material,
            proof = CryptAuthKeyOperations.createKeyProof(
                keyType,
                material,
                AUTHZEN_KEY_NAME,
                randomSessionId,
            ),
        )
        clientEphemeralDh = AuthZenWire.encodeGenericPublicKey(ephemeralKeyPair.public as ECPublicKey)
        authZenWillBeActive = authZenResponse?.opt("keyCreation").isActiveKeyCreation()
        pendingAuthZenKey = PendingAuthZenKey(handle, material, authZenWillBeActive)
    }

    if (fidoEnrollment?.creationRequested == true) {
        val enrollmentKey = fidoEnrollment.createEnrollmentKey(randomSessionId) ?: return false
        enrollmentKeys += enrollmentKey
    }

    val cryptAuthEnrollKeysResult = cryptAuthEnrollKeys(
        authToken = authToken,
        session = session,
        keys = enrollmentKeys,
        clientEphemeralDh = clientEphemeralDh,
    )

    val enrollSingleKeyResponses = cryptAuthEnrollKeysResult?.optJSONArray("enrollSingleKeyResponses")
    val enrollResponseCountMatches = when {
        cryptAuthEnrollKeysResult == null -> false
        !cryptAuthEnrollKeysResult.has("enrollSingleKeyResponses") -> true
        enrollSingleKeyResponses == null -> false
        enrollSingleKeyResponses.length() == 0 -> true
        else -> enrollSingleKeyResponses.length() == enrollmentKeys.size
    }
    return if (cryptAuthEnrollKeysResult != null && enrollResponseCountMatches) {
        val records = ArrayList<CryptAuthKeyRecord>(enrollmentKeys.size)
        if (publicKeyCreationRequested) {
            records += authZenKeyStore.publicKeyRecord(
                accountName,
                publicKeyWillBeActive,
                creationTime,
                expirationTime,
            )
        }
        val pendingKey = pendingAuthZenKey
        if (authZenCreationRequested && pendingKey != null) {
            records += authZenKeyStore.authZenKeyRecord(
                accountIdentifier = accountName,
                handle = pendingKey.handle,
                keyMaterial = pendingKey.material,
                enrolled = pendingKey.active,
                creationTime = creationTime,
                expirationTime = expirationTime,
            )
        }
        fidoEnrollment?.keyRecord(creationTime, expirationTime)?.let(records::add)
        CryptAuthKeyStore(context).put(records)
        if (authZenCreationRequested && pendingKey != null) {
            authZenKeyStore.finishAuthZenKeyStore(accountName)
        }
        fidoEnrollment?.finishCommit()
        /* Give Google server some time to process the new information.
         * This leads to higher success rate compared to sending
         * the next query immediately after this one. Tests show that it
         * makes sense to wait multiple seconds.
         */
        delay(AFTER_REQUEST_DELAY)
        publicKeyWillBeActive && authZenWillBeActive && fidoEnrollment?.willBeUsable != false
    } else {
        if (cryptAuthEnrollKeysResult != null) {
            Log.w(
                TAG,
                "CryptAuth enrollKeys response count mismatch " +
                    "actual=${enrollSingleKeyResponses?.length() ?: 0} expected=${enrollmentKeys.size}",
            )
        }
        false
    }
}

private data class PendingAuthZenKey(
    val handle: ByteArray,
    val material: ByteArray,
    val active: Boolean,
)

private fun applyPublicKeyAction(
    keyStore: AuthZenKeyStore,
    accountName: String,
    action: SyncKeysResponse.SyncSingleKeyResponse.KeyAction?,
) {
    when (action) {
        SyncKeysResponse.SyncSingleKeyResponse.KeyAction.ACTIVATE -> keyStore.setPublicKeyState(accountName, true)
        SyncKeysResponse.SyncSingleKeyResponse.KeyAction.DEACTIVATE -> keyStore.setPublicKeyState(accountName, false)
        SyncKeysResponse.SyncSingleKeyResponse.KeyAction.DELETE -> keyStore.deletePublicKey(accountName)
        else -> Unit
    }
}

private fun applyAuthZenKeyActions(
    keyStore: AuthZenKeyStore,
    accountName: String,
    handles: List<ByteArray>,
    actions: List<SyncKeysResponse.SyncSingleKeyResponse.KeyAction>,
) {
    handles.forEachIndexed { index, handle ->
        when (actions.getOrNull(index)) {
            SyncKeysResponse.SyncSingleKeyResponse.KeyAction.ACTIVATE ->
                keyStore.setAuthZenEnrolled(accountName, handle, true)
            SyncKeysResponse.SyncSingleKeyResponse.KeyAction.DEACTIVATE ->
                keyStore.setAuthZenEnrolled(accountName, handle, false)
            SyncKeysResponse.SyncSingleKeyResponse.KeyAction.DELETE ->
                keyStore.deleteAuthZenKey(accountName, handle)
            else -> Unit
        }
    }
}

private fun parseKeyActions(response: JSONObject?): List<SyncKeysResponse.SyncSingleKeyResponse.KeyAction> {
    val actions = response?.optJSONArray("keyActions") ?: return emptyList()
    return (0 until actions.length()).mapNotNull { index -> parseKeyAction(actions.opt(index)) }
}

private fun Any?.isKeyCreationRequested(): Boolean {
    val keyCreation = parseKeyCreation(this)
    return keyCreation != null && keyCreation != SyncKeysResponse.SyncSingleKeyResponse.KeyCreation.NONE
}

private fun Any?.isActiveKeyCreation(): Boolean =
    parseKeyCreation(this) == SyncKeysResponse.SyncSingleKeyResponse.KeyCreation.ACTIVE

private fun Any?.isP256KeyType(): Boolean = parseKeyType(this) == KeyType.P256

internal fun parseKeyCreation(value: Any?): SyncKeysResponse.SyncSingleKeyResponse.KeyCreation? = when (value) {
    is Number -> SyncKeysResponse.SyncSingleKeyResponse.KeyCreation.fromValue(value.toInt())
    is String -> {
        val numericValue = value.toIntOrNull()
        if (numericValue != null) {
            SyncKeysResponse.SyncSingleKeyResponse.KeyCreation.fromValue(numericValue)
        } else {
            SyncKeysResponse.SyncSingleKeyResponse.KeyCreation.values().firstOrNull { it.name == value }
        }
    }
    else -> null
}

internal fun parseKeyAction(value: Any?): SyncKeysResponse.SyncSingleKeyResponse.KeyAction? = when (value) {
    is Number -> SyncKeysResponse.SyncSingleKeyResponse.KeyAction.fromValue(value.toInt())
    is String -> {
        val numericValue = value.toIntOrNull()
        if (numericValue != null) {
            SyncKeysResponse.SyncSingleKeyResponse.KeyAction.fromValue(numericValue)
        } else {
            SyncKeysResponse.SyncSingleKeyResponse.KeyAction.values().firstOrNull { it.name == value }
        }
    }
    else -> null
}

internal fun parseCryptAuthKeyType(value: Any?): CryptAuthKeyType? = when (parseKeyType(value)) {
    KeyType.RAW128 -> CryptAuthKeyType.RAW128
    KeyType.RAW256 -> CryptAuthKeyType.RAW256
    KeyType.P256 -> CryptAuthKeyType.P256
    else -> null
}

private fun JSONArray?.firstKeyAction(): SyncKeysResponse.SyncSingleKeyResponse.KeyAction? {
    if (this == null || length() == 0) return null
    return parseKeyAction(opt(0))
}

internal fun parseKeyType(value: Any?): KeyType? = when (value) {
    is Number -> KeyType.fromValue(value.toInt())
    is String -> {
        val numericValue = value.toIntOrNull()
        if (numericValue != null) {
            KeyType.fromValue(numericValue)
        } else {
            KeyType.values().firstOrNull { it.name == value }
        }
    }
    else -> null
}

private fun Any?.asKeyStorageLevel(): SyncKeysResponse.SyncSingleKeyResponse.KeyStorageLevel? = when (this) {
    is Number -> SyncKeysResponse.SyncSingleKeyResponse.KeyStorageLevel.fromValue(toInt())
    is String -> toIntOrNull()?.let(SyncKeysResponse.SyncSingleKeyResponse.KeyStorageLevel::fromValue)
        ?: SyncKeysResponse.SyncSingleKeyResponse.KeyStorageLevel.values().firstOrNull { it.name == this }
    else -> null
}

private fun Any?.asLongOrNull(): Long? = when (this) {
    is Number -> toLong()
    is String -> toLongOrNull()
    else -> null
}

private fun Any?.asIntOrZero(): Int = when (this) {
    is Number -> toInt()
    is String -> toIntOrNull() ?: 0
    else -> 0
}

private fun Any?.asBooleanOrFalse(): Boolean = when (this) {
    is Boolean -> this
    is Number -> toInt() != 0
    is String -> equals("true", ignoreCase = true) || this == "1"
    else -> false
}

private fun Any?.asServerStatus(): SyncKeysResponse.ServerStatus? = when (this) {
    null -> SyncKeysResponse.ServerStatus.SERVER_OK
    is Number -> SyncKeysResponse.ServerStatus.fromValue(toInt())
    is String -> toIntOrNull()?.let(SyncKeysResponse.ServerStatus::fromValue)
        ?: SyncKeysResponse.ServerStatus.values().firstOrNull { it.name == this }
    else -> null
}

private suspend fun registerForCryptAuth(
    context: Context,
    checkinInfo: LastCheckinInfo,
    instanceId: String,
): Bundle {
    val database = GcmDatabase(context)
    return try {
        completeRegisterRequest(
            context = context,
            database = database,
            request = RegisterRequest().build(context)
                .checkin(checkinInfo)
                .app("com.google.android.gms", CERTIFICATE.lowercase(), BuildConfig.VERSION_CODE)
                .sender(GCM_REGISTER_SENDER)
                .extraParam("subscription", GCM_REGISTER_SUBSCRIPTION)
                .extraParam("subtype", GCM_REGISTER_SUBTYPE)
                .extraParam("app_ver", BuildConfig.VERSION_CODE.toString())
                .extraParam("osv", "29")
                .extraParam("cliv", GCM_REGISTER_CLIV)
                .extraParam("gmsv", BuildConfig.VERSION_CODE.toString())
                .extraParam("appid", instanceId)
                .extraParam("scope", GCM_REGISTER_SCOPE)
                .extraParam("app_ver_name", "%09d".format(BuildConfig.VERSION_CODE).let {
                    "${it.substring(0, 2)}.${it.substring(2, 4)}.${it.substring(4, 6)} (190800-{{cl}})"
                })
                .info(GCM_REGISTER_INFO)
        )
    } finally {
        database.close()
    }
}

/** Returns the package-wide GCM registration used by application metadata. */
private suspend fun getOrRegisterGmsGcmId(context: Context): String? {
    val preferences = context.getSharedPreferences(GMS_GCM_PREFERENCES, Context.MODE_PRIVATE)
    preferences.getString(KEY_GCM_REG_ID, null)?.takeIf(String::isNotEmpty)?.let {
        return it
    }

    return withContext(Dispatchers.IO) {
        val request = RegisterRequest().build(context)
            .checkin(LastCheckinInfo.read(context))
            .app(Constants.GMS_PACKAGE_NAME, Constants.GMS_PACKAGE_SIGNATURE_SHA1, BuildConfig.VERSION_CODE)
            .sender(GMS_GCM_REGISTER_SENDER)
            .extraParam("subscription", GMS_GCM_REGISTER_SUBSCRIPTION)
            .extraParam("subtype", GMS_GCM_REGISTER_SUBTYPE)
            .extraParam("scope", GMS_GCM_REGISTER_SCOPE)
        val database = GcmDatabase(context)
        try {
            ensureAppRegistrationAllowed(context, database, request.app)
            completeRegisterRequest(context, database, request)
                .getString(GcmConstants.EXTRA_REGISTRATION_ID)
        } finally {
            database.close()
        }
    }
}

private suspend fun authenticateForCryptAuth(context: Context, accountName: String): String? {
    val cryptAuthServiceOauth2 = AuthConstants.SCOPE_OAUTH2 + Scopes.CRYPTAUTH
    val authManager = AuthManager(context, accountName, Constants.GMS_PACKAGE_NAME, cryptAuthServiceOauth2)
    return withContext(Dispatchers.IO) { authManager.requestAuth(false).auth }
}
