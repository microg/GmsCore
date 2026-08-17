/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.safetynet;

import android.os.RemoteException;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.safetynet.RecaptchaResultData;
import com.google.android.gms.safetynet.SafetyNet;
import com.google.android.gms.safetynet.SafetyNetApi;
import com.google.android.gms.safetynet.internal.ISafetyNetCallbacks;

import org.microg.gms.common.GmsConnector;

public class SafetyNetApiImpl implements SafetyNetApi {
    @Override
    public PendingResult<RecaptchaTokenResult> verifyWithRecaptcha(GoogleApiClient apiClient, String siteKey) {
        return GmsConnector.call(apiClient, SafetyNet.API, (GmsConnector.Callback<SafetyNetGmsClient, RecaptchaTokenResult>) (client, resultProvider) -> client.verifyWithRecaptcha(new ISafetyNetCallbacksDefaultStub() {
            @Override
            public void onRecaptchaResult(Status status, RecaptchaResultData recaptchaResultData) throws RemoteException {
                resultProvider.onResultAvailable(new RecaptchaTokenResult() {
                    @Override
                    public String getTokenResult() {
                        return recaptchaResultData.token;
                    }

                    @Override
                    public Status getStatus() {
                        return status;
                    }
                });
            }
        }, siteKey));
    }
}
