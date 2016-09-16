package com.google.android.gms.safetynet.internal;

import com.google.android.gms.safetynet.internal.ISafetyNetCallbacks;

interface ISafetyNetService {
    void attest(ISafetyNetCallbacks callbacks, in byte[] nonce) = 0;
    void getSharedUuid(ISafetyNetCallbacks callbacks) = 1;
    void lookupUri(ISafetyNetCallbacks callbacks, String s1, in int[] threatTypes, int i, String s2) = 2;
    void init(ISafetyNetCallbacks callbacks) = 3;
    void unknown4(ISafetyNetCallbacks callbacks) = 4;
}