package com.google.android.gms.checkin.internal;

interface ICheckinService {
    String getDeviceDataVersionInfo();
    long getLastCheckinSuccessTime();
    String getLastSimOperator();
}
