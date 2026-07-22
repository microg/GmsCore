package com.google.android.gms.location;

import com.google.android.gms.location.DeviceOrientation;

interface IDeviceOrientationListener {
    oneway void onDeviceOrientationChanged(in DeviceOrientation deviceOrientation);
}
