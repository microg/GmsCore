/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0 AND CC-BY-4.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.nearby.exposurenotification;

import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.HasApiKey;
import com.google.android.gms.tasks.Task;

import org.microg.gms.common.PublicApi;
import org.microg.gms.nearby.exposurenotification.Constants;

import java.io.File;
import java.util.List;

@PublicApi
public interface ExposureNotificationClient extends HasApiKey<Api.ApiOptions.NoOptions> {
    String ACTION_EXPOSURE_NOTIFICATION_SETTINGS = Constants.ACTION_EXPOSURE_NOTIFICATION_SETTINGS;
    String ACTION_EXPOSURE_NOT_FOUND = Constants.ACTION_EXPOSURE_NOT_FOUND;
    String ACTION_EXPOSURE_STATE_UPDATED = Constants.ACTION_EXPOSURE_STATE_UPDATED;
    String EXTRA_EXPOSURE_SUMMARY = Constants.EXTRA_EXPOSURE_SUMMARY;
    String EXTRA_TOKEN = Constants.EXTRA_TOKEN;

    Task<Void> start();

    Task<Void> stop();

    Task<Boolean> isEnabled();

    Task<List<TemporaryExposureKey>> getTemporaryExposureKeyHistory();

    Task<Void> provideDiagnosisKeys(List<File> keys, ExposureConfiguration configuration, String token);

    Task<ExposureSummary> getExposureSummary(String token);

    Task<List<ExposureInformation>> getExposureInformation(String token);
}
