package com.google.android.gms.fido.sourcedevice.internal;

import android.app.PendingIntent;
import com.google.android.gms.common.api.ApiMetadata;
import com.google.android.gms.common.api.Status;

interface IStartDirectTransferCallbacks {
    void onResult(in Status status, in PendingIntent pendingIntent, in ApiMetadata apiMetadata);
}
