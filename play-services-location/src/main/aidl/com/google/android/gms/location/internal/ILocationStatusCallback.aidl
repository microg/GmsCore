package com.google.android.gms.location.internal;

import android.location.Location;
import com.google.android.gms.common.api.Status;

interface ILocationStatusCallback {
    void onLocationStatus(in Status status, in Location location);
}
