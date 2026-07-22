package com.google.android.gms.cast.framework;

import com.google.android.gms.cast.LaunchOptions;

interface ICastConnectionController {
    void joinApplication(String applicationId, String sessionId) = 0;
    void launchApplication(String applicationId, in LaunchOptions launchOptions) = 1;
    void stopApplication(String sessionId) = 2;
    void closeConnection(int reason) = 3; // Maybe?
    int getSupportedVersion() = 4;
}
