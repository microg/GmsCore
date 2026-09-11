package com.google.android.gms.ads.measurement;

import com.google.android.gms.ads.measurement.IAppMeasurementProxy;
import com.google.android.gms.dynamic.IObjectWrapper;

interface IMeasurementManager {
    void initialize(IObjectWrapper context, IAppMeasurementProxy proxy) = 1;
}