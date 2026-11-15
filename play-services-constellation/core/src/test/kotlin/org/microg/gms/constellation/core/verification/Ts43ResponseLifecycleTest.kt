/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation.core.verification

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.telephony.TelephonyManager
import okhttp3.Call
import okhttp3.Connection
import okhttp3.EventListener
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.microg.gms.constellation.core.proto.OdsaOperation
import org.microg.gms.constellation.core.proto.ServiceEntitlementRequest
import org.microg.gms.constellation.core.proto.Ts43ChallengeResponseError
import org.microg.gms.constellation.core.verification.ts43.EapAkaService
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.lang.reflect.InvocationTargetException
import java.net.InetAddress
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Ts43ResponseLifecycleTest {
    private val context = mock(Context::class.java)
    private val telephony = mock(TelephonyManager::class.java)

    init {
        val packages = mock(PackageManager::class.java)
        val info = mock(PackageInfo::class.java).apply { versionName = "test" }
        `when`(context.packageName).thenReturn("org.example.test")
        `when`(context.packageManager).thenReturn(packages)
        `when`(packages.getPackageInfo("org.example.test", 0)).thenReturn(info)
        `when`(telephony.simOperator).thenReturn("310260")
        `when`(telephony.subscriberId).thenReturn("310260000000000")
        `when`(telephony.imei).thenReturn("000000000000001")
    }

    @Test
    fun rejectedOdsaResponsesReleaseEveryConnection() {
        withServer(403, "denied") { client, url, requests, released ->
            repeat(3) {
                val history = mutableListOf<String>()
                val failure = runCatching { odsa(client, url, history) }.exceptionOrNull()!!
                assertFailure(failure, 403, 31, Ts43ChallengeResponseError.RequestType.TS43_REQUEST_TYPE_GET_PHONE_NUMBER_API)
                assertHistory(history, 403)
            }
            assertEquals(3, requests.get())
            assertEquals(3, released.get())
        }
    }

    @Test
    fun successfulOdsaResponsePreservesBodyAndHistory() {
        withServer(200, "phone-number-result") { client, url, requests, released ->
            val history = mutableListOf<String>()
            assertEquals("phone-number-result", odsa(client, url, history))
            assertHistory(history, 200)
            assertEquals(1, requests.get())
            assertEquals(1, released.get())
        }
    }

    @Test
    fun eapTokenEarlyReturnReleasesConnection() {
        withServer(200, "{\"Token\":{\"token\":\"synthetic-token\"}}") { client, url, requests, released ->
            val history = mutableListOf<String>()
            val result = eap(client, url, history)
            assertEquals("synthetic-token", result?.authentication_token)
            assertHistory(history, 200)
            assertEquals(1, requests.get())
            assertEquals(1, released.get())
        }
    }

    @Test
    fun rejectedEapResponsePreservesErrorAndReleasesConnection() {
        withServer(403, "denied") { client, url, requests, released ->
            val history = mutableListOf<String>()
            val failure = runCatching { eap(client, url, history) }.exceptionOrNull()!!
            assertFailure(failure, 403, 31, Ts43ChallengeResponseError.RequestType.TS43_REQUEST_TYPE_AUTH_API)
            assertHistory(history, 403)
            assertEquals(1, requests.get())
            assertEquals(1, released.get())
        }
    }

    private fun odsa(client: OkHttpClient, url: String, history: MutableList<String>): String =
        invokeVerifier(
            "performOdsaRequest", context, telephony, client, history, url,
            OdsaOperation(operation = "GetPhoneNumber"), null, null,
            Ts43ChallengeResponseError.RequestType.TS43_REQUEST_TYPE_GET_PHONE_NUMBER_API
        ) as String

    private fun eap(client: OkHttpClient, url: String, history: MutableList<String>): ServiceEntitlementRequest? =
        invokeVerifier(
            "buildOdsaRequestPayload", context, telephony, EapAkaService(telephony), client,
            history, url, null, ServiceEntitlementRequest(), null
        ) as ServiceEntitlementRequest?

    private fun invokeVerifier(name: String, vararg arguments: Any?): Any? {
        val method = Class.forName("org.microg.gms.constellation.core.verification.Ts43VerifierKt")
            .declaredMethods.single { it.name == name }.apply { isAccessible = true }
        return try {
            method.invoke(null, *arguments)
        } catch (error: InvocationTargetException) {
            throw error.targetException
        }
    }

    private fun assertFailure(error: Throwable, status: Int, code: Int, requestType: Ts43ChallengeResponseError.RequestType) {
        fun property(name: String): Any? = error.javaClass.getDeclaredMethod(name)
            .apply { isAccessible = true }.invoke(error)
        assertEquals(status, property("getHttpStatus"))
        assertEquals(code, (property("getErrorCode") as Ts43ChallengeResponseError.Code).value)
        assertEquals(requestType, property("getRequestType"))
    }

    private fun assertHistory(history: List<String>, code: Int) {
        assertEquals(2, history.size)
        assertTrue(history[0].startsWith("GET http://127.0.0.1:"))
        assertEquals("RESP $code ${history[0].removePrefix("GET ")}", history[1])
    }

    private fun withServer(
        status: Int,
        body: String,
        block: (OkHttpClient, String, AtomicInteger, AtomicInteger) -> Unit
    ) {
        val requests = AtomicInteger()
        val released = AtomicInteger()
        val responses = Collections.synchronizedList(mutableListOf<Response>())
        val server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                requests.incrementAndGet()
                return MockResponse().setResponseCode(status).setBody(body)
            }
        }
        server.start(InetAddress.getByName("127.0.0.1"), 0)
        val client = OkHttpClient.Builder()
            .eventListener(object : EventListener() {
                override fun connectionReleased(call: Call, connection: Connection) {
                    released.incrementAndGet()
                }
            })
            .addNetworkInterceptor { chain ->
                chain.proceed(chain.request()).also { responses += it }
            }
            .build()
        try {
            block(client, "http://127.0.0.1:${server.port}/entitlement", requests, released)
        } finally {
            // Also clean up the deliberately leaking baseline during regression reproduction.
            responses.forEach { it.close() }
            client.connectionPool.evictAll()
            client.dispatcher.executorService.shutdown()
            server.shutdown()
        }
    }
}
