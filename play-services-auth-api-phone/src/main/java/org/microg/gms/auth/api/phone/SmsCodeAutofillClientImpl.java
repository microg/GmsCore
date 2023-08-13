/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth.api.phone;

import android.content.Context;
import com.google.android.gms.auth.api.phone.SmsCodeAutofillClient;
import com.google.android.gms.auth.api.phone.internal.IAutofillPermissionStateCallback;
import com.google.android.gms.auth.api.phone.internal.IOngoingSmsRequestCallback;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApi;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.Task;
import org.microg.gms.common.api.PendingGoogleApiCall;

public class SmsCodeAutofillClientImpl extends GoogleApi<Api.ApiOptions.NoOptions> implements SmsCodeAutofillClient {
    public SmsCodeAutofillClientImpl(Context context) {
        super(context, SmsRetrieverApiClient.API);
    }

    @Override
    public Task<@PermissionState Integer> checkPermissionState() {
        return scheduleTask((PendingGoogleApiCall<Integer, SmsRetrieverApiClient>) (client, completionSource) -> client.checkAutofillPermissionState(new IAutofillPermissionStateCallback.Stub() {
            @Override
            public void onCheckPermissionStateResult(Status status, int result) {
                if (status.isSuccess()) {
                    completionSource.trySetResult(result);
                } else {
                    completionSource.trySetException(new ApiException(status));
                }
            }
        }));
    }

    @Override
    public Task<Boolean> hasOngoingSmsRequest(String packageName) {
        return scheduleTask((PendingGoogleApiCall<Boolean, SmsRetrieverApiClient>) (client, completionSource) -> client.checkOngoingSmsRequest(packageName, new IOngoingSmsRequestCallback.Stub() {
            @Override
            public void onHasOngoingSmsRequestResult(Status status, boolean result) {
                if (status.isSuccess()) {
                    completionSource.trySetResult(result);
                } else {
                    completionSource.trySetException(new ApiException(status));
                }
            }
        }));
    }

    @Override
    public Task<Void> startSmsCodeRetriever() {
        return scheduleTask((PendingGoogleApiCall<Void, SmsRetrieverApiClient>) (client, completionSource) -> client.startSmsCodeAutofill(new StatusCallbackImpl(completionSource)));
    }
}
