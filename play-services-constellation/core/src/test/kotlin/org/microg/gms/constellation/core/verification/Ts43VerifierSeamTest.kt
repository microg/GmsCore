/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation.core.verification

import android.content.Context
import android.content.ContextWrapper
import android.telephony.TelephonyManager
import android.util.Base64
import androidx.test.core.app.ApplicationProvider
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.microg.gms.constellation.core.proto.ClientChallenge
import org.microg.gms.constellation.core.proto.OdsaOperation
import org.microg.gms.constellation.core.proto.ServiceEntitlementRequest
import org.microg.gms.constellation.core.proto.Ts43Challenge
import org.microg.gms.constellation.core.proto.Ts43ChallengeResponseError
import org.microg.gms.constellation.core.proto.Ts43ChallengeResponseStatus
import org.microg.gms.constellation.core.verification.ts43.Fips186Prf
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Drives Ts43Challenge.verify -> EapAkaService -> ServiceEntitlementBuilder -> OkHttp against a
 * local entitlement server. The server side checks what the client actually sent; failures use
 * the HTTP/EAP shapes an entitlement server returns.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Ts43VerifierSeamTest {
    private val imsi = "310260123456789"
    private val res = ByteArray(8) { (0xA0 + it).toByte() }
    private val ck = ByteArray(16) { (0x10 + it).toByte() }
    private val ik = ByteArray(16) { (0x30 + it).toByte() }
    private lateinit var server: MockWebServer
    private lateinit var context: Context

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        val sim = byteArrayOf(0xDB.toByte(), res.size.toByte()) + res +
                byteArrayOf(ck.size.toByte()) + ck + byteArrayOf(ik.size.toByte()) + ik
        val tm = Mockito.mock(TelephonyManager::class.java)
        Mockito.`when`(tm.simOperator).thenReturn("310260")
        Mockito.`when`(tm.subscriberId).thenReturn(imsi)
        Mockito.`when`(tm.imei).thenReturn("490154203237518")
        Mockito.`when`(tm.getIccAuthentication(anyInt(), anyInt(), anyString()))
            .thenReturn(Base64.encodeToString(sim, Base64.NO_WRAP))
        val base: Context = ApplicationProvider.getApplicationContext()
        context = object : ContextWrapper(base) {
            override fun getSystemService(name: String): Any? =
                if (name == Context.TELEPHONY_SERVICE) tm else super.getSystemService(name)
        }
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun challenge(realm: String = "") = Ts43Challenge(
        entitlement_url = server.url("/entitlement").toString(),
        eap_aka_realm = realm,
        service_entitlement_request = ServiceEntitlementRequest(
            entitlement_version = "12.0",
            terminal_vendor = "Goog",
            terminal_model = "Pixel",
            terminal_software_version = "14",
            app_name = "constellation"
        ),
        client_challenge = ClientChallenge(operation = OdsaOperation(operation = "GetPhoneNumber"))
    )

    private fun eapRelay(packet: ByteArray) =
        JSONObject().put("eap-relay-packet", Base64.encodeToString(packet, Base64.NO_WRAP)).toString()

    private fun akaChallenge(id: Byte): ByteArray {
        val attrs = byteArrayOf(1, 5, 0, 0) + ByteArray(16) { 0x55 } +
                byteArrayOf(2, 5, 0, 0) + ByteArray(16) { 0x66 }
        val len = 8 + attrs.size
        return byteArrayOf(1, id, (len shr 8).toByte(), len.toByte(), 23, 1, 0, 0) + attrs
    }

    private fun macValid(packet: ByteArray, identity: String): Boolean {
        val kAut = Fips186Prf.deriveKeys(identity.toByteArray(), ik, ck)["K_aut"]!!
        val macOffset = packet.size - 16
        val zeroed = packet.copyOf().also { it.fill(0, macOffset, packet.size) }
        val mac = Mac.getInstance("HmacSHA1").apply { init(SecretKeySpec(kAut, "HmacSHA1")) }
            .doFinal(zeroed).copyOf(16)
        return mac.contentEquals(packet.copyOfRange(macOffset, packet.size))
    }

    @Test
    fun authHttpError_isReportedAsAuthApiFailureWithStatus() {
        server.enqueue(MockResponse().setResponseCode(403).setBody("{\"error\":\"forbidden\"}"))

        val response = challenge().verify(context, -1).ts43_challenge_response!!

        val error = response.status!!.error!!
        assertEquals(Ts43ChallengeResponseError.Code.TS43_ERROR_CODE_HTTP_ERROR_STATUS, error.error_code)
        assertEquals(403, error.http_status)
        assertEquals(Ts43ChallengeResponseError.RequestType.TS43_REQUEST_TYPE_AUTH_API, error.request_type)
        assertTrue(response.http_history.any { it.startsWith("RESP 403") })
    }

    @Test
    fun malformedAuthBody_isReportedAsMalformedResponse() {
        server.enqueue(MockResponse().setBody("<html>captive portal</html>"))

        val error = challenge().verify(context, -1).ts43_challenge_response!!.status!!.error!!

        assertEquals(Ts43ChallengeResponseError.Code.TS43_ERROR_CODE_HTTP_MALFORMED_RESPONSE, error.error_code)
        assertEquals(200, error.http_status)
    }

    @Test
    fun connectionDrop_isReportedAsConnectivityFailure() {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

        val error = challenge().verify(context, -1).ts43_challenge_response!!.status!!.error!!

        assertEquals(Ts43ChallengeResponseError.Code.TS43_ERROR_CODE_CONNECTIVITY_FAILURE, error.error_code)
        assertEquals(-1, error.http_status)
    }

    @Test
    fun eapFailureFromServer_isInternalErrorAndNoSecondRoundIsSent() {
        server.enqueue(MockResponse().setBody(eapRelay(byteArrayOf(4, 7, 0, 4))))

        val response = challenge().verify(context, -1).ts43_challenge_response!!

        assertEquals(
            Ts43ChallengeResponseStatus.Code.TS43_STATUS_INTERNAL_ERROR,
            response.status!!.status_code
        )
        assertEquals(1, server.requestCount)
    }

    @Test
    fun akaChallenge_clientResponseVerifiesAgainstAnnouncedIdentity_thenOdsaErrorIsReported() {
        val realm = "nai.epc.carrier.example"
        server.enqueue(MockResponse().setBody(eapRelay(akaChallenge(0x21))))
        // After a correct AKA response the entitlement server issues a token; the ODSA call then fails
        server.enqueue(MockResponse().setBody("{\"Token\":{\"token\":\"tok-1\"}}"))
        server.enqueue(MockResponse().setResponseCode(500))

        val response = challenge(realm).verify(context, -1).ts43_challenge_response!!

        val first = server.takeRequest()
        assertEquals("GET", first.method)
        val eapId = first.requestUrl!!.queryParameter("EAP_ID")
        assertEquals("0$imsi@$realm", eapId)

        val second = server.takeRequest()
        assertEquals("POST", second.method)
        assertEquals("application/vnd.gsma.eap-relay.v1.0+json", second.getHeader("Content-Type")?.substringBefore(';'))
        val sent = Base64.decode(JSONObject(second.body.readUtf8()).getString("eap-relay-packet"), Base64.DEFAULT)
        assertEquals(2, sent[0].toInt())
        assertEquals(0x21, sent[1].toInt())
        assertTrue("AT_MAC must verify for the EAP_ID the client announced", macValid(sent, eapId!!))

        val third = server.takeRequest()
        assertEquals("tok-1", third.requestUrl!!.queryParameter("token"))
        assertEquals("GetPhoneNumber", third.requestUrl!!.queryParameter("operation"))

        val error = response.status!!.error
        assertNotNull(error)
        assertEquals(500, error!!.http_status)
        assertEquals(
            Ts43ChallengeResponseError.RequestType.TS43_REQUEST_TYPE_GET_PHONE_NUMBER_API,
            error.request_type
        )
    }
}
