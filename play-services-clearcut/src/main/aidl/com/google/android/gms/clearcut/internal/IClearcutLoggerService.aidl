package com.google.android.gms.clearcut.internal;

import com.google.android.gms.clearcut.internal.IClearcutLoggerCallbacks;
import com.google.android.gms.clearcut.LogEventParcelable;

interface IClearcutLoggerService {
    oneway void log(IClearcutLoggerCallbacks callbacks, in LogEventParcelable event) = 0;
    oneway void forceUpload(IClearcutLoggerCallbacks callbacks) = 1;
    oneway void startCollectForDebug(IClearcutLoggerCallbacks callbacks) = 2;
    oneway void stopCollectForDebug(IClearcutLoggerCallbacks callbacks) = 3;
    oneway void getCollectForDebugExpiryTime(IClearcutLoggerCallbacks callbacks) = 4;
    oneway void getLogEventParcelablesLegacy(IClearcutLoggerCallbacks callbacks) = 5;
    oneway void getLogEventParcelables(IClearcutLoggerCallbacks callbacks) = 6;
}
