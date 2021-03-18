package com.google.android.gms.droidguard.internal;

import com.google.android.gms.droidguard.internal.IDroidGuardCallbacks;
import com.google.android.gms.droidguard.internal.IDroidGuardHandle;
import com.google.android.gms.droidguard.internal.DroidGuardResultsRequest;

interface IDroidGuardService {
    void guard(IDroidGuardCallbacks callbacks, String flow, in Map map) = 0;
    void guardWithRequest(IDroidGuardCallbacks callbacks, String flow, in Map map, in DroidGuardResultsRequest request) = 3;

    IDroidGuardHandle getHandle() = 1;

    int getClientTimeoutMillis() = 2;
}
