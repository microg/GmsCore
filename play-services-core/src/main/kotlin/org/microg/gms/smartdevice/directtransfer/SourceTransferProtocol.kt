/* SPDX-License-Identifier: Apache-2.0 */
package org.microg.gms.smartdevice.directtransfer

import android.accounts.Account
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okio.ByteString
import okio.ByteString.Companion.decodeBase64
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import org.microg.gms.cryptauth.CryptAuthBootstrapAssertion
import org.microg.gms.cryptauth.CryptAuthBootstrapAuthorization
import java.io.Closeable
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.util.concurrent.atomic.AtomicBoolean
import org.microg.gms.smartdevice.directtransfer.SourceTransferDiagnosticReason as Reason

/** The account identifier is an email; the key is the enrolled GenericPublicKey, never a secret. */
internal class SourceTransferBootstrapInfo(val accountIdentifier: String, val publicKey: ByteString)

internal class SourceTransferProtocolException(
    val statusCode: Int, val reason: Reason
) : IOException("Account transfer failed ($statusCode)")

/**
 * Version 3 classic account bootstrap over an already authenticated UKEY2 channel. The target
 * obtains the challenge and redeems the assertion. No local acknowledgement proves redemption.
 * Hooks are owned by our authorized session, not caller-supplied configuration or remote JSON.
 */
internal class SourceTransferProtocol(
    private val peerVerification: ByteString,
    private val hooks: Hooks
) : Closeable {
    internal interface Hooks {
        fun checkCurrent()
        suspend fun requestConsent(sessionId: Long, peerVerification: ByteString, targetDisplayName: String?): List<CryptAuthBootstrapAuthorization>
        suspend fun getBootstrapInfo(authorization: CryptAuthBootstrapAuthorization): SourceTransferBootstrapInfo
        suspend fun createAssertion(authorization: CryptAuthBootstrapAuthorization, expectedPublicKey: ByteString, challenge: ByteString): CryptAuthBootstrapAssertion
        suspend fun verifyCredentials(authorization: CryptAuthBootstrapAuthorization, fallbackUrl: String): String
        fun sourceAndroidDeviceId(): Long
        suspend fun sendPayload(payload: ByteArray)
    }

    internal enum class State { OPTIONS, CONSENT, CHALLENGE, RESULTS, VERIFICATION, FINISH, COMPLETE, CLOSED }
    @Volatile var state = State.OPTIONS
        private set
    var sessionId: Long? = null
        private set
    private val closed = AtomicBoolean()
    private val stateLock = Any()
    private val mutex = Mutex()
    private var messages = 0
    private var authorization: CryptAuthBootstrapAuthorization? = null
    private var selectedAccount: Account? = null
    private var advertisedKey: ByteString? = null
    private var results: List<SourceTransferAccountResult>? = null
    private var sourceSideChallengeRequired = false
    private var verificationRequested = false

    init { require(peerVerification.size == 32) { "Invalid peer verification" } }

    /** Serializes state transitions, including asynchronous consent/signing. close() never waits. */
    suspend fun receive(payload: ByteArray): List<SourceTransferAccountResult>? = mutex.withLock {
        try {
            checkCurrent()
            requireProtocol(state != State.COMPLETE, Reason.PROTOCOL_COMPLETE)
            requireProtocol(++messages <= MAX_MESSAGES, Reason.PROTOCOL_MESSAGE_LIMIT)
            val message = parse(payload)
            requireProtocol(message.integer("protocolVersion") == 3, Reason.PROTOCOL_VERSION)
            message.knownFields(ENVELOPE_FIELDS)
            val bootstrapState = message.integer("bootstrapState", 0)
            when (bootstrapState) {
                1 -> fail(Reason.PROTOCOL_REMOTE_CANCEL, 10564)
                3 -> fail(Reason.PROTOCOL_REMOTE_ERROR, 10575)
                0, 2 -> Unit
                else -> fail(Reason.PROTOCOL_REMOTE_STATE, 10575)
            }
            message.optionalObject("deviceStatus")?.let {
                requireProtocol(it.integer("errorCode", 0) == 0 && it.integer("errorCodeFromSource", 0) == 0, Reason.PROTOCOL_DEVICE_STATUS)
            }
            val options = message.optionalObject("bootstrapOptions")
            val bootstrap = message.optionalObject("accountBootstrapPayload")
            val remoteResults = message.optionalArray("accountTransferResults")
            requireProtocol(listOf(options, bootstrap, remoteResults).count { it != null } <= 1, Reason.PROTOCOL_MESSAGE_CONTENT)
            if (options != null) {
                requireProtocol(bootstrapState == 0, Reason.PROTOCOL_MESSAGE_STATE)
                acceptOptions(options)
            } else if (bootstrap != null) {
                requireProtocol(bootstrapState == 0, Reason.PROTOCOL_MESSAGE_STATE)
                if (!bootstrap.isNull("userCredentials")) acceptCredentials(bootstrap)
                else acceptChallenge(bootstrap)
            } else if (remoteResults != null) {
                acceptResults(remoteResults)
            } else {
                requireProtocol(bootstrapState == 2 || message.has("progressEvent") || message.has("displayText"), Reason.PROTOCOL_MESSAGE_EMPTY)
            }
            if (bootstrapState == 2) {
                requireProtocol(state == State.FINISH && results?.size == 1, Reason.PROTOCOL_FINAL_STATE)
                send(envelope().put("bootstrapState", 2))
                checkCurrent()
                transition(State.COMPLETE)
                return@withLock requireNotNull(results).toList()
            }
            null
        } catch (e: Throwable) {
            close()
            throw e
        }
    }

    private suspend fun acceptOptions(options: JSONObject) {
        requireProtocol(state == State.OPTIONS, Reason.PROTOCOL_OPTIONS_ORDER)
        // Device type 4 is Wear OS. Optional feature advertisements never grant capabilities.
        requireProtocol(options.integer("deviceType") == 4, Reason.PROTOCOL_OPTIONS_DEVICE)
        val id = options.longInteger("sessionId")
        requireProtocol(id != 0L && id != -1L, Reason.PROTOCOL_OPTIONS_SESSION)
        sessionId = id
        val explicitChallenge = if (options.isNull("isSourceSideChallengeRequired")) false
            else options.opt("isSourceSideChallengeRequired") as? Boolean
                ?: fail(Reason.PROTOCOL_OPTIONS_VERIFICATION, 10589)
        // Legacy sources use the explicit flag; the modern classic-account controller derives
        // this requirement from flowType == 1. Neither advertisement replaces local consent.
        val flowType = options.integer("flowType", 0, Reason.PROTOCOL_OPTIONS_VERIFICATION)
        sourceSideChallengeRequired = explicitChallenge || flowType == 1
        val targetName = options.optionalString("deviceName", 128)?.takeIf {
            it.none { character -> character < ' ' || character in '\u202a'..'\u202e' || character in '\u2066'..'\u2069' }
        }
        transition(State.CONSENT)
        val accepted = hooks.requestConsent(id, peerVerification, targetName)
        checkCurrent()
        requireProtocol(accepted.size == 1, Reason.PROTOCOL_CONSENT_COUNT, 10562)
        val auth = accepted.single()
        auth.enforceCurrent()
        val account = auth.account
        requireProtocol(account.type == "com.google" && account.name.length in 1..320 && account.name.none { it < ' ' }, Reason.PROTOCOL_CONSENT_ACCOUNT, 10562)
        authorization = auth
        selectedAccount = Account(account.name, account.type)
        val info = hooks.getBootstrapInfo(auth)
        checkCurrent()
        requireProtocol(info.accountIdentifier == account.name && info.publicKey.size in 1..4096, Reason.PROTOCOL_BOOTSTRAP_IDENTITY, 10573)
        advertisedKey = info.publicKey
        // These statements follow our own consent and Keyguard result, never peer/caller flags.
        send(envelope().put("bootstrapConfigurations", JSONObject()
            .put("hasUserConfirmed", true).put("isLockScreenShown", true)
            .put("supportsUnencryptedCommunication", false).put("maxPacketSize", 0)
            .put("optionFlags", 0L).put("optionFlagSetIndicators", 0L)))
        val userInfo = JSONObject().put("accountIdentifier", info.accountIdentifier).put("userPublicKey", info.publicKey.base64())
        send(envelope().put("accountBootstrapPayload", JSONObject().put("userBootstrapInfos", JSONArray().put(userInfo))))
        transition(State.CHALLENGE)
    }

    private suspend fun acceptChallenge(bootstrap: JSONObject) {
        requireProtocol(state == State.CHALLENGE, Reason.PROTOCOL_CHALLENGE_ORDER)
        bootstrap.knownFields(setOf("challenges"), Reason.PROTOCOL_CHALLENGE_FIELDS)
        val challenges = bootstrap.optionalArray("challenges") ?: fail(Reason.PROTOCOL_CHALLENGE_PAYLOAD, 10650)
        requireProtocol(challenges.length() == 1, Reason.PROTOCOL_CHALLENGE_COUNT, 10650)
        val challenge = challenges.opt(0) as? JSONObject ?: fail(Reason.PROTOCOL_CHALLENGE_ENTRY, 10650)
        challenge.knownFields(setOf("status", "accountIdentifier", "reason", "challenge", "challengeSessionState"), Reason.PROTOCOL_CHALLENGE_FIELDS)
        requireProtocol(challenge.integer("status") == 0, Reason.PROTOCOL_CHALLENGE_STATUS, 10650)
        val account = requireNotNull(selectedAccount)
        requireProtocol(challenge.requiredString("accountIdentifier", 320) == account.name, Reason.PROTOCOL_CHALLENGE_ACCOUNT, 10650)
        challenge.optionalString("reason", 1024)
        val bytes = challenge.bytes("challenge", 1, 4096)
        val session = if (!challenge.isNull("challengeSessionState")) challenge.bytes("challengeSessionState", 0, 65536) else null
        checkCurrent()
        val androidId = hooks.sourceAndroidDeviceId()
        requireProtocol(androidId != 0L, Reason.PROTOCOL_CHECKIN_MISSING, 10650)
        val assertion = hooks.createAssertion(requireNotNull(authorization), requireNotNull(advertisedKey), bytes)
        checkCurrent()
        requireProtocol(hooks.sourceAndroidDeviceId() == androidId, Reason.PROTOCOL_CHECKIN_CHANGED, 10650)
        requireProtocol(assertion.clientData.size in 1..MAX_CLIENT_DATA_BYTES && assertion.encryptedAssertion.size in 1..65536, Reason.PROTOCOL_ASSERTION_SIZE, 10650)
        val encoded = JSONObject().put("accountIdentifier", account.name)
            .put("clientData", assertion.clientData.base64()).put("encryptedUserAssertion", assertion.encryptedAssertion.base64())
            .put("challenge", bytes.base64()).put("assertionType", 1)
        if (session != null) encoded.put("challengeSessionState", session.base64())
        val request = JSONObject().put("assertions", JSONArray().put(encoded)).put("credentialType", 3)
            .put("clientId", ACCOUNT_MANAGER_CLIENT_ID).put("platformVariant", 0)
            .put("sourceAndroidDeviceId", java.lang.Long.toHexString(androidId))
            .put("targetDeviceSignals", JSONObject.NULL).put("locale", JSONObject.NULL)
            // No backup account selected: the target requires a non-null metadata string.
            .put("sourceBackupAccountId", "").put("deferCredentialsAfterFallback", false)
        send(envelope().put("accountBootstrapPayload", JSONObject().put("exchangeAssertionsForUserCredentialsRequest", request)))
        transition(State.RESULTS)
    }

    private suspend fun acceptCredentials(bootstrap: JSONObject) {
        requireProtocol(state == State.RESULTS && !verificationRequested, Reason.PROTOCOL_CREDENTIAL_ORDER)
        bootstrap.knownFields(setOf("userCredentials"), Reason.PROTOCOL_CREDENTIAL_FIELDS)
        requireProtocol(sourceSideChallengeRequired, Reason.PROTOCOL_CREDENTIAL_UNEXPECTED)
        val entries = bootstrap.optionalArray("userCredentials") ?: fail(Reason.PROTOCOL_CREDENTIAL_FORMAT, 10589)
        requireProtocol(entries.length() == 1, Reason.PROTOCOL_CREDENTIAL_COUNT)
        val entry = entries.opt(0) as? JSONObject ?: fail(Reason.PROTOCOL_CREDENTIAL_FORMAT, 10589)
        entry.knownFields(setOf("accountIdentifier", "status", "reason", "fallbackUrl", "credential",
            "firstName", "lastName", "obfuscatedGaiaId"), Reason.PROTOCOL_CREDENTIAL_FIELDS)
        requireProtocol(entry.requiredString("accountIdentifier", 320) == selectedAccount?.name, Reason.PROTOCOL_CREDENTIAL_ACCOUNT)
        requireProtocol(entry.integer("status") == 0, Reason.PROTOCOL_CREDENTIAL_STATUS)
        entry.optionalString("reason", 1024)
        entry.optionalString("credential", 65536) // Never imported on the source or echoed to the peer.
        entry.optionalString("firstName", 1024)
        entry.optionalString("lastName", 1024)
        entry.optionalString("obfuscatedGaiaId", 1024)
        val url = entry.requiredString("fallbackUrl", 8192)
        requireProtocol(SourceTransferWebPolicy.isNavigationAllowed(url), Reason.PROTOCOL_VERIFICATION_URL)
        verificationRequested = true
        transition(State.VERIFICATION)
        val checkpoint = hooks.verifyCredentials(requireNotNull(authorization), url)
        checkCurrent()
        requireProtocol(SourceTransferWebPolicy.isCheckpointValid(checkpoint), Reason.PROTOCOL_CHECKPOINT_VALUE)
        val bound = JSONObject().put("accountIdentifier", requireNotNull(selectedAccount).name)
            .put("sessionCheckpoint", checkpoint)
        val request = JSONObject().put("sessionCheckpoints", JSONArray().put(bound))
            .put("targetDeviceRiskSignals", JSONObject.NULL)
        send(envelope().put("accountBootstrapPayload", JSONObject()
            .put("exchangeSessionCheckpointsForUserCredentialsRequest", request)))
        // A checkpoint only lets the target retry redemption. Results and final ACK are mandatory.
        transition(State.RESULTS)
    }

    private fun acceptResults(remote: JSONArray) {
        requireProtocol(state == State.RESULTS, Reason.PROTOCOL_RESULT_ORDER)
        requireProtocol(remote.length() == 1, Reason.PROTOCOL_RESULT_COUNT)
        val result = remote.opt(0) as? JSONObject ?: fail(Reason.PROTOCOL_RESULT_ENTRY)
        result.knownFields(setOf("bootstrapAccount", "RESULT", "lockScreenAuthenticationType"), Reason.PROTOCOL_RESULT_FIELDS)
        val account = result.optionalObject("bootstrapAccount", Reason.PROTOCOL_RESULT_ACCOUNT_FORMAT)
            ?: fail(Reason.PROTOCOL_RESULT_ACCOUNT_FORMAT)
        requireProtocol(account.requiredString("name", 320, Reason.PROTOCOL_RESULT_ACCOUNT_FORMAT) == selectedAccount?.name &&
            account.requiredString("type", 64, Reason.PROTOCOL_RESULT_ACCOUNT_FORMAT) == selectedAccount?.type, Reason.PROTOCOL_RESULT_ACCOUNT)
        // RESULT is deliberately upper case in the v3 wire schema. Only 1 means success.
        requireProtocol(result.integer("RESULT", reason = Reason.PROTOCOL_RESULT_STATUS_FORMAT) == 1, Reason.PROTOCOL_RESULT_STATUS)
        checkCurrent()
        // Android Keyguard confirms credentials, but does not reveal whether PIN/pattern was used.
        results = listOf(SourceTransferAccountResult(requireNotNull(selectedAccount), 1, 0))
        transition(State.FINISH)
    }

    private suspend fun send(message: JSONObject) {
        checkCurrent()
        val bytes = message.toString().toByteArray(Charsets.UTF_8)
        requireProtocol(bytes.size in 1..MAX_MESSAGE_BYTES, Reason.PROTOCOL_OUTGOING_SIZE)
        hooks.sendPayload(bytes)
        checkCurrent()
    }

    private fun checkCurrent() {
        requireProtocol(!closed.get(), Reason.PROTOCOL_CLOSED, 10564)
        hooks.checkCurrent()
        authorization?.let {
            it.enforceCurrent()
            requireProtocol(it.account == selectedAccount, Reason.PROTOCOL_AUTH_ACCOUNT_CHANGED, 10587)
        }
        requireProtocol(!closed.get(), Reason.PROTOCOL_CLOSED, 10564)
    }

    override fun close() = synchronized(stateLock) {
        closed.set(true)
        state = State.CLOSED
    }

    private fun transition(next: State) = synchronized(stateLock) {
        requireProtocol(!closed.get(), Reason.PROTOCOL_CLOSED, 10564)
        state = next
    }

    private fun parse(payload: ByteArray): JSONObject {
        requireProtocol(payload.size in 1..MAX_MESSAGE_BYTES, Reason.PROTOCOL_INCOMING_SIZE, 10589)
        val text = Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(payload)).toString()
        // Bound recursive org.json parsing before entering it. Quoted braces do not add depth.
        var depth = 0
        var quote = '\u0000'
        var escaped = false
        var previous = '\u0000'
        for (character in text) {
            if (quote != '\u0000') {
                if (escaped) escaped = false
                else if (character == '\\') escaped = true
                else if (character == quote) quote = '\u0000'
            } else when (character) {
                '"' -> {
                    // JSONTokener also permits unquoted literals containing quotes. Do not let
                    // those literals hide structural brackets from this pre-parsing limit.
                    requireProtocol(previous == '\u0000' || previous in "{[:,", Reason.PROTOCOL_JSON_SYNTAX, 10589)
                    quote = character
                }
                // org.json is lenient about comments and single quotes; the wire format is JSON.
                // Comments could otherwise hide a quote from JSONTokener but not from this guard.
                '\'', '/', '#', '\\' -> fail(Reason.PROTOCOL_JSON_SYNTAX, 10589)
                '{', '[' -> { depth++; requireProtocol(depth <= 24, Reason.PROTOCOL_JSON_DEPTH, 10589) }
                '}', ']' -> { depth--; requireProtocol(depth >= 0, Reason.PROTOCOL_JSON_SYNTAX, 10589) }
            }
            if (quote == '\u0000' && !character.isWhitespace()) previous = character
        }
        requireProtocol(depth == 0 && quote == '\u0000', Reason.PROTOCOL_JSON_SYNTAX, 10589)
        val tokener = JSONTokener(text)
        val result = tokener.nextValue() as? JSONObject ?: fail(Reason.PROTOCOL_JSON_ROOT, 10589)
        requireProtocol(tokener.nextClean() == '\u0000', Reason.PROTOCOL_JSON_TRAILING, 10589)
        return result
    }

    private fun JSONObject.knownFields(allowed: Set<String>, reason: Reason = Reason.PROTOCOL_FIELD_UNKNOWN) {
        keys().forEach { requireProtocol(it in allowed || isNull(it), reason, 10589) }
    }
    private fun JSONObject.optionalObject(name: String, reason: Reason = Reason.PROTOCOL_FIELD_OBJECT): JSONObject? =
        if (isNull(name)) null else opt(name) as? JSONObject ?: fail(reason, 10589)
    private fun JSONObject.optionalArray(name: String): JSONArray? =
        if (isNull(name)) null else opt(name) as? JSONArray ?: fail(Reason.PROTOCOL_FIELD_ARRAY, 10589)
    private fun JSONObject.longInteger(name: String, reason: Reason = Reason.PROTOCOL_FIELD_INTEGER): Long = when (val value = opt(name)) {
        is Int -> value.toLong()
        is Long -> value
        else -> fail(reason, 10589)
    }
    private fun JSONObject.integer(name: String, default: Int? = null, reason: Reason = Reason.PROTOCOL_FIELD_INTEGER): Int {
        if (!has(name) && default != null) return default
        val value = longInteger(name, reason)
        requireProtocol(value in Int.MIN_VALUE..Int.MAX_VALUE, reason, 10589)
        return value.toInt()
    }
    private fun JSONObject.requiredString(name: String, maxLength: Int, reason: Reason = Reason.PROTOCOL_FIELD_STRING): String {
        val value = opt(name) as? String ?: fail(reason, 10589)
        requireProtocol(value.length in 1..maxLength, reason, 10589)
        return value
    }
    private fun JSONObject.optionalString(name: String, maxLength: Int): String? {
        if (isNull(name)) return null
        val value = opt(name) as? String ?: fail(Reason.PROTOCOL_FIELD_STRING, 10589)
        requireProtocol(value.length <= maxLength, Reason.PROTOCOL_FIELD_STRING, 10589)
        return value
    }
    private fun JSONObject.bytes(name: String, minimum: Int, maximum: Int): ByteString {
        val value = opt(name) as? String ?: fail(Reason.PROTOCOL_FIELD_BINARY_TYPE, 10589)
        requireProtocol(value.length <= ((maximum + 2) / 3) * 4 + maximum / 16 + 16, Reason.PROTOCOL_FIELD_BINARY_SIZE, 10589)
        // FastJsonResponse field type8 uses standard Base64, not the URL alphabet.
        requireProtocol(value.all { it in 'A'..'Z' || it in 'a'..'z' || it in '0'..'9' || it == '+' || it == '/' || it == '=' || it in " \r\n\t" }, Reason.PROTOCOL_FIELD_BINARY_ALPHABET, 10589)
        val compact = value.filterNot { it in " \r\n\t" }
        val decoded = compact.decodeBase64() ?: fail(Reason.PROTOCOL_FIELD_BINARY_DECODE, 10589)
        requireProtocol(decoded.size in minimum..maximum && decoded.base64().trimEnd('=') == compact.trimEnd('='), Reason.PROTOCOL_FIELD_BINARY_VALUE, 10589)
        return decoded
    }

    private fun envelope() = JSONObject().put("protocolVersion", 3)
    private fun requireProtocol(value: Boolean, reason: Reason, code: Int = 10575) { if (!value) fail(reason, code) }
    private fun fail(reason: Reason, code: Int = 10575): Nothing = throw SourceTransferProtocolException(code, reason)

    companion object {
        // Public OAuth client identifier used by the target's account-manager exchange, not a secret.
        private const val ACCOUNT_MANAGER_CLIENT_ID = "1070009224336-sdh77n7uot3oc99ais00jmuft6sk2fg9.apps.googleusercontent.com"
        private const val MAX_MESSAGES = 64
        // A 64 KiB DroidGuard result expands inside Base64 signals, then clientData is encoded
        // again below. This is a local resource budget; send() still rejects any combination of
        // clientData, challenge, target session state and assertion exceeding the unchanged total.
        private const val MAX_CLIENT_DATA_BYTES = 128 * 1024
        private const val MAX_MESSAGE_BYTES = 256 * 1024
        private val ENVELOPE_FIELDS = setOf("protocolVersion", "bootstrapState", "bootstrapOptions", "accountBootstrapPayload",
            "accountTransferResults", "deviceStatus", "displayText", "progressEvent", "priorityMessage")
    }
}
