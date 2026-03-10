package com.google.android.gms.safetynet.internal;

import com.google.android.gms.safetynet.internal.ISafetyNetCallbacks;

interface ISafetyNetService {
    void attest(ISafetyNetCallbacks callbacks, in byte[] nonce) = 0;
    void attestWithApiKey(ISafetyNetCallbacks callbacks, in byte[] nonce, String apiKey) = 6;
    void getSharedUuid(ISafetyNetCallbacks callbacks) = 1;
    void lookupUri(ISafetyNetCallbacks callbacks, String apiKey, in int[] threatTypes, int version, String uri) = 2;
    void enableVerifyApps(ISafetyNetCallbacks callbacks) = 3;
    void listHarmfulApps(ISafetyNetCallbacks callbacks) = 4;
    void verifyWithRecaptcha(ISafetyNetCallbacks callbacks, String siteKey) = 5;

//    void fun9(ISafetyNetCallbacks callbacks) = 8;
//    void fun10(ISafetyNetCallbacks callbacks, String s1, int i1, in byte[] b1) = 9;
//    void fun11(int i1, in Bundle b1) = 10;
    void initSafeBrowsing(ISafetyNetCallbacks callbacks) = 11;
    void shutdownSafeBrowsing() = 12;
    void isVerifyAppsEnabled(ISafetyNetCallbacks callbacks) = 13;
//
//    void fun18(ISafetyNetCallbacks callbacks, int i1, String s1) = 17;
//    void fun19(ISafetyNetCallbacks callbacks, int i1) = 18;
//    void removeHarmfulApp(ISafetyNetCallbacks callbacks, String packageName, in byte[] digest) = 19;
//    void fun21(ISafetyNetCallbacks callbacks, in Bundle b1) = 20;
}
