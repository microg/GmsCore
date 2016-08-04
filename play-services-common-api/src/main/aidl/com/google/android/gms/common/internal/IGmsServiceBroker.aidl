package com.google.android.gms.common.internal;

import android.os.Bundle;

import com.google.android.gms.common.internal.IGmsCallbacks;
import com.google.android.gms.common.internal.GetServiceRequest;
import com.google.android.gms.common.internal.ValidateAccountRequest;

interface IGmsServiceBroker {
    void getPlusService(IGmsCallbacks callback, int code, String packageName, String authPackage, in String[] scopes, String accountName, in Bundle params) = 0;
    void getPanoramaService(IGmsCallbacks callback, int code, String packageName, in Bundle params) = 1;
    void getAppDataSearchService(IGmsCallbacks callback, int code, String packageName) = 2;
    void getWalletService(IGmsCallbacks callback, int code) = 3;
    void getPeopleService(IGmsCallbacks callback, int code, String str, in Bundle params) = 4;
    void getReportingService(IGmsCallbacks callback, int code, String str, in Bundle params) = 5;
    void getLocationService(IGmsCallbacks callback, int code, String str, in Bundle params) = 6;
    void getGoogleLocationManagerService(IGmsCallbacks callback, int code, String str, in Bundle params) = 7;
    void getGamesService(IGmsCallbacks callback, int code, String packageName, String accountName, in String[] scopes, String gamePackageName, IBinder popupWindowToken, String desiredLocale, in Bundle params) = 8;
    void getAppStateService(IGmsCallbacks callback, int code, String packageName, String accountName, in String[] scopes) = 9;
    void getPlayLogService(IGmsCallbacks callback, int code, String str, in Bundle params) = 10;
    void getAdMobService(IGmsCallbacks callback, int code, String str, in Bundle params) = 11;
    void getDroidGuardService(IGmsCallbacks callback, int code, String str, in Bundle params) = 12;
    void getLockboxService(IGmsCallbacks callback, int code, String str, in Bundle params) = 13;
    void getCastMirroringService(IGmsCallbacks callback, int code, String str, in Bundle params) = 14;
    void getNetworkQualityService(IGmsCallbacks callback, int code, String str, in Bundle params) = 15;
    void getGoogleIdentityService(IGmsCallbacks callback, int code, String str, in Bundle params) = 16;
    void getGoogleFeedbackService(IGmsCallbacks callback, int code, String str, in Bundle params) = 17;
    void getCastService(IGmsCallbacks callback, int code, String str, IBinder binder, in Bundle params) = 18;
    void getDriveService(IGmsCallbacks callback, int code, String str1, in String[] args, String str2, in Bundle params) = 19;
    void getLightweightAppDataSearchService(IGmsCallbacks callback, int code, String str) = 20;
    void getSearchAdministrationService(IGmsCallbacks callback, int code, String str) = 21;
    void getAutoBackupService(IGmsCallbacks callback, int code, String str, in Bundle params) = 22;
    void getAddressService(IGmsCallbacks callback, int code, String str) = 23;

    void getWalletServiceWithPackageName(IGmsCallbacks callback, int code, String packageName) = 41;

    void getService(IGmsCallbacks callback, in GetServiceRequest request) = 45;
    void validateAccount(IGmsCallbacks callback, in ValidateAccountRequest request) = 46;
}
