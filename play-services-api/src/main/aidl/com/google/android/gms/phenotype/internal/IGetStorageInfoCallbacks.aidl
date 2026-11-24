package com.google.android.gms.phenotype.internal;

import com.google.android.gms.common.api.Status;

interface IGetStorageInfoCallbacks {
    oneway void onStorageInfo(in Status status, in byte[] data) = 1;
}
