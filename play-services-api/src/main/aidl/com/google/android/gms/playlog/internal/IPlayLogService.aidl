package com.google.android.gms.playlog.internal;

import com.google.android.gms.playlog.internal.LogEvent;
import com.google.android.gms.playlog.internal.PlayLoggerContext;

interface IPlayLogService {
    void onEvent(String packageName, in PlayLoggerContext context, in LogEvent event) = 1;
    void onMultiEvent(String packageName, in PlayLoggerContext context, in List<LogEvent> events) = 2;
}
