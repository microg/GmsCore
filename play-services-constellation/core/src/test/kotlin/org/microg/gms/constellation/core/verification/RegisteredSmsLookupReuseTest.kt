/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation.core.verification

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import okio.ByteString
import okio.ByteString.Companion.decodeHex
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import org.microg.gms.constellation.core.proto.ChallengeResponse
import org.microg.gms.constellation.core.proto.PhoneNumberID
import org.microg.gms.constellation.core.proto.RegisteredSmsChallenge
import org.microg.gms.constellation.core.proto.RegisteredSmsChallengeResponse
import org.microg.gms.constellation.core.proto.RegisteredSmsChallengeResponseItem
import org.microg.gms.constellation.core.proto.RegisteredSmsChallengeResponsePayload
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.io.Closeable

class RegisteredSmsLookupReuseTest {
    @Test
    fun repeatedSecondarySimRowsReuseLookupAndPreserveResponseBytes() {
        Fixture().use { fixture ->
            fixture.setNumbers(PRIMARY_NUMBER, SECONDARY_NUMBER)
            // Current-SIM and unknown-SIM rows must continue using the initial lookup.
            val cursor = fixture.inbox(List(32) { 2 } + listOf(1, -1))

            val response = challenge(SECONDARY_PAYLOAD, PRIMARY_PAYLOAD).verify(fixture.context, 1)

            assertArrayEquals(expected(SECONDARY_PAYLOAD, PRIMARY_PAYLOAD), response.encode())
            verify(cursor).close()
            assertEquals(2, fixture.subscriptionReads)
            assertEquals(2, fixture.lineNumberReads)
        }
    }

    @Test
    fun emptySecondaryLookupDoesNotReuseRequestedSimNumber() {
        Fixture().use { fixture ->
            fixture.setNumbers(PRIMARY_NUMBER, null)
            val cursor = fixture.inbox(List(32) { 2 })

            val response = challenge(PRIMARY_PAYLOAD).verify(fixture.context, 1)

            assertArrayEquals(expected(), response.encode())
            verify(cursor).close()
            assertEquals(2, fixture.subscriptionReads)
            assertEquals(1, fixture.lineNumberReads)
        }
    }

    @Test
    fun platformFailureBeforeSubscriptionLookupReturnsEmptyResponse() {
        Fixture().use { fixture ->
            fixture.setNumbers(PRIMARY_NUMBER, null)
            fixture.failCarrierPrivilegeLookup()

            val response = challenge(PRIMARY_PAYLOAD).verify(fixture.context, 1)

            assertArrayEquals(expected(), response.encode())
            assertEquals(0, fixture.subscriptionReads)
            assertEquals(0, fixture.lineNumberReads)
        }
    }

    @Test
    fun laterInvocationRefreshesNumbersIncludingDefaultSubscription() {
        Fixture().use { fixture ->
            fixture.setNumbers(PRIMARY_NUMBER, SECONDARY_NUMBER)
            val firstCursor = fixture.inbox(List(32) { 2 } + listOf(-1))
            val first = challenge(SECONDARY_PAYLOAD, PRIMARY_PAYLOAD).verify(fixture.context, -1)
            assertArrayEquals(expected(SECONDARY_PAYLOAD, PRIMARY_PAYLOAD), first.encode())
            verify(firstCursor).close()

            fixture.setNumbers(PRIMARY_NUMBER, UPDATED_NUMBER)
            val secondCursor = fixture.inbox(List(32) { 2 } + listOf(-1))
            val second = challenge(UPDATED_PAYLOAD, PRIMARY_PAYLOAD).verify(fixture.context, -1)
            assertArrayEquals(expected(UPDATED_PAYLOAD, PRIMARY_PAYLOAD), second.encode())
            verify(secondCursor).close()
            assertEquals(4, fixture.subscriptionReads)
            assertEquals(6, fixture.lineNumberReads)
        }
    }

    private fun challenge(vararg payloads: ByteString) = RegisteredSmsChallenge(
        verified_senders = payloads.map { PhoneNumberID(phone_number_id = it) }
    )

    private fun expected(vararg payloads: ByteString) = ChallengeResponse(
        registered_sms_response = RegisteredSmsChallengeResponse(
            items = payloads.map {
                RegisteredSmsChallengeResponseItem(
                    payload = RegisteredSmsChallengeResponsePayload(payload = it)
                )
            }
        )
    ).encode()

    @Suppress("DEPRECATION")
    private class Fixture : Closeable {
        val context = mock(Context::class.java)
        private val resolver = mock(ContentResolver::class.java)
        private val subscriptions = mock(SubscriptionManager::class.java)
        private val telephony = mock(TelephonyManager::class.java)
        private val platform = mockStatic(ContextCompat::class.java)
        private var infos: List<SubscriptionInfo> = emptyList()
        private var currentCursor: Cursor? = null
        var subscriptionReads = 0
            private set
        var lineNumberReads = 0
            private set

        init {
            platform.`when`<SubscriptionManager> {
                ContextCompat.getSystemService(context, SubscriptionManager::class.java)
            }.thenReturn(subscriptions)
            platform.`when`<TelephonyManager> {
                ContextCompat.getSystemService(context, TelephonyManager::class.java)
            }.thenReturn(telephony)
            for (permission in listOf(Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_PHONE_NUMBERS)) {
                platform.`when`<Int> { ContextCompat.checkSelfPermission(context, permission) }
                    .thenReturn(PackageManager.PERMISSION_GRANTED)
            }
            `when`(context.contentResolver).thenReturn(resolver)
            doAnswer { currentCursor }.`when`(resolver).query(
                org.mockito.ArgumentMatchers.nullable(Uri::class.java),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
            )
            `when`(telephony.hasCarrierPrivileges()).thenReturn(false)
            `when`(telephony.createForSubscriptionId(anyInt())).thenReturn(telephony)
            `when`(subscriptions.getPhoneNumber(anyInt(), anyInt())).thenReturn("")
            doAnswer {
                subscriptionReads++
                infos
            }.`when`(subscriptions).activeSubscriptionInfoList
            doAnswer {
                lineNumberReads++
                ""
            }.`when`(telephony).line1Number
        }

        fun failCarrierPrivilegeLookup() {
            `when`(telephony.hasCarrierPrivileges())
                .thenThrow(IllegalStateException("telephony service unavailable"))
        }

        fun setNumbers(primary: String, secondary: String?) {
            infos = listOfNotNull(1 to primary, secondary?.let { 2 to it }).map { (id, number) ->
                mock(SubscriptionInfo::class.java).also {
                    `when`(it.subscriptionId).thenReturn(id)
                    `when`(it.number).thenReturn(number)
                }
            }
        }

        fun inbox(subIds: List<Int>): Cursor {
            var row = -1
            return mock(Cursor::class.java).also { cursor ->
                `when`(cursor.moveToNext()).thenAnswer { ++row < subIds.size }
                for ((index, column) in listOf("date", "address", "body", "sub_id").withIndex()) {
                    `when`(cursor.getColumnIndexOrThrow(column)).thenReturn(index)
                }
                `when`(cursor.getLong(0)).thenReturn(1791072000000L)
                `when`(cursor.getString(1)).thenReturn("+15550000099")
                `when`(cursor.getString(2)).thenReturn("Registered SMS test")
                `when`(cursor.getInt(3)).thenAnswer { subIds[row] }
                currentCursor = cursor
            }
        }

        override fun close() = platform.close()
    }

    private companion object {
        const val PRIMARY_NUMBER = "+15550000001"
        const val SECONDARY_NUMBER = "+15550000002"
        const val UPDATED_NUMBER = "+15550000003"

        // Fixed SHA-512 vectors for the inbox timestamp, sender and body above.
        val PRIMARY_PAYLOAD = (
                "ad163e392d947f69bcc0059918551207329723b8c672f269435352d315586cd341" +
                        "ffd7d5735810a570d64fb145328277b63260445e999b8237fa6749762c6186"
                ).decodeHex()
        val SECONDARY_PAYLOAD = (
                "17a95eb0341cd17f6a54680fe78d7b0674e7fa35f962f056b7ae60a0858d13c0a" +
                        "8971a39d509bcfef92f8e112e2763d575e9b1f88c395e1f52fcc0baf3b6355b"
                ).decodeHex()
        val UPDATED_PAYLOAD = (
                "e3758ba61ac0f5d7c0553581c2d586106583c63cb577106f2eb35fa7403463ec89" +
                        "1ca2eabf6cc04975ccbcf329e04a2174a95002484974f0c31aad3da2bf7875"
                ).decodeHex()
    }
}
