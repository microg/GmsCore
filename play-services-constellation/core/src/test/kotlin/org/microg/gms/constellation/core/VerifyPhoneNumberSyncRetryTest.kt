/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation.core

import android.content.Context
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import com.google.android.gms.constellation.VerifyPhoneNumberRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import okio.ByteString
import okio.ByteString.Companion.toByteString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.microg.gms.constellation.core.proto.ClientAuth
import org.microg.gms.constellation.core.proto.ClientAuthStatus
import org.microg.gms.constellation.core.proto.ClientInfo
import org.microg.gms.constellation.core.proto.Param
import org.microg.gms.constellation.core.proto.PendingVerificationInfo
import org.microg.gms.constellation.core.proto.ProceedResponse
import org.microg.gms.constellation.core.proto.PublicKeyStatus
import org.microg.gms.constellation.core.proto.RequestHeader
import org.microg.gms.constellation.core.proto.SIMAssociation
import org.microg.gms.constellation.core.proto.SyncRequest
import org.microg.gms.constellation.core.proto.SyncResponse
import org.microg.gms.constellation.core.proto.Verification
import org.microg.gms.constellation.core.proto.VerificationAssociation
import org.microg.gms.constellation.core.proto.VerificationInfo
import org.microg.gms.constellation.core.proto.VerificationMethod
import org.microg.gms.constellation.core.proto.VerificationResponse
import org.microg.gms.constellation.core.proto.builder.RequestBuildContext
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class VerifyPhoneNumberSyncRetryTest {
    private lateinit var context: Context
    private lateinit var publicKey: ByteString

    @Before
    fun setUp() {
        AuthManager::class.java.getDeclaredField("instance").apply {
            isAccessible = true
            set(null, null)
        }
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("constellation_prefs", Context.MODE_PRIVATE)
            .edit().clear().commit()
        context.getSharedPreferences("com.google.android.gms.constellation", Context.MODE_PRIVATE)
            .edit().clear().commit()
        publicKey = context.authManager.getOrCreateKeyPair().public.encoded.toByteString()
    }

    @Test
    fun newKeyAcknowledgementAndUnknown_retriesWithAuthAndReturnsVerifiedToken() = runBlocking {
        val requests = mutableListOf<SyncRequest>()
        val responses = listOf(
            response(Verification(), acknowledged = true),
            response(verified("msisdn-token"), acknowledged = false)
        )

        val result = execute(
            requests,
            responses,
            request(includeUnverified = false),
            authenticatedRequest()
        )

        assertEquals(2, requests.size)
        assertNull(requests[0].header_?.client_auth)
        assertNotNull(requests[1].header_?.client_auth)
        assertTrue((requests[1].header_?.client_auth?.signature?.size ?: 0) > 0)
        assertEquals(publicKey, requests[1].header_?.client_info?.client_public_key)
        assertTrue(ConstellationStateStore.isPublicKeyAcked(context))
        assertEquals(1, result.size)
        assertEquals("msisdn-token", result[0].verificationToken)
        assertEquals(1, result[0].verificationStatus)
    }

    @Test
    fun persistentUnknown_isRetriedOnlyOnce_andRemainsUnverified() = runBlocking {
        val requests = mutableListOf<SyncRequest>()
        val result = execute(
            requests,
            listOf(
                response(Verification(), acknowledged = true),
                response(Verification(), acknowledged = false)
            ),
            request(includeUnverified = true),
            authenticatedRequest()
        )

        assertEquals(2, requests.size)
        assertTrue(ConstellationStateStore.isPublicKeyAcked(context))
        assertEquals(1, result.size)
        assertEquals(0, result[0].verificationStatus)
        assertNull(result[0].verificationToken)
    }

    @Test
    fun alreadyAuthenticatedRequest_doesNotRetryAgain() = runBlocking {
        val requests = mutableListOf<SyncRequest>()
        val result = execute(
            requests,
            listOf(response(Verification(), acknowledged = true)),
            request(includeUnverified = true),
            initialSyncRequest = authenticatedRequest()
        )

        assertEquals(1, requests.size)
        assertEquals(0, result[0].verificationStatus)
    }

    @Test
    fun missingAcknowledgement_doesNotRetryOrPersistKeyAck() = runBlocking {
        val requests = mutableListOf<SyncRequest>()
        val result = execute(
            requests,
            listOf(response(Verification(), acknowledged = false)),
            request(includeUnverified = true)
        )

        assertEquals(1, requests.size)
        assertFalse(ConstellationStateStore.isPublicKeyAcked(context))
        assertEquals(1, result.size)
        assertEquals(0, result[0].verificationStatus)
    }

    @Test
    fun nonUnknownInitialResult_doesNotRetry() = runBlocking {
        val requests = mutableListOf<SyncRequest>()
        val result = execute(
            requests,
            listOf(response(verified("already-verified"), acknowledged = true)),
            request(includeUnverified = true)
        )

        assertEquals(1, requests.size)
        assertTrue(ConstellationStateStore.isPublicKeyAcked(context))
        assertEquals(1, result.size)
        assertEquals("already-verified", result[0].verificationToken)
    }

    @Test
    fun pendingInitialResult_doesNotRetry() = runBlocking {
        val requests = mutableListOf<SyncRequest>()
        val result = execute(
            requests,
            listOf(
                response(
                    Verification(pending_verification_info = PendingVerificationInfo()),
                    acknowledged = true
                )
            ),
            request(includeUnverified = true)
        )

        assertEquals(1, requests.size)
        assertTrue(ConstellationStateStore.isPublicKeyAcked(context))
        assertEquals(1, result.size)
        assertEquals(6, result[0].verificationStatus)
    }

    @Test
    fun mixedInitialResults_doNotRetryOrDropVerifiedResult() = runBlocking {
        val requests = mutableListOf<SyncRequest>()
        val result = execute(
            requests,
            listOf(
                SyncResponse(
                    responses = listOf(
                        VerificationResponse(verification = Verification()),
                        VerificationResponse(verification = verified("already-verified"))
                    ),
                    client_auth_status = acknowledgement()
                )
            ),
            request(includeUnverified = true)
        )

        assertEquals(1, requests.size)
        assertEquals(2, result.size)
        assertEquals(0, result[0].verificationStatus)
        assertEquals("already-verified", result[1].verificationToken)
    }

    @Test
    fun mismatchedAcknowledgementSession_doesNotRetryOrPersistKeyAck() = runBlocking {
        val requests = mutableListOf<SyncRequest>()
        val mismatch = response(
            Verification(),
            acknowledged = true,
            ackSessionId = "different-session"
        )

        execute(
            requests,
            listOf(mismatch),
            request(includeUnverified = true)
        )

        assertEquals(1, requests.size)
        assertFalse(ConstellationStateStore.isPublicKeyAcked(context))
    }

    @Test
    fun wireAcknowledgementFieldsDecodeForSyncAndProceedResponses() {
        val clientAuthStatusWire = byteArrayOf(0x0A, 0x02, 0x08, 0x01)
        val syncResponse = SyncResponse.ADAPTER.decode(
            byteArrayOf(0x1A, clientAuthStatusWire.size.toByte()) + clientAuthStatusWire
        )
        val proceedResponse = ProceedResponse.ADAPTER.decode(
            byteArrayOf(0x12, clientAuthStatusWire.size.toByte()) + clientAuthStatusWire
        )

        assertEquals(
            1,
            syncResponse.client_auth_status?.public_key_status?.status
        )
        assertEquals(
            1,
            proceedResponse.client_auth_status?.public_key_status?.status
        )
    }

    @Test
    fun retryMerge_preservesUnretriedImsisFromMultiSimInitialResult() {
        val first = response(
            associated(
                Verification(),
                listOf("imsi-1", "imsi-2")
            ),
            acknowledged = true
        )
        val retry = response(
            associated(
                verified("token-1"),
                listOf("imsi-1")
            ),
            acknowledged = false
        )

        val merged = mergeUnknownSyncResults(first, retry)

        assertEquals(2, merged.responses.size)
        assertEquals(
            listOf("imsi-1"),
            merged.responses[0].verification?.association?.sim?.sim_info?.imsi
        )
        assertEquals(
            listOf("imsi-2"),
            merged.responses[1].verification?.association?.sim?.sim_info?.imsi
        )
    }

    @Test
    fun acknowledgementCannotBePersistedForDifferentKey() {
        assertFalse(context.authManager.setPublicKeyAckedIfCurrent(byteArrayOf(1, 2, 3)))
        assertFalse(ConstellationStateStore.isPublicKeyAcked(context))
    }

    @Test
    fun cancellationDuringFirstRpc_preventsRetryAndAckPersistence() = runBlocking {
        val requests = mutableListOf<SyncRequest>()
        val thrown = runCatching {
            execute(
                requests,
                listOf(response(Verification(), acknowledged = true)),
                request(includeUnverified = true),
                authenticatedRequest(),
                cancelAfterFirstResponse = true
            )
        }.exceptionOrNull()

        assertTrue(thrown is CancellationException)
        assertEquals(1, requests.size)
        assertFalse(ConstellationStateStore.isPublicKeyAcked(context))
    }

    private suspend fun execute(
        requests: MutableList<SyncRequest>,
        responses: List<SyncResponse>,
        request: VerifyPhoneNumberRequest,
        retryRequest: SyncRequest? = null,
        cancelAfterFirstResponse: Boolean = false,
        initialSyncRequest: SyncRequest = initialRequest()
    ) = executeSyncFlow(
        context = context,
        sessionId = SESSION_ID,
        request = request,
        syncRequest = initialSyncRequest,
        buildContext = RequestBuildContext("synthetic-iid", emptyList()),
        imsiToInfoMap = emptyMap(),
        callingPackage = "com.example.messages",
        syncCall = { syncRequest ->
            requests += syncRequest
            val response = responses[requests.lastIndex]
            if (cancelAfterFirstResponse && requests.size == 1) {
                throw CancellationException("cancel before authenticated retry")
            }
            response
        },
        retryRequestBuilder = {
            retryRequest ?: error("Unexpected authenticated retry")
        }
    )

    private fun initialRequest() = SyncRequest(
        header_ = RequestHeader(
            client_info = ClientInfo(client_public_key = publicKey),
            session_id = SESSION_ID
        )
    )

    private fun authenticatedRequest() = SyncRequest(
        header_ = RequestHeader(
            client_info = ClientInfo(client_public_key = publicKey),
            client_auth = ClientAuth(signature = byteArrayOf(1).toByteString()),
            session_id = SESSION_ID
        )
    )

    private fun request(includeUnverified: Boolean) = VerifyPhoneNumberRequest(
        /* policyId */ "",
        /* timeout */ 300L,
        /* idTokenRequest */ null,
        /* extras */ Bundle(),
        /* targetedSims */ emptyList(),
        includeUnverified,
        /* apiVersion */ 0,
        /* verificationMethodsValues */ emptyList()
    )

    private fun response(
        verification: Verification,
        acknowledged: Boolean,
        ackSessionId: String = SESSION_ID
    ) = SyncResponse(
        responses = listOf(VerificationResponse(verification = verification)),
        client_auth_status = if (acknowledged) acknowledgement(ackSessionId) else null
    )

    private fun acknowledgement(sessionId: String = SESSION_ID) = ClientAuthStatus(
        public_key_status = PublicKeyStatus(status = 1),
        session_id = sessionId
    )

    private fun verified(token: String) = Verification(
        verification_info = VerificationInfo(
            phone_number = "+15551234567",
            challenge_method = VerificationMethod.UNKNOWN
        ),
        api_params = listOf(Param(key = "id_token", value_ = token))
    )

    private fun associated(verification: Verification, imsis: List<String>) =
        verification.copy(
            association = VerificationAssociation(
                sim = SIMAssociation(
                    sim_info = SIMAssociation.SIMInfo(imsi = imsis)
                )
            )
        )

    private companion object {
        const val SESSION_ID = "sync-retry-test-session"
    }
}
