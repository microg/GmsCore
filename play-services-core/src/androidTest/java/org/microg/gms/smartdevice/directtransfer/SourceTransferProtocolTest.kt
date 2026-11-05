/* SPDX-License-Identifier: Apache-2.0 */
package org.microg.gms.smartdevice.directtransfer

import android.accounts.Account
import android.test.InstrumentationTestCase
import cryptauthv2.KeyType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import okio.ByteString
import okio.ByteString.Companion.encodeUtf8
import okio.ByteString.Companion.toByteString
import org.json.JSONArray
import org.json.JSONObject
import org.microg.gms.cryptauth.CryptAuthBootstrapAssertion
import org.microg.gms.cryptauth.CryptAuthBootstrapAuthorization
import org.microg.gms.cryptauth.CryptAuthBootstrapAssertions
import org.microg.gms.cryptauth.CryptAuthBootstrapCrypto
import org.microg.gms.cryptauth.CryptAuthBootstrapPayload
import org.microg.gms.cryptauth.CryptAuthCrypto
import org.microg.gms.cryptauth.CryptAuthEnrollment
import org.microg.gms.cryptauth.CryptAuthKey
import org.microg.gms.cryptauth.CryptAuthState
import org.microg.gms.cryptauth.proto.BootstrapAssertionBody
import org.microg.gms.cryptauth.proto.BootstrapScreenLockSignals
import org.microg.gms.cryptauth.proto.BootstrapSourceSignals
import java.io.IOException
import java.security.interfaces.ECPublicKey
import org.microg.gms.smartdevice.directtransfer.SourceTransferDiagnosticReason as Reason

/** Real Android org.json and Account; no Google calls or real account credentials. */
@Suppress("DEPRECATION")
class SourceTransferProtocolTest : InstrumentationTestCase() {
    private var fixtureAccount = Account("source-fixture@example.invalid", "com.google")
    private val fixtureKey = "synthetic-public-key".encodeUtf8()
    private var fixtureChallenge = byteArrayOf(0xfb.toByte(), 0xff.toByte(), 7).toByteString()
    private var fixtureSession = "opaque-target-session".encodeUtf8()
    private val peer = ByteArray(32) { it.toByte() }.toByteString()

    private inner class Authorization : CryptAuthBootstrapAuthorization {
        override val account = fixtureAccount
        var valid = true
        override fun enforceCurrent() { check(valid) { "Synthetic authorization revoked" } }
    }

    private inner class Hooks : SourceTransferProtocol.Hooks {
        val authorization = Authorization()
        val sent = mutableListOf<JSONObject>()
        val sentByteSizes = mutableListOf<Int>()
        val events = mutableListOf<String>()
        var consent: suspend () -> List<CryptAuthBootstrapAuthorization> = { listOf(authorization) }
        var beforeAssertion: () -> Unit = {}
        var beforeSend: (JSONObject) -> Unit = {}
        var sourceValid = true
        var signingCalls = 0
        var infoCalls = 0
        var verificationCalls = 0
        var verify: suspend () -> String = { "synthetic-checkpoint" }
        var androidId = 0x1020304050607080L
        var assertion = CryptAuthBootstrapAssertion("synthetic-client-data".encodeUtf8(), "synthetic-signed-envelope".encodeUtf8())
        override fun checkCurrent() { check(sourceValid) }
        override fun sourceAndroidDeviceId() = androidId
        override suspend fun requestConsent(sessionId: Long, peerVerification: ByteString, targetDisplayName: String?): List<CryptAuthBootstrapAuthorization> {
            assertEquals(-55L, sessionId)
            assertEquals(peer, peerVerification)
            assertEquals("Synthetic Watch", targetDisplayName)
            events.add("consent")
            assertTrue(sent.isEmpty())
            return consent()
        }
        override suspend fun getBootstrapInfo(authorization: CryptAuthBootstrapAuthorization): SourceTransferBootstrapInfo {
            assertSame(this.authorization, authorization)
            infoCalls++
            events.add("info")
            return SourceTransferBootstrapInfo(fixtureAccount.name, fixtureKey)
        }
        override suspend fun createAssertion(authorization: CryptAuthBootstrapAuthorization, expectedPublicKey: ByteString, challenge: ByteString): CryptAuthBootstrapAssertion {
            assertSame(this.authorization, authorization)
            assertEquals(fixtureKey, expectedPublicKey)
            assertEquals(fixtureChallenge, challenge)
            signingCalls++
            beforeAssertion()
            return assertion
        }
        override suspend fun sendPayload(payload: ByteArray) {
            val objectValue = JSONObject(payload.toString(Charsets.UTF_8))
            beforeSend(objectValue)
            events.add("send")
            sent.add(objectValue)
            sentByteSizes.add(payload.size)
        }
        override suspend fun verifyCredentials(authorization: CryptAuthBootstrapAuthorization, fallbackUrl: String): String {
            assertSame(this.authorization, authorization)
            assertEquals("https://accounts.google.com/transfer?fixture=1", fallbackUrl)
            verificationCalls++
            return verify()
        }
    }

    fun testClassicTransferWireFieldsAndFinalAcknowledgement() = runBlocking {
        val hooks = Hooks()
        val protocol = SourceTransferProtocol(peer, hooks)
        assertNull(protocol.receive(options()))
        assertEquals(listOf("consent", "info", "send", "send"), hooks.events)
        assertEquals(SourceTransferProtocol.State.CHALLENGE, protocol.state)
        val configuration = hooks.sent[0].getJSONObject("bootstrapConfigurations")
        assertTrue(configuration.getBoolean("hasUserConfirmed"))
        assertFalse(configuration.getBoolean("supportsUnencryptedCommunication"))
        assertEquals(0, configuration.getInt("maxPacketSize"))
        assertEquals(0L, configuration.getLong("optionFlags"))
        assertFalse(configuration.has("bootstrapAccounts"))
        val info = hooks.sent[1].getJSONObject("accountBootstrapPayload").getJSONArray("userBootstrapInfos").getJSONObject(0)
        assertEquals(fixtureAccount.name, info.getString("accountIdentifier"))
        assertEquals(fixtureKey.base64(), info.getString("userPublicKey"))
        assertNull(protocol.receive(challenge()))
        val request = hooks.sent[2].getJSONObject("accountBootstrapPayload").getJSONObject("exchangeAssertionsForUserCredentialsRequest")
        assertEquals(3, request.getInt("credentialType"))
        assertEquals("1070009224336-sdh77n7uot3oc99ais00jmuft6sk2fg9.apps.googleusercontent.com", request.getString("clientId"))
        assertEquals("1020304050607080", request.getString("sourceAndroidDeviceId"))
        assertEquals(0, request.getInt("platformVariant"))
        assertTrue(request.has("sourceBackupAccountId"))
        assertFalse(request.isNull("sourceBackupAccountId"))
        assertTrue(request.get("sourceBackupAccountId") is String)
        assertEquals("", request.getString("sourceBackupAccountId"))
        assertTrue(request.isNull("targetDeviceSignals"))
        assertFalse(request.getBoolean("deferCredentialsAfterFallback"))
        val assertion = request.getJSONArray("assertions").getJSONObject(0)
        assertEquals(fixtureAccount.name, assertion.getString("accountIdentifier"))
        assertEquals(1, assertion.getInt("assertionType"))
        assertEquals(fixtureChallenge.base64(), assertion.getString("challenge"))
        assertEquals(fixtureSession.base64(), assertion.getString("challengeSessionState"))
        assertEquals("synthetic-client-data".encodeUtf8().base64(), assertion.getString("clientData"))
        assertEquals("synthetic-signed-envelope".encodeUtf8().base64(), assertion.getString("encryptedUserAssertion"))
        assertNull(protocol.receive(results()))
        assertEquals(SourceTransferProtocol.State.FINISH, protocol.state)
        val completed = protocol.receive(finish())!!
        assertEquals(1, completed.size)
        assertEquals(fixtureAccount, completed.single().account)
        assertEquals(1, completed.single().result)
        assertEquals(0, completed.single().lockScreenAuthenticationType)
        assertEquals(2, hooks.sent.last().getInt("bootstrapState"))
        assertEquals(SourceTransferProtocol.State.COMPLETE, protocol.state)
        assertEquals(1, hooks.signingCalls)
    }

    fun testNoRemoteResultsMeansNoCompletion() = runBlocking {
        val hooks = Hooks()
        val protocol = SourceTransferProtocol(peer, hooks)
        protocol.receive(options())
        protocol.receive(challenge())
        rejects { protocol.receive(finish()) }
        assertEquals(3, hooks.sent.size)
        assertEquals(SourceTransferProtocol.State.CLOSED, protocol.state)
    }

    fun testSendFailureCannotReportSuccessfulCompletion() = runBlocking {
        val hooks = Hooks()
        val protocol = SourceTransferProtocol(peer, hooks)
        protocol.receive(options())
        protocol.receive(challenge())
        protocol.receive(results())
        hooks.beforeSend = { if (it.optInt("bootstrapState") == 2) throw IOException("Synthetic broken pipe") }
        rejects { protocol.receive(finish()) }
        assertEquals(SourceTransferProtocol.State.CLOSED, protocol.state)
        assertEquals(3, hooks.sent.size)
    }

    fun testAnotherAccountCannotRequestAssertion() = runBlocking {
        val hooks = Hooks()
        val protocol = SourceTransferProtocol(peer, hooks)
        protocol.receive(options())
        rejects { protocol.receive(challenge(identifier = "another@example.invalid")) }
        assertEquals(0, hooks.signingCalls)
    }

    fun testRepeatedChallengeCannotSignTwice() = runBlocking {
        val hooks = Hooks()
        val protocol = SourceTransferProtocol(peer, hooks)
        protocol.receive(options())
        protocol.receive(challenge())
        rejects { protocol.receive(challenge()) }
        assertEquals(1, hooks.signingCalls)
    }

    fun testMultipleOrFailedChallengesAreRejectedBeforeSigning() = runBlocking {
        for (bad in listOf(challenge(count = 2), challenge(status = 1))) {
            val hooks = Hooks()
            val protocol = SourceTransferProtocol(peer, hooks)
            protocol.receive(options())
            rejects { protocol.receive(bad) }
            assertEquals(0, hooks.signingCalls)
        }
    }

    fun testResultsForAnotherAccountOrFailureAreRejected() = runBlocking {
        for (bad in listOf(results(identifier = "another@example.invalid"), results(result = 2), results(count = 0), results(count = 2))) {
            val hooks = Hooks()
            val protocol = SourceTransferProtocol(peer, hooks)
            protocol.receive(options())
            protocol.receive(challenge())
            rejects { protocol.receive(bad) }
            assertEquals(SourceTransferProtocol.State.CLOSED, protocol.state)
        }
    }

    fun testCancellationDuringConsentLeaksNoAccountMetadata() = runBlocking {
        val hooks = Hooks()
        val consent = CompletableDeferred<List<CryptAuthBootstrapAuthorization>>()
        hooks.consent = { consent.await() }
        val protocol = SourceTransferProtocol(peer, hooks)
        val receiving = async(start = CoroutineStart.UNDISPATCHED) { runCatching { protocol.receive(options()) } }
        assertEquals(SourceTransferProtocol.State.CONSENT, protocol.state)
        protocol.close()
        consent.complete(listOf(hooks.authorization))
        assertTrue(receiving.await().isFailure)
        assertEquals(SourceTransferProtocol.State.CLOSED, protocol.state)
        assertEquals(0, hooks.infoCalls)
        assertTrue(hooks.sent.isEmpty())
    }

    fun testCancellationDuringSigningCannotEmitAssertion() = runBlocking {
        val hooks = Hooks()
        val protocol = SourceTransferProtocol(peer, hooks)
        protocol.receive(options())
        hooks.beforeAssertion = { protocol.close() }
        rejects { protocol.receive(challenge()) }
        assertEquals(SourceTransferProtocol.State.CLOSED, protocol.state)
        assertEquals(2, hooks.sent.size)
    }

    fun testRevokedConsentStopsBeforeSigning() = runBlocking {
        val hooks = Hooks()
        val protocol = SourceTransferProtocol(peer, hooks)
        protocol.receive(options())
        hooks.authorization.valid = false
        rejects { protocol.receive(challenge()) }
        assertEquals(0, hooks.signingCalls)
    }

    fun testMissingOrChangedCheckinIdentityCannotSendAssertion() = runBlocking {
        for (missing in listOf(true, false)) {
            val hooks = Hooks()
            val protocol = SourceTransferProtocol(peer, hooks)
            protocol.receive(options())
            if (missing) hooks.androidId = 0 else hooks.beforeAssertion = { hooks.androidId++ }
            rejects { protocol.receive(challenge()) }
            assertEquals(2, hooks.sent.size)
            assertEquals(if (missing) 0 else 1, hooks.signingCalls)
        }
    }

    fun testEmptyOrMultipleAccountConsentDoesNotEnumerateAccounts() = runBlocking {
        for (number in listOf(0, 2)) {
            val hooks = Hooks()
            hooks.consent = { List(number) { hooks.authorization } }
            val protocol = SourceTransferProtocol(peer, hooks)
            rejects { protocol.receive(options()) }
            assertTrue(hooks.sent.isEmpty())
            assertEquals(0, hooks.infoCalls)
        }
    }

    fun testFidoAndOtherUnsupportedFlowsNeverReportSuccess() = runBlocking {
        val bad = listOf("""{"protocolVersion":3,"bootstrapState":5,"secondDeviceAuthPayload":{}}""",
            """{"protocolVersion":3,"accountTransferMsg":{}}""",
            """{"protocolVersion":4,"bootstrapState":2}""")
        for (json in bad) {
            val hooks = Hooks()
            val protocol = SourceTransferProtocol(peer, hooks)
            rejects { protocol.receive(json.toByteArray()) }
            assertTrue(hooks.sent.isEmpty())
        }
    }

    fun testRemoteCancelAndErrorInvalidateSession() = runBlocking {
        for (state in listOf(1, 3)) {
            val hooks = Hooks()
            val protocol = SourceTransferProtocol(peer, hooks)
            protocol.receive(options())
            rejects { protocol.receive("""{"protocolVersion":3,"bootstrapState":$state}""".toByteArray()) }
            rejects { protocol.receive(challenge()) }
            assertEquals(0, hooks.signingCalls)
        }
    }

    fun testMalformedOversizedNestedAndTrailingInputIsBounded() = runBlocking {
        val invalid = listOf(ByteArray(262145), byteArrayOf(0xc3.toByte(), 0x28),
            ("[".repeat(25) + "0" + "]".repeat(25)).toByteArray(),
            """{"protocolVersion":3} {}""".toByteArray(),
            """{"protocolVersion":"3","bootstrapState":2}""".toByteArray(),
            """{"protocolVersion":3.1,"bootstrapState":2}""".toByteArray())
        for (payload in invalid) {
            val hooks = Hooks()
            val protocol = SourceTransferProtocol(peer, hooks)
            rejects { protocol.receive(payload) }
            assertTrue(hooks.sent.isEmpty())
            assertEquals(SourceTransferProtocol.State.CLOSED, protocol.state)
        }
    }

    fun testLenientJsonCommentsAndLiteralQuotesCannotBypassDepthBound() = runBlocking {
        val nested = "[".repeat(200) + "0" + "]".repeat(200)
        val invalid = listOf("{/*\"*/\"x\":" + nested + ",/*\"*/\"y\":0}",
            "{x:foo\",y:" + nested + ",z:foo\"}",
            "{#\"\n\"x\":" + nested + ",#\"\n\"y\":0}",
            "{'protocolVersion':3,'bootstrapState':2}")
        for (text in invalid) {
            val hooks = Hooks()
            val protocol = SourceTransferProtocol(peer, hooks)
            rejects { protocol.receive(text.toByteArray()) }
            assertTrue(hooks.sent.isEmpty())
            assertEquals(SourceTransferProtocol.State.CLOSED, protocol.state)
        }
    }

    fun testInvalidSessionIdsAndProtocolOrderRejected() = runBlocking {
        for (bad in listOf(options(id = 0), options(id = -1), challenge(), results())) {
            val hooks = Hooks()
            val protocol = SourceTransferProtocol(peer, hooks)
            rejects { protocol.receive(bad) }
            assertTrue(hooks.sent.isEmpty())
        }
    }

    fun testBase64MayBeWrappedButNeverUrlAlphabetOrOversized() = runBlocking {
        val hooks = Hooks()
        val protocol = SourceTransferProtocol(peer, hooks)
        protocol.receive(options())
        protocol.receive(challenge(encoded = " \n" + fixtureChallenge.base64() + "\r\n"))
        assertEquals(1, hooks.signingCalls)
        for (bad in listOf(fixtureChallenge.base64Url(), ByteArray(4097).toByteString().base64(), "!!!")) {
            val invalidHooks = Hooks()
            val invalidProtocol = SourceTransferProtocol(peer, invalidHooks)
            invalidProtocol.receive(options())
            rejects { invalidProtocol.receive(challenge(encoded = bad)) }
            assertEquals(0, invalidHooks.signingCalls)
        }
    }

    fun testBoundedDroidGuardSignalsFitTheCompleteEncodedTransfer() = runBlocking {
        fixtureAccount = Account("\u6f22".repeat(320), "com.google")
        fixtureChallenge = ByteArray(4096) { 0xff.toByte() }.toByteString()
        fixtureSession = ByteArray(65536).toByteString()
        val metadata = "\u6f22".repeat(256)
        val signals = BootstrapSourceSignals(android_id = "ffffffffffffffff", device = metadata, model = metadata,
            sdk_version = metadata, gms_version = metadata, droidguard_result = "A".repeat(CryptAuthBootstrapAssertions.MAX_DROIDGUARD_BYTES),
            screen_lock = BootstrapScreenLockSignals(secure_screen_lock = true, screen_lock_confirmed = true,
                screen_lock_state = 2, seconds_since_unlock = -1, seconds_since_screen_lock_setup = -1))
        CryptAuthBootstrapAssertions.validateSignals(signals)
        CryptAuthBootstrapAssertions.validateChallenge(fixtureChallenge)
        val clientData = CryptAuthBootstrapAssertions.clientData(fixtureAccount.name, fixtureChallenge, signals.encodeByteString())
        assertTrue(clientData.size > 65536)
        assertTrue(clientData.size <= 128 * 1024)
        val pair = CryptAuthCrypto.generateKeyPair()
        val identity = CryptAuthKey("PublicKey", CryptAuthEnrollment.DEVICE_KEY_HANDLE, KeyType.P256,
            CryptAuthCrypto.publicKey(pair.public as ECPublicKey), pair.private.encoded.toByteString(), true)
        val secret = ByteArray(32) { it.toByte() }.toByteString()
        val authzen = CryptAuthKey("authzen", secret.sha256().base64Url().encodeUtf8(), KeyType.RAW256, secret = secret, active = true)
        val state = CryptAuthState("synthetic-wire-budget", keys = listOf(identity, authzen))
        val body = BootstrapAssertionBody(client_data_hash = clientData.sha256(), counter = Long.MAX_VALUE, version = 1)
        val signed = CryptAuthBootstrapCrypto.createEnvelope(state,
            CryptAuthBootstrapPayload(body.encodeByteString(), publicMetadata = byteArrayOf(8, 11, 16, 1).toByteString()))
        val hooks = Hooks().apply { assertion = CryptAuthBootstrapAssertion(clientData, signed.secureMessage) }
        val protocol = SourceTransferProtocol(peer, hooks)
        protocol.receive(options())
        protocol.receive(challenge())
        assertEquals(SourceTransferProtocol.State.RESULTS, protocol.state)
        assertEquals(3, hooks.sent.size)
        val wireBytes = hooks.sentByteSizes.last()
        assertTrue(wireBytes > 200 * 1024)
        assertTrue(wireBytes <= 256 * 1024)
        assertTrue(wireBytes < D2DConnectionContextV1.MAX_PAYLOAD_BYTES)
        val encoded = hooks.sent.last().getJSONObject("accountBootstrapPayload")
            .getJSONObject("exchangeAssertionsForUserCredentialsRequest").getJSONArray("assertions").getJSONObject(0)
        assertEquals(clientData.base64(), encoded.getString("clientData"))
        assertEquals(fixtureSession.base64(), encoded.getString("challengeSessionState"))
        assertEquals(signed.secureMessage.base64(), encoded.getString("encryptedUserAssertion"))

        // Android JSONObject additionally escapes '/' in standard Base64. This adversarial
        // maximum session expands further and must fail atomically at the total message budget.
        fixtureSession = ByteArray(65536) { 0xff.toByte() }.toByteString()
        val expandedHooks = Hooks().apply { assertion = hooks.assertion }
        val expandedProtocol = SourceTransferProtocol(peer, expandedHooks)
        expandedProtocol.receive(options())
        rejects { expandedProtocol.receive(challenge()) }
        assertEquals(2, expandedHooks.sent.size)
        assertEquals(SourceTransferProtocol.State.CLOSED, expandedProtocol.state)
    }

    fun testOversizedCombinationIsRejectedBeforeSendingAnyAssertionBytes() = runBlocking {
        fixtureSession = ByteArray(65536).toByteString()
        val hooks = Hooks().apply {
            assertion = CryptAuthBootstrapAssertion(ByteArray(128 * 1024).toByteString(), ByteArray(65536).toByteString())
        }
        val protocol = SourceTransferProtocol(peer, hooks)
        protocol.receive(options())
        rejects { protocol.receive(challenge()) }
        assertEquals(2, hooks.sent.size)
        assertEquals(SourceTransferProtocol.State.CLOSED, protocol.state)
    }

    fun testClientDataOverItsIndependentBudgetIsRejectedBeforeSending() = runBlocking {
        val hooks = Hooks().apply {
            assertion = CryptAuthBootstrapAssertion(ByteArray(128 * 1024 + 1).toByteString(), "synthetic-signed-envelope".encodeUtf8())
        }
        val protocol = SourceTransferProtocol(peer, hooks)
        protocol.receive(options())
        rejects { protocol.receive(challenge()) }
        assertEquals(2, hooks.sent.size)
        assertEquals(SourceTransferProtocol.State.CLOSED, protocol.state)
    }


    fun testResultGuardDiagnosticsAreDistinctAndNeverAcknowledge() = runBlocking {
        fun altered(change: (JSONObject) -> Unit): ByteArray = JSONObject(String(results(), Charsets.UTF_8)).apply {
            change(getJSONArray("accountTransferResults").getJSONObject(0))
        }.toString().toByteArray()
        val invalid = listOf(
            Triple(results(count = 0), Reason.PROTOCOL_RESULT_COUNT, 10575),
            Triple(results(count = 2), Reason.PROTOCOL_RESULT_COUNT, 10575),
            Triple("""{"protocolVersion":3,"accountTransferResults":[false]}""".toByteArray(), Reason.PROTOCOL_RESULT_ENTRY, 10575),
            Triple(altered { it.put("unknown", "synthetic-private-value") }, Reason.PROTOCOL_RESULT_FIELDS, 10589),
            Triple(altered { it.remove("bootstrapAccount") }, Reason.PROTOCOL_RESULT_ACCOUNT_FORMAT, 10575),
            Triple(altered { it.put("bootstrapAccount", true) }, Reason.PROTOCOL_RESULT_ACCOUNT_FORMAT, 10589),
            Triple(altered { it.getJSONObject("bootstrapAccount").put("name", 123) }, Reason.PROTOCOL_RESULT_ACCOUNT_FORMAT, 10589),
            Triple(results(identifier = "another@example.invalid"), Reason.PROTOCOL_RESULT_ACCOUNT, 10575),
            Triple(altered { it.getJSONObject("bootstrapAccount").put("type", "other") }, Reason.PROTOCOL_RESULT_ACCOUNT, 10575),
            Triple(altered { it.remove("RESULT") }, Reason.PROTOCOL_RESULT_STATUS_FORMAT, 10589),
            Triple(altered { it.put("RESULT", "1") }, Reason.PROTOCOL_RESULT_STATUS_FORMAT, 10589),
            Triple(results(result = 2), Reason.PROTOCOL_RESULT_STATUS, 10575)
        )
        for ((payload, reason, code) in invalid) {
            val hooks = Hooks()
            val protocol = SourceTransferProtocol(peer, hooks)
            protocol.receive(options())
            protocol.receive(challenge())
            rejectsWithReason(reason, code) { protocol.receive(payload) }
            assertEquals(SourceTransferProtocol.State.CLOSED, protocol.state)
            assertEquals(3, hooks.sent.size)
            assertEquals(1, hooks.signingCalls)
        }
    }

    fun testResultsPhaseEnvelopeDiagnosticsStayClosed() = runBlocking {
        val invalid = listOf(
            Triple("""{"protocolVersion":3,"bootstrapState":1}""".toByteArray(), Reason.PROTOCOL_REMOTE_CANCEL, 10564),
            Triple("""{"protocolVersion":3,"bootstrapState":3}""".toByteArray(), Reason.PROTOCOL_REMOTE_ERROR, 10575),
            Triple("""{"protocolVersion":3,"bootstrapState":9}""".toByteArray(), Reason.PROTOCOL_REMOTE_STATE, 10575),
            Triple("""{"protocolVersion":3,"deviceStatus":{"errorCode":1}}""".toByteArray(), Reason.PROTOCOL_DEVICE_STATUS, 10575),
            Triple("""{"protocolVersion":3,"deviceStatus":{"errorCodeFromSource":1}}""".toByteArray(), Reason.PROTOCOL_DEVICE_STATUS, 10575),
            Triple("""{"protocolVersion":3,"unknown":"synthetic-private-value"}""".toByteArray(), Reason.PROTOCOL_FIELD_UNKNOWN, 10589),
            Triple("""{"protocolVersion":3,"accountBootstrapPayload":{},"accountTransferResults":[]}""".toByteArray(), Reason.PROTOCOL_MESSAGE_CONTENT, 10575),
            Triple("""{"protocolVersion":3}""".toByteArray(), Reason.PROTOCOL_MESSAGE_EMPTY, 10575),
            Triple("""{"protocolVersion":4}""".toByteArray(), Reason.PROTOCOL_VERSION, 10575),
            Triple(challenge(), Reason.PROTOCOL_CHALLENGE_ORDER, 10575),
            Triple(options(), Reason.PROTOCOL_OPTIONS_ORDER, 10575),
            Triple(finish(), Reason.PROTOCOL_FINAL_STATE, 10575)
        )
        for ((payload, reason, code) in invalid) {
            val hooks = Hooks()
            val protocol = SourceTransferProtocol(peer, hooks)
            protocol.receive(options())
            protocol.receive(challenge())
            rejectsWithReason(reason, code) { protocol.receive(payload) }
            assertEquals(SourceTransferProtocol.State.CLOSED, protocol.state)
            assertEquals(3, hooks.sent.size)
        }
    }

    fun testInputGuardDiagnosticsKeepOriginalStatus() = runBlocking {
        val invalid = listOf(
            ByteArray(262145) to Reason.PROTOCOL_INCOMING_SIZE,
            ("[".repeat(25) + "0" + "]".repeat(25)).toByteArray() to Reason.PROTOCOL_JSON_DEPTH,
            "[]".toByteArray() to Reason.PROTOCOL_JSON_ROOT,
            "{} {}".toByteArray() to Reason.PROTOCOL_JSON_TRAILING,
            """{"protocolVersion":"3"}""".toByteArray() to Reason.PROTOCOL_FIELD_INTEGER,
            "{'protocolVersion':3}".toByteArray() to Reason.PROTOCOL_JSON_SYNTAX
        )
        for ((payload, reason) in invalid) {
            val hooks = Hooks()
            val protocol = SourceTransferProtocol(peer, hooks)
            rejectsWithReason(reason, 10589) { protocol.receive(payload) }
            assertEquals(SourceTransferProtocol.State.CLOSED, protocol.state)
            assertTrue(hooks.sent.isEmpty())
        }
    }

    fun testExternalFailureAndCancellationAreNotWrappedByDiagnostics() = runBlocking {
        for (failure in listOf(IOException("synthetic failure"), CancellationException("synthetic cancel"))) {
            val hooks = Hooks()
            val protocol = SourceTransferProtocol(peer, hooks)
            protocol.receive(options())
            hooks.beforeAssertion = { throw failure }
            val received = runCatching { protocol.receive(challenge()) }.exceptionOrNull()
            assertSame(failure, received)
            assertEquals(SourceTransferProtocol.State.CLOSED, protocol.state)
            assertEquals(2, hooks.sent.size)
        }
    }

    fun testCredentialCheckpointRequiresAccountResultsAndFinalAck() = runBlocking {
        val hooks = Hooks()
        val protocol = SourceTransferProtocol(peer, hooks)
        protocol.receive(verificationOptions())
        protocol.receive(challenge())
        assertNull(protocol.receive(credentials()))
        assertEquals(1, hooks.verificationCalls)
        assertEquals(SourceTransferProtocol.State.RESULTS, protocol.state)
        val request = hooks.sent.last().getJSONObject("accountBootstrapPayload")
            .getJSONObject("exchangeSessionCheckpointsForUserCredentialsRequest")
        val checkpoint = request.getJSONArray("sessionCheckpoints").getJSONObject(0)
        assertEquals(fixtureAccount.name, checkpoint.getString("accountIdentifier"))
        assertEquals("synthetic-checkpoint", checkpoint.getString("sessionCheckpoint"))
        assertTrue(request.isNull("targetDeviceRiskSignals"))
        assertNull(protocol.receive(results()))
        assertEquals(fixtureAccount, protocol.receive(finish())!!.single().account)
    }

    fun testCheckpointAloneDoesNotCompleteTransfer() = runBlocking {
        val hooks = Hooks()
        val protocol = SourceTransferProtocol(peer, hooks)
        protocol.receive(verificationOptions())
        protocol.receive(challenge())
        protocol.receive(credentials())
        rejectsWithReason(Reason.PROTOCOL_FINAL_STATE, 10575) { protocol.receive(finish()) }
        assertEquals(4, hooks.sent.size)
    }

    fun testModernClassicFlowNegotiatesVerificationWithoutLegacyFlag() = runBlocking {
        for (explicit in listOf(null, false)) {
            val hooks = Hooks()
            val protocol = SourceTransferProtocol(peer, hooks)
            val modern = JSONObject(String(options())).apply {
                getJSONObject("bootstrapOptions").put("flowType", 1)
                    .put("isSourceSideChallengeRequired", explicit)
            }.toString().toByteArray()
            protocol.receive(modern)
            protocol.receive(challenge())
            assertNull(protocol.receive(credentials()))
            assertEquals(1, hooks.verificationCalls)
            assertEquals(SourceTransferProtocol.State.RESULTS, protocol.state)
            assertNull(protocol.receive(results()))
            assertEquals(fixtureAccount, protocol.receive(finish())!!.single().account)
        }
    }

    fun testOtherFlowTypesDoNotEnableVerification() = runBlocking {
        for (flow in listOf(-1, 0, 2, Int.MAX_VALUE)) {
            val hooks = Hooks()
            val protocol = SourceTransferProtocol(peer, hooks)
            val payload = JSONObject(String(options())).apply {
                getJSONObject("bootstrapOptions").put("flowType", flow)
            }.toString().toByteArray()
            protocol.receive(payload)
            protocol.receive(challenge())
            rejectsWithReason(Reason.PROTOCOL_CREDENTIAL_UNEXPECTED, 10575) { protocol.receive(credentials()) }
            assertEquals(0, hooks.verificationCalls)
        }
    }

    fun testMalformedVerificationOptionsAreRejectedBeforeConsent() = runBlocking {
        for ((field, value) in listOf("flowType" to "1", "flowType" to 1.5,
            "flowType" to true, "isSourceSideChallengeRequired" to "true")) {
            val hooks = Hooks()
            val protocol = SourceTransferProtocol(peer, hooks)
            val payload = JSONObject(String(options())).apply {
                getJSONObject("bootstrapOptions").put(field, value)
            }.toString().toByteArray()
            rejectsWithReason(Reason.PROTOCOL_OPTIONS_VERIFICATION, 10589) { protocol.receive(payload) }
            assertEquals(SourceTransferProtocol.State.CLOSED, protocol.state)
            assertTrue(hooks.sent.isEmpty())
        }
    }

    fun testCredentialAccountStatusCountAndUrlAreValidatedBeforeVerification() = runBlocking {
        val invalid = listOf(
            credentials(identifier = "other@example.invalid") to Reason.PROTOCOL_CREDENTIAL_ACCOUNT,
            credentials(status = 1) to Reason.PROTOCOL_CREDENTIAL_STATUS,
            credentials(count = 0) to Reason.PROTOCOL_CREDENTIAL_COUNT,
            credentials(count = 2) to Reason.PROTOCOL_CREDENTIAL_COUNT,
            credentials(url = "https://accounts.google.com.evil.invalid/transfer") to Reason.PROTOCOL_VERIFICATION_URL,
            credentials(url = "http://accounts.google.com/transfer") to Reason.PROTOCOL_VERIFICATION_URL,
            credentials(url = "https://accounts.google.com:444/transfer") to Reason.PROTOCOL_VERIFICATION_URL
        )
        for ((payload, reason) in invalid) {
            val hooks = Hooks()
            val protocol = SourceTransferProtocol(peer, hooks)
            protocol.receive(verificationOptions())
            protocol.receive(challenge())
            rejectsWithReason(reason, 10575) { protocol.receive(payload) }
            assertEquals(0, hooks.verificationCalls)
            assertEquals(3, hooks.sent.size)
        }
    }

    fun testSourceSideVerificationMustBeRequestedInOptions() = runBlocking {
        val hooks = Hooks()
        val protocol = SourceTransferProtocol(peer, hooks)
        protocol.receive(options())
        protocol.receive(challenge())
        rejectsWithReason(Reason.PROTOCOL_CREDENTIAL_UNEXPECTED, 10575) { protocol.receive(credentials()) }
        assertEquals(0, hooks.verificationCalls)
    }

    fun testCredentialPayloadCannotMixChallengesAndCredentials() = runBlocking {
        val hooks = Hooks()
        val protocol = SourceTransferProtocol(peer, hooks)
        protocol.receive(verificationOptions())
        protocol.receive(challenge())
        val payload = JSONObject(String(credentials())).apply {
            getJSONObject("accountBootstrapPayload").put("challenges", JSONArray())
        }.toString().toByteArray()
        rejectsWithReason(Reason.PROTOCOL_CREDENTIAL_FIELDS, 10589) { protocol.receive(payload) }
        assertEquals(0, hooks.verificationCalls)
    }

    fun testCredentialPayloadBeforeAssertionIsRejected() = runBlocking {
        val hooks = Hooks()
        val protocol = SourceTransferProtocol(peer, hooks)
        protocol.receive(verificationOptions())
        rejectsWithReason(Reason.PROTOCOL_CREDENTIAL_ORDER, 10575) { protocol.receive(credentials()) }
        assertEquals(0, hooks.verificationCalls)
    }

    fun testCredentialVerificationCannotReplay() = runBlocking {
        val hooks = Hooks()
        val protocol = SourceTransferProtocol(peer, hooks)
        protocol.receive(verificationOptions())
        protocol.receive(challenge())
        protocol.receive(credentials())
        rejectsWithReason(Reason.PROTOCOL_CREDENTIAL_ORDER, 10575) { protocol.receive(credentials()) }
        assertEquals(1, hooks.verificationCalls)
        assertEquals(4, hooks.sent.size)
    }

    fun testRevocationDuringWebVerificationPreventsCheckpointSend() = runBlocking {
        val hooks = Hooks()
        val protocol = SourceTransferProtocol(peer, hooks)
        protocol.receive(verificationOptions())
        protocol.receive(challenge())
        hooks.verify = { hooks.authorization.valid = false; "synthetic-checkpoint" }
        rejects { protocol.receive(credentials()) }
        assertEquals(3, hooks.sent.size)
    }

    fun testCloseDuringWebVerificationPreventsCheckpointSend() = runBlocking {
        val hooks = Hooks()
        val protocol = SourceTransferProtocol(peer, hooks)
        protocol.receive(verificationOptions())
        protocol.receive(challenge())
        hooks.verify = { protocol.close(); "synthetic-checkpoint" }
        rejectsWithReason(Reason.PROTOCOL_CLOSED, 10564) { protocol.receive(credentials()) }
        assertEquals(3, hooks.sent.size)
    }

    fun testMalformedCheckpointCannotBeSent() = runBlocking {
        for (value in listOf("", "synthetic;other=cookie", "synthetic\nvalue", "x".repeat(65537))) {
            val hooks = Hooks()
            val protocol = SourceTransferProtocol(peer, hooks)
            protocol.receive(verificationOptions())
            protocol.receive(challenge())
            hooks.verify = { value }
            rejectsWithReason(Reason.PROTOCOL_CHECKPOINT_VALUE, 10575) { protocol.receive(credentials()) }
            assertEquals(3, hooks.sent.size)
        }
    }

    private fun verificationOptions(): ByteArray = JSONObject(String(options())).apply {
        getJSONObject("bootstrapOptions").put("isSourceSideChallengeRequired", true)
    }.toString().toByteArray()

    private fun credentials(identifier: String = fixtureAccount.name, status: Int = 0, count: Int = 1,
                            url: String = "https://accounts.google.com/transfer?fixture=1"): ByteArray {
        val entries = JSONArray()
        repeat(count) { entries.put(JSONObject().put("accountIdentifier", identifier).put("status", status)
            .put("fallbackUrl", url).put("credential", "synthetic-credential-never-echoed")) }
        return JSONObject().put("protocolVersion", 3).put("accountBootstrapPayload",
            JSONObject().put("userCredentials", entries)).toString().toByteArray()
    }

    private suspend fun rejectsWithReason(reason: Reason, code: Int, action: suspend () -> Any?) {
        val failure = runCatching { action() }.exceptionOrNull()
        assertTrue("Expected a protocol guard", failure is SourceTransferProtocolException)
        failure as SourceTransferProtocolException
        assertEquals(reason, failure.reason)
        assertEquals(code, failure.statusCode)
        assertEquals("Account transfer failed ($code)", failure.message)
    }

    private fun options(id: Long = -55): ByteArray = JSONObject().put("protocolVersion", 3)
        .put("bootstrapOptions", JSONObject().put("deviceType", 4).put("sessionId", id)
            .put("deviceName", "Synthetic Watch").put("optionFlags", (1L shl 12) or (1L shl 14))
            .put("optionFlagsSetIndicator", -1L).put("supportsPacketMode", true).put("maxPacketSize", 1024))
        .toString().toByteArray()

    private fun challenge(identifier: String = fixtureAccount.name, count: Int = 1, status: Int = 0,
                          encoded: String = fixtureChallenge.base64()): ByteArray {
        val array = JSONArray()
        repeat(count) { array.put(JSONObject().put("status", status).put("accountIdentifier", identifier)
            .put("challenge", encoded).put("challengeSessionState", fixtureSession.base64())) }
        return JSONObject().put("protocolVersion", 3).put("accountBootstrapPayload", JSONObject().put("challenges", array)).toString().toByteArray()
    }

    private fun results(identifier: String = fixtureAccount.name, result: Int = 1, count: Int = 1): ByteArray {
        val array = JSONArray()
        repeat(count) { array.put(JSONObject().put("bootstrapAccount", JSONObject().put("name", identifier).put("type", "com.google"))
            .put("RESULT", result).put("lockScreenAuthenticationType", 999)) }
        return JSONObject().put("protocolVersion", 3).put("accountTransferResults", array).toString().toByteArray()
    }

    private fun finish() = """{"protocolVersion":3,"bootstrapState":2}""".toByteArray()
    private suspend fun rejects(action: suspend () -> Any?) {
        var thrown = false
        try { action() } catch (e: Exception) { thrown = true }
        assertTrue("Expected rejected transfer", thrown)
    }
}
