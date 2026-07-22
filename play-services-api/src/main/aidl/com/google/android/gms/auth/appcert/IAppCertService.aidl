package com.google.android.gms.auth.appcert;

interface IAppCertService {
    boolean fetchDeviceKey() = 0;
    String getSpatulaHeader(String packageName) = 1;
}
