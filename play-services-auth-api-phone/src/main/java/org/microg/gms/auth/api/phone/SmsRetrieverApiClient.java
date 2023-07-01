/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth.api.phone;

import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;
import androidx.annotation.Nullable;
import com.google.android.gms.auth.api.phone.internal.IAutofillPermissionStateCallback;
import com.google.android.gms.auth.api.phone.internal.IOngoingSmsRequestCallback;
import com.google.android.gms.auth.api.phone.internal.ISmsRetrieverApiService;
import com.google.android.gms.auth.api.phone.internal.ISmsRetrieverResultCallback;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.api.internal.IStatusCallback;
import org.microg.gms.common.GmsClient;
import org.microg.gms.common.GmsService;
import org.microg.gms.common.api.ConnectionCallbacks;
import org.microg.gms.common.api.OnConnectionFailedListener;

public class SmsRetrieverApiClient extends GmsClient<ISmsRetrieverApiService> {
    public static final Api<Api.ApiOptions.NoOptions> API = new Api<>((options, context, looper, clientSettings, callbacks, connectionFailedListener) -> new SmsRetrieverApiClient(context, callbacks, connectionFailedListener));

    public SmsRetrieverApiClient(Context context, ConnectionCallbacks callbacks, OnConnectionFailedListener connectionFailedListener) {
        super(context, callbacks, connectionFailedListener, GmsService.SMS_RETRIEVER.ACTION);
        serviceId = GmsService.SMS_RETRIEVER.SERVICE_ID;
    }

    @Override
    protected ISmsRetrieverApiService interfaceFromBinder(IBinder binder) {
        return ISmsRetrieverApiService.Stub.asInterface(binder);
    }

    public void startSmsRetriever(ISmsRetrieverResultCallback callback) {
        try {
            getServiceInterface().startSmsRetriever(callback);
        } catch (RemoteException e) {
            try {
                callback.onResult(Status.INTERNAL_ERROR);
            } catch (RemoteException ignored) {
            }
        }
    }

    public void startWithConsentPrompt(@Nullable String senderAddress, ISmsRetrieverResultCallback callback) {
        try {
            getServiceInterface().startWithConsentPrompt(senderAddress, callback);
        } catch (RemoteException e) {
            try {
                callback.onResult(Status.INTERNAL_ERROR);
            } catch (RemoteException ignored) {
            }
        }
    }

    public void startSmsCodeAutofill(IStatusCallback callback) {
        try {
            getServiceInterface().startSmsCodeAutofill(callback);
        } catch (RemoteException e) {
            try {
                callback.onResult(Status.INTERNAL_ERROR);
            } catch (RemoteException ignored) {
            }
        }
    }

    public void checkAutofillPermissionState(IAutofillPermissionStateCallback callback) {
        try {
            getServiceInterface().checkAutofillPermissionState(callback);
        } catch (RemoteException e) {
            try {
                callback.onCheckPermissionStateResult(Status.INTERNAL_ERROR, -1);
            } catch (RemoteException ignored) {
            }
        }
    }

    public void checkOngoingSmsRequest(String packageName, IOngoingSmsRequestCallback callback) {
        try {
            getServiceInterface().checkOngoingSmsRequest(packageName, callback);
        } catch (RemoteException e) {
            try {
                callback.onHasOngoingSmsRequestResult(Status.INTERNAL_ERROR, false);
            } catch (RemoteException ignored) {
            }
        }
    }

    public void startSmsCodeBrowser(IStatusCallback callback) {
        try {
            getServiceInterface().startSmsCodeBrowser(callback);
        } catch (RemoteException e) {
            try {
                callback.onResult(Status.INTERNAL_ERROR);
            } catch (RemoteException ignored) {
            }
        }
    }
}
