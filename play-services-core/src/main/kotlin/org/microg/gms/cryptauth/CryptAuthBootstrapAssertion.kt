/* SPDX-License-Identifier: Apache-2.0 */
package org.microg.gms.cryptauth

import android.accounts.Account
import android.content.Context
import android.os.Build
import com.google.android.gms.BuildConfig
import com.google.android.gms.droidguard.DroidGuardClient
import com.google.android.gms.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okio.ByteString
import okio.ByteString.Companion.decodeBase64
import okio.ByteString.Companion.decodeHex
import okio.ByteString.Companion.encodeUtf8
import org.microg.gms.checkin.LastCheckinInfo
import org.microg.gms.cryptauth.proto.BootstrapAssertionBody
import org.microg.gms.cryptauth.proto.BootstrapScreenLockSignals
import org.microg.gms.cryptauth.proto.BootstrapSourceSignals

/**
 * Implemented by the authorized transfer session, never by a caller-supplied Parcelable or flags.
 * Each check must bind account + caller UID/certificate + authenticated peer/session and enforce
 * expiry plus a recent, successful platform device-credential confirmation from our consent UI.
 */
internal interface CryptAuthBootstrapAuthorization {
    val account: Account
    fun enforceCurrent()
}

internal class CryptAuthBootstrapInfo(val accountIdentifier: String, val publicKey: ByteString) {
    override fun toString() = "CryptAuthBootstrapInfo(redacted)"
}

/** Announces an enrolled identity only after the transfer session has authorized this account. */
internal suspend fun Context.getCryptAuthBootstrapInfo(
    authorization: CryptAuthBootstrapAuthorization
): CryptAuthBootstrapInfo {
    val diagnostic = CryptAuthTransferDiagnostics()
    diagnostic.at(CryptAuthTransferDiagnosticReason.AUTHORIZATION_INITIAL) { authorization.enforceCurrent() }
    val account = authorization.account
    diagnostic.at(CryptAuthTransferDiagnosticReason.ACCOUNT_TYPE) {
        require(account.type == "com.google") { "Unsupported bootstrap account" }
    }
    val state = applicationContext.ensureCryptAuthTransferKeys(account, diagnostic)
    diagnostic.at(CryptAuthTransferDiagnosticReason.AUTHORIZATION_FINAL) { authorization.enforceCurrent() }
    diagnostic.at(CryptAuthTransferDiagnosticReason.ACCOUNT_UNCHANGED) {
        require(account == authorization.account) { "Bootstrap account changed" }
    }
    val publicKey = diagnostic.at(CryptAuthTransferDiagnosticReason.IDENTITY_PUBLIC_KEY) {
        CryptAuthBootstrapAssertions.identityPublicKey(state)
    }
    return CryptAuthBootstrapInfo(account.name, publicKey)
}

internal class CryptAuthBootstrapAssertion(val clientData: ByteString, val encryptedAssertion: ByteString) {
    override fun toString() = "CryptAuthBootstrapAssertion(redacted)"
}

/** Source-side operation only; the official target obtains challenges and exchanges assertions. */
internal suspend fun Context.createCryptAuthBootstrapAssertion(
    authorization: CryptAuthBootstrapAuthorization,
    expectedPublicKey: ByteString,
    challenge: ByteString
): CryptAuthBootstrapAssertion {
    val diagnostic = CryptAuthTransferDiagnostics()
    diagnostic.at(CryptAuthTransferDiagnosticReason.AUTHORIZATION_INITIAL) { authorization.enforceCurrent() }
    val account = authorization.account
    diagnostic.at(CryptAuthTransferDiagnosticReason.ACCOUNT_TYPE) {
        require(account.type == "com.google") { "Unsupported bootstrap account" }
    }
    diagnostic.at(CryptAuthTransferDiagnosticReason.ASSERTION_CHALLENGE) {
        CryptAuthBootstrapAssertions.validateChallenge(challenge)
    }
    diagnostic.at(CryptAuthTransferDiagnosticReason.ASSERTION_IDENTITY) { CryptAuthCrypto.parsePublicKey(expectedPublicKey) }
    val context = applicationContext
    val store = diagnostic.at(CryptAuthTransferDiagnosticReason.STORAGE_OPEN) { CryptAuthKeyStore(context, account.name) }
    // Enrollment belongs before UserBootstrapInfo. Re-syncing here could rotate the keys
    // after the target has already obtained a challenge for the advertised identity.
    val handle = diagnostic.at(CryptAuthTransferDiagnosticReason.ASSERTION_KEYS) {
        withContext(Dispatchers.IO) { CryptAuthBootstrapAssertions.readConfirmedAuthzenHandle(store, expectedPublicKey) }
    }
    diagnostic.at(CryptAuthTransferDiagnosticReason.AUTHORIZATION_FINAL) { authorization.enforceCurrent() }
    diagnostic.at(CryptAuthTransferDiagnosticReason.ACCOUNT_UNCHANGED) {
        require(account == authorization.account) { "Bootstrap account changed" }
    }
    diagnostic.at(CryptAuthTransferDiagnosticReason.ASSERTION_SCREEN_LOCK) {
        check(context.isLockscreenConfigured()) { "Bootstrap requires a secure screen lock" }
    }
    val checkin = diagnostic.at(CryptAuthTransferDiagnosticReason.ASSERTION_CHECKIN) {
        LastCheckinInfo.read(context).also { check(it.androidId != 0L) { "Bootstrap requires device check-in" } }
    }
    val androidId = java.lang.Long.toHexString(checkin.androidId)
    // Use the existing provider. Missing/disabled/crashed DroidGuard is an error, never a fake
    // attestation or an invented claim that this device met Google's acceptance policy.
    val droidGuardResult = diagnostic.at(CryptAuthTransferDiagnosticReason.DROIDGUARD_REQUEST) {
        DroidGuardClient.getResults(context, "android_d2d", mapOf(
            "androidId" to androidId,
            "challengeHash" to challenge.sha1().base64Url().trimEnd('=')
        )).await()
    }
    val droidGuard = CryptAuthBootstrapAssertions.checkedDroidGuardResult(droidGuardResult, diagnostic)
    diagnostic.at(CryptAuthTransferDiagnosticReason.AUTHORIZATION_FINAL) { authorization.enforceCurrent() }
    diagnostic.at(CryptAuthTransferDiagnosticReason.ACCOUNT_UNCHANGED) {
        require(account == authorization.account) { "Bootstrap account changed" }
    }
    diagnostic.at(CryptAuthTransferDiagnosticReason.ASSERTION_SCREEN_LOCK) {
        check(context.isLockscreenConfigured()) { "Bootstrap screen lock changed" }
    }
    val signals = diagnostic.at(CryptAuthTransferDiagnosticReason.ASSERTION_SIGNALS) { BootstrapSourceSignals(
        android_id = androidId, device = Build.DEVICE, model = Build.MODEL,
        sdk_version = Build.VERSION.SDK_INT.toString(), gms_version = BuildConfig.VERSION_CODE.toString(),
        droidguard_result = droidGuard,
        screen_lock = BootstrapScreenLockSignals(secure_screen_lock = true, screen_lock_confirmed = true,
            screen_lock_state = 2, seconds_since_unlock = -1, seconds_since_screen_lock_setup = -1)
    ).also(CryptAuthBootstrapAssertions::validateSignals) }
    return diagnostic.at(CryptAuthTransferDiagnosticReason.ASSERTION_COUNTER_AND_SIGN) {
        withContext(Dispatchers.IO) {
            CryptAuthBootstrapAssertions.create(store, handle, expectedPublicKey,
                authorization, challenge, signals)
        }
    }
}

/** Minimal assertion contract observed in account bootstrap; cryptography is public SecureMessage. */
internal object CryptAuthBootstrapAssertions {
    // Local resource budget, not a documented Google limit. Signals and clientData are each
    // Base64-encoded on the wire; SourceTransferProtocol independently bounds the whole message.
    const val MAX_DROIDGUARD_BYTES = 64 * 1024
    private val metadata = "080b1001".decodeHex() // Public securegcm.GcmMetadata(type=11, version=1).
    // The existing DroidGuard client and fallback encode this local error prefix as Base64.
    // Its message can contain sensitive input, so only classify the prefix, never expose it.
    private val droidGuardLocalErrorPrefix = "ERROR : ".encodeUtf8()

    internal fun droidGuardFailureReason(result: String?): CryptAuthTransferDiagnosticReason? = when {
        result.isNullOrEmpty() -> CryptAuthTransferDiagnosticReason.DROIDGUARD_EMPTY
        result.length > MAX_DROIDGUARD_BYTES || result.toByteArray(Charsets.UTF_8).size > MAX_DROIDGUARD_BYTES ->
            CryptAuthTransferDiagnosticReason.DROIDGUARD_TOO_LARGE
        result.decodeBase64()?.startsWith(droidGuardLocalErrorPrefix) == true ->
            CryptAuthTransferDiagnosticReason.DROIDGUARD_LOCAL_ERROR
        else -> null
    }

    /** A bounded non-error result is only provider output; Google still decides its validity. */
    internal suspend fun checkedDroidGuardResult(result: String?, diagnostic: CryptAuthTransferDiagnostics): String {
        val failure = droidGuardFailureReason(result)
        return diagnostic.at(failure ?: CryptAuthTransferDiagnosticReason.DROIDGUARD_RESULT) {
            check(failure == null) { "Bootstrap device verification unavailable" }
            checkNotNull(result)
        }
    }

    /** Read-only selection; the OTP operation rechecks both keys under the same mutex later. */
    internal suspend fun readConfirmedAuthzenHandle(store: CryptAuthStateStore, expectedPublicKey: ByteString): ByteString =
        cryptAuthStateMutex.withLock {
            currentCoroutineContext().ensureActive()
            val state = store.read()
            requireIdentity(state, expectedPublicKey)
            val key = requireNotNull(state.keys.singleOrNull { it.name == "authzen" && it.active }) {
                "Missing active CryptAuth authzen key"
            }
            require(key.type == cryptauthv2.KeyType.RAW256 && key.secret.size == 32 && key.publicKey.size == 0 &&
                key.handle == key.secret.sha256().base64Url().encodeUtf8()) { "Invalid bootstrap encryption key" }
            currentCoroutineContext().ensureActive()
            key.handle
        }

    suspend fun create(store: CryptAuthStateStore, expectedAuthzenHandle: ByteString, expectedPublicKey: ByteString,
                       authorization: CryptAuthBootstrapAuthorization, challenge: ByteString,
                       signals: BootstrapSourceSignals): CryptAuthBootstrapAssertion {
        authorization.enforceCurrent()
        validateChallenge(challenge)
        CryptAuthCrypto.parsePublicKey(expectedPublicKey)
        validateSignals(signals)
        val account = authorization.account
        require(account.type == "com.google" && account.name.length in 1..320 && account.name.none { it < ' ' }) {
            "Invalid bootstrap account"
        }
        val clientData = clientData(account.name, challenge, signals.encodeByteString())
        return CryptAuthOtpCounter(store).useNextCounter(expectedAuthzenHandle) { state, counter ->
            authorization.enforceCurrent()
            require(account == authorization.account) { "Bootstrap account changed" }
            requireIdentity(state, expectedPublicKey)
            val body = BootstrapAssertionBody(client_data_hash = clientData.sha256(), counter = counter, version = 1)
            val envelope = CryptAuthBootstrapCrypto.createEnvelope(state,
                CryptAuthBootstrapPayload(body.encodeByteString(), publicMetadata = metadata))
            authorization.enforceCurrent()
            CryptAuthBootstrapAssertion(clientData, envelope.secureMessage)
        }
    }

    internal fun identityPublicKey(state: CryptAuthState): ByteString {
        require(state.pendingKeys.isEmpty()) { "CryptAuth enrollment is unconfirmed" }
        val identity = requireNotNull(state.keys.singleOrNull { it.name == "PublicKey" && it.active }) {
            "Missing active CryptAuth identity"
        }
        require(identity.type == cryptauthv2.KeyType.P256 &&
            identity.handle == CryptAuthEnrollment.DEVICE_KEY_HANDLE) { "Invalid CryptAuth identity" }
        CryptAuthCrypto.parsePublicKey(identity.publicKey)
        return identity.publicKey
    }

    internal fun requireIdentity(state: CryptAuthState, expectedPublicKey: ByteString) {
        require(identityPublicKey(state) == expectedPublicKey) { "Advertised bootstrap identity changed" }
    }

    internal fun validateChallenge(challenge: ByteString) {
        require(challenge.size in 1..4096) { "Invalid bootstrap challenge" }
    }

    internal fun clientData(account: String, challenge: ByteString, signals: ByteString): ByteString =
        ("{\"typ\":\"navigator.id.getAssertion\",\"challenge\":\"${challenge.base64Url().trimEnd('=')}\"," +
            "\"source_device_signals\":\"${signals.base64Url().trimEnd('=')}\",\"account_identifier\":${quote(account)}}").encodeUtf8()

    private fun quote(value: String): String {
        require(value.toByteArray(Charsets.UTF_8).toString(Charsets.UTF_8) == value) { "Invalid account encoding" }
        return buildString {
            append('"')
            for (character in value) when (character) {
                '"', '\\' -> { append('\\'); append(character) }
                in '\u0000'..'\u001f' -> { append("\\u"); append(character.code.toString(16).padStart(4, '0')) }
                else -> append(character)
            }
            append('"')
        }
    }

    internal fun validateSignals(signals: BootstrapSourceSignals) {
        val droidGuard = signals.droidguard_result
        require(signals.unknownFields.size == 0 && signals.android_id?.matches(Regex("[0-9a-f]{1,16}")) == true &&
            signals.android_id != "0" && droidGuardFailureReason(droidGuard) == null &&
            listOf(signals.device, signals.model, signals.sdk_version, signals.gms_version).all { it != null && it.length in 1..256 }) {
            "Invalid bootstrap device signals"
        }
        val lock = requireNotNull(signals.screen_lock) { "Missing bootstrap screen-lock signals" }
        require(lock.unknownFields.size == 0 && lock.secure_screen_lock == true && lock.screen_lock_confirmed == true &&
            lock.screen_lock_state == 2 && lock.seconds_since_unlock == -1L && lock.seconds_since_screen_lock_setup == -1L) {
            "Unconfirmed bootstrap screen lock"
        }
    }
}
