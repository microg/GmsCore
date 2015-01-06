package com.google.android.gms.common.internal;

import com.google.android.gms.common.internal.IGmsCallbacks;

interface IGmsServiceBroker {
    void getPlusService(IGmsCallbacks callback, int code, String str1, String str2, in String[] paramArrayOfString, String str3, in Bundle params); 
    void getPanoramaService(IGmsCallbacks callback, int code, String str, in Bundle params); 
    void getAppDataSearchService(IGmsCallbacks callback, int code, String str); 
    void getWalletService(IGmsCallbacks callback, int code); 
    void getPeopleService(IGmsCallbacks callback, int code, String str, in Bundle params);
    void getReportingService(IGmsCallbacks callback, int code, String str, in Bundle params);
    void getLocationService(IGmsCallbacks callback, int code, String str, in Bundle params);
    void getGoogleLocationManagerService(IGmsCallbacks callback, int code, String str, in Bundle params);
    void getGamesService(IGmsCallbacks callback, int code, String str1, String str2, in String[] args, String str3, IBinder binder, String str4, in Bundle params);
    void getAppStateService(IGmsCallbacks callback, int code, String str1, String str2, in String[] args);
    void getPlayLogService(IGmsCallbacks callback, int code, String str, in Bundle params);
    void getAdMobService(IGmsCallbacks callback, int code, String str, in Bundle params);
    void getDroidGuardService(IGmsCallbacks callback, int code, String str, in Bundle params);
    void getLockboxService(IGmsCallbacks callback, int code, String str, in Bundle params);
    void getCastMirroringService(IGmsCallbacks callback, int code, String str, in Bundle params);
    void getNetworkQualityService(IGmsCallbacks callback, int code, String str, in Bundle params);
    void getGoogleIdentityService(IGmsCallbacks callback, int code, String str, in Bundle params);
    void getGoogleFeedbackService(IGmsCallbacks callback, int code, String str, in Bundle params);
    void getCastService(IGmsCallbacks callback, int code, String str, IBinder binder, in Bundle params);
    void getDriveService(IGmsCallbacks callback, int code, String str1, in String[] args, String str2, in Bundle params);
    void getLightweightAppDataSearchService(IGmsCallbacks callback, int code, String str); 
    void getSearchAdministrationService(IGmsCallbacks callback, int code, String str); 
    void getAutoBackupService(IGmsCallbacks callback, int code, String str, in Bundle params);
    void getAddressService(IGmsCallbacks callback, int code, String str); 
}
