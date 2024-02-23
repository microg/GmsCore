/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.fido2;

import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;

import com.google.android.gms.fido.fido2.api.IBooleanCallback;
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialCreationOptions;
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialRequestOptions;
import com.google.android.gms.fido.fido2.internal.regular.IFido2AppCallbacks;
import com.google.android.gms.fido.fido2.internal.regular.IFido2AppService;

import org.microg.gms.common.GmsClient;
import org.microg.gms.common.GmsService;
import org.microg.gms.common.api.ConnectionCallbacks;
import org.microg.gms.common.api.OnConnectionFailedListener;

public class Fido2GmsClient extends GmsClient<IFido2AppService> {
    public Fido2GmsClient(Context context, ConnectionCallbacks callbacks, OnConnectionFailedListener connectionFailedListener) {
        super(context, callbacks, connectionFailedListener, GmsService.FIDO2_REGULAR.ACTION);
        serviceId = GmsService.FIDO2_REGULAR.SERVICE_ID;
    }

    public void getRegisterPendingIntent(IFido2AppCallbacks callbacks, PublicKeyCredentialCreationOptions options) throws RemoteException {
        getServiceInterface().getRegisterPendingIntent(callbacks, options);
    }

    public void getSignPendingIntent(IFido2AppCallbacks callbacks, PublicKeyCredentialRequestOptions options) throws RemoteException {
        getServiceInterface().getSignPendingIntent(callbacks, options);
    }

    public void isUserVerifyingPlatformAuthenticatorAvailable(IBooleanCallback callback) throws RemoteException {
        getServiceInterface().isUserVerifyingPlatformAuthenticatorAvailable(callback);
    }

    @Override
    protected IFido2AppService interfaceFromBinder(IBinder binder) {
        return IFido2AppService.Stub.asInterface(binder);
    }
}
