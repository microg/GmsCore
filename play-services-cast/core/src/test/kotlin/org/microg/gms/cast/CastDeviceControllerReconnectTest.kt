/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.cast

import android.os.Bundle
import com.google.android.gms.cast.ApplicationMetadata
import com.google.android.gms.cast.ApplicationStatus
import com.google.android.gms.cast.CastDevice
import com.google.android.gms.cast.CastDeviceStatus
import com.google.android.gms.cast.internal.ICastDeviceControllerListener
import com.google.android.gms.common.api.GoogleApiClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
class CastDeviceControllerReconnectTest {
    private val events = mutableListOf<String>()
    private val executors = mutableListOf<SessionExecutor>()

    @After
    fun closeExecutors() {
        executors.forEach { it.shutdownNow() }
    }

    @Test
    fun unexpectedDropSuspendsAndReconnectsInsteadOfEnding() {
        val listener = RecordingListener()
        val controller = controller(listener)
        val session = session(controller)
        val callbacks = callbacks(session)

        callbacks.onDisconnected(CastDeviceSession.STATUS_NETWORK_ERROR)

        assertEquals(listOf("onConnectionSuspended"), listener.events)
        assertFalse(listener.events.contains("onDisconnected"))
        assertEquals(1, executors.single().pending.size)
        // Drop the real socket connect; this test only checks binder session ownership.
        executors.single().pending.clear()

        callbacks.onConnected()

        assertTrue(listener.events.contains("onConnectedWithResult:0"))
        assertFalse(listener.events.contains("onDisconnected"))
    }

    @Test
    fun reconnectRejoinsTheAttachedApplication() {
        val listener = RecordingListener()
        val controller = controller(listener)
        val session = session(controller)
        val callbacks = callbacks(session)
        val application = org.microg.gms.cast.channel.ReceiverApplication(
            "CC1AD845", "Default Media Receiver", "app-session", "transport", "Ready", null, emptyList()
        )
        callbacks.onApplicationConnected(application, true)
        assertEquals("CC1AD845", attached(controller, "attachedApplicationId"))
        assertEquals("app-session", attached(controller, "attachedSessionId"))

        callbacks.onDisconnected(CastDeviceSession.STATUS_NETWORK_ERROR)
        executors.single().pending.clear()
        assertEquals(listOf("onApplicationConnectionSuccess", "onConnectionSuspended"), listener.events)

        callbacks.onConnected()

        assertTrue(listener.events.contains("onConnectedWithResult:0"))
        // Rejoin is posted on the session executor and must not open a socket in this test.
        assertEquals(1, executors.single().pending.size)
        executors.single().pending.clear()
    }

    @Test
    fun failedAutomaticRejoinNotifiesConnectionlessClientAndDropsStaleTarget() {
        val listener = RecordingListener()
        val controller = controller(listener)
        val session = session(controller)
        val callbacks = callbacks(session)
        callbacks.onApplicationConnected(org.microg.gms.cast.channel.ReceiverApplication(
            "CC1AD845", "Default Media Receiver", "app-session", "transport", "Ready", null, emptyList()
        ), true)

        callbacks.onDisconnected(CastDeviceSession.STATUS_NETWORK_ERROR)
        executors.single().pending.clear()
        callbacks.onConnected()

        assertTrue(listener.events.contains("onConnectedWithResult:0"))
        assertEquals(1, executors.single().pending.size)
        // Do not run the real join. Deliver the receiver's "application gone" result directly.
        executors.single().pending.clear()
        callbacks.onApplicationConnectionFailed(CastDeviceSession.STATUS_APPLICATION_NOT_RUNNING)

        assertTrue(listener.events.contains("onApplicationConnectionFailure"))
        assertNull(attached(controller, "attachedApplicationId"))
        assertNull(attached(controller, "attachedSessionId"))

        callbacks.onDisconnected(CastDeviceSession.STATUS_NETWORK_ERROR)
        executors.single().pending.clear()
        callbacks.onConnected()
        assertEquals("A cleared failed rejoin target must not be retried", 0, executors.single().pending.size)
    }
    @Test
    fun successfulLeaveDoesNotRejoinAfterConnectionLoss() {
        val listener = RecordingListener()
        val controller = controller(listener)
        val callbacks = callbacks(session(controller))
        callbacks.onApplicationConnected(org.microg.gms.cast.channel.ReceiverApplication(
            "CC1AD845", "Default Media Receiver", "app-session", "transport", "Ready", null, emptyList()
        ), true)

        callbacks.onLeaveApplicationResult(CastDeviceSession.STATUS_SUCCESS)

        assertEquals(listOf("onApplicationConnectionSuccess", "onLeaveApplicationResult:0"), listener.events)
        callbacks.onDisconnected(CastDeviceSession.STATUS_NETWORK_ERROR)
        executors.single().pending.clear()
        callbacks.onConnected()

        assertTrue(listener.events.contains("onConnectedWithResult:0"))
        // The device connection resumes, but the application the client left must not be rejoined.
        assertEquals("A successful leave must not queue another application join", 0, executors.single().pending.size)
        assertNull(attached(controller, "attachedApplicationId"))
        assertNull(attached(controller, "attachedSessionId"))
    }

    @Test
    fun failedLeavePreservesApplicationForReconnect() {
        val listener = RecordingListener()
        val controller = controller(listener)
        val callbacks = callbacks(session(controller))
        callbacks.onApplicationConnected(org.microg.gms.cast.channel.ReceiverApplication(
            "CC1AD845", "Default Media Receiver", "app-session", "transport", "Ready", null, emptyList()
        ), true)

        callbacks.onLeaveApplicationResult(CastDeviceSession.STATUS_NETWORK_ERROR)

        assertEquals("CC1AD845", attached(controller, "attachedApplicationId"))
        assertEquals("app-session", attached(controller, "attachedSessionId"))
        assertEquals(listOf("onApplicationConnectionSuccess", "onLeaveApplicationResult:7"), listener.events)
        callbacks.onDisconnected(CastDeviceSession.STATUS_NETWORK_ERROR)
        executors.single().pending.clear()
        callbacks.onConnected()

        assertEquals(1, executors.single().pending.size)
        executors.single().pending.clear()
    }

    @Test
    fun exhaustedReconnectsDisconnect() {
        val listener = RecordingListener()
        val controller = controller(listener)
        val session = session(controller)
        val callbacks = callbacks(session)
        repeat(CastDeviceControllerImpl.MAX_RECONNECT_ATTEMPTS) {
            callbacks.onDisconnected(CastDeviceSession.STATUS_NETWORK_ERROR)
            executors.single().pending.clear()
        }
        callbacks.onDisconnected(CastDeviceSession.STATUS_NETWORK_ERROR)
        assertTrue(listener.events.contains("onDisconnected"))
        // Connectionless clients keep the binder so they can call connect() again.
        assertFalse(events.contains("release"))
    }

    @Test
    fun delayedConnectSuccessAfterDisconnectIsIgnored() {
        val listener = RecordingListener()
        val controller = controller(listener)
        val session = session(controller)
        val callbacks = callbacks(session)

        controller.connect()
        // Do not run the real socket connect. Disconnect while that result is still pending.
        executors.single().pending.clear()
        controller.disconnect()
        executors.single().pending.clear()

        // A late success from the released session must not publish a successful reconnect.
        callbacks.onConnected()

        assertEquals(listOf("release"), events)
        assertFalse(listener.events.contains("onConnectedWithResult:0"))
        assertEquals(0, executors.single().pending.size)
    }

    @Test
    fun lateApplicationFailureAfterDisconnectIsIgnored() {
        val listener = RecordingListener()
        val controller = controller(listener)
        val callbacks = callbacks(session(controller))

        controller.disconnect()
        callbacks.onApplicationConnectionFailed(CastDeviceSession.STATUS_APPLICATION_NOT_RUNNING)

        assertEquals(listOf("release"), events)
        assertFalse(listener.events.contains("onApplicationConnectionFailure"))
    }

    @Test
    fun userDisconnectClearsAttachedApplicationAndEndsWithoutReconnect() {
        val listener = RecordingListener()
        val controller = controller(listener)
        callbacks(session(controller)).onApplicationConnected(org.microg.gms.cast.channel.ReceiverApplication(
            "CC1AD845", "Default Media Receiver", "app-session", "transport", "Ready", null, emptyList()
        ), true)

        controller.disconnect()
        executors.single().runPending()

        assertNull(attached(controller, "attachedApplicationId"))
        assertNull(attached(controller, "attachedSessionId"))
        assertEquals(listOf("release"), events)
        assertFalse(listener.events.contains("onConnectionSuspended"))
    }

    private fun controller(listener: RecordingListener): CastDeviceControllerImpl {
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
            it.addListener(listener)
        }
    }

    private fun controlSessionQueue(controller: CastDeviceControllerImpl) {
        val session = session(controller)
        val executorField = CastDeviceSession::class.java.getDeclaredField("executor").apply { isAccessible = true }
        (executorField.get(session) as ScheduledThreadPoolExecutor).shutdownNow()
        val executor = SessionExecutor()
        executorField.set(session, executor)
        executors += executor
    }

    private fun session(controller: CastDeviceControllerImpl): CastDeviceSession {
        return CastDeviceControllerImpl::class.java.getDeclaredField("session")
            .apply { isAccessible = true }.get(controller) as CastDeviceSession
    }

    private fun callbacks(session: CastDeviceSession): CastDeviceSession.Callbacks {
        return CastDeviceSession::class.java.getDeclaredField("callbacks")
            .apply { isAccessible = true }.get(session) as CastDeviceSession.Callbacks
    }

    private fun attached(controller: CastDeviceControllerImpl, name: String): String? {
        return CastDeviceControllerImpl::class.java.getDeclaredField(name)
            .apply { isAccessible = true }.get(controller) as String?
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

    private class RecordingListener : ICastDeviceControllerListener.Stub() {
        val events = mutableListOf<String>()

        override fun onDisconnected(reason: Int) {
            events += "onDisconnected"
        }

        override fun onApplicationConnectionSuccess(
            applicationMetadata: ApplicationMetadata?,
            applicationStatus: String?,
            sessionId: String?,
            wasLaunched: Boolean,
        ) {
            events += "onApplicationConnectionSuccess"
        }

        override fun onApplicationConnectionFailure(statusCode: Int) {
            events += "onApplicationConnectionFailure"
        }

        override fun onTextMessageReceived(namespace: String?, message: String?) {}
        override fun onBinaryMessageReceived(namespace: String?, data: ByteArray?) {}
        override fun onLeaveApplicationResult(statusCode: Int) {
            events += "onLeaveApplicationResult:$statusCode"
        }
        override fun onStopApplicationResult(statusCode: Int) {}
        override fun onApplicationDisconnected(statusCode: Int) {
            events += "onApplicationDisconnected"
        }

        override fun onSendMessageFailure(namespace: String?, requestId: Long, statusCode: Int) {}
        override fun onSendMessageSuccess(namespace: String?, requestId: Long) {}
        override fun onApplicationStatusChanged(applicationStatus: ApplicationStatus?) {}
        override fun onDeviceStatusChanged(deviceStatus: CastDeviceStatus?) {}
        override fun onConnectedWithResult(statusCode: Int) {
            events += "onConnectedWithResult:$statusCode"
        }

        override fun onConnectionSuspended(reason: Int) {
            assertEquals(GoogleApiClient.ConnectionCallbacks.CAUSE_NETWORK_LOST, reason)
            events += "onConnectionSuspended"
        }
    }
}
