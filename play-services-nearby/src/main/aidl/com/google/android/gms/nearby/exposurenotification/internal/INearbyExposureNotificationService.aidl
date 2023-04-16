/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.nearby.exposurenotification.internal;

import com.google.android.gms.nearby.exposurenotification.internal.StartParams;
import com.google.android.gms.nearby.exposurenotification.internal.StopParams;
import com.google.android.gms.nearby.exposurenotification.internal.IsEnabledParams;
import com.google.android.gms.nearby.exposurenotification.internal.GetTemporaryExposureKeyHistoryParams;
import com.google.android.gms.nearby.exposurenotification.internal.ProvideDiagnosisKeysParams;
import com.google.android.gms.nearby.exposurenotification.internal.GetExposureSummaryParams;
import com.google.android.gms.nearby.exposurenotification.internal.GetExposureInformationParams;
import com.google.android.gms.nearby.exposurenotification.internal.GetExposureWindowsParams;
import com.google.android.gms.nearby.exposurenotification.internal.GetVersionParams;
import com.google.android.gms.nearby.exposurenotification.internal.GetCalibrationConfidenceParams;
import com.google.android.gms.nearby.exposurenotification.internal.GetDailySummariesParams;
import com.google.android.gms.nearby.exposurenotification.internal.SetDiagnosisKeysDataMappingParams;
import com.google.android.gms.nearby.exposurenotification.internal.GetDiagnosisKeysDataMappingParams;
import com.google.android.gms.nearby.exposurenotification.internal.GetStatusParams;
import com.google.android.gms.nearby.exposurenotification.internal.GetPackageConfigurationParams;
import com.google.android.gms.nearby.exposurenotification.internal.RequestPreAuthorizedTemporaryExposureKeyHistoryParams;
import com.google.android.gms.nearby.exposurenotification.internal.RequestPreAuthorizedTemporaryExposureKeyReleaseParams;

interface INearbyExposureNotificationService{
    oneway void start(in StartParams params) = 0;
    oneway void stop(in StopParams params) = 1;
    oneway void isEnabled(in IsEnabledParams params) = 2;
    oneway void getTemporaryExposureKeyHistory(in GetTemporaryExposureKeyHistoryParams params) = 3;
    oneway void provideDiagnosisKeys(in ProvideDiagnosisKeysParams params) = 4;
    //oneway void getMaxDiagnosisKeyCount(in GetMaxDiagnosisKeyCountParams params) = 5;
    oneway void getExposureSummary(in GetExposureSummaryParams params) = 6;
    oneway void getExposureInformation(in GetExposureInformationParams params) = 7;
    //oneway void resetAllData(in ResetAllDataParams params) = 8;
    //oneway void resetTemporaryExposureKeys(in ResetTemporaryExposureKeysParams params) = 9;
    //oneway void startForPackage(in StartForPackageParams params) = 10;
    //oneway void isEnabledForPackage(in IsEnabledForPackageParams params) = 11;
    oneway void getExposureWindows(in GetExposureWindowsParams params) = 12;
    oneway void getVersion(in GetVersionParams params) = 13;
    oneway void getCalibrationConfidence(in GetCalibrationConfidenceParams params) = 14;
    oneway void getDailySummaries(in GetDailySummariesParams params) = 15;
    oneway void setDiagnosisKeysDataMapping(in SetDiagnosisKeysDataMappingParams params) = 16;
    oneway void getDiagnosisKeysDataMapping(in GetDiagnosisKeysDataMappingParams params) = 17;
    oneway void getStatus(in GetStatusParams params) = 18;
    oneway void getPackageConfiguration(in GetPackageConfigurationParams params) = 19;
    oneway void requestPreAuthorizedTemporaryExposureKeyHistory(in RequestPreAuthorizedTemporaryExposureKeyHistoryParams params) = 20;
    oneway void requestPreAuthorizedTemporaryExposureKeyRelease(in RequestPreAuthorizedTemporaryExposureKeyReleaseParams params) = 21;
}
