/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.location.internal;

import android.app.PendingIntent;
import android.location.Location;
import android.os.Bundle;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.api.internal.IStatusCallback;
import com.google.android.gms.common.internal.ICancelToken;
import com.google.android.gms.location.internal.DeviceOrientationRequestUpdateData;
import com.google.android.gms.location.internal.IBooleanStatusCallback;
import com.google.android.gms.location.internal.IFusedLocationProviderCallback;
import com.google.android.gms.location.internal.IGeofencerCallbacks;
import com.google.android.gms.location.internal.ILocationStatusCallback;
import com.google.android.gms.location.internal.ISettingsCallbacks;
import com.google.android.gms.location.internal.LocationReceiver;
import com.google.android.gms.location.internal.LocationRequestInternal;
import com.google.android.gms.location.internal.LocationRequestUpdateData;
import com.google.android.gms.location.internal.ParcelableGeofence;
import com.google.android.gms.location.internal.RemoveGeofencingRequest;
import com.google.android.gms.location.internal.SetGoogleLocationAccuracyRequest;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.ActivityRecognitionRequest;
import com.google.android.gms.location.ActivityTransitionRequest;
import com.google.android.gms.location.CurrentLocationRequest;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.GestureRequest;
import com.google.android.gms.location.ILocationListener;
import com.google.android.gms.location.LastLocationRequest;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationAvailabilityRequest;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationStatus;
import com.google.android.gms.location.SleepSegmentRequest;

interface IGoogleLocationManagerService {

    void addGeofencesWithCallback(in GeofencingRequest request, in PendingIntent pendingIntent, IStatusCallback callback) = 96;
    void removeGeofencesWithCallback(in RemoveGeofencingRequest request, IStatusCallback callback) = 97;

    void requestActivityTransitionUpdates(in ActivityTransitionRequest request, in PendingIntent pendingIntent, IStatusCallback callback) = 71;
    void requestActivityUpdates(long detectionIntervalMillis, boolean triggerUpdates, in PendingIntent callbackIntent) = 4;
    void requestActivityUpdatesWithCallback(in ActivityRecognitionRequest request, in PendingIntent pendingIntent, IStatusCallback callback) = 69;
    void removeActivityTransitionUpdates(in PendingIntent pendingIntent, IStatusCallback callback) = 72;
    void removeActivityUpdates(in PendingIntent callbackIntent) = 5;

    void getLastLocationWithReceiver(in LastLocationRequest request, in LocationReceiver receiver) = 89;

    ICancelToken getCurrentLocationWithReceiver(in CurrentLocationRequest request, in LocationReceiver receiver) = 91;

    void requestLocationUpdatesWithCallback(in LocationReceiver receiver, in LocationRequest request, IStatusCallback callback) = 87;
    void removeLocationUpdatesWithCallback(in LocationReceiver receiver, IStatusCallback callback) = 88;

    void flushLocations(IFusedLocationProviderCallback callback) = 66;

    void getLocationAvailabilityWithReceiver(in LocationAvailabilityRequest request, in LocationReceiver receiver) = 90;

    void setMockModeWithCallback(boolean mockMode, IStatusCallback callback) = 83;
    void setMockLocationWithCallback(in Location mockLocation, IStatusCallback callback) = 84;

    void requestSleepSegmentUpdates(in PendingIntent pendingIntent, in SleepSegmentRequest request, IStatusCallback callback) = 78;
    void removeSleepSegmentUpdates(in PendingIntent pendingIntent, IStatusCallback callback) = 68;

    void requestLocationSettingsDialog(in LocationSettingsRequest settingsRequest, ISettingsCallbacks callback, String packageName) = 62;

    void updateDeviceOrientationRequest(in DeviceOrientationRequestUpdateData request) = 74;

    void isGoogleLocationAccuracyEnabled(in IBooleanStatusCallback callback) = 94;

    // deprecated
    // these are marked as deprecated in latest version of play-services-location client library and will likely move to outdated eventually

    void addGeofences(in GeofencingRequest request, in PendingIntent pendingIntent, IGeofencerCallbacks callbacks) = 56;
    void removeGeofences(in RemoveGeofencingRequest request, IGeofencerCallbacks callback) = 73;

    Location getLastLocation() = 6;
    void getLastLocationWithRequest(in LastLocationRequest request, ILocationStatusCallback callback) = 81;

    ICancelToken getCurrentLocation(in CurrentLocationRequest request, ILocationStatusCallback callback) = 86;

    void updateLocationRequest(in LocationRequestUpdateData locationRequestUpdateData) = 58;

    LocationAvailability getLocationAvailabilityWithPackage(String packageName) = 33;

    void setMockMode(boolean mockMode) = 11;
    void setMockLocation(in Location mockLocation) = 12;

    // outdated or private
    // these are not present in latest version of play-services-location client library but might be in use by outdated apps or apps with private API access

    void addGeofencesList(in List<ParcelableGeofence> geofences, in PendingIntent pendingIntent, IGeofencerCallbacks callbacks, String packageName) = 0;
    void removeGeofencesByIntent(in PendingIntent pendingIntent, IGeofencerCallbacks callbacks, String packageName) = 1;
    void removeGeofencesById(in String[] geofenceRequestIds, IGeofencerCallbacks callbacks, String packageName) = 2;
    void removeAllGeofences(IGeofencerCallbacks callbacks, String packageName) = 3;

    ActivityRecognitionResult getLastActivity(String packageName) = 63;

    Location getLastLocationWithPackage(String packageName) = 20;
    Location getLastLocationWith(String s) = 79;

    void requestLocationUpdatesWithListener(in LocationRequest request, ILocationListener listener) = 7;
    void requestLocationUpdatesWithPackage(in LocationRequest request, ILocationListener listener, String packageName) = 19;
    void requestLocationUpdatesWithIntent(in LocationRequest request, in PendingIntent callbackIntent) = 8;
    void requestLocationUpdatesInternalWithListener(in LocationRequestInternal request, ILocationListener listener) = 51;
    void requestLocationUpdatesInternalWithIntent(in LocationRequestInternal request, in PendingIntent callbackIntent) = 52;
    void removeLocationUpdatesWithListener(ILocationListener listener) = 9;
    void removeLocationUpdatesWithIntent(in PendingIntent callbackIntent) = 10;

//    void injectLocation(in Location mockLocation, int injectionType) = 25;
//    void injectLocationWithCallback(in Location mockLocation, int injectionType, IStatusCallback callback) = 85;

    void setGoogleLocationAccuracy(in SetGoogleLocationAccuracyRequest request, IStatusCallback callback) = 95;

    // unsupported

//    void iglms14(in LatLngBounds var1, int var2, in PlaceFilter var3, in PlacesParams var4, IPlacesCallbacks var5) = 13;
//    void iglms15(String var1, in PlacesParams var2, IPlacesCallbacks var3) = 14;
//    void iglms16(in LatLng var1, in PlaceFilter var2, in PlacesParams var3, IPlacesCallbacks var4) = 15;
//    void iglms17(in PlaceFilter var1, in PlacesParams var2, IPlacesCallbacks var3) = 16;
//    void iglms18(in PlaceRequest var1, in PlacesParams var2, in PendingIntent var3) = 17;
//    void iglms19(in PlacesParams var1, in PendingIntent var2) = 18;

//    void iglms25(in PlaceReport var1, in PlacesParams var2) = 24;

//    void iglms42(String var1, in PlacesParams var2, IPlacesCallbacks var3) = 41;

//    void iglms46(in UserAddedPlace var1, in PlacesParams var2, IPlacesCallbacks var3) = 45;
//    void iglms47(in LatLngBounds var1, int var2, String var3, in PlaceFilter var4, in PlacesParams var5, IPlacesCallbacks var6) = 46;
//    void iglms48(in NearbyAlertRequest var1, in PlacesParams var2, in PendingIntent var3) = 47;
//    void iglms49(in PlacesParams var1, in PendingIntent var2) = 48;
//    void iglms50(in UserDataType var1, in LatLngBounds var2, in List var3, in PlacesParams var4, IPlacesCallbacks var5) = 49;
//    IBinder iglms51() = 50;

//    IBinder iglms54() = 53;
//    void iglms55(String var1, in LatLngBounds var2, in AutocompleteFilter var3, in PlacesParams var4, IPlacesCallbacks var5) = 54;

//    void iglms58(in List var1, in PlacesParams var2, IPlacesCallbacks var3) = 57;

//    Status requestGestureUpdates(in GestureRequest request, in PendingIntent pendingIntent) = 59;
//    Status iglms61(in PendingIntent pendingIntent) = 60;

//    void iglms65(in PendingIntent pendingIntent, IStatusCallback callback) = 64;
//    void iglms66(in PendingIntent pendingIntent, IStatusCallback callback) = 65;

//    void iglms68(in PendingIntent pendingIntent, IStatusCallback callback) = 67;

//    void iglms71(IStatusCallback callback) = 70;

//    void iglms76(in PendingIntent pendingIntent) = 75;
//    boolean setActivityRecognitionMode(int mode) = 76;
//    int getActivityRecognitionMode() = 77;
}
