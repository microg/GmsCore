/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.safetynet;

import android.os.RemoteException;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.safetynet.AttestationData;
import com.google.android.gms.safetynet.HarmfulAppsData;
import com.google.android.gms.safetynet.HarmfulAppsInfo;
import com.google.android.gms.safetynet.RecaptchaResultData;
import com.google.android.gms.safetynet.RemoveHarmfulAppData;
import com.google.android.gms.safetynet.SafeBrowsingData;
import com.google.android.gms.safetynet.internal.ISafetyNetCallbacks;

import java.util.List;

public class ISafetyNetCallbacksDefaultStub extends ISafetyNetCallbacks.Stub {
    @Override
    public void onAttestationResult(Status status, AttestationData attestationData) throws RemoteException {
    }

    @Override
    public void onSharedUuid(String s) throws RemoteException {
    }

    @Override
    public void onSafeBrowsingData(Status status, SafeBrowsingData safeBrowsingData) throws RemoteException {
    }

    @Override
    public void onVerifyAppsUserResult(Status status, boolean enabled) throws RemoteException {

    }

    @Override
    public void onHarmfulAppsData(Status status, List<HarmfulAppsData> harmfulAppsData) throws RemoteException {
    }

    @Override
    public void onRecaptchaResult(Status status, RecaptchaResultData recaptchaResultData) throws RemoteException {
    }

    @Override
    public void onHarmfulAppsInfo(Status status, HarmfulAppsInfo harmfulAppsInfo) throws RemoteException {
    }

    @Override
    public void onInitSafeBrowsingResult(Status status) throws RemoteException {
    }

    @Override
    public void onRemoveHarmfulAppData(Status status, RemoveHarmfulAppData removeHarmfulAppData) throws RemoteException {
    }
}
