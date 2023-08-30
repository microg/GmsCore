package com.google.android.gms.measurement.internal;

import com.google.android.gms.measurement.internal.AppMetadata;
import com.google.android.gms.measurement.internal.ConditionalUserPropertyParcel;
import com.google.android.gms.measurement.internal.EventParcel;
import com.google.android.gms.measurement.internal.UserAttributeParcel;

interface IMeasurementService {
    void sendEvent(in EventParcel event, in AppMetadata app) = 0;
    void sendUserProperty(in UserAttributeParcel attribute, in AppMetadata app) = 1;
    void sendAppLaunch(in AppMetadata app) = 3;
//    void f5(in EventParcel event, String p1, String p2) = 4;
    void sendMeasurementEnabled(in AppMetadata p0) = 5;
    List<UserAttributeParcel> getAllUserProperties(in AppMetadata app, boolean includeInternal) = 6;
//    byte[] f9(in EventParcel event, String p1) = 8;
    void sendCurrentScreen(long id, String name, String referrer, String packageName) = 9;
    String getAppInstanceId(in AppMetadata app) = 10;
    void sendConditionalUserProperty(in ConditionalUserPropertyParcel property, in AppMetadata app) = 11;
//    void f13(ConditionalUserPropertyParcel p0) = 12;
    List<UserAttributeParcel> getUserProperties(String origin, String propertyNamePrefix, boolean includeInternal, in AppMetadata app) = 13;
    List<UserAttributeParcel> getUserPropertiesAs(String packageName, String origin, String propertyNamePrefix, boolean includeInternal) = 14;
    List<ConditionalUserPropertyParcel> getConditionalUserProperties(String origin, String propertyNamePrefix, in AppMetadata app) = 15;
    List<ConditionalUserPropertyParcel> getConditionalUserPropertiesAs(String packageName, String origin, String propertyNamePrefix) = 16;
    void reset(in AppMetadata app) = 17;
    void sendDefaultEventParameters(in Bundle params, in AppMetadata app) = 18;
    void sendConsentSettings(in AppMetadata app) = 19;
}
