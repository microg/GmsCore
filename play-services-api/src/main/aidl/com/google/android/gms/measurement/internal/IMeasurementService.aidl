package com.google.android.gms.measurement.internal;

import com.google.android.gms.measurement.internal.AppMetadata;
import com.google.android.gms.measurement.internal.ConditionalUserPropertyParcel;
import com.google.android.gms.measurement.internal.EventParcel;

interface IMeasurementService {
    void f1(in EventParcel p0, in AppMetadata p1) = 0;
//    void zza(UserAttributeParcel p0, AppMetadata p1) = 1;
    void f4(in AppMetadata p0) = 3;
//    void zza(EventParcel p0, String p1, String p2) = 4;
//    void zzb(AppMetadata p0) = 5;
//    List<UserAttributeParcel> zza(AppMetadata p0, boolean p1) = 6;
//    byte[] zza(EventParcel p0, String p1) = 8;
    void f10(long p0, String p1, String p2, String p3) = 9;
    String f11(in AppMetadata p0) = 10;
    void f12(in ConditionalUserPropertyParcel p0, in AppMetadata p1) = 11;
//    void zza(ConditionalUserPropertyParcel p0) = 12;
//    List<UserAttributeParcelzkr> zza(String p0, String p1, boolean p2, AppMetadata p3) = 13;
//    List<UserAttributeParcel> zza(String p0, String p1, String p2, boolean p3) = 14;
//    List<ConditionalUserPropertyParcel> zza(String p0, String p1, AppMetadata p2) = 15;
//    List<ConditionalUserPropertyParcel> zza(String p0, String p1, String p2) = 16;
//    void zzd(AppMetadata p0) = 17;
//    void zza(Bundle p0, AppMetadata p1) = 18;
}
