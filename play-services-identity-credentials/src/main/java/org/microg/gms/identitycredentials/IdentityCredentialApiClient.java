/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.identitycredentials;

import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.ApiMetadata;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.api.internal.ConnectionCallbacks;
import com.google.android.gms.common.api.internal.OnConnectionFailedListener;
import com.google.android.gms.identitycredentials.ClearCreationOptionsRequest;
import com.google.android.gms.identitycredentials.ClearCredentialStateRequest;
import com.google.android.gms.identitycredentials.ClearExportRequest;
import com.google.android.gms.identitycredentials.ClearRegistryRequest;
import com.google.android.gms.identitycredentials.CreateCredentialRequest;
import com.google.android.gms.identitycredentials.GetCredentialRequest;
import com.google.android.gms.identitycredentials.ImportCredentialsRequest;
import com.google.android.gms.identitycredentials.RegisterCreationOptionsRequest;
import com.google.android.gms.identitycredentials.RegisterExportRequest;
import com.google.android.gms.identitycredentials.RegistrationRequest;
import com.google.android.gms.identitycredentials.SignalCredentialStateRequest;
import com.google.android.gms.identitycredentials.internal.IIdentityCredentialCallbacks;
import com.google.android.gms.identitycredentials.internal.IIdentityCredentialService;
import org.microg.gms.common.GmsClient;
import org.microg.gms.common.GmsService;

public class IdentityCredentialApiClient extends GmsClient<IIdentityCredentialService> {
    public static final Api<Api.ApiOptions.NoOptions> API = new Api<>(
            (options, context, looper, clientSettings, callbacks, connectionFailedListener) ->
                    new IdentityCredentialApiClient(context, callbacks, connectionFailedListener));

    public IdentityCredentialApiClient(Context context, ConnectionCallbacks callbacks, OnConnectionFailedListener connectionFailedListener) {
        super(context, callbacks, connectionFailedListener, GmsService.IDENTITY_CREDENTIALS.ACTION);
        serviceId = GmsService.IDENTITY_CREDENTIALS.SERVICE_ID;
    }

    @Override
    protected IIdentityCredentialService interfaceFromBinder(IBinder binder) {
        return IIdentityCredentialService.Stub.asInterface(binder);
    }

    public void getCredential(IIdentityCredentialCallbacks callbacks, GetCredentialRequest request) {
        try {
            getServiceInterface().getCredential(callbacks, request, ApiMetadata.SKIP);
        } catch (RemoteException e) {
            tryNotifyError(() -> callbacks.onGetCredential(Status.INTERNAL_ERROR, null, ApiMetadata.SKIP));
        }
    }

    public void registerCredentials(IIdentityCredentialCallbacks callbacks, RegistrationRequest request) {
        try {
            getServiceInterface().register(callbacks, request, ApiMetadata.SKIP);
        } catch (RemoteException e) {
            tryNotifyError(() -> callbacks.onRegister(Status.INTERNAL_ERROR, null, ApiMetadata.SKIP));
        }
    }

    public void clearRegistry(IIdentityCredentialCallbacks callbacks, ClearRegistryRequest request) {
        try {
            getServiceInterface().clearRegistry(callbacks, request, ApiMetadata.SKIP);
        } catch (RemoteException e) {
            tryNotifyError(() -> callbacks.onClearRegistry(Status.INTERNAL_ERROR, null, ApiMetadata.SKIP));
        }
    }

    public void importCredentials(IIdentityCredentialCallbacks callbacks, ImportCredentialsRequest request) {
        try {
            getServiceInterface().importCredentials(callbacks, request, ApiMetadata.SKIP);
        } catch (RemoteException e) {
            tryNotifyError(() -> callbacks.onImportCredentials(Status.INTERNAL_ERROR, null, ApiMetadata.SKIP));
        }
    }

    public void registerExport(IIdentityCredentialCallbacks callbacks, RegisterExportRequest request) {
        try {
            getServiceInterface().registerExport(callbacks, request, ApiMetadata.SKIP);
        } catch (RemoteException e) {
            tryNotifyError(() -> callbacks.onRegisterExport(Status.INTERNAL_ERROR, null, ApiMetadata.SKIP));
        }
    }

    public void createCredential(IIdentityCredentialCallbacks callbacks, CreateCredentialRequest request) {
        try {
            getServiceInterface().createCredential(callbacks, request, ApiMetadata.SKIP);
        } catch (RemoteException e) {
            tryNotifyError(() -> callbacks.onCreateCredential(Status.INTERNAL_ERROR, null, ApiMetadata.SKIP));
        }
    }

    public void registerCreationOptions(IIdentityCredentialCallbacks callbacks, RegisterCreationOptionsRequest request) {
        try {
            getServiceInterface().registerCreationOptions(callbacks, request, ApiMetadata.SKIP);
        } catch (RemoteException e) {
            tryNotifyError(() -> callbacks.onRegisterCreationOptions(Status.INTERNAL_ERROR, null, ApiMetadata.SKIP));
        }
    }

    public void clearCredentialState(IIdentityCredentialCallbacks callbacks, ClearCredentialStateRequest request) {
        try {
            getServiceInterface().clearCredentialState(callbacks, request, ApiMetadata.SKIP);
        } catch (RemoteException e) {
            tryNotifyError(() -> callbacks.onClearCredentialState(Status.INTERNAL_ERROR, null, ApiMetadata.SKIP));
        }
    }

    public void signalCredentialState(IIdentityCredentialCallbacks callbacks, SignalCredentialStateRequest request) {
        try {
            getServiceInterface().signalCredentialState(callbacks, request, ApiMetadata.SKIP);
        } catch (RemoteException e) {
            tryNotifyError(() -> callbacks.onSignalCredentialState(Status.INTERNAL_ERROR, null, ApiMetadata.SKIP));
        }
    }

    public void clearExport(IIdentityCredentialCallbacks callbacks, ClearExportRequest request) {
        try {
            getServiceInterface().clearExport(callbacks, request, ApiMetadata.SKIP);
        } catch (RemoteException e) {
            tryNotifyError(() -> callbacks.onClearExport(Status.INTERNAL_ERROR, null, ApiMetadata.SKIP));
        }
    }

    public void clearCreationOptions(IIdentityCredentialCallbacks callbacks, ClearCreationOptionsRequest request) {
        try {
            getServiceInterface().clearCreationOptions(callbacks, request, ApiMetadata.SKIP);
        } catch (RemoteException e) {
            tryNotifyError(() -> callbacks.onClearCreationOptions(Status.INTERNAL_ERROR, null, ApiMetadata.SKIP));
        }
    }

    private interface RemoteRunnable {
        void run() throws RemoteException;
    }

    private static void tryNotifyError(RemoteRunnable runnable) {
        try {
            runnable.run();
        } catch (RemoteException ignored) {
        }
    }
}
