/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby;

import android.content.Context;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApi;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.api.internal.ApiKey;
import com.google.android.gms.common.api.internal.IStatusCallback;
import com.google.android.gms.nearby.exposurenotification.ExposureConfiguration;
import com.google.android.gms.nearby.exposurenotification.ExposureInformation;
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationClient;
import com.google.android.gms.nearby.exposurenotification.ExposureSummary;
import com.google.android.gms.nearby.exposurenotification.TemporaryExposureKey;
import com.google.android.gms.nearby.exposurenotification.internal.GetExposureInformationParams;
import com.google.android.gms.nearby.exposurenotification.internal.GetExposureSummaryParams;
import com.google.android.gms.nearby.exposurenotification.internal.GetTemporaryExposureKeyHistoryParams;
import com.google.android.gms.nearby.exposurenotification.internal.IBooleanCallback;
import com.google.android.gms.nearby.exposurenotification.internal.IExposureInformationListCallback;
import com.google.android.gms.nearby.exposurenotification.internal.IExposureSummaryCallback;
import com.google.android.gms.nearby.exposurenotification.internal.ITemporaryExposureKeyListCallback;
import com.google.android.gms.nearby.exposurenotification.internal.IsEnabledParams;
import com.google.android.gms.nearby.exposurenotification.internal.ProvideDiagnosisKeysParams;
import com.google.android.gms.nearby.exposurenotification.internal.StartParams;
import com.google.android.gms.nearby.exposurenotification.internal.StopParams;
import com.google.android.gms.tasks.Task;

import org.microg.gms.common.api.PendingGoogleApiCall;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ExposureNotificationClientImpl extends GoogleApi<Api.ApiOptions.NoOptions> implements ExposureNotificationClient {
    private static final Api<Api.ApiOptions.NoOptions> API = new Api<>((options, context, looper, clientSettings, callbacks, connectionFailedListener) -> new ExposureNotificationApiClient(context, callbacks, connectionFailedListener));

    public ExposureNotificationClientImpl(Context context) {
        super(context, API);
    }

    private static final String TAG = "ENClientImpl";

    @Override
    public Task<Void> start() {
        return scheduleTask((PendingGoogleApiCall<Void, ExposureNotificationApiClient>) (client, completionSource) -> {
            StartParams params = new StartParams(new IStatusCallback.Stub() {
                @Override
                public void onResult(Status status) {
                    if (status == Status.SUCCESS) {
                        completionSource.setResult(null);
                    } else {
                        completionSource.setException(new ApiException(status));
                    }
                }
            });
            try {
                client.start(params);
            } catch (Exception e) {
                completionSource.setException(e);
            }
        });
    }

    @Override
    public Task<Void> stop() {
        return scheduleTask((PendingGoogleApiCall<Void, ExposureNotificationApiClient>) (client, completionSource) -> {
            StopParams params = new StopParams(new IStatusCallback.Stub() {
                @Override
                public void onResult(Status status) {
                    if (status == Status.SUCCESS) {
                        completionSource.setResult(null);
                    } else {
                        completionSource.setException(new ApiException(status));
                    }
                }
            });
            try {
                client.stop(params);
            } catch (Exception e) {
                completionSource.setException(e);
            }
        });
    }

    @Override
    public Task<Boolean> isEnabled() {
        return scheduleTask((PendingGoogleApiCall<Boolean, ExposureNotificationApiClient>) (client, completionSource) -> {
            IsEnabledParams params = new IsEnabledParams(new IBooleanCallback.Stub() {
                @Override
                public void onResult(Status status, boolean result) {
                    if (status == Status.SUCCESS) {
                        completionSource.setResult(result);
                    } else {
                        completionSource.setException(new ApiException(status));
                    }
                }
            });
            try {
                client.isEnabled(params);
            } catch (Exception e) {
                completionSource.setException(e);
            }
        });
    }

    @Override
    public Task<List<TemporaryExposureKey>> getTemporaryExposureKeyHistory() {
        return scheduleTask((PendingGoogleApiCall<List<TemporaryExposureKey>, ExposureNotificationApiClient>) (client, completionSource) -> {
            GetTemporaryExposureKeyHistoryParams params = new GetTemporaryExposureKeyHistoryParams(new ITemporaryExposureKeyListCallback.Stub() {
                @Override
                public void onResult(Status status, List<TemporaryExposureKey> result) {
                    if (status == Status.SUCCESS) {
                        completionSource.setResult(result);
                    } else {
                        completionSource.setException(new ApiException(status));
                    }
                }
            });
            try {
                client.getTemporaryExposureKeyHistory(params);
            } catch (Exception e) {
                completionSource.setException(e);
            }
        });
    }

    @Override
    public Task<Void> provideDiagnosisKeys(List<File> keyFiles, ExposureConfiguration configuration, String token) {
        return scheduleTask((PendingGoogleApiCall<Void, ExposureNotificationApiClient>) (client, completionSource) -> {
            List<ParcelFileDescriptor> fds = new ArrayList<>(keyFiles.size());
            for (File kf: keyFiles) {
                ParcelFileDescriptor fd;
                try {
                    fd = ParcelFileDescriptor.open(kf, ParcelFileDescriptor.MODE_READ_ONLY);
                } catch (FileNotFoundException e) {
                    for (ParcelFileDescriptor ofd : fds) {
                        try {
                            ofd.close();
                        } catch (IOException e2) {
                            Log.w(TAG, "Failed to close file", e2);
                        }
                    }
                    completionSource.setException(e);
                    return;
                }
                fds.add(fd);
            }

            ProvideDiagnosisKeysParams params = new ProvideDiagnosisKeysParams(new IStatusCallback.Stub() {
                @Override
                public void onResult(Status status) {
                    if (status == Status.SUCCESS) {
                        completionSource.setResult(null);
                    } else {
                        completionSource.setException(new ApiException(status));
                    }
                }
            }, fds, configuration, token);
            try {
                client.provideDiagnosisKeys(params);
            } catch (Exception e) {
                completionSource.setException(e);
            }
        });
    }

    @Override
    public Task<ExposureSummary> getExposureSummary(String token) {
        return scheduleTask((PendingGoogleApiCall<ExposureSummary, ExposureNotificationApiClient>) (client, completionSource) -> {
            GetExposureSummaryParams params = new GetExposureSummaryParams(new IExposureSummaryCallback.Stub() {
                @Override
                public void onResult(Status status, ExposureSummary result) {
                    if (status == Status.SUCCESS) {
                        completionSource.setResult(result);
                    } else {
                        completionSource.setException(new ApiException(status));
                    }
                }
            }, token);
            try {
                client.getExposureSummary(params);
            } catch (Exception e) {
                completionSource.setException(e);
            }
        });
    }

    @Override
    public Task<List<ExposureInformation>> getExposureInformation(String token) {
        return scheduleTask((PendingGoogleApiCall<List<ExposureInformation>, ExposureNotificationApiClient>) (client, completionSource) -> {
            GetExposureInformationParams params = new GetExposureInformationParams(new IExposureInformationListCallback.Stub() {
                @Override
                public void onResult(Status status, List<ExposureInformation> result) {
                    if (status == Status.SUCCESS) {
                        completionSource.setResult(result);
                    } else {
                        completionSource.setException(new ApiException(status));
                    }
                }
            }, token);
            try {
                client.getExposureInformation(params);
            } catch (Exception e) {
                completionSource.setException(e);
            }
        });
    }

    @Override
    public ApiKey<Api.ApiOptions.NoOptions> getApiKey() {
        return null;
    }
}
