/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.oss.licenses;

import android.content.Context;
import android.os.RemoteException;

import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApi;
import com.google.android.gms.tasks.Task;

import org.microg.gms.common.api.PendingGoogleApiCall;
import org.microg.gms.oss.licenses.OssLicenseServiceApiClient;

import java.util.List;

public class OssLicensesServiceImpl extends GoogleApi<Api.ApiOptions.NoOptions> {
    private static final Api<Api.ApiOptions.NoOptions> API = new Api<>((options, context, looper, clientSettings, callbacks, connectionFailedListener) -> new OssLicenseServiceApiClient(context, callbacks, connectionFailedListener));

    public OssLicensesServiceImpl(Context context) {
        super(context, API);
    }

    public Task<String> getLicenseLayoutPackage(String packageName) {
        return scheduleTask((PendingGoogleApiCall<String, OssLicenseServiceApiClient>) (client, completionSource) -> {
            String result;
            try {
                result = client.getLicenseLayoutPackage(packageName);
            } catch (RemoteException e) {
                completionSource.setException(e);
                return;
            }
            completionSource.setResult(result);
        });
    }

    public Task<String> getListLayoutPackage(String packageName) {
        return scheduleTask((PendingGoogleApiCall<String, OssLicenseServiceApiClient>) (client, completionSource) -> {
            String result;
            try {
                result = client.getListLayoutPackage(packageName);
            } catch (RemoteException e) {
                completionSource.setException(e);
                return;
            }
            completionSource.setResult(result);
        });
    }

    public Task<String> getLicenseDetail(License license) {
        return scheduleTask((PendingGoogleApiCall<String, OssLicenseServiceApiClient>) (client, completionSource) -> {
            String result;
            try {
                result = client.getLicenseDetail(license);
            } catch (RemoteException e) {
                completionSource.setException(e);
                return;
            }
            completionSource.setResult(result);
        });
    }

    public Task<List<License>> getLicenseList(List<License> licenses) {
        return scheduleTask((PendingGoogleApiCall<List<License>, OssLicenseServiceApiClient>) (client, completionSource) -> {
            List<License> result;
            try {
                result = client.getLicenseList(licenses);
            } catch (RemoteException e) {
                completionSource.setException(e);
                return;
            }
            completionSource.setResult(result);
        });
    }
}
