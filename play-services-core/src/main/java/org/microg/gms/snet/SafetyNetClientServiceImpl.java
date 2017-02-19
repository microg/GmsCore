/*
 * Copyright 2013-2016 microG Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.microg.gms.snet;

import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Base64;
import android.util.Log;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.safetynet.AttestationData;
import com.google.android.gms.safetynet.HarmfulAppsData;
import com.google.android.gms.safetynet.internal.ISafetyNetCallbacks;
import com.google.android.gms.safetynet.internal.ISafetyNetService;

import org.microg.gms.checkin.LastCheckinInfo;
import org.microg.gms.common.PackageUtils;
import org.microg.gms.common.Utils;
import org.microg.gms.droidguard.RemoteDroidGuardConnector;

import java.io.IOException;
import java.util.ArrayList;

public class SafetyNetClientServiceImpl extends ISafetyNetService.Stub {
    private static final String TAG = "GmsSafetyNetClientImpl";

    private Context context;
    private String packageName;
    private Attestation attestation;

    public SafetyNetClientServiceImpl(Context context, String packageName) {
        this.context = context;
        this.packageName = packageName;
        this.attestation = new Attestation(context, packageName);
    }

    @Override
    public void attest(final ISafetyNetCallbacks callbacks, final byte[] nonce) throws RemoteException {
        if (nonce == null) {
            callbacks.onAttestationData(new Status(10), null);
            return;
        }

        if (!SafetyNetPrefs.get(context).isEnabled()) {
            Log.d(TAG, "ignoring SafetyNet request, it's disabled");
            callbacks.onAttestationData(Status.CANCELED, null);
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    try {
                        attestation.buildPayload(nonce);
                        RemoteDroidGuardConnector conn = new RemoteDroidGuardConnector(context);
                        Bundle bundle = new Bundle();
                        bundle.putString("contentBinding", attestation.getPayloadHashBase64());
                        RemoteDroidGuardConnector.Result dg = conn.guard("attest", Long.toString(LastCheckinInfo.read(context).androidId), bundle);
                        if (!SafetyNetPrefs.get(context).isOfficial() || dg != null && dg.getStatusCode() == 0 && dg.getResult() != null) {
                            if (dg != null && dg.getStatusCode() == 0 && dg.getResult() != null) {
                                attestation.setDroidGaurdResult(Base64.encodeToString(dg.getResult(), Base64.NO_WRAP + Base64.NO_PADDING + Base64.URL_SAFE));
                            }
                            AttestationData data = new AttestationData(attestation.attest());
                            callbacks.onAttestationData(Status.SUCCESS, data);
                        } else {
                            callbacks.onAttestationData(dg == null ? Status.INTERNAL_ERROR : new Status(dg.getStatusCode()), null);
                        }
                    } catch (IOException e) {
                        Log.w(TAG, e);
                        callbacks.onAttestationData(Status.INTERNAL_ERROR, null);
                    }
                } catch (RemoteException e) {
                    Log.w(TAG, e);
                }
            }
        }).start();
    }
    
    @Override
    public void attestWithApiKey(ISafetyNetCallbacks callbacks, final byte[] nonce, String apiKey) throws RemoteException {
        Log.d(TAG, "dummy Method: attestWithApiKey");
    }
    
    @Override
    public void getSharedUuid(ISafetyNetCallbacks callbacks) throws RemoteException {
        PackageUtils.checkPackageUid(context, packageName, getCallingUid());
        PackageUtils.assertExtendedAccess(context);

        // TODO
        Log.d(TAG, "dummy Method: getSharedUuid");
        callbacks.onString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    }

    @Override
    public void lookupUri(ISafetyNetCallbacks callbacks, String s1, int[] threatTypes, int i, String s2) throws RemoteException {
        Log.d(TAG, "unimplemented Method: lookupUri");

    }

    @Override
    public void init(ISafetyNetCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "dummy Method: init");
        callbacks.onBoolean(Status.SUCCESS, true);
    }

    @Override
    public void getHarmfulAppsList(ISafetyNetCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "dummy Method: getHarmfulAppsList");
        callbacks.onHarmfulAppsData(Status.SUCCESS, new ArrayList<HarmfulAppsData>());
    }
}
