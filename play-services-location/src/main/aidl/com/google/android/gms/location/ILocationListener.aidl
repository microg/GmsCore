package com.google.android.gms.location;

import android.location.Location;

interface ILocationListener {
    oneway void onLocationChanged(in Location location) = 0;
    oneway void cancel() = 1;
}
