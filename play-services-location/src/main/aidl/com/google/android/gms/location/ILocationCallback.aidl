package com.google.android.gms.location;

import android.location.Location;

import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationResult;

interface ILocationCallback {
    void onLocationResult(in LocationResult result) = 0;
    void onLocationAvailability(in LocationAvailability availability) = 1;
}
