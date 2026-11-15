/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation.core

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.IBinder
import android.os.IInterface
import android.os.Parcel
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.common.api.ApiMetadata
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.internal.IGmsServiceBroker
import com.google.android.gms.constellation.GetIidTokenResponse
import com.google.android.gms.constellation.GetPnvCapabilitiesResponse
import com.google.android.gms.constellation.PhoneNumberInfo
import com.google.android.gms.constellation.VerifyPhoneNumberResponse
import com.google.android.gms.constellation.internal.IConstellationCallbacks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class ConstellationAidlTransportTest {
    @Test
    fun actionBindsToConstellationBroker() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val connected = CountDownLatch(1)
        var binder: IBinder? = null
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                binder = service
                connected.countDown()
            }

            override fun onServiceDisconnected(name: ComponentName) = Unit
        }
        val intent = Intent("com.google.android.gms.constellation.service.START")
            .setPackage(context.packageName)

        assertTrue(context.bindService(intent, connection, Context.BIND_AUTO_CREATE))
        try {
            assertTrue("Constellation service did not connect", connected.await(10, TimeUnit.SECONDS))
            val serviceBinder = requireNotNull(binder)
            assertNotNull(IGmsServiceBroker.Stub.asInterface(serviceBinder))
            assertEquals(
                "com.google.android.gms.common.internal.IGmsServiceBroker",
                serviceBinder.interfaceDescriptor
            )
        } finally {
            context.unbindService(connection)
        }
    }

    @Test
    fun callbackResponseSurvivesAidlProxyParcelRoundTrip() {
        var deliveredStatus: Status? = null
        var deliveredResponse: GetIidTokenResponse? = null
        val delivered = CountDownLatch(1)
        val stub = object : IConstellationCallbacks.Stub() {
            override fun onIidTokenGenerated(
                status: Status?,
                response: GetIidTokenResponse?,
                apiMetadata: ApiMetadata?
            ) {
                deliveredStatus = status
                deliveredResponse = response
                delivered.countDown()
            }

            override fun onPhoneNumberVerified(
                status: Status?,
                phoneNumbers: MutableList<PhoneNumberInfo?>?,
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
        val proxy = IConstellationCallbacks.Stub.asInterface(ProxyForcingBinder(stub))
        val response = GetIidTokenResponse("iid", "fid", byteArrayOf(1, 2, 3), 1234L)

        proxy.onIidTokenGenerated(Status.SUCCESS, response, ApiMetadata.DEFAULT)

        assertTrue("AIDL callback was not delivered", delivered.await(5, TimeUnit.SECONDS))
        assertEquals(Status.SUCCESS.statusCode, deliveredStatus?.statusCode)
        assertEquals("iid", deliveredResponse?.iidToken)
        assertEquals("fid", deliveredResponse?.fid)
        assertTrue(deliveredResponse?.signature?.contentEquals(byteArrayOf(1, 2, 3)) == true)
        assertEquals(1234L, deliveredResponse?.timestamp)
    }

    private class ProxyForcingBinder(private val delegate: IBinder) : Binder() {
        override fun queryLocalInterface(descriptor: String): IInterface? = null

        override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean =
            delegate.transact(code, data, reply, flags)
    }
}
