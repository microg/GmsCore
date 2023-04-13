/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.recaptcha;

import android.content.Context;
import android.os.RemoteException;

import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApi;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.recaptcha.Recaptcha;
import com.google.android.gms.recaptcha.RecaptchaAction;
import com.google.android.gms.recaptcha.RecaptchaClient;
import com.google.android.gms.recaptcha.RecaptchaHandle;
import com.google.android.gms.recaptcha.RecaptchaResultData;
import com.google.android.gms.recaptcha.VerificationHandle;
import com.google.android.gms.recaptcha.VerificationResult;
import com.google.android.gms.recaptcha.internal.ExecuteParams;
import com.google.android.gms.recaptcha.internal.ExecuteResults;
import com.google.android.gms.recaptcha.internal.ICloseCallback;
import com.google.android.gms.recaptcha.internal.IExecuteCallback;
import com.google.android.gms.recaptcha.internal.IInitCallback;
import com.google.android.gms.recaptcha.internal.InitParams;
import com.google.android.gms.recaptcha.internal.InitResults;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import org.microg.gms.common.api.PendingGoogleApiCall;
import org.microg.gms.tasks.TaskImpl;

public class RecaptchaClientImpl extends GoogleApi<Api.ApiOptions.NoOptions> implements RecaptchaClient {
    public RecaptchaClientImpl(Context context) {
        super(context, new Api<>((options, c, looper, clientSettings, callbacks, connectionFailedListener) -> new RecaptchaGmsClient(c, callbacks, connectionFailedListener)));
    }

    @Override
    public Task<VerificationHandle> challengeAccount(RecaptchaHandle recaptchaHandle, String challengeRequestToken) {
        return Tasks.forException(new UnsupportedOperationException());
    }

    @Override
    public Task<Boolean> close(RecaptchaHandle handle) {
        return scheduleTask((PendingGoogleApiCall<Boolean, RecaptchaGmsClient>) (client, completionSource) -> {
            client.close(new ICloseCallback.Stub() {
                @Override
                public void onClosed(Status status, boolean closed) throws RemoteException {
                    if (status.isSuccess()) {
                        completionSource.trySetResult(closed);
                    } else {
                        completionSource.trySetException(new RuntimeException(status.getStatusMessage()));
                    }
                }
            }, handle);
        });
    }

    @Override
    public Task<RecaptchaResultData> execute(RecaptchaHandle handle, RecaptchaAction action) {
        return scheduleTask((PendingGoogleApiCall<RecaptchaResultData, RecaptchaGmsClient>) (client, completionSource) -> {
            ExecuteParams params = new ExecuteParams();
            params.handle = handle;
            params.action = action;
            params.version = "17.0.1";
            client.execute(new IExecuteCallback.Stub() {
                @Override
                public void onData(Status status, RecaptchaResultData data) throws RemoteException {
                    if (status.isSuccess()) {
                        completionSource.trySetResult(data);
                    } else {
                        completionSource.trySetException(new RuntimeException(status.getStatusMessage()));
                    }
                }

                @Override
                public void onResults(Status status, ExecuteResults results) throws RemoteException {
                    if (status.isSuccess()) {
                        completionSource.trySetResult(results.data);
                    } else {
                        completionSource.trySetException(new RuntimeException(status.getStatusMessage()));
                    }
                }
            }, params);
        });
    }

    @Override
    public Task<RecaptchaHandle> init(String siteKey) {
        return scheduleTask((PendingGoogleApiCall<RecaptchaHandle, RecaptchaGmsClient>) (client, completionSource) -> {
            InitParams params = new InitParams();
            params.siteKey = siteKey;
            params.version = "17.0.1";
            client.init(new IInitCallback.Stub() {
                @Override
                public void onHandle(Status status, RecaptchaHandle handle) throws RemoteException {
                    if (status.isSuccess()) {
                        completionSource.trySetResult(handle);
                    } else {
                        completionSource.trySetException(new RuntimeException(status.getStatusMessage()));
                    }
                }

                @Override
                public void onResults(Status status, InitResults results) throws RemoteException {
                    if (status.isSuccess()) {
                        completionSource.trySetResult(results.handle);
                    } else {
                        completionSource.trySetException(new RuntimeException(status.getStatusMessage()));
                    }
                }
            }, params);
        });
    }

    @Override
    public Task<VerificationResult> verifyAccount(String pin, VerificationHandle verificationHandle) {
        return Tasks.forException(new UnsupportedOperationException());
    }
}
