/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.cast

import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.os.RemoteException
import com.google.android.gms.cast.CastDevice
import com.google.android.gms.cast.internal.ICastDeviceControllerListener
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.microg.gms.cast.channel.CastDeviceSession
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.InetAddress
import java.util.ArrayDeque
import java.util.concurrent.ScheduledThreadPoolExecutor

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class CastDeviceControllerLifecycleTest {
    private val events = mutableListOf<String>()
    private val executors = mutableListOf<SessionExecutor>()

    @After
    fun closeExecutors() {
        executors.forEach { it.shutdownNow() }
    }

    @Test
    fun reconnectRestoresListenerDeathNotification() {
        val binder = ListenerBinder()
        val controller = controller(binder)
        controller.disconnect()
        executors.single().runPending()
        assertEquals(0, binder.recipients.size)

        controller.connect()

        assertEquals(listOf("release", "reopen"), events)
        assertEquals(2, binder.linkAttempts)
        assertEquals(1, binder.recipients.size)
        assertTrue(controller.hasInitialListener)

        binder.die()

        assertEquals(listOf("release", "reopen", "release"), events)
        assertFalse(controller.hasInitialListener)
        assertTrue(binder.recipients.isEmpty())
    }

    @Test
    fun staleCallbackFromReplacedListenerDoesNotReleaseCurrentListener() {
        val first = ListenerBinder()
        val controller = controller(first)
        val staleCallback = first.recipients.single()
        val replacement = ListenerBinder()

        controller.addListener(ICastDeviceControllerListener.Stub.asInterface(replacement))

        assertTrue(first.recipients.isEmpty())
        assertEquals(1, replacement.recipients.size)
        staleCallback.binderDied()

        assertTrue(controller.hasInitialListener)
        assertTrue(events.isEmpty())
        assertEquals(1, replacement.recipients.size)

        replacement.die()

        assertFalse(controller.hasInitialListener)
        assertEquals(listOf("release"), events)
    }

    @Test
    fun deadRetainedListenerReleasesReopenedControllerWithoutConnecting() {
        val binder = ListenerBinder()
        val controller = controller(binder)
        controller.disconnect()
        executors.single().runPending()
        binder.die()

        controller.connect()

        assertEquals(listOf("release", "reopen", "release"), events)
        assertEquals(2, binder.linkAttempts)
        assertFalse(controller.hasInitialListener)
        assertTrue(binder.recipients.isEmpty())
        // The only queued operation must close the new session; connect must not be queued after it.
        val reopenedExecutor = executors.last()
        assertEquals(1, reopenedExecutor.pending.size)
        reopenedExecutor.runPending()
        assertTrue(reopenedExecutor.isShutdown)
    }

    private fun controller(binder: ListenerBinder): CastDeviceControllerImpl {
        val device = CastDevice(
            "test-device", "test-device", InetAddress.getByAddress(byteArrayOf(127, 0, 0, 1)),
            8009, "1", "Test receiver", "Test model", null, 0, 1,
        )
        val extras = Bundle().also { device.putInBundle(it) }
        return CastDeviceControllerImpl("test.client", extras, {
            events += "release"
        }, {
            events += "reopen"
            controlSessionQueue(it)
        }).also {
            controlSessionQueue(it)
            it.addListener(ICastDeviceControllerListener.Stub.asInterface(binder))
        }
    }

    // Keep transport work queued: these tests exercise Binder ownership without opening a receiver socket.
    private fun controlSessionQueue(controller: CastDeviceControllerImpl) {
        val session = CastDeviceControllerImpl::class.java.getDeclaredField("session")
            .apply { isAccessible = true }.get(controller) as CastDeviceSession
        val executorField = CastDeviceSession::class.java.getDeclaredField("executor")
            .apply { isAccessible = true }
        (executorField.get(session) as ScheduledThreadPoolExecutor).shutdownNow()
        val executor = SessionExecutor()
        executorField.set(session, executor)
        executors += executor
    }

    private class SessionExecutor : ScheduledThreadPoolExecutor(1) {
        val pending = ArrayDeque<Runnable>()

        override fun execute(command: Runnable) {
            pending.addLast(command)
        }

        fun runPending() {
            while (pending.isNotEmpty()) pending.removeFirst().run()
        }
    }

    private class ListenerBinder : Binder() {
        var linkAttempts = 0
        val recipients = mutableListOf<IBinder.DeathRecipient>()
        private var alive = true

        override fun linkToDeath(recipient: IBinder.DeathRecipient, flags: Int) {
            linkAttempts++
            if (!alive) throw RemoteException("Listener process is dead")
            recipients += recipient
        }

        override fun unlinkToDeath(recipient: IBinder.DeathRecipient, flags: Int) = recipients.remove(recipient)

        fun die() {
            alive = false
            val callbacks = recipients.toList()
            recipients.clear()
            callbacks.forEach { it.binderDied() }
        }
    }
}
