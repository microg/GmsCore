package com.google.android.gms.location.internal;

import android.app.PendingIntent;
import android.location.Location;
import android.os.Bundle;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.api.internal.IStatusCallback;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.internal.IPlacesCallbacks;
import com.google.android.gms.location.internal.DeviceOrientationRequestUpdateData;
import com.google.android.gms.location.internal.ISettingsCallbacks;
import com.google.android.gms.location.internal.LocationRequestInternal;
import com.google.android.gms.location.internal.LocationRequestUpdateData;
import com.google.android.gms.location.internal.ParcelableGeofence;
import com.google.android.gms.location.places.NearbyAlertRequest;
import com.google.android.gms.location.places.PlaceFilter;
import com.google.android.gms.location.places.PlaceRequest;
import com.google.android.gms.location.places.PlaceReport;
import com.google.android.gms.location.places.internal.PlacesParams;
import com.google.android.gms.location.places.UserAddedPlace;
import com.google.android.gms.location.places.UserDataType;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.GestureRequest;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationStatus;
import com.google.android.gms.location.internal.IGeofencerCallbacks;
import com.google.android.gms.location.ILocationListener;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

interface IGoogleLocationManagerService {
    void addGeofencesList(in List<ParcelableGeofence> geofences, in PendingIntent pendingIntent, IGeofencerCallbacks callbacks, String packageName) = 0;
    void addGeofences(in GeofencingRequest geofencingRequest, in PendingIntent pendingIntent, IGeofencerCallbacks callbacks) = 56;
    void removeGeofencesByIntent(in PendingIntent pendingIntent, IGeofencerCallbacks callbacks, String packageName) = 1;
    void removeGeofencesById(in String[] geofenceRequestIds, IGeofencerCallbacks callbacks, String packageName) = 2;
    void removeAllGeofences(IGeofencerCallbacks callbacks, String packageName) = 3;
//    void removeGeofences(in RemoveGeofencingRequest request, IGeofencerCallbacks callback) = 73;

    void requestActivityUpdates(long detectionIntervalMillis, boolean alwaysTrue, in PendingIntent callbackIntent) = 4;
    void removeActivityUpdates(in PendingIntent callbackIntent) = 5;
    ActivityRecognitionResult getLastActivity(String packageName) = 63;

    Status requestGestureUpdates(in GestureRequest request, in PendingIntent pendingIntent) = 59;
    Status iglms61(in PendingIntent pendingIntent) = 60;

    Location getLastLocation() = 6;
    Location getLastLocationWithPackage(String packageName) = 20;
    Location getLastLocationWith(String s) = 79;

    void requestLocationUpdatesWithListener(in LocationRequest request, ILocationListener listener) = 7;
    void requestLocationUpdatesWithPackage(in LocationRequest request, ILocationListener listener, String packageName) = 19;
    void requestLocationUpdatesWithIntent(in LocationRequest request, in PendingIntent callbackIntent) = 8;
    void requestLocationUpdatesInternalWithListener(in LocationRequestInternal request, ILocationListener listener) = 51;
    void requestLocationUpdatesInternalWithIntent(in LocationRequestInternal request, in PendingIntent callbackIntent) = 52;
    void removeLocationUpdatesWithListener(ILocationListener listener) = 9;
    void removeLocationUpdatesWithIntent(in PendingIntent callbackIntent) = 10;
    void updateLocationRequest(in LocationRequestUpdateData locationRequestUpdateData) = 58;
//    void flushLocations(IFusedLocationProviderCallback callback) = 66;

    void setMockMode(boolean mockMode) = 11;
    void setMockLocation(in Location mockLocation) = 12;
    void injectLocation(in Location mockLocation, int injectionType) = 25;

    LocationAvailability getLocationAvailabilityWithPackage(String packageName) = 33;

//    void requestSleepSegmentUpdates(in PendingIntent pendingIntent, in SleepSegmentRequest request, IStatusCallback callback) = 78;
    void removeSleepSegmentUpdates(in PendingIntent pendingIntent, IStatusCallback callback) = 68;

    void requestLocationSettingsDialog(in LocationSettingsRequest settingsRequest, ISettingsCallbacks callback, String packageName) = 62;

//    void requestActivityTransitionUpdates(in ActivityTransitionRequest request, in PendingIntent pendingIntent, IStatusCallback callback) = 71;
    void removeActivityTransitionUpdates(in PendingIntent pendingIntent, IStatusCallback callback) = 72;

    void updateDeviceOrientationRequest(in DeviceOrientationRequestUpdateData request) = 74;

    boolean setActivityRecognitionMode(int mode) = 76;

    void iglms14(in LatLngBounds var1, int var2, in PlaceFilter var3, in PlacesParams var4, IPlacesCallbacks var5) = 13;
    void iglms15(String var1, in PlacesParams var2, IPlacesCallbacks var3) = 14;
    void iglms16(in LatLng var1, in PlaceFilter var2, in PlacesParams var3, IPlacesCallbacks var4) = 15;
    void iglms17(in PlaceFilter var1, in PlacesParams var2, IPlacesCallbacks var3) = 16;
    void iglms18(in PlaceRequest var1, in PlacesParams var2, in PendingIntent var3) = 17;
    void iglms19(in PlacesParams var1, in PendingIntent var2) = 18;

    void iglms25(in PlaceReport var1, in PlacesParams var2) = 24;

    void iglms42(String var1, in PlacesParams var2, IPlacesCallbacks var3) = 41;

    void iglms46(in UserAddedPlace var1, in PlacesParams var2, IPlacesCallbacks var3) = 45;
    void iglms47(in LatLngBounds var1, int var2, String var3, in PlaceFilter var4, in PlacesParams var5, IPlacesCallbacks var6) = 46;
    void iglms48(in NearbyAlertRequest var1, in PlacesParams var2, in PendingIntent var3) = 47;
    void iglms49(in PlacesParams var1, in PendingIntent var2) = 48;
    void iglms50(in UserDataType var1, in LatLngBounds var2, in List var3, in PlacesParams var4, IPlacesCallbacks var5) = 49;
    IBinder iglms51() = 50;

    IBinder iglms54() = 53;
    void iglms55(String var1, in LatLngBounds var2, in AutocompleteFilter var3, in PlacesParams var4, IPlacesCallbacks var5) = 54;

    void iglms58(in List var1, in PlacesParams var2, IPlacesCallbacks var3) = 57;

    void iglms65(in PendingIntent pendingIntent, IStatusCallback callback) = 64;
    void iglms66(in PendingIntent pendingIntent, IStatusCallback callback) = 65;

    void iglms68(in PendingIntent pendingIntent, IStatusCallback callback) = 67;
//    void iglms70(in ActivityRecognitionRequest request, in PendingIntent pendingIntent, IStatusCallback callback) = 69;
    void iglms71(IStatusCallback callback) = 70;
    void iglms76(in PendingIntent pendingIntent) = 75;
    int iglms78() = 77;
}
