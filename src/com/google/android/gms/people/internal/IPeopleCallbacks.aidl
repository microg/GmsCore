package com.google.android.gms.people.internal;

import com.google.android.gms.common.data.DataHolder;

interface IPeopleCallbacks {
    void onDataHolders(int code, in Bundle meta, in DataHolder[] data) = 3;
}
