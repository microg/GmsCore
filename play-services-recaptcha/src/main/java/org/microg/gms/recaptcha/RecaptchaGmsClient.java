/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.recaptcha;

import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;

import org.microg.gms.common.GmsClient;
import org.microg.gms.common.GmsService;
import org.microg.gms.common.api.ConnectionCallbacks;
import org.microg.gms.common.api.OnConnectionFailedListener;

import com.google.android.gms.recaptcha.RecaptchaAction;
import com.google.android.gms.recaptcha.RecaptchaHandle;
import com.google.android.gms.recaptcha.internal.ExecuteParams;
import com.google.android.gms.recaptcha.internal.ICloseCallback;
import com.google.android.gms.recaptcha.internal.IExecuteCallback;
import com.google.android.gms.recaptcha.internal.IInitCallback;
import com.google.android.gms.recaptcha.internal.IRecaptchaService;
import com.google.android.gms.recaptcha.internal.InitParams;

public class RecaptchaGmsClient extends GmsClient<IRecaptchaService> {
    public RecaptchaGmsClient(Context context, ConnectionCallbacks callbacks, OnConnectionFailedListener connectionFailedListener) {
        super(context, callbacks, connectionFailedListener, GmsService.RECAPTCHA.ACTION);
        serviceId = GmsService.RECAPTCHA.SERVICE_ID;
    }

    public void init(IInitCallback callback, String siteKey) throws RemoteException {
        getServiceInterface().init(callback, siteKey);
    }

    public void init(IInitCallback callback, InitParams params) throws RemoteException {
        getServiceInterface().init2(callback, params);
    }

    public void execute(IExecuteCallback callback, RecaptchaHandle handle, RecaptchaAction action) throws RemoteException {
        getServiceInterface().execute(callback, handle, action);
    }

    public void execute(IExecuteCallback callback, ExecuteParams params) throws RemoteException {
        getServiceInterface().execute2(callback, params);
    }

    public void close(ICloseCallback callback, RecaptchaHandle handle) throws RemoteException {
        getServiceInterface().close(callback, handle);
    }

    @Override
    protected IRecaptchaService interfaceFromBinder(IBinder binder) {
        return IRecaptchaService.Stub.asInterface(binder);
    }
}
