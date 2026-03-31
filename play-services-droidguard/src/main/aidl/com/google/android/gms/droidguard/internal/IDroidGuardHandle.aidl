package com.google.android.gms.droidguard.internal;

import com.google.android.gms.droidguard.internal.DroidGuardInitReply;
import com.google.android.gms.droidguard.internal.DroidGuardResultsRequest;

interface IDroidGuardHandle {
    oneway void init(String flow) = 0;
    byte[] snapshot(in Map map) = 1;
    oneway void close() = 2;
    DroidGuardInitReply initWithRequest(String flow, in DroidGuardResultsRequest request) = 4;

    /**
     * Multi-step (session-based) flow support.
     *
     * Backwards compatible: existing one-shot methods remain unchanged.
     *
     * Contract (recommended):
     * - begin(...) creates an independent multi-step session and returns a sessionId (>0).
     * - nextStep(...) advances the session by providing client-produced data from the previous step
     *   (or an empty map for the first call) and returns the next action to perform.
     *   Returning null means "no more init steps required".
     * - snapshotWithSession(...) produces the final snapshot bound to that session.
     * - closeSession(...) releases any session resources (idempotent).
     */
    long begin(String flow, in DroidGuardResultsRequest request, in Map initialData) = 5;
    DroidGuardInitReply nextStep(long sessionId, in Map stepData) = 6;
    byte[] snapshotWithSession(long sessionId, in Map map) = 7;
    oneway void closeSession(long sessionId) = 8;
}
