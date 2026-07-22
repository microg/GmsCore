package com.google.android.gms.location.internal;

import com.google.android.gms.location.LocationSettingsResult;

interface ISettingsCallbacks {
    void onLocationSettingsResult(in LocationSettingsResult result);
}
