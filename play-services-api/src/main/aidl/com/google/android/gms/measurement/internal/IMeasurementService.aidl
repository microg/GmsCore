package com.google.android.gms.measurement.internal;

import com.google.android.gms.measurement.internal.AppMetadata;
import com.google.android.gms.measurement.internal.BatchUploadStatusParcel;
import com.google.android.gms.measurement.internal.ConditionalUserPropertyParcel;
import com.google.android.gms.measurement.internal.ConsentParcel;
import com.google.android.gms.measurement.internal.EventParcel;
import com.google.android.gms.measurement.internal.ITriggerUrisCallback;
import com.google.android.gms.measurement.internal.IUploadBatchesCallback;
import com.google.android.gms.measurement.internal.UploadBatchesCriteria;
import com.google.android.gms.measurement.internal.UserAttributeParcel;

interface IMeasurementService {
    void sendEvent(in EventParcel event, in AppMetadata app) = 0;
    void sendUserProperty(in UserAttributeParcel attribute, in AppMetadata app) = 1;

    void sendAppLaunch(in AppMetadata app) = 3;
//    void f5(in EventParcel event, String p1, String p2) = 4;
    void sendMeasurementEnabled(in AppMetadata p0) = 5;
    List<UserAttributeParcel> getAllUserProperties(in AppMetadata app, boolean includeInternal) = 6;
//    byte[] logAndBundleEvent(in EventParcel event, String packageName) = 8;
    void sendCurrentScreen(long id, String name, String referrer, String packageName) = 9;
    String getAppInstanceId(in AppMetadata app) = 10;
    void sendConditionalUserProperty(in ConditionalUserPropertyParcel property, in AppMetadata app) = 11;
//    void f13(in ConditionalUserPropertyParcel p0) = 12;
    List<UserAttributeParcel> getUserProperties(String origin, String propertyNamePrefix, boolean includeInternal, in AppMetadata app) = 13;
    List<UserAttributeParcel> getUserPropertiesAs(String packageName, String origin, String propertyNamePrefix, boolean includeInternal) = 14;
    List<ConditionalUserPropertyParcel> getConditionalUserProperties(String origin, String propertyNamePrefix, in AppMetadata app) = 15;
    List<ConditionalUserPropertyParcel> getConditionalUserPropertiesAs(String packageName, String origin, String propertyNamePrefix) = 16;
    void reset(in AppMetadata app) = 17;
    void sendDefaultEventParameters(in Bundle params, in AppMetadata app) = 18;
    void sendConsentSettings(in AppMetadata app) = 19;
//    ConsentParcel getConsents(in AppMetadata app) = 20;


//    void f24(in AppMetadata app, in Bundle bundle) = 23;
//    void sendStorageConsentSettings(in AppMetadata app) = 24;
//    void sendDmaConsentSettings(in AppMetadata app) = 25;
//    void sendAppBackgrounded(in AppMetadata app) = 26;

//    void getUploadBatches(in AppMetadata app, in UploadBatchesCriteria criteria, IUploadBatchesCallback callback) = 28;
//    void discardUploadData(in AppMetadata app, in BatchUploadStatusParcel status) = 29;
//    void getTriggerUris(in AppMetadata app, in Bundle extras, ITriggerUrisCallback callback) = 30;
}
