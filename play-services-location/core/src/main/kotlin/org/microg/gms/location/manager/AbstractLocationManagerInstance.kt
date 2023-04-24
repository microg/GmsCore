/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location.manager

import android.app.PendingIntent
import android.location.Location
import android.os.IBinder
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.api.internal.IStatusCallback
import com.google.android.gms.common.internal.ICancelToken
import com.google.android.gms.location.*
import com.google.android.gms.location.internal.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

abstract class AbstractLocationManagerInstance : IGoogleLocationManagerService.Stub() {

    override fun addGeofencesList(geofences: List<ParcelableGeofence>, pendingIntent: PendingIntent, callbacks: IGeofencerCallbacks, packageName: String) {
        val request = GeofencingRequest.Builder()
            .addGeofences(geofences)
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER or GeofencingRequest.INITIAL_TRIGGER_DWELL)
            .build()
        addGeofences(request, pendingIntent, callbacks)
    }

    override fun requestActivityUpdates(detectionIntervalMillis: Long, triggerUpdates: Boolean, callbackIntent: PendingIntent) {
        requestActivityUpdatesWithCallback(ActivityRecognitionRequest().apply {
            intervalMillis = detectionIntervalMillis
            triggerUpdate = triggerUpdates
        }, callbackIntent, EmptyStatusCallback())
    }

    override fun getLocationAvailabilityWithPackage(packageName: String?): LocationAvailability {
        val reference = AtomicReference(LocationAvailability.UNAVAILABLE)
        val latch = CountDownLatch(1)
        getLocationAvailabilityWithReceiver(LocationAvailabilityRequest(), LocationReceiver(object : ILocationAvailabilityStatusCallback.Stub() {
            override fun onLocationAvailabilityStatus(status: Status, location: LocationAvailability) {
                if (status.isSuccess) {
                    reference.set(location)
                }
                latch.countDown()
            }
        }))
        return reference.get()
    }

    override fun getCurrentLocation(request: CurrentLocationRequest, callback: ILocationStatusCallback): ICancelToken {
        return getCurrentLocationWithReceiver(request, LocationReceiver(callback))
    }

    // region Last location

    override fun getLastLocation(): Location? {
        val reference = AtomicReference<Location>()
        val latch = CountDownLatch(1)
        val request = LastLocationRequest.Builder().setMaxUpdateAgeMillis(Long.MAX_VALUE).setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL).build()
        getLastLocationWithReceiver(request, LocationReceiver(object : ILocationStatusCallback.Stub() {
            override fun onLocationStatus(status: Status, location: Location?) {
                if (status.isSuccess) {
                    reference.set(location)
                }
                latch.countDown()
            }
        }))
        if (latch.await(30, TimeUnit.SECONDS)) {
            return reference.get()
        }
        return null
    }

    override fun getLastLocationWithRequest(request: LastLocationRequest, callback: ILocationStatusCallback) {
        getLastLocationWithReceiver(request, LocationReceiver(callback))
    }

    override fun getLastLocationWithPackage(packageName: String?): Location? {
        return lastLocation
    }

    override fun getLastLocationWith(s: String?): Location? {
        return lastLocation
    }

    // endregion

    // region Mock locations

    override fun setMockMode(mockMode: Boolean) {
        val latch = CountDownLatch(1)
        setMockModeWithCallback(mockMode, object : IStatusCallback.Stub() {
            override fun onResult(status: Status?) {
                latch.countDown()
            }
        })
        latch.await(30, TimeUnit.SECONDS)
    }

    override fun setMockLocation(mockLocation: Location) {
        val latch = CountDownLatch(1)
        setMockLocationWithCallback(mockLocation, object : IStatusCallback.Stub() {
            override fun onResult(status: Status?) {
                latch.countDown()
            }
        })
        latch.await(30, TimeUnit.SECONDS)
    }

    // endregion

    // region Location updates

    abstract fun registerLocationUpdates(
        oldBinder: IBinder?,
        binder: IBinder,
        callback: ILocationCallback,
        request: LocationRequest,
        statusCallback: IStatusCallback
    )

    abstract fun registerLocationUpdates(pendingIntent: PendingIntent, request: LocationRequest, statusCallback: IStatusCallback)
    abstract fun unregisterLocationUpdates(binder: IBinder, statusCallback: IStatusCallback)
    abstract fun unregisterLocationUpdates(pendingIntent: PendingIntent, statusCallback: IStatusCallback)

    override fun requestLocationUpdatesWithCallback(receiver: LocationReceiver, request: LocationRequest, callback: IStatusCallback) {
        when (receiver.type) {
            LocationReceiver.TYPE_LISTENER -> registerLocationUpdates(
                receiver.oldBinderReceiver,
                receiver.binderReceiver!!,
                receiver.listener.asCallback(),
                request,
                callback
            )

            LocationReceiver.TYPE_CALLBACK -> registerLocationUpdates(
                receiver.oldBinderReceiver,
                receiver.binderReceiver!!,
                receiver.callback,
                request,
                callback
            )

            LocationReceiver.TYPE_PENDING_INTENT -> registerLocationUpdates(receiver.pendingIntentReceiver!!, request, callback)
            else -> throw IllegalArgumentException("unknown location receiver type");
        }
    }

    override fun removeLocationUpdatesWithCallback(receiver: LocationReceiver, callback: IStatusCallback) {
        when (receiver.type) {
            LocationReceiver.TYPE_LISTENER -> unregisterLocationUpdates(receiver.binderReceiver!!, callback)
            LocationReceiver.TYPE_CALLBACK -> unregisterLocationUpdates(receiver.binderReceiver!!, callback)
            LocationReceiver.TYPE_PENDING_INTENT -> unregisterLocationUpdates(receiver.pendingIntentReceiver!!, callback)
            else -> throw IllegalArgumentException("unknown location receiver type");
        }
    }

    override fun updateLocationRequest(data: LocationRequestUpdateData) {
        val statusCallback = object : IStatusCallback.Stub() {
            override fun onResult(status: Status) {
                data.fusedLocationProviderCallback?.onFusedLocationProviderResult(FusedLocationProviderResult.create(status))
            }
        }
        when (data.opCode) {
            LocationRequestUpdateData.REQUEST_UPDATES -> {
                when {
                    data.listener != null -> registerLocationUpdates(
                        null,
                        data.listener.asBinder(),
                        data.listener.asCallback().redirectCancel(data.fusedLocationProviderCallback),
                        data.request.request,
                        statusCallback
                    )

                    data.callback != null -> registerLocationUpdates(
                        null,
                        data.callback.asBinder(),
                        data.callback.redirectCancel(data.fusedLocationProviderCallback),
                        data.request.request,
                        statusCallback
                    )

                    data.pendingIntent != null -> registerLocationUpdates(data.pendingIntent, data.request.request, statusCallback)
                }
            }

            LocationRequestUpdateData.REMOVE_UPDATES -> {
                when {
                    data.listener != null -> unregisterLocationUpdates(data.listener.asBinder(), statusCallback)
                    data.callback != null -> unregisterLocationUpdates(data.callback.asBinder(), statusCallback)
                    data.pendingIntent != null -> unregisterLocationUpdates(data.pendingIntent, statusCallback)
                }
            }

            else -> {
                statusCallback.onResult(Status(CommonStatusCodes.ERROR, "invalid location request update operation: " + data.opCode))
            }
        }
    }

    override fun requestLocationUpdatesWithListener(request: LocationRequest, listener: ILocationListener) {
        requestLocationUpdatesWithCallback(LocationReceiver(listener), request, EmptyStatusCallback())
    }

    override fun requestLocationUpdatesWithPackage(request: LocationRequest, listener: ILocationListener, packageName: String?) {
        requestLocationUpdatesWithCallback(LocationReceiver(listener), request, EmptyStatusCallback())
    }

    override fun requestLocationUpdatesWithIntent(request: LocationRequest, callbackIntent: PendingIntent) {
        requestLocationUpdatesWithCallback(LocationReceiver(callbackIntent), request, EmptyStatusCallback())
    }

    override fun requestLocationUpdatesInternalWithListener(request: LocationRequestInternal, listener: ILocationListener) {
        requestLocationUpdatesWithCallback(LocationReceiver(listener), request.request, EmptyStatusCallback())
    }

    override fun requestLocationUpdatesInternalWithIntent(request: LocationRequestInternal, callbackIntent: PendingIntent) {
        requestLocationUpdatesWithCallback(LocationReceiver(callbackIntent), request.request, EmptyStatusCallback())
    }

    override fun removeLocationUpdatesWithListener(listener: ILocationListener) {
        removeLocationUpdatesWithCallback(LocationReceiver(listener), EmptyStatusCallback())
    }

    override fun removeLocationUpdatesWithIntent(callbackIntent: PendingIntent) {
        removeLocationUpdatesWithCallback(LocationReceiver(callbackIntent), EmptyStatusCallback())
    }

    // endregion

    class EmptyStatusCallback : IStatusCallback.Stub() {
        override fun onResult(status: Status?) = Unit
    }
}