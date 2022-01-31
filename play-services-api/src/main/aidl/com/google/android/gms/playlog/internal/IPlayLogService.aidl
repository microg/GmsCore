package com.google.android.gms.playlog.internal;

import com.google.android.gms.clearcut.internal.PlayLoggerContext;
import com.google.android.gms.playlog.internal.LogEvent;

// Deprecated
interface IPlayLogService {
    void onEvent(String packageName, in PlayLoggerContext context, in LogEvent event) = 1;
    void onMultiEvent(String packageName, in PlayLoggerContext context, in List<LogEvent> events) = 2;
}
