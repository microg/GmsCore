/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth.api.phone;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.auth.api.phone.SmsRetrieverClient;
import com.google.android.gms.tasks.Task;
import org.microg.gms.common.api.PendingGoogleApiCall;

public class SmsRetrieverClientImpl extends SmsRetrieverClient {
    public SmsRetrieverClientImpl(Context context) {
        super(context);
    }

    @NonNull
    @Override
    public Task<Void> startSmsRetriever() {
        return scheduleTask((PendingGoogleApiCall<Void, SmsRetrieverApiClient>) (client, completionSource) -> client.startSmsRetriever(new SmsRetrieverResultCallbackImpl(completionSource)));
    }

    @NonNull
    @Override
    public Task<Void> startSmsUserConsent(@Nullable String senderAddress) {
        return scheduleTask((PendingGoogleApiCall<Void, SmsRetrieverApiClient>) (client, completionSource) -> client.startWithConsentPrompt(senderAddress, new SmsRetrieverResultCallbackImpl(completionSource)));
    }

}
