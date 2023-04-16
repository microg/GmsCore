package com.google.android.gms.location;

import android.location.Location;

interface ILocationListener {
    void onLocationChanged(in Location location);
}
