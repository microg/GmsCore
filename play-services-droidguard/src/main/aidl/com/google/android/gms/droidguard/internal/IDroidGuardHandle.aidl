package com.google.android.gms.droidguard.internal;

import com.google.android.gms.droidguard.internal.DroidGuardInitReply;
import com.google.android.gms.droidguard.internal.DroidGuardResultsRequest;

interface IDroidGuardHandle {
    oneway void init(String flow) = 0;
    byte[] snapshot(in Map map) = 1;
    oneway void close() = 2;
    DroidGuardInitReply initWithRequest(String flow, in DroidGuardResultsRequest request) = 4;

    // Multi-step session support needed by Play Integrity.
    // Returns a non-negative session id on success, -1 otherwise.
    long begin(String flow, in DroidGuardResultsRequest request, in Map initialData) = 5;
    DroidGuardInitReply nextStep(long sessionId, in Map stepData) = 6;
    byte[] snapshotWithSession(long sessionId, in Map map) = 7;
    oneway void closeSession(long sessionId) = 8;
}
