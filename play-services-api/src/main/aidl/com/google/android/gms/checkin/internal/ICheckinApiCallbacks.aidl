package com.google.android.gms.checkin.internal;

import com.google.android.gms.common.api.Status;

interface ICheckinApiCallbacks {
    void onAndroidId(in Status status, String androidId) = 4;
}
