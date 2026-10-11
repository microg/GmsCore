/* SPDX-License-Identifier: Apache-2.0 */
package org.microg.gms.smartdevice.directtransfer

import android.os.Binder
import android.os.IBinder
import android.os.Parcel
import android.test.AndroidTestCase
import com.google.android.gms.common.internal.IGmsCallbacks

@Suppress("DEPRECATION")
class SourceTransferConnectionTest : AndroidTestCase() {
    fun testBrokerResponseAdvertisesOnlySourceTransferVersionOne() {
        val service = Binder()
        var calls = 0
        val callback = object : Binder() {
            override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
                assertEquals(3, code)
                assertEquals(0, flags)
                data.enforceInterface("com.google.android.gms.common.internal.IGmsCallbacks")
                assertEquals(0, data.readInt())
                assertSame(service, data.readStrongBinder())
                assertEquals(1, data.readInt())
                var foundFeatures = false
                readObject(data) { id, _ ->
                    if (id == 2) {
                        assertFalse(foundFeatures)
                        foundFeatures = true
                        // Decode the public SafeParcel contract independently of both CREATORs.
                        assertEquals(1, data.readInt())
                        assertTrue(data.readInt() != 0)
                        var name: String? = null
                        var version: Long? = null
                        readObject(data) { field, _ ->
                            when (field) {
                                1 -> name = data.readString()
                                3 -> version = data.readLong()
                            }
                        }
                        assertEquals("source_direct_transfer_api", name)
                        assertEquals(1L, requireNotNull(version))
                    }
                }
                assertTrue(foundFeatures)
                assertEquals(0, data.dataAvail())
                calls++
                requireNotNull(reply).writeNoException()
                return true
            }
        }
        // No local interface is attached, so this exercises the real callback proxy and Parcel.
        SourceDirectTransferService.completeConnection(IGmsCallbacks.Stub.asInterface(callback), service)
        assertEquals(1, calls)
    }

    private fun readObject(parcel: Parcel, field: (Int, Int) -> Unit) {
        val header = parcel.readInt()
        assertEquals(20293, header and 65535)
        val size = if (header ushr 16 == 65535) parcel.readInt() else header ushr 16
        val end = parcel.dataPosition() + size
        assertTrue(size >= 0 && end <= parcel.dataSize())
        while (parcel.dataPosition() < end) {
            val next = parcel.readInt()
            val length = if (next ushr 16 == 65535) parcel.readInt() else next ushr 16
            val fieldEnd = parcel.dataPosition() + length
            assertTrue(length >= 0 && fieldEnd <= end)
            field(next and 65535, fieldEnd)
            assertTrue(parcel.dataPosition() <= fieldEnd)
            parcel.setDataPosition(fieldEnd)
        }
        assertEquals(end, parcel.dataPosition())
    }
}
