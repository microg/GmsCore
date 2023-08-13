/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.pay;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApi;
import com.google.android.gms.pay.PayApiAvailabilityStatus;
import com.google.android.gms.pay.PayClient;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

public class PayClientImpl extends GoogleApi<Api.ApiOptions.NotRequiredOptions> implements PayClient {
    private static final Api<Api.ApiOptions.NotRequiredOptions> API = new Api<>((options, context, looper, clientSettings, callbacks, connectionFailedListener) -> new PayApiClient(context, callbacks, connectionFailedListener));

    public PayClientImpl(Context context) {
        super(context, API);
    }

    @Override
    public Task<@PayApiAvailabilityStatus Integer> getPayApiAvailabilityStatus(@RequestType int requestType) {
        return Tasks.forResult(PayApiAvailabilityStatus.NOT_ELIGIBLE);
    }

    @Override
    public Task<PendingIntent> getPendingIntentForWalletOnWear(String wearNodeId, @WearWalletIntentSource int intentSource) {
        return null;
    }

    @Override
    public ProductName getProductName() {
        return ProductName.GOOGLE_WALLET;
    }

    @Override
    public void savePasses(String json, Activity activity, int requestCode) {

    }

    @Override
    public void savePassesJwt(String jwt, Activity activity, int requestCode) {

    }
}
