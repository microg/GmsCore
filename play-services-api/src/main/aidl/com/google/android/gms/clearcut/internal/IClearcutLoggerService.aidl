package com.google.android.gms.clearcut.internal;

import com.google.android.gms.clearcut.internal.IClearcutLoggerCallbacks;
import com.google.android.gms.clearcut.LogEventParcelable;

interface IClearcutLoggerService {
    void log(IClearcutLoggerCallbacks callbacks, in LogEventParcelable event) = 0;
}
