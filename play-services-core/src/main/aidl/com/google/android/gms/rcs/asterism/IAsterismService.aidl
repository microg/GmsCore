package com.google.android.gms.rcs.asterism;

interface IAsterismService {
    void registerAsterismCallback(in IAsterismCallback callback);
    void unregisterAsterismCallback(in IAsterismCallback callback);
}
