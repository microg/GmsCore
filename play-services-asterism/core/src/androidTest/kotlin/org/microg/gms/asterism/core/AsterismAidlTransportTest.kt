/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.asterism.core

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.IBinder
import android.os.IInterface
import android.os.Parcel
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.asterism.GetAsterismConsentResponse
import com.google.android.gms.asterism.SetAsterismConsentResponse
import com.google.android.gms.asterism.internal.IAsterismCallbacks
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.internal.IGmsServiceBroker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class AsterismAidlTransportTest {
    @Test
    fun actionBindsToAsterismBroker() {
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
        val intent = Intent("com.google.android.gms.asterism.service.START")
            .setPackage(context.packageName)

        assertTrue(context.bindService(intent, connection, Context.BIND_AUTO_CREATE))
        try {
            assertTrue("Asterism service did not connect", connected.await(10, TimeUnit.SECONDS))
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
    fun callbackStatusAndBooleanSurviveAidlProxyParcelRoundTrip() {
        var deliveredStatus: Status? = null
        var deliveredValue: Boolean? = null
        val delivered = CountDownLatch(1)
        val stub = object : IAsterismCallbacks.Stub() {
            override fun onConsentFetched(status: Status?, response: GetAsterismConsentResponse?) {
                deliveredStatus = status
            }

            override fun onConsentRegistered(status: Status?, response: SetAsterismConsentResponse?) = Unit

            override fun onIsPnvrConstellationDevice(status: Status?, isPnvrDevice: Boolean) {
                deliveredStatus = status
                deliveredValue = isPnvrDevice
                delivered.countDown()
            }
        }
        val proxy = IAsterismCallbacks.Stub.asInterface(ProxyForcingBinder(stub))

        proxy.onIsPnvrConstellationDevice(Status.SUCCESS, true)

        assertTrue("AIDL callback was not delivered", delivered.await(5, TimeUnit.SECONDS))
        assertEquals(Status.SUCCESS.statusCode, deliveredStatus?.statusCode)
        assertEquals(true, deliveredValue)
    }

    private class ProxyForcingBinder(private val delegate: IBinder) : Binder() {
        override fun queryLocalInterface(descriptor: String): IInterface? = null

        override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean =
            delegate.transact(code, data, reply, flags)
    }
}
