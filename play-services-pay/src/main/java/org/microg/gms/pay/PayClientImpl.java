/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.pay;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.os.RemoteException;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApi;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.pay.*;
import com.google.android.gms.pay.internal.IPayServiceCallbacks;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.lang.ref.WeakReference;

public class PayClientImpl extends GoogleApi<Api.ApiOptions.NotRequiredOptions> implements PayClient {
    private static final Api<Api.ApiOptions.NotRequiredOptions> API = new Api<>((options, context, looper, clientSettings, callbacks, connectionFailedListener) -> new ThirdPartyPayApiClient(context, callbacks, connectionFailedListener));

    public PayClientImpl(Context context) {
        super(context, API);
    }

    @Override
    public Task<EmoneyReadiness> checkReadinessForEmoney(String serviceProvider, String accountName) {
        return null;
    }

    @Override
    public Task<@PayApiAvailabilityStatus Integer> getPayApiAvailabilityStatus(@RequestType int requestType) {
        return this.<Integer, ThirdPartyPayApiClient>scheduleTask((client, completionSource) -> {
            client.getServiceInterface().getPayApiAvailabilityStatus(new GetPayApiAvailabilityStatusRequest(requestType), new PayServiceCallbacks() {
                @Override
                public void onPayApiAvailabilityStatus(Status status, int availabilityStatus) {
                    if (status.isSuccess() && availabilityStatus == 3) {
                        // Invalid availabilityStatus
                        completionSource.trySetException(new ApiException(Status.INTERNAL_ERROR));
                    } else if (availabilityStatus == 1) {
                        if (status.isSuccess()) {
                            completionSource.trySetResult(PayApiAvailabilityStatus.NOT_ELIGIBLE);
                        } else {
                            completionSource.trySetException(new ApiException(status));
                        }
                    } else {
                        if (status.isSuccess()) {
                            completionSource.trySetResult(availabilityStatus);
                        } else {
                            completionSource.trySetException(new ApiException(status));
                        }
                    }
                }
            });
        });
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
    public Task<Void> notifyCardTapEvent(String eventJson) {
        return null;
    }

    @Override
    public Task<Void> notifyEmoneyCardStatusUpdate(String json) {
        return null;
    }

    @Override
    public Task<Void> pushEmoneyCard(String json, ActivityResultLauncher<IntentSenderRequest> activityResultLauncher) {
        return null;
    }

    @Override
    public void savePasses(String json, Activity activity, int requestCode) {
        savePasses(new SavePassesRequest(json, null), activity, requestCode);
    }

    @Override
    public void savePassesJwt(String jwt, Activity activity, int requestCode) {
        savePasses(new SavePassesRequest(null, jwt), activity, requestCode);
    }

    private void savePasses(SavePassesRequest request, Activity activity, int requestCode) {
        // We don't want to keep a reference to the activity to not leak it
        WeakReference<Activity> weakActivity = new WeakReference<>(activity);
        PayServiceCallbacks callbacks = new PayServiceCallbacks() {
            @Override
            public void onPendingIntent(Status status) {
                Activity activity = weakActivity.get();
                if (activity != null) {
                    if (status.hasResolution()) {
                        try {
                            status.startResolutionForResult(activity, requestCode);
                        } catch (IntentSender.SendIntentException e) {
                            // Ignored
                        }
                    } else {
                        PendingIntent resultIntent = activity.createPendingResult(requestCode, new Intent(), PendingIntent.FLAG_ONE_SHOT);
                        if (resultIntent != null) {
                            try {
                                resultIntent.send(status.isSuccess() ? -1 : status.getStatusCode());
                            } catch (PendingIntent.CanceledException e) {
                                // Ignored
                            }
                        }
                    }
                }
            }
        };
        this.<Void, ThirdPartyPayApiClient>scheduleTask((client, completionSource) -> client.getServiceInterface().savePasses(request, callbacks))
                .addOnFailureListener((exception) -> callbacks.onPendingIntent(Status.INTERNAL_ERROR));
    }
}
