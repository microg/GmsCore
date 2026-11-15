/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation.core

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.microg.gms.constellation.core.proto.AsterismConsent.DeviceConsentVersion
import org.microg.gms.constellation.core.proto.Consent
import org.microg.gms.constellation.core.proto.ConsentSource
import org.microg.gms.constellation.core.proto.RcsState
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ConstellationStateStoreCacheTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        tokenPreferences().edit().clear().commit()
    }

    @After
    fun tearDown() {
        tokenPreferences().edit().clear().commit()
    }

    @Test
    fun concurrentWritesForDifferentSimsAreMerged() = runBlocking {
        val start = CountDownLatch(1)
        val expiration = System.currentTimeMillis() + 60_000L
        val writes = listOf(
            record("001010111111111", "+15555550101", expiration),
            record("001010222222222", "+15555550202", expiration)
        )

        val jobs = writes.map { entry ->
            async(Dispatchers.IO) {
                assertTrue(start.await(5, TimeUnit.SECONDS))
                ConstellationStateStore.storeVerifiedNumbers(context, listOf(entry))
            }
        }
        start.countDown()
        withContext(Dispatchers.IO) { jobs.forEach { it.await() } }

        val cached = ConstellationStateStore.loadVerifiedNumbers(context).associateBy { it.imsi }
        assertEquals(writes.size, cached.size)
        writes.forEach { assertEquals(it.phoneNumber, cached[it.imsi]?.phoneNumber) }
    }

    @Test
    fun expiredAndIncompleteRecordsAreNeverCached() {
        val now = System.currentTimeMillis()
        ConstellationStateStore.storeVerifiedNumbers(
            context,
            listOf(
                record("expired", "+15555550303", now - 1),
                record("missing-number", "", now + 60_000L),
                record("", "+15555550404", now + 60_000L)
            )
        )

        assertTrue(ConstellationStateStore.loadVerifiedNumbers(context).isEmpty())
        assertNull(tokenPreferences().getString("verified_phone_numbers_v1", null))
    }

    @Test
    fun pnvrConsentPersistsExplicitAcceptanceAndRejection() {
        assertEquals(Consent.CONSENT_UNKNOWN, ConstellationStateStore.loadPnvrNoticeConsent(context))

        ConstellationStateStore.storePnvrNotice(
            context,
            Consent.CONSENTED,
            ConsentSource.ANDROID_DEVICE_SETTINGS,
            DeviceConsentVersion.PHONE_VERIFICATION_REACHABILITY_INTL_SMS_CALLS
        )
        assertEquals(Consent.CONSENTED, ConstellationStateStore.loadPnvrNoticeConsent(context))

        ConstellationStateStore.storePnvrNotice(
            context,
            Consent.NO_CONSENT,
            ConsentSource.ANDROID_DEVICE_SETTINGS,
            DeviceConsentVersion.PHONE_VERIFICATION_REACHABILITY_INTL_SMS_CALLS
        )
        assertEquals(Consent.NO_CONSENT, ConstellationStateStore.loadPnvrNoticeConsent(context))
    }

    private fun record(imsi: String, phoneNumber: String, expiration: Long) =
        StoredVerifiedNumber(
            imsi = imsi,
            phoneNumber = phoneNumber,
            verificationTimeMillis = 1_700_000_000_000L,
            idToken = null,
            rcsState = RcsState.ACTIVE.value,
            expirationMillis = expiration
        )

    private fun tokenPreferences() =
        context.getSharedPreferences("com.google.android.gms.constellation", Context.MODE_PRIVATE)
}
