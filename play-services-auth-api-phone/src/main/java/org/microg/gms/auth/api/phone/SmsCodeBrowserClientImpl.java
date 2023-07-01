/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth.api.phone;

import android.content.Context;
import com.google.android.gms.auth.api.phone.SmsCodeBrowserClient;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApi;
import com.google.android.gms.tasks.Task;
import org.microg.gms.common.api.PendingGoogleApiCall;

public class SmsCodeBrowserClientImpl extends GoogleApi<Api.ApiOptions.NoOptions> implements SmsCodeBrowserClient {
    public SmsCodeBrowserClientImpl(Context context) {
        super(context, SmsRetrieverApiClient.API);
    }

    @Override
    public Task<Void> startSmsCodeRetriever() {
        return scheduleTask((PendingGoogleApiCall<Void, SmsRetrieverApiClient>) (client, completionSource) -> client.startSmsCodeBrowser(new StatusCallbackImpl(completionSource)));
    }
}
