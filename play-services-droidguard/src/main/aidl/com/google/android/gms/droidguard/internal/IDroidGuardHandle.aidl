package com.google.android.gms.droidguard.internal;

import com.google.android.gms.droidguard.internal.DroidGuardInitReply;
import com.google.android.gms.droidguard.internal.DroidGuardResultsRequest;

interface IDroidGuardHandle {
    oneway void init(String flow) = 0;
    byte[] snapshot(in Map map) = 1;
    oneway void close() = 2;
    DroidGuardInitReply initWithRequest(String flow, in DroidGuardResultsRequest request) = 4;
}
