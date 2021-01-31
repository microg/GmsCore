package com.google.android.gms.droidguard.internal;

import com.google.android.gms.droidguard.internal.DroidGuardInitReply;
import com.google.android.gms.droidguard.internal.DroidGuardResultsRequest;

interface IDroidGuardHandle {
    void init(String flow) = 0;
    DroidGuardInitReply initWithRequest(String flow, in DroidGuardResultsRequest request) = 4;

    byte[] guard(in Map map) = 1;

    void close() = 2;
}
