/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0 AND CC-BY-4.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */
package com.google.android.gms.nearby.exposurenotification

import com.google.android.gms.common.api.Api.ApiOptions.NoOptions
import com.google.android.gms.common.api.HasApiKey
import com.google.android.gms.tasks.Task
import org.microg.gms.common.PublicApi
import org.microg.gms.nearby.exposurenotification.Constants
import java.io.File

@PublicApi
interface ExposureNotificationClient : HasApiKey<NoOptions?> {
    fun start(): Task<Void?>?
    fun stop(): Task<Void?>?
    fun isEnabled(): Task<Boolean?>?
    fun getTemporaryExposureKeyHistory(): Task<List<TemporaryExposureKey?>?>?
    fun provideDiagnosisKeys(keys: List<File?>?, configuration: ExposureConfiguration?, token: String?): Task<Void?>?
    fun getExposureSummary(token: String?): Task<ExposureSummary?>?
    fun getExposureInformation(token: String?): Task<List<ExposureInformation?>?>?

    companion object {
        const val ACTION_EXPOSURE_NOTIFICATION_SETTINGS = Constants.ACTION_EXPOSURE_NOTIFICATION_SETTINGS
        const val ACTION_EXPOSURE_NOT_FOUND = Constants.ACTION_EXPOSURE_NOT_FOUND
        const val ACTION_EXPOSURE_STATE_UPDATED = Constants.ACTION_EXPOSURE_STATE_UPDATED
        const val EXTRA_EXPOSURE_SUMMARY = Constants.EXTRA_EXPOSURE_SUMMARY
        const val EXTRA_TOKEN = Constants.EXTRA_TOKEN
    }
}