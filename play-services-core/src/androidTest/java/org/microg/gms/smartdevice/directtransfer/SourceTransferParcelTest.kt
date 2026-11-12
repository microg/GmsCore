/* SPDX-License-Identifier: Apache-2.0 */
package org.microg.gms.smartdevice.directtransfer

import android.accounts.Account
import android.os.BadParcelableException
import android.os.Binder
import android.os.IBinder
import android.os.Parcel
import android.test.AndroidTestCase
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.internal.safeparcel.SafeParcelWriter

@Suppress("DEPRECATION")
class SourceTransferParcelTest : AndroidTestCase() {
    fun testPresentationAndSkipFlagsDoNotAuthorizeAnAccount() = parcel { data ->
        val start = SafeParcelWriter.writeObjectHeader(data)
        SafeParcelWriter.write(data, 6, false)
        SafeParcelWriter.write(data, 12, 1)
        SafeParcelWriter.finishObjectHeader(data, start)
        data.setDataPosition(0)
        val result = SourceTransferParcel.restrictions(data)
        assertNull(result.accountNames)
        assertNull(result.authenticatingEmail)
        assertFalse(result.allows(Account("test@example.invalid", "other")))
    }

    fun testExplicitEmptyAccountListAllowsNoAccount() = parcel { data ->
        val start = SafeParcelWriter.writeObjectHeader(data)
        data.writeInt((4 shl 16) or 11) // Field 11: typed account list of size 0.
        data.writeInt(0)
        SafeParcelWriter.finishObjectHeader(data, start)
        data.setDataPosition(0)
        val result = SourceTransferParcel.restrictions(data)
        assertEquals(emptySet<String>(), result.accountNames)
        assertFalse(result.allows(Account("test@example.invalid", "com.google")))
    }

    fun testDuplicateSafeParcelFieldsRejected() = parcel { data ->
        val start = SafeParcelWriter.writeObjectHeader(data)
        SafeParcelWriter.write(data, 6, false)
        SafeParcelWriter.write(data, 6, true)
        SafeParcelWriter.finishObjectHeader(data, start)
        data.setDataPosition(0)
        try { SourceTransferParcel.restrictions(data); fail("Accepted duplicate field") }
        catch (_: BadParcelableException) { }
    }

    fun testOversizedSafeParcelRejectedBeforeReading() = parcel { data ->
        data.writeInt((0xffff shl 16) or 20293)
        data.writeInt(Int.MAX_VALUE)
        data.setDataPosition(0)
        try { SourceTransferParcel.restrictions(data); fail("Accepted oversized object") }
        catch (_: BadParcelableException) { }
    }

    fun testCallbackStatusUsesTransactionOneAndOneway() {
        var received = false
        val binder = object : Binder() {
            override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
                assertEquals(1, code)
                assertEquals(IBinder.FLAG_ONEWAY, flags)
                data.enforceInterface("com.google.android.gms.smartdevice.directtransfer.internal.IDirectTransferCallback")
                assertEquals(1, data.readInt())
                assertEquals(10565, Status.CREATOR.createFromParcel(data).statusCode)
                assertEquals(0, data.dataAvail())
                received = true
                return true
            }
        }
        SourceTransferCallback(binder).onStatus(10565)
        assertTrue(received)
    }

    fun testErrorListenerUsesSynchronousTransactionThree() {
        // Cancellation, timeout and failure retain their codes and share a non-sensitive description.
        for (expectedCode in listOf(16, 15, 8)) {
            var received = 0
            val binder = object : Binder() {
                override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
                    assertEquals(3, code)
                    assertEquals(0, flags)
                    data.enforceInterface("com.google.android.gms.smartdevice.d2d.internal.IDirectTransferListener")
                    assertEquals(expectedCode, data.readInt())
                    assertEquals("Direct transfer did not complete.", data.readString())
                    assertEquals(0, data.dataAvail())
                    requireNotNull(reply).writeNoException()
                    received++
                    return true
                }
            }
            SourceTransferListener(binder).onError(expectedCode)
            assertEquals(1, received)
        }
    }

    private fun parcel(block: (Parcel) -> Unit) {
        val parcel = Parcel.obtain()
        try { block(parcel) } finally { parcel.recycle() }
    }
}
