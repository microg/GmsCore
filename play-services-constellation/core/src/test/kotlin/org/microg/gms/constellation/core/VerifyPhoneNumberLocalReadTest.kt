/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation.core

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import androidx.test.core.app.ApplicationProvider
import com.google.android.gms.common.api.ApiMetadata
import com.google.android.gms.common.api.Status
import com.google.android.gms.constellation.GetIidTokenResponse
import com.google.android.gms.constellation.GetPnvCapabilitiesResponse
import com.google.android.gms.constellation.PhoneNumberInfo
import com.google.android.gms.constellation.VerifyPhoneNumberRequest
import com.google.android.gms.constellation.VerifyPhoneNumberResponse
import com.google.android.gms.constellation.internal.IConstellationCallbacks
import com.squareup.wire.Instant
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.microg.gms.constellation.core.proto.RcsState
import org.microg.gms.constellation.core.proto.ServerTimestamp
import org.microg.gms.constellation.core.proto.VerifiedPhoneNumber
import org.microg.gms.settings.SettingsContract
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver

/**
 * Real-API regression for local-read mode. Drives the REAL handleVerifyPhoneNumberRequest with the
 * typed read callback mode and local-read enabled, against a REAL Robolectric Context, real
 * SharedPreferences-backed local state and a test ContentProvider at the module's real settings
 * authority.
 *
 * A local-read request whose targeted SIM already has a stored, unexpired verification must be
 * answered from local state, with no read-only RPC at all. When the stored record is expired, or
 * there is none for the targeted SIM, the RPC must run instead.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VerifyPhoneNumberLocalReadTest {

    private class RecordingCallbacks : IConstellationCallbacks {
        var deliveries = 0
        var lastResponse: VerifyPhoneNumberResponse? = null
        var lastStatus: Status? = null
        override fun onPhoneNumberVerified(
            status: Status?, phoneNumbers: List<PhoneNumberInfo?>?, apiMetadata: ApiMetadata?
        ) {
            deliveries++
        }

        override fun onPhoneNumberVerificationsCompleted(
            status: Status?, response: VerifyPhoneNumberResponse?, apiMetadata: ApiMetadata?
        ) {
            deliveries++
            lastStatus = status
            lastResponse = response
        }

        override fun onIidTokenGenerated(
            status: Status?, response: GetIidTokenResponse?, apiMetadata: ApiMetadata?
        ) {
            deliveries++
        }

        override fun onGetPnvCapabilitiesCompleted(
            status: Status?, response: GetPnvCapabilitiesResponse?, apiMetadata: ApiMetadata?
        ) {
            deliveries++
        }

        override fun asBinder(): IBinder = Binder()
    }

    private class EnabledSettingsProvider : ContentProvider() {
        override fun onCreate() = true
        override fun query(
            uri: Uri, projection: Array<out String>?, selection: String?,
            selectionArgs: Array<out String>?, sortOrder: String?
        ): Cursor = MatrixCursor(arrayOf("v")).apply { addRow(arrayOf<Any>(1)) }

        override fun update(
            uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?
        ): Int = 1

        override fun insert(uri: Uri, values: ContentValues?): Uri? = null
        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
        override fun getType(uri: Uri): String? = null
    }

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        ShadowContentResolver.registerProviderInternal(
            SettingsContract.getAuthority(context), EnabledSettingsProvider()
        )
        // Local state is real SharedPreferences, so clear it between cases.
        context.getSharedPreferences("com.google.android.gms.constellation", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    /** An apiVersion 3 local-read request for one targeted SIM. */
    private fun localReadRequest(imsi: String) = VerifyPhoneNumberRequest(
        /* policyId */ "emergency_location",
        /* timeout */ 300L,
        /* idTokenRequest */ null,
        /* extras */ Bundle(),
        /* targetedSims */ listOf(VerifyPhoneNumberRequest.ImsiRequest(imsi, "")),
        /* includeUnverified */ false,
        /* apiVersion */ 3,
        /* verificationMethodsValues */ emptyList()
    )

    private fun storeNumber(
        imsi: String,
        expirationMillis: Long,
        phoneNumber: String = "+15555550123",
        idToken: String? = "stored-id-token"
    ) = ConstellationStateStore.storeVerifiedNumbers(
        context,
        listOf(
            StoredVerifiedNumber(
                imsi = imsi,
                phoneNumber = phoneNumber,
                verificationTimeMillis = 1_700_000_000_000L,
                idToken = idToken,
                rcsState = RcsState.ACTIVE.value,
                expirationMillis = expirationMillis
            )
        )
    )

    /** Counts read-only RPC attempts and answers with a number that local state never stores. */
    private class CountingRemoteRead {
        var calls = 0
        suspend fun read(): List<VerifiedPhoneNumber> {
            calls++
            return listOf(
                VerifiedPhoneNumber(
                    phone_number = "+15555559999",
                    verification_time = Instant.ofEpochMilli(1_800_000_000_000L),
                    id_token = "remote-id-token",
                    rcs_state = RcsState.ACTIVE
                )
            )
        }
    }

    private fun runLocalRead(
        request: VerifyPhoneNumberRequest,
        remote: CountingRemoteRead,
        callbacks: RecordingCallbacks
    ) = runBlocking {
        handleVerifyPhoneNumberRequest(
            context,
            callbacks,
            request,
            "com.example.caller",
            ReadCallbackMode.TYPED,
            localReadFallback = true,
            readRemoteVerifiedNumbers = { remote.read() }
        )
    }

    @Test
    fun storedUnexpiredNumberForTargetedSim_isAnsweredLocally_withoutRpc() {
        val imsi = "001010123456789"
        storeNumber(imsi, expirationMillis = System.currentTimeMillis() + 600_000L)

        val remote = CountingRemoteRead()
        val callbacks = RecordingCallbacks()
        runLocalRead(localReadRequest(imsi), remote, callbacks)

        assertEquals("local-read must not call the read-only RPC", 0, remote.calls)
        assertEquals("the caller is answered exactly once", 1, callbacks.deliveries)
        assertTrue("a local answer is successful", callbacks.lastStatus?.isSuccess == true)

        val verifications = callbacks.lastResponse?.verifications
        assertEquals("one targeted SIM is answered once", 1, verifications?.size)
        val verification = verifications!![0]
        assertEquals("+15555550123", verification.phoneNumber)
        assertEquals(1_700_000_000_000L, verification.timestampMillis)
        assertEquals("stored-id-token", verification.verificationToken)
        // Same response shape as the read-only RPC path: verified, method/slot unset, no retry.
        assertEquals(1, verification.verificationStatus)
        assertEquals(0, verification.verificationMethod)
        assertEquals(-1, verification.simSlot)
        assertEquals(-1L, verification.retryAfterSeconds)
        assertEquals(RcsState.ACTIVE.value, verification.extras?.getInt("rcs_state"))
    }

    @Test
    fun expiredStoredNumber_fallsBackToRpc() {
        val imsi = "001010123456789"
        // Store it while valid, then let it expire.
        storeNumber(imsi, expirationMillis = System.currentTimeMillis() + 50L)
        Thread.sleep(80L)

        val remote = CountingRemoteRead()
        val callbacks = RecordingCallbacks()
        runLocalRead(localReadRequest(imsi), remote, callbacks)

        assertEquals("an expired record must not be served", 1, remote.calls)
        assertEquals(1, callbacks.deliveries)
        assertEquals(
            "the RPC answer is delivered",
            "+15555559999",
            callbacks.lastResponse?.verifications?.get(0)?.phoneNumber
        )
        assertNull(
            "an expired record is dropped from local state",
            ConstellationStateStore.loadVerifiedNumbers(context).firstOrNull { it.imsi == imsi }
        )
    }

    @Test
    fun noStoredNumberForTargetedSim_fallsBackToRpc() {
        // Valid state, but for a different SIM than the caller targets.
        storeNumber("001010999999999", expirationMillis = System.currentTimeMillis() + 600_000L)

        val remote = CountingRemoteRead()
        val callbacks = RecordingCallbacks()
        runLocalRead(localReadRequest("001010123456789"), remote, callbacks)

        assertEquals("an untargeted SIM's record must not answer this request", 1, remote.calls)
        assertEquals(1, callbacks.deliveries)
        assertEquals(
            "+15555559999",
            callbacks.lastResponse?.verifications?.get(0)?.phoneNumber
        )
    }

    @Test
    fun incompleteTargetedSimDescriptor_fallsBackToRpcInsteadOfServingPartialLocalSubset() {
        val cachedImsi = "001010123456789"
        storeNumber(cachedImsi, expirationMillis = System.currentTimeMillis() + 600_000L)

        val request = VerifyPhoneNumberRequest(
            /* policyId */ "emergency_location",
            /* timeout */ 300L,
            /* idTokenRequest */ null,
            /* extras */ Bundle(),
            /* targetedSims */ listOf(
                VerifyPhoneNumberRequest.ImsiRequest(cachedImsi, ""),
                VerifyPhoneNumberRequest.ImsiRequest("", "")
            ),
            /* includeUnverified */ false,
            /* apiVersion */ 3,
            /* verificationMethodsValues */ emptyList()
        )
        val remote = CountingRemoteRead()
        val callbacks = RecordingCallbacks()

        runLocalRead(request, remote, callbacks)

        assertEquals(
            "an incomplete targeted SIM descriptor must force the authoritative RPC",
            1,
            remote.calls
        )
        assertEquals("the caller is answered exactly once", 1, callbacks.deliveries)
        assertEquals(
            "the partial cached subset must not be returned",
            "+15555559999",
            callbacks.lastResponse?.verifications?.get(0)?.phoneNumber
        )
    }

    @Test
    fun emptyLocalState_fallsBackToRpc() {
        val remote = CountingRemoteRead()
        val callbacks = RecordingCallbacks()
        runLocalRead(localReadRequest("001010123456789"), remote, callbacks)

        assertEquals("empty local state must use the RPC", 1, remote.calls)
        assertEquals(1, callbacks.deliveries)
    }

    @Test
    fun refreshedSimWithoutCacheableReplacement_isEvicted_withoutTouchingOtherSim() {
        val refreshedImsi = "001010123456789"
        val untouchedImsi = "001010999999999"
        val expirationMillis = System.currentTimeMillis() + 600_000L
        storeNumber(
            refreshedImsi,
            expirationMillis = expirationMillis,
            phoneNumber = "+15555550123"
        )
        storeNumber(
            untouchedImsi,
            expirationMillis = expirationMillis,
            phoneNumber = "+15555550456"
        )

        ConstellationStateStore.storeVerifiedNumbers(
            context,
            emptyList(),
            refreshedImsis = setOf(refreshedImsi)
        )

        val remaining = ConstellationStateStore.loadVerifiedNumbers(context)
            .associateBy { it.imsi }
        assertNull(
            "a refreshed SIM without a cacheable replacement must evict stale state",
            remaining[refreshedImsi]
        )
        assertEquals(
            "an untouched SIM must retain its valid cached number",
            "+15555550456",
            remaining[untouchedImsi]?.phoneNumber
        )
    }

    @Test
    fun nextSyncDeadlineMillis_appliesServerReportedOffset() {
        // GMS stores the next sync deadline as local wall clock plus the offset between the
        // server-provided deadline and the server-reported current time.
        val serverNow = 1_800_000_000_000L
        val before = System.currentTimeMillis()
        val deadline = ConstellationStateStore.nextSyncDeadlineMillis(
            ServerTimestamp(
                timestamp = Instant.ofEpochMilli(serverNow + 600_000L),
                now = Instant.ofEpochMilli(serverNow)
            )
        )
        val after = System.currentTimeMillis()

        assertNotNull("both timestamp fields present must yield a deadline", deadline)
        assertTrue(
            "deadline must equal local-now plus the 600s server offset",
            deadline!! in (before + 600_000L)..(after + 600_000L)
        )
    }

    @Test
    fun nextSyncDeadlineMillis_missingTimestamps_returnsNull() {
        val serverNow = Instant.ofEpochMilli(1_800_000_000_000L)
        assertNull(
            "absent next-sync timestamp must not establish freshness",
            ConstellationStateStore.nextSyncDeadlineMillis(null)
        )
        assertNull(
            "missing server timestamp must not establish freshness",
            ConstellationStateStore.nextSyncDeadlineMillis(
                ServerTimestamp(timestamp = null, now = serverNow)
            )
        )
        assertNull(
            "missing server-reported now must not establish freshness",
            ConstellationStateStore.nextSyncDeadlineMillis(
                ServerTimestamp(timestamp = serverNow, now = null)
            )
        )
    }
}
