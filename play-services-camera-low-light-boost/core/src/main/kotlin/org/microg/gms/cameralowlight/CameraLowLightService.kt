/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.cameralowlight

import android.os.Binder
import android.os.Parcel
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.internal.ConnectionInfo
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import com.google.android.libraries.camera.capture.lowlightboost.internal.ILowLightBoostCallback
import com.google.android.libraries.camera.capture.lowlightboost.internal.ILowLightBoostService
import com.google.android.libraries.camera.capture.lowlightboost.internal.LowLightBoostOptionsParcelable
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService
import org.microg.gms.common.PackageUtils
import org.microg.gms.utils.warnOnTransactionIssues

class CameraLowLightService : BaseService(TAG, GmsService.CAMERA_LOW_LIGHT) {
    private val sessionRegistry = LowLightBoostSessionRegistry
    private val lock = Any()
    private val serviceImplementations = mutableSetOf<LowLightBoostServiceImpl>()
    private var destroyed = false

    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        val packageName = PackageUtils.getAndCheckCallingPackage(this, request.packageName)
            ?: throw IllegalArgumentException("Missing package name")
        val clientUid = Binder.getCallingUid()
        val serviceImpl = synchronized(lock) {
            check(!destroyed) { "Camera low light service is destroyed" }
            LowLightBoostServiceImpl(packageName, clientUid, sessionRegistry, lifecycle) { released ->
                synchronized(lock) { serviceImplementations.remove(released) }
            }.also { serviceImplementations += it }
        }
        var delivered = false
        try {
            callback.onPostInitCompleteWithConnectionInfo(
                CommonStatusCodes.SUCCESS,
                serviceImpl.asBinder(),
                ConnectionInfo().apply { features = cameraLowLightFeatures() }
            )
            delivered = true
        } finally {
            if (!delivered) serviceImpl.close()
        }
    }

    override fun onDestroy() {
        val activeImplementations = synchronized(lock) {
            destroyed = true
            serviceImplementations.toList().also { serviceImplementations.clear() }
        }
        activeImplementations.forEach(LowLightBoostServiceImpl::close)
        super.onDestroy()
    }
}

private class LowLightBoostServiceImpl(
    private val packageName: String,
    private val clientUid: Int,
    private val sessionRegistry: LowLightBoostSessionRegistry,
    override val lifecycle: Lifecycle,
    private val onReleased: (LowLightBoostServiceImpl) -> Unit,
) : ILowLightBoostService.Stub(), LifecycleOwner {
    private val lock = Any()
    private val reservationToken = Any()
    private var session: LowLightBoostSessionImpl? = null
    private var reservationHeld = false
    private var released = false

    override fun createSession(options: LowLightBoostOptionsParcelable?, callback: ILowLightBoostCallback?) {
        Log.d(TAG, "createSession for $packageName")
        if (callback == null) return
        if (options == null || !options.isValidForSession()) {
            Log.w(TAG, "createSession: invalid options from $packageName")
            enqueueSessionStatus(callback, LowLightBoostWireStatus.SESSION_INIT_FAILED)
            return
        }

        val result = synchronized(lock) {
            when {
                released -> LowLightBoostSessionCreationResult(LowLightBoostWireStatus.SERVICE_RELEASED)
                session != null -> {
                    Log.w(TAG, "createSession: maximum session count reached for $packageName")
                    LowLightBoostSessionCreationResult(LowLightBoostWireStatus.MAX_SESSIONS_REACHED)
                }

                else -> createSessionLocked(options, callback)
            }
        }

        val createdSession = result.session
        if (createdSession == null) {
            enqueueSessionStatus(callback, result.status)
            return
        }
        if (!createdSession.notifyCreated()) {
            createdSession.releaseInternal(LowLightBoostSessionTermination.NONE)
        }
    }

    private fun createSessionLocked(
        options: LowLightBoostOptionsParcelable,
        callback: ILowLightBoostCallback,
    ): LowLightBoostSessionCreationResult {
        if (!sessionRegistry.tryAcquire(clientUid, reservationToken)) {
            return LowLightBoostSessionCreationResult(LowLightBoostWireStatus.MAX_SESSIONS_REACHED)
        }
        reservationHeld = true
        var newSession: LowLightBoostSessionImpl? = null
        var reservationTransferred = false
        try {
            newSession = LowLightBoostSessionImpl(options, callback) { closed ->
                synchronized(lock) {
                    if (session === closed) session = null
                    releaseReservationLocked()
                }
            }
            reservationTransferred = true
            val status = newSession.open()
            if (status != LowLightBoostWireStatus.SUCCESS) {
                newSession.releaseInternal(LowLightBoostSessionTermination.NONE)
                return LowLightBoostSessionCreationResult(status)
            }
            if (!newSession.monitorClient()) {
                newSession.releaseInternal(LowLightBoostSessionTermination.NONE)
                return LowLightBoostSessionCreationResult(LowLightBoostWireStatus.BINDER_DIED)
            }
            session = newSession
            return LowLightBoostSessionCreationResult(LowLightBoostWireStatus.SUCCESS, newSession)
        } catch (t: Throwable) {
            try {
                newSession?.releaseInternal(LowLightBoostSessionTermination.NONE)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clean up an incomplete low light boost session", e)
            }
            if (t is Error) throw t
            Log.e(TAG, "Unexpected failure while creating low light boost session", t)
            return LowLightBoostSessionCreationResult(LowLightBoostWireStatus.SESSION_INIT_FAILED)
        } finally {
            if (!reservationTransferred) releaseReservationLocked()
        }
    }

    override fun release() {
        Log.d(TAG, "release service for $packageName")
        close()
    }

    fun close() {
        val activeSession = synchronized(lock) {
            if (released) return
            released = true
            session.also {
                session = null
                if (it == null) releaseReservationLocked()
            }
        }
        try {
            activeSession?.releaseInternal(LowLightBoostSessionTermination.DESTROYED)
        } finally {
            onReleased(this)
        }
    }

    private fun enqueueSessionStatus(callback: ILowLightBoostCallback, status: Int) {
        if (!LowLightBoostDispatcher.tryEnqueueSessionStatus(callback, status)) {
            Log.w(TAG, "Dropping session status because the callback queue is full")
        }
    }

    private fun releaseReservationLocked() {
        if (!reservationHeld) return
        reservationHeld = false
        sessionRegistry.release(clientUid, reservationToken)
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean =
        warnOnTransactionIssues(code, reply, flags, TAG) {
            super.onTransact(code, data, reply, flags)
        }
}
