package com.google.android.gms.people.internal;

import android.os.Bundle;
import android.os.ParcelFileDescriptor;

import com.google.android.gms.common.data.DataHolder;

interface IPeopleCallbacks {
    void onDataHolder(int code, in Bundle resolution, in DataHolder holder) = 1;
    void onDataHolders(int code, in Bundle resolution, in DataHolder[] holders) = 3;
    void onParcelFileDescriptor(int code, in Bundle resolution, in ParcelFileDescriptor fileDescriptor, in Bundle extras) = 4;
}
