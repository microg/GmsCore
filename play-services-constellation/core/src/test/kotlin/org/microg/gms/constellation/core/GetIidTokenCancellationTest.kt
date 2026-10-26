/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation.core

import android.os.IBinder
import com.google.android.gms.common.api.ApiMetadata
import com.google.android.gms.common.api.Status
import com.google.android.gms.constellation.GetIidTokenRequest
import com.google.android.gms.constellation.GetIidTokenResponse
import com.google.android.gms.constellation.GetPnvCapabilitiesResponse
import com.google.android.gms.constellation.PhoneNumberInfo
import com.google.android.gms.constellation.VerifyPhoneNumberResponse
import com.google.android.gms.constellation.internal.IConstellationCallbacks
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Drives the REAL handleGetIidToken handler (its real withContext(Dispatchers.IO) body and
 * exception mapping) through the module's own IidCredentialProvider injection seam — not a mock
 * body. Proves that a cancellation surfacing inside the handler propagates and is NOT turned into
 * an INTERNAL_ERROR delivery to a (dead) caller, while genuine failures still map to INTERNAL_ERROR.
 */
class GetIidTokenCancellationTest {

    private class RecordingCallbacks : IConstellationCallbacks {
        var iidStatus: Status? = null
        var deliveries = 0
        override fun onPhoneNumberVerified(
            status: Status?, phoneNumbers: List<PhoneNumberInfo?>?, apiMetadata: ApiMetadata?
        ) { deliveries++ }
        override fun onPhoneNumberVerificationsCompleted(
            status: Status?, response: VerifyPhoneNumberResponse?, apiMetadata: ApiMetadata?
        ) { deliveries++ }
        override fun onIidTokenGenerated(
            status: Status?, response: GetIidTokenResponse?, apiMetadata: ApiMetadata?
        ) { iidStatus = status; deliveries++ }
        override fun onGetPnvCapabilitiesCompleted(
            status: Status?, response: GetPnvCapabilitiesResponse?, apiMetadata: ApiMetadata?
        ) { deliveries++ }
        override fun asBinder(): IBinder = Mockito.mock(IBinder::class.java)
    }

    private fun provider(getToken: () -> String) = object : IidCredentialProvider {
        override fun getIidToken(projectNumber: String?): String = getToken()
        override fun getFid(): String = "fid"
        override fun signIidToken(iidToken: String): Pair<ByteArray, Long> = byteArrayOf(1, 2, 3) to 1234L
    }

    @Test
    fun cancellationSurfacingInHandler_propagates_andDoesNotDeliver() {
        val callbacks = RecordingCallbacks()
        val thrown = runCatching {
            runBlocking {
                handleGetIidToken(
                    callbacks,
                    GetIidTokenRequest(null),
                    provider { throw CancellationException("caller process died") }
                )
            }
        }.exceptionOrNull()

        assertTrue("cancellation must propagate, not be mapped to a Status", thrown is CancellationException)
        assertEquals("a cancelled request must not deliver anything to the caller", 0, callbacks.deliveries)
        assertNull(callbacks.iidStatus)
    }

    @Test
    fun genuineFailure_stillMapsToInternalError_andDelivers() {
        val callbacks = RecordingCallbacks()
        runBlocking {
            handleGetIidToken(callbacks, GetIidTokenRequest(null), provider { throw IOException("boom") })
        }
        assertEquals(1, callbacks.deliveries)
        assertEquals(Status.INTERNAL_ERROR.statusCode, callbacks.iidStatus?.statusCode)
    }

    @Test
    fun success_delivers() {
        val callbacks = RecordingCallbacks()
        runBlocking {
            handleGetIidToken(callbacks, GetIidTokenRequest(null), provider { "iid-token" })
        }
        assertEquals(1, callbacks.deliveries)
        assertEquals(Status.SUCCESS.statusCode, callbacks.iidStatus?.statusCode)
    }

    private fun cancelWhileBlocked(
        callbacks: RecordingCallbacks,
        providerFactory: (() -> Unit) -> IidCredentialProvider
    ) = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val release = CountDownLatch(1)
        val request = launch {
            val credentials = providerFactory {
                entered.complete(Unit)
                check(release.await(5, TimeUnit.SECONDS)) { "credential call was not released" }
            }
            handleGetIidToken(callbacks, GetIidTokenRequest(null), credentials)
        }
        try {
            withTimeout(5_000) { entered.await() }
            request.cancel()
        } finally {
            release.countDown()
            request.join()
        }
    }

    @Test
    fun cancelledWhileTokenLookupBlocks_doesNotSignOrDeliver() {
        val callbacks = RecordingCallbacks()
        var subsequentCredentialCalls = 0
        cancelWhileBlocked(callbacks) { block ->
            object : IidCredentialProvider {
                override fun getIidToken(projectNumber: String?): String {
                    block()
                    return "iid-token"
                }
                override fun getFid(): String {
                    subsequentCredentialCalls++
                    return "fid"
                }
                override fun signIidToken(iidToken: String): Pair<ByteArray, Long> {
                    subsequentCredentialCalls++
                    return byteArrayOf(1) to 1234L
                }
            }
        }
        assertEquals(0, callbacks.deliveries)
        assertEquals(0, subsequentCredentialCalls)
    }

    @Test
    fun cancelledWhileSigningBlocks_doesNotDeliver() {
        val callbacks = RecordingCallbacks()
        cancelWhileBlocked(callbacks) { block ->
            object : IidCredentialProvider {
                override fun getIidToken(projectNumber: String?): String = "iid-token"
                override fun getFid(): String = "fid"
                override fun signIidToken(iidToken: String): Pair<ByteArray, Long> {
                    block()
                    return byteArrayOf(1) to 1234L
                }
            }
        }
        assertEquals(0, callbacks.deliveries)
        assertNull(callbacks.iidStatus)
    }

    @Test
    fun cancelledWhileTokenLookupFails_doesNotDeliverInternalError() {
        val callbacks = RecordingCallbacks()
        cancelWhileBlocked(callbacks) { block ->
            provider {
                block()
                throw IOException("token lookup failed after cancellation")
            }
        }
        assertEquals(0, callbacks.deliveries)
        assertNull(callbacks.iidStatus)
    }
}
