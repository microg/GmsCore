/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.cameralowlight

import android.os.IBinder
import android.os.Parcel
import android.os.RemoteException
import android.util.Log
import android.view.Surface
import com.google.android.libraries.camera.capture.lowlightboost.internal.CaptureResultParcelable
import com.google.android.libraries.camera.capture.lowlightboost.internal.ILowLightBoostCallback
import com.google.android.libraries.camera.capture.lowlightboost.internal.ILowLightBoostSession
import com.google.android.libraries.camera.capture.lowlightboost.internal.LowLightBoostOptionsParcelable
import org.microg.gms.utils.warnOnTransactionIssues
import java.util.concurrent.atomic.AtomicBoolean

private const val MAX_GLOBAL_SESSIONS = 2
private const val MAX_SESSIONS_PER_UID = 1

internal object LowLightBoostSessionRegistry {
    private val lock = Any()
    private val reservations = mutableSetOf<Any>()
    private val reservationsByUid = mutableMapOf<Int, Int>()

    fun tryAcquire(uid: Int, token: Any): Boolean = synchronized(lock) {
        val uidCount = reservationsByUid[uid] ?: 0
        if (reservations.size >= MAX_GLOBAL_SESSIONS || uidCount >= MAX_SESSIONS_PER_UID) {
            return@synchronized false
        }
        reservations += token
        reservationsByUid[uid] = uidCount + 1
        true
    }

    fun release(uid: Int, token: Any) {
        synchronized(lock) {
            if (!reservations.remove(token)) return
            val remaining = (reservationsByUid[uid] ?: 1) - 1
            if (remaining > 0) reservationsByUid[uid] = remaining else reservationsByUid.remove(uid)
        }
    }
}

internal data class LowLightBoostSessionCreationResult(
    val status: Int,
    val session: LowLightBoostSessionImpl? = null,
)

internal class LowLightBoostSessionImpl(
    private val options: LowLightBoostOptionsParcelable,
    private val callback: ILowLightBoostCallback,
    private val onClosed: (LowLightBoostSessionImpl) -> Unit,
) : ILowLightBoostSession.Stub() {
    private val lock = Any()
    private var renderer: LowLightBoostRenderer? = null
    private var callbackDeathLinked = false
    private var sceneCallbackRejected = false
    private val callbackActive = AtomicBoolean(true)
    private val lifecycleCallbacks = LowLightBoostLifecycleCallbackQueue()
    private var creationCallbackScheduled = false
    private var creationCallbackDelivered = false

    @Volatile
    private var boostMode = options.enableLowLightBoost

    @Volatile
    private var released = false

    lateinit var cameraSurface: Surface
        private set

    private val callbackDeathRecipient = IBinder.DeathRecipient {
        Log.w(TAG, "Client binder died; releasing low light boost session")
        releaseInternal(LowLightBoostSessionTermination.NONE)
    }

    fun open(): Int {
        val newRenderer = LowLightBoostRenderer(
            outputSurface = options.target,
            captureWidth = options.captureWidth,
            captureHeight = options.captureHeight,
            onBoostStrengthChanged = ::emitBoostStrength,
            onRendererFailure = ::handleRendererFailure,
            initialEnabled = boostMode.isLowLightBoostEnabled,
        )
        synchronized(lock) {
            renderer = newRenderer
        }
        if (!newRenderer.start()) {
            return LowLightBoostWireStatus.SESSION_INIT_FAILED
        }
        synchronized(lock) {
            cameraSurface = newRenderer.inputSurface
        }
        Log.d(TAG, "Low light boost session ready (enabled=${boostMode.isLowLightBoostEnabled})")
        return LowLightBoostWireStatus.SUCCESS
    }

    fun monitorClient(): Boolean {
        synchronized(lock) { if (released) return false }
        return try {
            callback.asBinder().linkToDeath(callbackDeathRecipient, 0)
            val keepLinked = synchronized(lock) {
                if (released) {
                    false
                } else {
                    callbackDeathLinked = true
                    true
                }
            }
            if (!keepLinked) {
                try {
                    callback.asBinder().unlinkToDeath(callbackDeathRecipient, 0)
                } catch (_: Exception) {
                    // The callback is already gone.
                }
            }
            keepLinked
        } catch (e: RemoteException) {
            Log.w(TAG, "Client disconnected while creating low light boost session", e)
            false
        }
    }

    fun notifyCreated(): Boolean = synchronized(lock) {
        if (released || creationCallbackScheduled) return@synchronized false
        val accepted = lifecycleCallbacks.tryEnqueue {
            val shouldDeliver = synchronized(lock) { !released }
            if (!shouldDeliver) return@tryEnqueue
            val delivered = callback.tryNotifySessionCreated(this, cameraSurface)
            synchronized(lock) { creationCallbackDelivered = delivered }
            if (!delivered) releaseInternal(LowLightBoostSessionTermination.NONE)
        }
        if (accepted) creationCallbackScheduled = true
        accepted
    }

    override fun processCaptureResult(captureResult: CaptureResultParcelable?) {
        val sensorTimestamp = captureResult?.sensorTimestamp ?: return
        if (sensorTimestamp <= 0) return
        synchronized(lock) { renderer }?.queueCaptureTimestamp(sensorTimestamp)
    }

    override fun isLowLightBoostEnabled(): Int = boostMode

    override fun enableLowLightBoost(boostMode: Int) {
        if (!boostMode.isSupportedLowLightBoostMode) {
            Log.w(TAG, "Ignoring invalid low light boost mode: $boostMode")
            return
        }
        val enabled = boostMode.isLowLightBoostEnabled
        Log.d(TAG, "enableLowLightBoost: $enabled")
        this.boostMode = boostMode
        synchronized(lock) { renderer }?.setEnabled(enabled)
    }

    override fun release() {
        releaseInternal(LowLightBoostSessionTermination.DESTROYED)
    }

    fun releaseInternal(termination: LowLightBoostSessionTermination) {
        val resources = synchronized(lock) {
            if (released) return
            released = true
            callbackActive.set(false)
            val resources = LowLightBoostSessionResources(renderer, callbackDeathLinked)
            renderer = null
            callbackDeathLinked = false
            resources
        }
        Log.d(TAG, "Release low light boost session (termination=$termination)")
        if (resources.callbackDeathLinked) {
            try {
                callback.asBinder().unlinkToDeath(callbackDeathRecipient, 0)
            } catch (_: Exception) {
                // The callback is already gone.
            }
        }
        val renderer = resources.renderer
        val rendererReleased = try {
            renderer == null || renderer.release()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to release renderer; scheduling cleanup retry", e)
            false
        }
        if (rendererReleased) {
            completeRelease(termination)
        } else if (renderer != null) {
            val cleanupScheduled = LowLightBoostDispatcher.tryAwaitRendererRelease(renderer) {
                completeRelease(termination)
            }
            if (!cleanupScheduled) {
                Log.e(TAG, "Renderer cleanup could not be scheduled; keeping its session reservation")
            }
        }
    }

    private fun completeRelease(termination: LowLightBoostSessionTermination) {
        onClosed(this)
        if (termination == LowLightBoostSessionTermination.NONE) return
        val shouldNotify = synchronized(lock) { creationCallbackScheduled }
        if (!shouldNotify) return
        if (!lifecycleCallbacks.tryEnqueue {
                val created = synchronized(lock) { creationCallbackDelivered }
                if (!created) return@tryEnqueue
                when (termination) {
                    LowLightBoostSessionTermination.DESTROYED -> callback.tryNotifySessionDestroyed()
                    LowLightBoostSessionTermination.RENDER_FAILED -> {
                        callback.tryNotifySessionDisconnected(LowLightBoostWireStatus.RENDER_FAILED)
                    }

                    LowLightBoostSessionTermination.NONE -> Unit
                }
            }) {
            Log.w(TAG, "Dropping terminal callback because its lifecycle queue is unavailable")
        }
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean =
        warnOnTransactionIssues(code, reply, flags, TAG) {
            super.onTransact(code, data, reply, flags)
        }

    private fun emitBoostStrength(boostStrength: Float) {
        if (!callbackActive.get()) return
        if (!LowLightBoostDispatcher.tryEnqueueSceneBrightness(callback, boostStrength, callbackActive)) {
            if (!sceneCallbackRejected) {
                sceneCallbackRejected = true
                Log.w(TAG, "Dropping scene callbacks because the callback queue is full")
            }
        }
    }

    private fun handleRendererFailure() {
        releaseInternal(LowLightBoostSessionTermination.RENDER_FAILED)
    }
}

internal enum class LowLightBoostSessionTermination {
    NONE,
    DESTROYED,
    RENDER_FAILED,
}

private data class LowLightBoostSessionResources(
    val renderer: LowLightBoostRenderer?,
    val callbackDeathLinked: Boolean,
)
