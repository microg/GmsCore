package com.google.android.gms.cast.framework;

interface ICastConnectionController {
    void joinApplication(String applicationId, String sessionId) = 0;
    //void launchApplication(String applicationId, LaunchOptions options) = 1;
    void stopApplication(String sessionId) = 2;
    //void unknown(int i) = 3;
    int getSupportedVersion() = 4;
}