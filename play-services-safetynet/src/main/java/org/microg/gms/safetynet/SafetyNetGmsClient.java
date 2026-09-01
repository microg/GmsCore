/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.safetynet;

import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;

import com.google.android.gms.safetynet.internal.ISafetyNetCallbacks;
import com.google.android.gms.safetynet.internal.ISafetyNetService;

import org.microg.gms.common.GmsClient;
import org.microg.gms.common.GmsService;
import com.google.android.gms.common.api.internal.ConnectionCallbacks;
import com.google.android.gms.common.api.internal.OnConnectionFailedListener;

public class SafetyNetGmsClient extends GmsClient<ISafetyNetService> {
    public SafetyNetGmsClient(Context context, ConnectionCallbacks callbacks, OnConnectionFailedListener connectionFailedListener) {
        super(context, callbacks, connectionFailedListener, GmsService.SAFETY_NET.ACTION);
        serviceId = GmsService.SAFETY_NET.SERVICE_ID;
    }

    public void attest(ISafetyNetCallbacks callbacks, byte[] nonce, String apiKey) throws RemoteException {
        getServiceInterface().attestWithApiKey(callbacks, nonce, apiKey);
    }

    public void verifyWithRecaptcha(ISafetyNetCallbacks callbacks, String siteKey) throws RemoteException {
        getServiceInterface().verifyWithRecaptcha(callbacks, siteKey);
    }

    public void enableVerifyApps(ISafetyNetCallbacks callbacks) throws RemoteException {
        getServiceInterface().enableVerifyApps(callbacks);
    }

    public void isVerifyAppsEnabled(ISafetyNetCallbacks callbacks) throws RemoteException {
        getServiceInterface().isVerifyAppsEnabled(callbacks);
    }

    @Override
    protected ISafetyNetService interfaceFromBinder(IBinder binder) {
        return ISafetyNetService.Stub.asInterface(binder);
    }
}
