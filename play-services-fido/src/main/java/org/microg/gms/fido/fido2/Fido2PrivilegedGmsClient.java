/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.fido2;

import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;

import com.google.android.gms.fido.fido2.api.IBooleanCallback;
import com.google.android.gms.fido.fido2.api.ICredentialListCallback;
import com.google.android.gms.fido.fido2.api.common.BrowserPublicKeyCredentialCreationOptions;
import com.google.android.gms.fido.fido2.api.common.BrowserPublicKeyCredentialRequestOptions;
import com.google.android.gms.fido.fido2.internal.privileged.IFido2PrivilegedCallbacks;
import com.google.android.gms.fido.fido2.internal.privileged.IFido2PrivilegedService;

import org.microg.gms.common.GmsClient;
import org.microg.gms.common.GmsService;
import org.microg.gms.common.api.ConnectionCallbacks;
import org.microg.gms.common.api.OnConnectionFailedListener;

public class Fido2PrivilegedGmsClient extends GmsClient<IFido2PrivilegedService> {
    public Fido2PrivilegedGmsClient(Context context, ConnectionCallbacks callbacks, OnConnectionFailedListener connectionFailedListener) {
        super(context, callbacks, connectionFailedListener, GmsService.FIDO2_PRIVILEGED.ACTION);
        serviceId = GmsService.FIDO2_PRIVILEGED.SERVICE_ID;
    }

    public void getRegisterPendingIntent(IFido2PrivilegedCallbacks callbacks, BrowserPublicKeyCredentialCreationOptions options) throws RemoteException {
        getServiceInterface().getRegisterPendingIntent(callbacks, options);
    }

    public void getSignPendingIntent(IFido2PrivilegedCallbacks callbacks, BrowserPublicKeyCredentialRequestOptions options) throws RemoteException {
        getServiceInterface().getSignPendingIntent(callbacks, options);
    }

    public void isUserVerifyingPlatformAuthenticatorAvailable(IBooleanCallback callback) throws RemoteException {
        getServiceInterface().isUserVerifyingPlatformAuthenticatorAvailable(callback);
    }

    public void getCredentialList(ICredentialListCallback callbacks, String rpId) throws RemoteException {
        getServiceInterface().getCredentialList(callbacks, rpId);
    }

    @Override
    protected IFido2PrivilegedService interfaceFromBinder(IBinder binder) {
        return IFido2PrivilegedService.Stub.asInterface(binder);
    }
}
