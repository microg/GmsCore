package com.google.android.gms.fido.fido2.internal.regular;

import android.app.PendingIntent;
import com.google.android.gms.common.api.Status;

interface IFido2AppCallbacks {
    void onPendingIntent(in Status status, in PendingIntent pendingIntent);
}
