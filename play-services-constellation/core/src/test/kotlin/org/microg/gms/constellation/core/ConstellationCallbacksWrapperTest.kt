/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation.core

import android.os.IBinder
import android.os.RemoteException
import com.google.android.gms.constellation.internal.IConstellationCallbacks
import org.junit.Assert.assertSame
import org.junit.Test
import org.mockito.Mockito
import kotlin.coroutines.cancellation.CancellationException

class ConstellationCallbacksWrapperTest {

    private val binder: IBinder = Mockito.mock(IBinder::class.java)

    private fun delegate(): IConstellationCallbacks =
        Mockito.mock(IConstellationCallbacks::class.java).also {
            Mockito.`when`(it.asBinder()).thenReturn(binder)
        }

    @Test
    fun remoteExceptionFromCaller_isContained() {
        val delegate = delegate()
        Mockito.doThrow(Mockito.mock(RemoteException::class.java))
            .`when`(delegate).onPhoneNumberVerified(Mockito.any(), Mockito.any(), Mockito.any())

        // Must not propagate: a dead caller cannot be allowed to crash the request coroutine.
        ConstellationCallbacksWrapper(delegate).onPhoneNumberVerified(null, null, null)
    }

    @Test
    fun runtimeExceptionFromCaller_isContained() {
        val delegate = delegate()
        Mockito.doThrow(IllegalStateException("caller proxy is dead"))
            .`when`(delegate).onIidTokenGenerated(Mockito.any(), Mockito.any(), Mockito.any())

        // The old wrapper only caught RemoteException; this would have escaped and failed the request.
        ConstellationCallbacksWrapper(delegate).onIidTokenGenerated(null, null, null)
    }

    @Test(expected = CancellationException::class)
    fun cancellation_isNotSwallowed() {
        val delegate = delegate()
        Mockito.doThrow(CancellationException("cancelled"))
            .`when`(delegate).onGetPnvCapabilitiesCompleted(Mockito.any(), Mockito.any(), Mockito.any())

        ConstellationCallbacksWrapper(delegate).onGetPnvCapabilitiesCompleted(null, null, null)
    }

    @Test
    fun successfulDelivery_passesThroughToCaller() {
        val delegate = delegate()
        ConstellationCallbacksWrapper(delegate).onPhoneNumberVerificationsCompleted(null, null, null)

        Mockito.verify(delegate)
            .onPhoneNumberVerificationsCompleted(Mockito.any(), Mockito.any(), Mockito.any())
    }

    @Test
    fun asBinder_delegatesToWrappedCallback() {
        val delegate = delegate()
        assertSame(binder, ConstellationCallbacksWrapper(delegate).asBinder())
    }
}
