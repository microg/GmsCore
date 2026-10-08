/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation.core

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.squareup.wire.GrpcClient
import com.squareup.wire.GrpcException
import com.squareup.wire.GrpcStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.microg.gms.constellation.core.proto.AsterismClient
import org.microg.gms.constellation.core.proto.Consent
import org.microg.gms.constellation.core.proto.ConsentVersion
import org.microg.gms.constellation.core.proto.PhoneDeviceVerificationClient
import org.microg.gms.constellation.core.proto.RcsConsent
import org.microg.gms.constellation.core.proto.SetConsentRequest
import org.microg.gms.constellation.core.proto.SetConsentResponse
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.InetAddress
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AutoSetConsentTest {
    private val request = SetConsentRequest(
        asterism_client = AsterismClient.RCS,
        rcs_consent = RcsConsent(
            consent = Consent.CONSENTED,
            consent_version = ConsentVersion.RCS_DEFAULT_ON_LEGAL_FYI
        ),
        consent_version = ConsentVersion.RCS_DEFAULT_ON_LEGAL_FYI
    )

    @Test
    fun permissionDeniedClearsCurrentAndLegacyTokenAndStops() =
        assertRejectedAuthentication(7, GrpcStatus.PERMISSION_DENIED)

    @Test
    fun unauthenticatedClearsCurrentAndLegacyTokenAndStops() =
        assertRejectedAuthentication(16, GrpcStatus.UNAUTHENTICATED)

    private fun assertRejectedAuthentication(code: Int, expected: GrpcStatus) {
        val context = contextWithCachedToken()
        withServer(response(code)) { server, client ->
            var continued = false
            val failure = runCatching {
                runBlocking {
                    autoSetConsent(context, request, client)
                    continued = true
                }
            }.exceptionOrNull()

            assertTrue("authentication status must propagate", failure is GrpcException)
            assertEquals(expected, (failure as GrpcException).grpcStatus)
            assertFalse("the verification flow must not continue after authentication rejection", continued)
            assertNull(ConstellationStateStore.loadDroidGuardToken(context))
            for (name in listOf("constellation_prefs", "com.google.android.gms.constellation")) {
                val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
                assertFalse(prefs.contains("droidguard_token"))
                assertFalse(prefs.contains("droidguard_token_ttl"))
            }
            assertRequest(server)
        }
    }

    @Test
    fun unavailablePreservesBestEffortContinuationAndToken() {
        val context = contextWithCachedToken()
        withServer(response(14)) { server, client ->
            runBlocking { autoSetConsent(context, request, client) }
            assertEquals("cached-token", ConstellationStateStore.loadDroidGuardToken(context))
            assertRequest(server)
        }
    }

    @Test
    fun successPreservesContinuationAndToken() {
        val context = contextWithCachedToken()
        withServer(response(0)) { server, client ->
            runBlocking { autoSetConsent(context, request, client) }
            assertEquals("cached-token", ConstellationStateStore.loadDroidGuardToken(context))
            assertRequest(server)
        }
    }

    @Test
    fun cancelledRpcDoesNotContinueOrClearToken() {
        val context = contextWithCachedToken()
        val delayed = response(0).setBodyDelay(30, TimeUnit.SECONDS)
        withServer(delayed) { server, client ->
            var continued = false
            runBlocking {
                val job = launch(Dispatchers.IO) {
                    autoSetConsent(context, request, client)
                    continued = true
                }
                assertRequest(server)
                job.cancelAndJoin()
            }
            assertFalse("cancellation must leave the production helper", continued)
            assertEquals("cached-token", ConstellationStateStore.loadDroidGuardToken(context))
        }
    }

    private fun contextWithCachedToken(): Context {
        val context = ApplicationProvider.getApplicationContext<Context>()
        for (name in listOf("constellation_prefs", "com.google.android.gms.constellation")) {
            context.getSharedPreferences(name, Context.MODE_PRIVATE).edit()
                .putString("droidguard_token", "cached-token")
                .putLong("droidguard_token_ttl", System.currentTimeMillis() + 60_000)
                .commit()
        }
        return context
    }

    private fun response(code: Int): MockResponse {
        val response = MockResponse()
            .setHeader("content-type", "application/grpc")
            .setHeader("grpc-status", code.toString())
        if (code == 0) {
            val message = SetConsentResponse.ADAPTER.encode(SetConsentResponse())
            response.setBody(Buffer().writeByte(0).writeInt(message.size).write(message))
        }
        return response
    }

    private fun assertRequest(server: MockWebServer) {
        val recorded = server.takeRequest(5, TimeUnit.SECONDS)
        assertTrue("the actual Wire SetConsent RPC must reach the loopback server", recorded != null)
        assertTrue(recorded!!.path!!.endsWith("/SetConsent"))
        val body = recorded.body
        assertEquals(0, body.readByte().toInt())
        val size = body.readInt()
        assertEquals(request, SetConsentRequest.ADAPTER.decode(body.readByteArray(size.toLong())))
        assertEquals(0L, body.size)
        assertEquals(1, server.requestCount)
    }

    private fun withServer(
        response: MockResponse,
        block: (MockWebServer, PhoneDeviceVerificationClient) -> Unit
    ) {
        val server = MockWebServer().apply {
            protocols = listOf(Protocol.H2_PRIOR_KNOWLEDGE)
            enqueue(response)
            start(InetAddress.getByName("127.0.0.1"), 0)
        }
        val httpClient = OkHttpClient.Builder()
            .protocols(listOf(Protocol.H2_PRIOR_KNOWLEDGE))
            .readTimeout(5, TimeUnit.SECONDS)
            .build()
        val client = GrpcClient.Builder()
            .client(httpClient)
            .minMessageToCompress(Long.MAX_VALUE)
            .baseUrl(server.url("/").toString())
            .build()
            .create<PhoneDeviceVerificationClient>()
        try {
            block(server, client)
        } finally {
            httpClient.connectionPool.evictAll()
            httpClient.dispatcher.executorService.shutdown()
            server.shutdown()
        }
    }
}
