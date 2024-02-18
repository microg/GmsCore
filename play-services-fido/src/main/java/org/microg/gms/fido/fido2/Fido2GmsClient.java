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
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialCreationOptions;
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialRequestOptions;
import com.google.android.gms.fido.fido2.internal.regular.IFido2RegularCallbacks;
import com.google.android.gms.fido.fido2.internal.regular.IFido2RegularService;

import org.microg.gms.common.GmsClient;
import org.microg.gms.common.GmsService;
import org.microg.gms.common.api.ConnectionCallbacks;
import org.microg.gms.common.api.OnConnectionFailedListener;

public class Fido2GmsClient extends GmsClient<IFido2RegularService> {
    public Fido2GmsClient(Context context, ConnectionCallbacks callbacks, OnConnectionFailedListener connectionFailedListener) {
        super(context, callbacks, connectionFailedListener, GmsService.FIDO2_REGULAR.ACTION);
        serviceId = GmsService.FIDO2_REGULAR.SERVICE_ID;
    }

    public void getRegisterPendingIntent(IFido2RegularCallbacks callbacks, PublicKeyCredentialCreationOptions options) throws RemoteException {
        getServiceInterface().getRegisterPendingIntent(callbacks, options);
    }

    public void getSignPendingIntent(IFido2RegularCallbacks callbacks, PublicKeyCredentialRequestOptions options) throws RemoteException {
        getServiceInterface().getSignPendingIntent(callbacks, options);
    }

    public void isUserVerifyingPlatformAuthenticatorAvailable(IBooleanCallback callback) throws RemoteException {
        getServiceInterface().isUserVerifyingPlatformAuthenticatorAvailable(callback);
    }

    public void getCredentialList(ICredentialListCallback callbacks, String rpId) throws RemoteException {
        getServiceInterface().getCredentialList(callbacks, rpId);
    }

    @Override
    protected IFido2RegularService interfaceFromBinder(IBinder binder) {
        return IFido2RegularService.Stub.asInterface(binder);
    }
}
