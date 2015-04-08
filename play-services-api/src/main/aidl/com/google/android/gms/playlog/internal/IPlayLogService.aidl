package com.google.android.gms.playlog.internal;

import com.google.android.gms.playlog.internal.LogEvent;
import com.google.android.gms.playlog.internal.PlayLoggerContext;

interface IPlayLogService {
    void event(String packageName, in PlayLoggerContext context, in LogEvent event) = 1;
}
