package com.google.android.gms.location.internal;

import android.location.Location;

interface ILocationListener {
    void onLocationChanged(in Location location);
}
