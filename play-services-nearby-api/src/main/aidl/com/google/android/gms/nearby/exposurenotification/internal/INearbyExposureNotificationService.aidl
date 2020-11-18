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

interface INearbyExposureNotificationService{
    void start(in StartParams params) = 0;
    void stop(in StopParams params) = 1;
    void isEnabled(in IsEnabledParams params) = 2;
    void getTemporaryExposureKeyHistory(in GetTemporaryExposureKeyHistoryParams params) = 3;
    void provideDiagnosisKeys(in ProvideDiagnosisKeysParams params) = 4;

    void getExposureSummary(in GetExposureSummaryParams params) = 6;
    void getExposureInformation(in GetExposureInformationParams params) = 7;

    void getExposureWindows(in GetExposureWindowsParams params) = 12;
    void getVersion(in GetVersionParams params) = 13;
    void getCalibrationConfidence(in GetCalibrationConfidenceParams params) = 14;
    void getDailySummaries(in GetDailySummariesParams params) = 15;
    void setDiagnosisKeysDataMapping(in SetDiagnosisKeysDataMappingParams params) = 16;
    void getDiagnosisKeysDataMapping(in GetDiagnosisKeysDataMappingParams params) = 17;
    void getStatus(in GetStatusParams params) = 18;
    void getPackageConfiguration(in GetPackageConfigurationParams params) = 19;
}
