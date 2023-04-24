/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location.manager

import android.Manifest.permission.*
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.location.Location
import android.os.IBinder
import android.os.Parcel
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.api.internal.IStatusCallback
import com.google.android.gms.common.internal.ICancelToken
import com.google.android.gms.location.*
import com.google.android.gms.location.internal.*
import com.google.android.gms.location.internal.DeviceOrientationRequestUpdateData.REMOVE_UPDATES
import com.google.android.gms.location.internal.DeviceOrientationRequestUpdateData.REQUEST_UPDATES
import org.microg.gms.common.NonCancelToken
import org.microg.gms.utils.warnOnTransactionIssues

class LocationManagerInstance(
    private val context: Context,
    private val locationManager: LocationManager,
    private val packageName: String,
    private val lifecycle: Lifecycle
) :
    AbstractLocationManagerInstance(), LifecycleOwner {

    // region Geofences

    override fun addGeofences(geofencingRequest: GeofencingRequest?, pendingIntent: PendingIntent?, callbacks: IGeofencerCallbacks?) {
        Log.d(TAG, "Not yet implemented: addGeofences by ${getClientIdentity().packageName}")
    }

    override fun removeGeofencesByIntent(pendingIntent: PendingIntent?, callbacks: IGeofencerCallbacks?, packageName: String?) {
        Log.d(TAG, "Not yet implemented: removeGeofencesByIntent by ${getClientIdentity().packageName}")
    }

    override fun removeGeofencesById(geofenceRequestIds: Array<out String>?, callbacks: IGeofencerCallbacks?, packageName: String?) {
        Log.d(TAG, "Not yet implemented: removeGeofencesById by ${getClientIdentity().packageName}")
    }

    override fun removeAllGeofences(callbacks: IGeofencerCallbacks?, packageName: String?) {
        Log.d(TAG, "Not yet implemented: removeAllGeofences by ${getClientIdentity().packageName}")
    }

    // endregion

    // region Activity

    override fun getLastActivity(packageName: String?): ActivityRecognitionResult {
        Log.d(TAG, "Not yet implemented: getLastActivity by ${getClientIdentity().packageName}")
        return ActivityRecognitionResult(listOf(DetectedActivity(DetectedActivity.UNKNOWN, 0)), System.currentTimeMillis(), SystemClock.elapsedRealtime())
    }

    override fun requestActivityTransitionUpdates(request: ActivityTransitionRequest?, pendingIntent: PendingIntent?, callback: IStatusCallback?) {
        Log.d(TAG, "Not yet implemented: requestActivityTransitionUpdates by ${getClientIdentity().packageName}")
        callback?.onResult(Status.SUCCESS)
    }

    override fun removeActivityTransitionUpdates(pendingIntent: PendingIntent?, callback: IStatusCallback?) {
        Log.d(TAG, "Not yet implemented: removeActivityTransitionUpdates by ${getClientIdentity().packageName}")
        callback?.onResult(Status.SUCCESS)
    }

    override fun requestActivityUpdatesWithCallback(request: ActivityRecognitionRequest?, pendingIntent: PendingIntent?, callback: IStatusCallback?) {
        Log.d(TAG, "Not yet implemented: requestActivityUpdatesWithCallback by ${getClientIdentity().packageName}")
        callback?.onResult(Status.SUCCESS)
    }

    override fun removeActivityUpdates(callbackIntent: PendingIntent?) {
        Log.d(TAG, "Not yet implemented: removeActivityUpdates by ${getClientIdentity().packageName}")
    }

    // endregion

    // region Sleep

    override fun removeSleepSegmentUpdates(pendingIntent: PendingIntent?, callback: IStatusCallback?) {
        Log.d(TAG, "Not yet implemented: removeSleepSegmentUpdates by ${getClientIdentity().packageName}")
        callback?.onResult(Status.SUCCESS)
    }

    override fun requestSleepSegmentUpdates(pendingIntent: PendingIntent?, request: SleepSegmentRequest?, callback: IStatusCallback?) {
        Log.d(TAG, "Not yet implemented: requestSleepSegmentUpdates by ${getClientIdentity().packageName}")
        callback?.onResult(Status.SUCCESS)
    }

    // endregion

    // region Location

    override fun flushLocations(callback: IFusedLocationProviderCallback?) {
        Log.d(TAG, "flushLocations by ${getClientIdentity().packageName}")
        checkHasAnyLocationPermission()
        Log.d(TAG, "Not yet implemented: flushLocations")
    }

    override fun getLocationAvailabilityWithReceiver(request: LocationAvailabilityRequest, receiver: LocationReceiver) {
        Log.d(TAG, "getLocationAvailabilityWithReceiver by ${getClientIdentity().packageName}")
        checkHasAnyLocationPermission()
        val callback = receiver.availabilityStatusCallback
        val clientIdentity = getClientIdentity()
        lifecycleScope.launchWhenStarted {
            try {
                callback.onLocationAvailabilityStatus(Status.SUCCESS, locationManager.getLocationAvailability(clientIdentity, request))
            } catch (e: Exception) {
                try {
                    callback.onLocationAvailabilityStatus(Status(CommonStatusCodes.ERROR, e.message), LocationAvailability.UNAVAILABLE)
                } catch (e2: Exception) {
                    Log.w(TAG, "Failed", e)
                }
            }
        }
    }

    override fun getCurrentLocationWithReceiver(request: CurrentLocationRequest, receiver: LocationReceiver): ICancelToken {
        Log.d(TAG, "getCurrentLocationWithReceiver by ${getClientIdentity().packageName}")
        checkHasAnyLocationPermission()
        Log.d(TAG, "Not yet implemented: getCurrentLocationWithReceiver")
        return NonCancelToken()
    }

    override fun getLastLocationWithReceiver(request: LastLocationRequest, receiver: LocationReceiver) {
        Log.d(TAG, "getLastLocationWithReceiver by ${getClientIdentity().packageName}")
        checkHasAnyLocationPermission()
        val callback = receiver.statusCallback
        val clientIdentity = getClientIdentity()
        lifecycleScope.launchWhenStarted {
            try {
                callback.onLocationStatus(Status.SUCCESS, locationManager.getLastLocation(clientIdentity, request))
            } catch (e: Exception) {
                try {
                    callback.onLocationStatus(Status(CommonStatusCodes.ERROR, e.message), null)
                } catch (e2: Exception) {
                    Log.w(TAG, "Failed", e)
                }
            }
        }
    }

    override fun requestLocationSettingsDialog(settingsRequest: LocationSettingsRequest?, callback: ISettingsCallbacks?, packageName: String?) {
        Log.d(TAG, "requestLocationSettingsDialog by ${getClientIdentity().packageName}")
        Log.d(TAG, "Not yet implemented: requestLocationSettingsDialog")
        callback?.onLocationSettingsResult(LocationSettingsResult(Status.SUCCESS))
    }

    // region Mock locations

    override fun setMockModeWithCallback(mockMode: Boolean, callback: IStatusCallback) {
        Log.d(TAG, "setMockModeWithCallback by ${getClientIdentity().packageName}")
        checkHasAnyLocationPermission()
        val clientIdentity = getClientIdentity()
        lifecycleScope.launchWhenStarted {
            try {
                Log.d(TAG, "Not yet implemented: setMockModeWithCallback")
                callback.onResult(Status.SUCCESS)
            } catch (e: Exception) {
                Log.w(TAG, "Failed", e)
            }
        }
    }

    override fun setMockLocationWithCallback(mockLocation: Location, callback: IStatusCallback) {
        Log.d(TAG, "setMockLocationWithCallback by ${getClientIdentity().packageName}")
        checkHasAnyLocationPermission()
        val clientIdentity = getClientIdentity()
        lifecycleScope.launchWhenStarted {
            try {
                Log.d(TAG, "Not yet implemented: setMockLocationWithCallback")
                callback.onResult(Status.SUCCESS)
            } catch (e: Exception) {
                Log.w(TAG, "Failed", e)
            }
        }
    }

    // endregion

    // region Location updates

    override fun registerLocationUpdates(
        oldBinder: IBinder?,
        binder: IBinder,
        callback: ILocationCallback,
        request: LocationRequest,
        statusCallback: IStatusCallback
    ) {
        Log.d(TAG, "registerLocationUpdates (callback) by ${getClientIdentity().packageName}")
        checkHasAnyLocationPermission()
        val clientIdentity = getClientIdentity()
        lifecycleScope.launchWhenStarted {
            try {
                if (oldBinder != null) {
                    locationManager.updateBinderRequest(clientIdentity, oldBinder, binder, callback, request)
                } else {
                    locationManager.addBinderRequest(clientIdentity, binder, callback, request)
                }
                statusCallback.onResult(Status.SUCCESS)
            } catch (e: Exception) {
                try {
                    statusCallback.onResult(Status(CommonStatusCodes.ERROR, e.message))
                } catch (e2: Exception) {
                    Log.w(TAG, "Failed", e)
                }
            }
        }
    }

    override fun registerLocationUpdates(pendingIntent: PendingIntent, request: LocationRequest, statusCallback: IStatusCallback) {
        Log.d(TAG, "registerLocationUpdates (intent) by ${getClientIdentity().packageName}")
        checkHasAnyLocationPermission()
        val clientIdentity = getClientIdentity()
        lifecycleScope.launchWhenStarted {
            try {
                locationManager.addIntentRequest(clientIdentity, pendingIntent, request)
                statusCallback.onResult(Status.SUCCESS)
            } catch (e: Exception) {
                try {
                    statusCallback.onResult(Status(CommonStatusCodes.ERROR, e.message))
                } catch (e2: Exception) {
                    Log.w(TAG, "Failed", e)
                }
            }
        }
    }

    override fun unregisterLocationUpdates(binder: IBinder, statusCallback: IStatusCallback) {
        Log.d(TAG, "unregisterLocationUpdates (callback) by ${getClientIdentity().packageName}")
        lifecycleScope.launchWhenStarted {
            try {
                locationManager.removeBinderRequest(binder)
                statusCallback.onResult(Status.SUCCESS)
            } catch (e: Exception) {
                try {
                    statusCallback.onResult(Status(CommonStatusCodes.ERROR, e.message))
                } catch (e2: Exception) {
                    Log.w(TAG, "Failed", e)
                }
            }
        }
    }

    override fun unregisterLocationUpdates(pendingIntent: PendingIntent, statusCallback: IStatusCallback) {
        Log.d(TAG, "unregisterLocationUpdates (intent) by ${getClientIdentity().packageName}")
        lifecycleScope.launchWhenStarted {
            try {
                locationManager.removeIntentRequest(pendingIntent)
                statusCallback.onResult(Status.SUCCESS)
            } catch (e: Exception) {
                try {
                    statusCallback.onResult(Status(CommonStatusCodes.ERROR, e.message))
                } catch (e2: Exception) {
                    Log.w(TAG, "Failed", e)
                }
            }
        }
    }

    // endregion

    // endregion

    // region Device Orientation

    override fun updateDeviceOrientationRequest(request: DeviceOrientationRequestUpdateData) {
        Log.d(TAG, "updateDeviceOrientationRequest by ${getClientIdentity().packageName}")
        checkHasAnyLocationPermission()
        val clientIdentity = getClientIdentity()
        val callback = request.fusedLocationProviderCallback
        lifecycleScope.launchWhenStarted {
            try {
                when (request.opCode) {
                    REQUEST_UPDATES -> locationManager.deviceOrientationManager.add(clientIdentity, request.request, request.listener)
                    REMOVE_UPDATES -> locationManager.deviceOrientationManager.remove(clientIdentity, request.listener)
                    else -> throw UnsupportedOperationException("Op code ${request.opCode} not supported")
                }
                callback?.onFusedLocationProviderResult(FusedLocationProviderResult.SUCCESS)
            } catch (e: Exception) {
                try {
                    callback?.onFusedLocationProviderResult(FusedLocationProviderResult.create(Status(CommonStatusCodes.ERROR, e.message)))
                } catch (e2: Exception) {
                    Log.w(TAG, "Failed", e)
                }
            }
        }
    }

    // endregion

    private fun getClientIdentity() = ClientIdentity(packageName).apply { uid = getCallingUid(); pid = getCallingPid() }

    private fun checkHasAnyLocationPermission() = checkHasAnyPermission(ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION)

    private fun checkHasAnyPermission(vararg permissions: String) {
        for (permission in permissions) {
            if (context.packageManager.checkPermission(permission, packageName) == PERMISSION_GRANTED) {
                return
            }
        }
        throw SecurityException("$packageName does not have any of $permissions")
    }

    override fun getLifecycle(): Lifecycle = lifecycle

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean =
        warnOnTransactionIssues(code, reply, flags) { super.onTransact(code, data, reply, flags) }
}