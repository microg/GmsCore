/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation.core

import com.google.android.gms.common.api.ApiMetadata
import com.google.android.gms.common.api.Status
import com.google.android.gms.constellation.GetIidTokenRequest
import com.google.android.gms.constellation.GetIidTokenResponse
import com.google.android.gms.constellation.GetPnvCapabilitiesResponse
import com.google.android.gms.constellation.PhoneNumberInfo
import com.google.android.gms.constellation.VerifyPhoneNumberResponse
import com.google.android.gms.constellation.internal.IConstellationCallbacks
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException

class GetIidTokenTest {
    @Test
    fun validTokenReturnsSuccess() = runBlocking {
        val callbacks = RecordingCallbacks()

        handleGetIidToken(callbacks, GetIidTokenRequest(null), provider { "iid-token" })

        assertEquals(Status.SUCCESS.statusCode, callbacks.status?.statusCode)
        assertEquals("iid-token", callbacks.response?.iidToken)
        assertEquals("fid", callbacks.response?.fid)
        assertArrayEquals(byteArrayOf(1, 2, 3), callbacks.response?.signature)
        assertEquals(1234L, callbacks.response?.timestamp)
    }

    @Test
    fun tokenFailureReturnsInternalError() = runBlocking {
        val callbacks = RecordingCallbacks()

        handleGetIidToken(callbacks, GetIidTokenRequest(null), provider {
            throw IOException("test failure")
        })

        assertEquals(Status.INTERNAL_ERROR.statusCode, callbacks.status?.statusCode)
        assertNull(callbacks.response)
    }

    @Test
    fun emptyTokenReturnsInternalError() = runBlocking {
        val callbacks = RecordingCallbacks()
        var tokenWasSigned = false

        val provider = object : IidCredentialProvider {
            override fun getIidToken(projectNumber: String?): String = ""
            override fun getFid(): String = "fid"
            override fun signIidToken(iidToken: String): Pair<ByteArray, Long> {
                tokenWasSigned = true
                return byteArrayOf(1, 2, 3) to 1234L
            }
        }
        handleGetIidToken(callbacks, GetIidTokenRequest(null), provider)

        assertEquals(Status.INTERNAL_ERROR.statusCode, callbacks.status?.statusCode)
        assertNull(callbacks.response)
        assertEquals(false, tokenWasSigned)
    }

    private fun provider(token: () -> String) = object : IidCredentialProvider {
        override fun getIidToken(projectNumber: String?): String = token()
        override fun getFid(): String = "fid"
        override fun signIidToken(iidToken: String): Pair<ByteArray, Long> =
            byteArrayOf(1, 2, 3) to 1234L
    }

    private class RecordingCallbacks : IConstellationCallbacks.Stub() {
        var status: Status? = null
        var response: GetIidTokenResponse? = null

        override fun onIidTokenGenerated(
            status: Status?,
            response: GetIidTokenResponse?,
            apiMetadata: ApiMetadata?
        ) {
            this.status = status
            this.response = response
        }

        override fun onPhoneNumberVerified(
            status: Status?,
            phoneNumbers: List<PhoneNumberInfo?>?,
            apiMetadata: ApiMetadata?
        ) = Unit

        override fun onPhoneNumberVerificationsCompleted(
            status: Status?,
            response: VerifyPhoneNumberResponse?,
            apiMetadata: ApiMetadata?
        ) = Unit

        override fun onGetPnvCapabilitiesCompleted(
            status: Status?,
            response: GetPnvCapabilitiesResponse?,
            apiMetadata: ApiMetadata?
        ) = Unit
    }
}
