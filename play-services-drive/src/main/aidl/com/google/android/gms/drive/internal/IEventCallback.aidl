package com.google.android.gms.drive.internal;

import com.google.android.gms.drive.internal.EventResponse;

interface IEventCallback {
    void onEventResponse(in EventResponse response) = 0;
}
