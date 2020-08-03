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

interface INearbyExposureNotificationService{
    void start(in StartParams params) = 0;
    void stop(in StopParams params) = 1;
    void isEnabled(in IsEnabledParams params) = 2;
    void getTemporaryExposureKeyHistory(in GetTemporaryExposureKeyHistoryParams params) = 3;
    void provideDiagnosisKeys(in ProvideDiagnosisKeysParams params) = 4;

    void getExposureSummary(in GetExposureSummaryParams params) = 6;
    void getExposureInformation(in GetExposureInformationParams params) = 7;
}
