/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby;

import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;

import com.google.android.gms.nearby.exposurenotification.internal.GetCalibrationConfidenceParams;
import com.google.android.gms.nearby.exposurenotification.internal.GetDailySummariesParams;
import com.google.android.gms.nearby.exposurenotification.internal.GetDiagnosisKeysDataMappingParams;
import com.google.android.gms.nearby.exposurenotification.internal.GetExposureInformationParams;
import com.google.android.gms.nearby.exposurenotification.internal.GetExposureSummaryParams;
import com.google.android.gms.nearby.exposurenotification.internal.GetExposureWindowsParams;
import com.google.android.gms.nearby.exposurenotification.internal.GetPackageConfigurationParams;
import com.google.android.gms.nearby.exposurenotification.internal.GetStatusParams;
import com.google.android.gms.nearby.exposurenotification.internal.GetTemporaryExposureKeyHistoryParams;
import com.google.android.gms.nearby.exposurenotification.internal.GetVersionParams;
import com.google.android.gms.nearby.exposurenotification.internal.INearbyExposureNotificationService;
import com.google.android.gms.nearby.exposurenotification.internal.IsEnabledParams;
import com.google.android.gms.nearby.exposurenotification.internal.ProvideDiagnosisKeysParams;
import com.google.android.gms.nearby.exposurenotification.internal.RequestPreAuthorizedTemporaryExposureKeyHistoryParams;
import com.google.android.gms.nearby.exposurenotification.internal.RequestPreAuthorizedTemporaryExposureKeyReleaseParams;
import com.google.android.gms.nearby.exposurenotification.internal.SetDiagnosisKeysDataMappingParams;
import com.google.android.gms.nearby.exposurenotification.internal.StartParams;
import com.google.android.gms.nearby.exposurenotification.internal.StopParams;

import org.microg.gms.common.GmsClient;
import org.microg.gms.common.GmsService;
import org.microg.gms.common.api.ConnectionCallbacks;
import org.microg.gms.common.api.OnConnectionFailedListener;

public class ExposureNotificationApiClient extends GmsClient<INearbyExposureNotificationService> {
    public ExposureNotificationApiClient(Context context, ConnectionCallbacks callbacks, OnConnectionFailedListener connectionFailedListener) {
        super(context, callbacks, connectionFailedListener, GmsService.NEARBY_EXPOSURE.ACTION, true);
        serviceId = GmsService.NEARBY_EXPOSURE.SERVICE_ID;
    }

    @Override
    protected INearbyExposureNotificationService interfaceFromBinder(IBinder binder) {
        return INearbyExposureNotificationService.Stub.asInterface(binder);
    }

    public void getVersion(GetVersionParams params) throws RemoteException {
        getServiceInterface().getVersion(params);
    }

    public void getCalibrationConfidence(GetCalibrationConfidenceParams params) throws RemoteException {
        getServiceInterface().getCalibrationConfidence(params);
    }

    public void start(StartParams params) throws RemoteException {
        getServiceInterface().start(params);
    }

    public void stop(StopParams params) throws RemoteException {
        getServiceInterface().stop(params);
    }

    public void isEnabled(IsEnabledParams params) throws RemoteException {
        getServiceInterface().isEnabled(params);
    }

    public void getTemporaryExposureKeyHistory(GetTemporaryExposureKeyHistoryParams params) throws RemoteException {
        getServiceInterface().getTemporaryExposureKeyHistory(params);
    }

    public void provideDiagnosisKeys(ProvideDiagnosisKeysParams params) throws RemoteException {
        getServiceInterface().provideDiagnosisKeys(params);
    }

    public void getExposureSummary(GetExposureSummaryParams params) throws RemoteException {
        getServiceInterface().getExposureSummary(params);
    }

    public void getExposureInformation(GetExposureInformationParams params) throws RemoteException {
        getServiceInterface().getExposureInformation(params);
    }

    public void getExposureWindows(GetExposureWindowsParams params) throws RemoteException {
        getServiceInterface().getExposureWindows(params);
    }

    public void getDailySummaries(GetDailySummariesParams params) throws RemoteException {
        getServiceInterface().getDailySummaries(params);
    }

    public void setDiagnosisKeysDataMapping(SetDiagnosisKeysDataMappingParams params) throws RemoteException {
        getServiceInterface().setDiagnosisKeysDataMapping(params);
    }

    public void getDiagnosisKeysDataMapping(GetDiagnosisKeysDataMappingParams params) throws RemoteException {
        getServiceInterface().getDiagnosisKeysDataMapping(params);
    }

    public void getPackageConfiguration(GetPackageConfigurationParams params) throws RemoteException {
        getServiceInterface().getPackageConfiguration(params);
    }

    public void getStatus(GetStatusParams params) throws RemoteException {
        getServiceInterface().getStatus(params);
    }

    public void requestPreAuthorizedTemporaryExposureKeyHistory(RequestPreAuthorizedTemporaryExposureKeyHistoryParams params) throws RemoteException {
        getServiceInterface().requestPreAuthorizedTemporaryExposureKeyHistory(params);
    }

    public void requestPreAuthorizedTemporaryExposureKeyRelease(RequestPreAuthorizedTemporaryExposureKeyReleaseParams params) throws RemoteException {
        getServiceInterface().requestPreAuthorizedTemporaryExposureKeyRelease(params);
    }
}
