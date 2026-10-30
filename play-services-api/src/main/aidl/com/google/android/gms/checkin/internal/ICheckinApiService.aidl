package com.google.android.gms.checkin.internal;

import com.google.android.gms.checkin.internal.ICheckinApiCallbacks;
import com.google.android.gms.common.api.internal.IStatusCallback;

interface ICheckinApiService {
    void checkin(IStatusCallback callback, in Bundle extras) = 0;
    void getAndroidId(ICheckinApiCallbacks callbacks) = 4;
}
