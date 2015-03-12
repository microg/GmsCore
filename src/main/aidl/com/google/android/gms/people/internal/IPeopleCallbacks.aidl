package com.google.android.gms.people.internal;

import com.google.android.gms.common.data.DataHolder;

interface IPeopleCallbacks {
    void onDataHolder(int code, in Bundle resolution, in DataHolder holder) = 1;
    void onDataHolders(int code, in Bundle resolution, in DataHolder[] holders) = 3;
}
