package com.google.android.gms.measurement.internal;

import com.google.android.gms.measurement.internal.AppMetadata;
import com.google.android.gms.measurement.internal.ConditionalUserPropertyParcel;
import com.google.android.gms.measurement.internal.EventParcel;
import com.google.android.gms.measurement.internal.UserAttributeParcel;

interface IMeasurementService {
    void f1(in EventParcel event, in AppMetadata app) = 0;
    void f2(in UserAttributeParcel attribute, in AppMetadata app) = 1;
    void f4(in AppMetadata app) = 3;
//    void f5(in EventParcel p0, String p1, String p2) = 4;
//    void f6(in AppMetadata p0) = 5;
//    List<UserAttributeParcel> f7(in AppMetadata p0, boolean p1) = 6;
//    byte[] f9(in EventParcel p0, String p1) = 8;
    void f10(long p0, String p1, String p2, String p3) = 9;
    String f11(in AppMetadata app) = 10;
    void f12(in ConditionalUserPropertyParcel property, in AppMetadata app) = 11;
//    void f13(ConditionalUserPropertyParcel p0) = 12;
//    List<UserAttributeParcel> getUserProperties(String p0, String p1, boolean p2, in AppMetadata p3) = 13;
//    List<UserAttributeParcel> getUserPropertiesAs(String p0, String p1, String p2, boolean p3) = 14;
//    List<ConditionalUserPropertyParcel> getConditionalUserProperties(String p0, String p1, in AppMetadata p2) = 15;
//    List<ConditionalUserPropertyParcel> getCondtionalUserPropertiesAs(String p0, String p1, String p2) = 16;
//    void f18(in AppMetadata p0) = 17;
    void setDefaultEventParameters(in Bundle params, in AppMetadata app) = 18;
//    void f20(in AppMetadata p0) = 19;
}
