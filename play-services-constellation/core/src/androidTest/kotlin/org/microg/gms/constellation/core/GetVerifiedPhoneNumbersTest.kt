/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation.core

import android.os.Bundle
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.constellation.VerifyPhoneNumberRequest
import com.squareup.wire.GrpcException
import com.squareup.wire.GrpcStatus
import com.squareup.wire.Instant
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.fail
import org.junit.Test
import org.microg.gms.constellation.core.proto.GetVerifiedPhoneNumbersRequest
import org.microg.gms.constellation.core.proto.GetVerifiedPhoneNumbersResponse
import org.microg.gms.constellation.core.proto.IIDTokenAuth
import java.io.IOException

class GetVerifiedPhoneNumbersTest {
    @Test
    fun invalidIidIsRejectedBeforeGpnvRequestIsSent() = runBlocking {
        val backend = RejectEmptyIidBackend()

        try {
            backend.execute(
                GetVerifiedPhoneNumbersRequest(
                    iid_token_auth = IIDTokenAuth(iid_token = "")
                )
            )
            fail("The mock backend accepted an empty IID token")
        } catch (e: GrpcException) {
            assertEquals(GrpcStatus.INVALID_ARGUMENT, e.grpcStatus)
        }
        assertEquals(1, backend.callCount)

        backend.callCount = 0
        var tokenWasSigned = false
        try {
            fetchVerifiedPhoneNumbers(
                context = InstrumentationRegistry.getInstrumentation().targetContext,
                bundle = Bundle(),
                getIidToken = { "" },
                signIidToken = {
                    tokenWasSigned = true
                    byteArrayOf(1) to Instant.ofEpochMilli(1)
                },
                execute = backend::execute
            )
            fail("An empty IID token reached GPNV")
        } catch (_: IllegalArgumentException) {
        }

        assertFalse(tokenWasSigned)
        assertEquals(0, backend.callCount)
    }

    @Test
    fun iidFailureDoesNotSendGpnvRequest() = runBlocking {
        val backend = RejectEmptyIidBackend()

        try {
            fetchVerifiedPhoneNumbers(
                context = InstrumentationRegistry.getInstrumentation().targetContext,
                bundle = Bundle(),
                getIidToken = { throw IOException("test failure") },
                execute = backend::execute
            )
            fail("An IID failure reached GPNV")
        } catch (_: IOException) {
        }

        assertEquals(0, backend.callCount)
    }

    @Test
    fun validIidSendsGpnvRequest() = runBlocking {
        val backend = RejectEmptyIidBackend()

        fetchVerifiedPhoneNumbers(
            context = InstrumentationRegistry.getInstrumentation().targetContext,
            bundle = Bundle(),
            getIidToken = { "iid-token" },
            signIidToken = { byteArrayOf(1) to Instant.ofEpochMilli(1) },
            execute = backend::execute
        )

        assertEquals(1, backend.callCount)
        assertEquals("iid-token", backend.lastRequest?.iid_token_auth?.iid_token)
    }

    @Test
    fun typedIdTokenRequestPopulatesGpnvTokenOption() = runBlocking {
        val backend = RejectEmptyIidBackend()
        val extras = Bundle().apply {
            putString("certificate_hash", "legacy-hash")
            putString("token_nonce", "legacy-nonce")
        }

        fetchVerifiedPhoneNumbers(
            context = InstrumentationRegistry.getInstrumentation().targetContext,
            bundle = extras,
            callingPackage = "com.google.android.apps.messaging",
            idTokenRequest = VerifyPhoneNumberRequest.IdTokenRequest("typed-hash", "typed-nonce"),
            getIidToken = { "iid-token" },
            signIidToken = { byteArrayOf(1) to Instant.ofEpochMilli(1) },
            execute = backend::execute
        )

        assertEquals("typed-hash", backend.lastRequest?.token_option?.certificate_hash)
        assertEquals("typed-nonce", backend.lastRequest?.token_option?.token_nonce)
        assertEquals("com.google.android.apps.messaging", backend.lastRequest?.token_option?.package_name)
        assertEquals("legacy-hash", extras.getString("certificate_hash"))
        assertEquals("legacy-nonce", extras.getString("token_nonce"))
    }

    @Test
    fun legacyExtrasStillPopulateGpnvTokenOption() = runBlocking {
        val backend = RejectEmptyIidBackend()
        val extras = Bundle().apply {
            putString("certificate_hash", "legacy-hash")
            putString("token_nonce", "legacy-nonce")
        }

        fetchVerifiedPhoneNumbers(
            context = InstrumentationRegistry.getInstrumentation().targetContext,
            bundle = extras,
            callingPackage = "com.google.android.apps.messaging",
            getIidToken = { "iid-token" },
            signIidToken = { byteArrayOf(1) to Instant.ofEpochMilli(1) },
            execute = backend::execute
        )

        assertEquals("legacy-hash", backend.lastRequest?.token_option?.certificate_hash)
        assertEquals("legacy-nonce", backend.lastRequest?.token_option?.token_nonce)
        assertEquals("com.google.android.apps.messaging", backend.lastRequest?.token_option?.package_name)
    }

    private class RejectEmptyIidBackend {
        var callCount = 0
        var lastRequest: GetVerifiedPhoneNumbersRequest? = null

        suspend fun execute(request: GetVerifiedPhoneNumbersRequest): GetVerifiedPhoneNumbersResponse {
            callCount++
            lastRequest = request
            if (request.iid_token_auth?.iid_token.isNullOrEmpty()) {
                throw GrpcException(GrpcStatus.INVALID_ARGUMENT, "invalid IID", null)
            }
            return GetVerifiedPhoneNumbersResponse()
        }
    }
}
