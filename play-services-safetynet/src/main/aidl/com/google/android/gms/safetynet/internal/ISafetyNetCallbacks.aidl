package com.google.android.gms.safetynet.internal;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.safetynet.AttestationData;
import com.google.android.gms.safetynet.HarmfulAppsData;
import com.google.android.gms.safetynet.HarmfulAppsInfo;
import com.google.android.gms.safetynet.RecaptchaResultData;
import com.google.android.gms.safetynet.RemoveHarmfulAppData;
import com.google.android.gms.safetynet.SafeBrowsingData;

interface ISafetyNetCallbacks {
    oneway void onAttestationResult(in Status status, in AttestationData attestationData) = 0;
    oneway void onString(String s) = 1;
    oneway void onSafeBrowsingData(in Status status, in SafeBrowsingData safeBrowsingData) = 2;
    oneway void onVerifyAppsUserResult(in Status status, boolean enabled) = 3;
    oneway void onHarmfulAppsData(in Status status, in List<HarmfulAppsData> harmfulAppsData) = 4;
    oneway void onRecaptchaResult(in Status status, in RecaptchaResultData recaptchaResultData) = 5;
    oneway void onHarmfulAppsInfo(in Status status, in HarmfulAppsInfo harmfulAppsInfo) = 7;
    oneway void onInitSafeBrowsingResult(in Status status) = 10;
    oneway void onRemoveHarmfulAppData(in Status status, in RemoveHarmfulAppData removeHarmfulAppData) = 14;
}
