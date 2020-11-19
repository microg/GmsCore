package com.google.android.gms.common.internal;

import android.os.Bundle;

import com.google.android.gms.common.internal.IGmsCallbacks;
import com.google.android.gms.common.internal.GetServiceRequest;
import com.google.android.gms.common.internal.ValidateAccountRequest;

interface IGmsServiceBroker {
    void getPeopleService(IGmsCallbacks callback, int code, String str, in Bundle params) = 4;
    void getReportingService(IGmsCallbacks callback, int code, String str, in Bundle params) = 5;
    void getGoogleLocationManagerService(IGmsCallbacks callback, int code, String str, in Bundle params) = 7;
    void getPlayLogService(IGmsCallbacks callback, int code, String str, in Bundle params) = 10;
    void getCastMirroringService(IGmsCallbacks callback, int code, String str, in Bundle params) = 14;
    void getGoogleIdentityService(IGmsCallbacks callback, int code, String str, in Bundle params) = 16;
    void getCastService(IGmsCallbacks callback, int code, String str, IBinder binder, in Bundle params) = 18;
    void getAddressService(IGmsCallbacks callback, int code, String str) = 23;
    void getService(IGmsCallbacks callback, in GetServiceRequest request) = 45;
    void validateAccount(IGmsCallbacks callback, in ValidateAccountRequest request) = 46;
}
