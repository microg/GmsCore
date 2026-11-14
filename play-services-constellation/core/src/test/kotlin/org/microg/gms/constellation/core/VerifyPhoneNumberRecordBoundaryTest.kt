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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.microg.gms.settings.SettingsContract
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver

/**
 * Real-API regression for the verification-record boundary. Drives the REAL public
 * handleVerifyPhoneNumberRequest entrypoint against a REAL Robolectric Context + ContentResolver,
 * routing the settings read/write to a test ContentProvider at the module's real settings authority.
 *
 * A CancellationException surfacing inside the handler's try (here, at the
 * "is verification enabled" settings read — the first collaborator inside the try, which the SAME
 * catch block guards as the rest of the flow) must propagate to the dispatcher and must NOT be
 * converted into a recorded verification outcome (ContentResolver.update) or a callback delivery to
 * the dead caller. The disabled-path control proves the handler really does record + deliver on a
 * normal terminal path, so the cancellation test's "zero writes / zero deliveries" is meaningful.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VerifyPhoneNumberRecordBoundaryTest {

    private class RecordingCallbacks : IConstellationCallbacks {
        var deliveries = 0
        override fun onPhoneNumberVerified(
            status: Status?, phoneNumbers: List<PhoneNumberInfo?>?, apiMetadata: ApiMetadata?
        ) { deliveries++ }
        override fun onPhoneNumberVerificationsCompleted(
            status: Status?, response: VerifyPhoneNumberResponse?, apiMetadata: ApiMetadata?
        ) { deliveries++ }
        override fun onIidTokenGenerated(
            status: Status?, response: GetIidTokenResponse?, apiMetadata: ApiMetadata?
        ) { deliveries++ }
        override fun onGetPnvCapabilitiesCompleted(
            status: Status?, response: GetPnvCapabilitiesResponse?, apiMetadata: ApiMetadata?
        ) { deliveries++ }
        override fun asBinder(): IBinder = Binder()
    }

    private class RecordingSettingsProvider(
        private val onQuery: () -> Cursor?,
        private val onUpdate: () -> Unit = {}
    ) : ContentProvider() {
        var updateCount = 0
            private set
        override fun onCreate() = true
        override fun query(
            uri: Uri, projection: Array<out String>?, selection: String?,
            selectionArgs: Array<out String>?, sortOrder: String?
        ): Cursor? = onQuery()
        override fun update(
            uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?
        ): Int { updateCount++; onUpdate(); return 1 }
        override fun insert(uri: Uri, values: ContentValues?): Uri? = null
        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
        override fun getType(uri: Uri): String? = null
    }

    private fun enabledCursor(enabled: Int): Cursor =
        MatrixCursor(arrayOf("v")).apply { addRow(arrayOf<Any>(enabled)) }

    private fun newRequest() = VerifyPhoneNumberRequest(
        /* policyId */ "",
        /* timeout */ 300L,
        /* idTokenRequest */ null,
        /* extras */ Bundle(),
        /* targetedSims */ emptyList(),
        /* includeUnverified */ false,
        /* apiVersion */ 0,
        /* verificationMethodsValues */ emptyList()
    )

    @Test
    fun cancellationInsideHandler_writesNoRecord_andDoesNotDeliver() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val provider = RecordingSettingsProvider(onQuery = { throw CancellationException("caller process died") })
        ShadowContentResolver.registerProviderInternal(SettingsContract.getAuthority(context), provider)

        val callbacks = RecordingCallbacks()
        val thrown = runCatching {
            runBlocking { handleVerifyPhoneNumberRequest(context, callbacks, newRequest(), "com.example.caller") }
        }.exceptionOrNull()

        assertTrue("cancellation must propagate to the dispatcher", thrown is CancellationException)
        assertEquals("a cancelled request must NOT write a verification record", 0, provider.updateCount)
        assertEquals("a cancelled request must NOT deliver to the caller", 0, callbacks.deliveries)
    }

    @Test
    fun cancellationDuringSettingsRead_writesNoRecord_andDoesNotDeliver() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val requestJob = Job()
        val provider = RecordingSettingsProvider(onQuery = {
            requestJob.cancel(CancellationException("caller died during settings read"))
            enabledCursor(0)
        })
        ShadowContentResolver.registerProviderInternal(SettingsContract.getAuthority(context), provider)

        val callbacks = RecordingCallbacks()
        val thrown = runCatching {
            runBlocking(requestJob) {
                handleVerifyPhoneNumberRequest(context, callbacks, newRequest(), "com.example.caller")
            }
        }.exceptionOrNull()

        assertTrue("cancellation must propagate to the dispatcher", thrown is CancellationException)
        assertEquals("cancellation during a synchronous read must prevent the record", 0, provider.updateCount)
        assertEquals("a cancelled request must NOT deliver to the caller", 0, callbacks.deliveries)
    }

    @Test
    fun cancellationDuringRecord_doesNotDeliver() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val requestJob = Job()
        val provider = RecordingSettingsProvider(
            onQuery = { enabledCursor(0) },
            onUpdate = { requestJob.cancel(CancellationException("caller died during settings update")) }
        )
        ShadowContentResolver.registerProviderInternal(SettingsContract.getAuthority(context), provider)

        val callbacks = RecordingCallbacks()
        val thrown = runCatching {
            runBlocking(requestJob) {
                handleVerifyPhoneNumberRequest(context, callbacks, newRequest(), "com.example.caller")
            }
        }.exceptionOrNull()

        assertTrue("cancellation must propagate to the dispatcher", thrown is CancellationException)
        assertEquals("the record began while the request was active", 1, provider.updateCount)
        assertEquals("cancellation during a synchronous update must prevent delivery", 0, callbacks.deliveries)
    }

    @Test
    fun nullParcelableFields_areNormalized_beforeTheHandlerBoundary() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val provider = RecordingSettingsProvider(onQuery = { enabledCursor(0) })
        ShadowContentResolver.registerProviderInternal(SettingsContract.getAuthority(context), provider)

        val callbacks = RecordingCallbacks()
        val request = VerifyPhoneNumberRequest(
            /* policyId */ null,
            /* timeout */ 300L,
            /* idTokenRequest */ null,
            /* extras */ null,
            /* targetedSims */ null,
            /* includeUnverified */ false,
            /* apiVersion */ 0,
            /* verificationMethodsValues */ null
        )

        runBlocking {
            handleVerifyPhoneNumberRequest(context, callbacks, request, "com.example.caller")
        }

        assertEquals("a nullable request still records exactly one terminal outcome", 1, provider.updateCount)
        assertEquals("a nullable request still delivers exactly once", 1, callbacks.deliveries)
    }

    @Test
    fun normalTerminalPath_recordsOutcome_andDelivers() {
        // Control: verification disabled -> a genuine (non-cancellation) terminal outcome.
        val context = ApplicationProvider.getApplicationContext<Context>()
        val provider = RecordingSettingsProvider(onQuery = { enabledCursor(0) })
        ShadowContentResolver.registerProviderInternal(SettingsContract.getAuthority(context), provider)

        val callbacks = RecordingCallbacks()
        runBlocking { handleVerifyPhoneNumberRequest(context, callbacks, newRequest(), "com.example.caller") }

        assertEquals("a completed (disabled) request records exactly one outcome", 1, provider.updateCount)
        assertEquals("a completed (disabled) request delivers exactly once", 1, callbacks.deliveries)
    }
}
