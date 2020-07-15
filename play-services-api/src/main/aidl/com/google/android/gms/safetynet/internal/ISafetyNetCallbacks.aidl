package com.google.android.gms.safetynet.internal;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.safetynet.AttestationData;
import com.google.android.gms.safetynet.HarmfulAppsData;
import com.google.android.gms.safetynet.SafeBrowsingData;

interface ISafetyNetCallbacks {
    void onAttestationData(in Status status, in AttestationData attestationData) = 0;
    void onString(String s) = 1;
    void onSafeBrowsingData(in Status status, in SafeBrowsingData safeBrowsingData) = 2;
    void onBoolean(in Status status, boolean b) = 3;
    void onHarmfulAppsData(in Status status, in List<HarmfulAppsData> harmfulAppsData) = 4;
}