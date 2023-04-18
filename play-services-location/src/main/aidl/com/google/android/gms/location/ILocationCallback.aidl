package com.google.android.gms.location;

import android.location.Location;

import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationResult;

interface ILocationCallback {
    oneway void onLocationResult(in LocationResult result) = 0;
    oneway void onLocationAvailability(in LocationAvailability availability) = 1;
    oneway void cancel() = 2;
}
