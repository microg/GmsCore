/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.languageprofile

import android.os.Bundle
import android.util.Log
import com.google.android.gms.common.Feature
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.api.internal.IStatusCallback
import com.google.android.gms.common.internal.ConnectionInfo
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import com.google.android.gms.languageprofile.ClientLanguageSettings
import com.google.android.gms.languageprofile.LanguageFluencyParams
import com.google.android.gms.languageprofile.LanguagePreferenceParams
import com.google.android.gms.languageprofile.internal.ILanguageProfileCallbacks
import com.google.android.gms.languageprofile.internal.ILanguageProfileService
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService

private const val TAG = "LanguageProfileService"
private val FEATURES = arrayOf(
    Feature("get_application_locale_suggestions_api", 1L),
    Feature("get_language_preferences_for_product_id_api", 1L),
    Feature("set_language_settings_api", 1L),
)

class LanguageProfileService : BaseService(TAG, GmsService.LANGUAGE_PROFILE) {
    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        callback.onPostInitCompleteWithConnectionInfo(CommonStatusCodes.SUCCESS, LanguageProfileServiceImpl(), ConnectionInfo().apply {
            features = FEATURES
        })
    }
}


class LanguageProfileServiceImpl : ILanguageProfileService.Stub() {
    override fun fun1(accountName: String?): String? {
        return null
    }

    override fun fun2(accountName: String?, callbacks: ILanguageProfileCallbacks?) {
        Log.d(TAG, "fun2($accountName)")
        callbacks?.onString(Status.SUCCESS, "ULP not available.")
    }

    override fun getLanguagePreferences(
        accountName: String?,
        params: LanguagePreferenceParams?,
        callbacks: ILanguageProfileCallbacks?
    ) {
        Log.d(TAG, "getLanguagePreferences($accountName, $params)")
        callbacks?.onLanguagePreferences(Status.SUCCESS, emptyList())
    }

    override fun getLanguageFluencies(
        accountName: String?,
        params: LanguageFluencyParams?,
        callbacks: ILanguageProfileCallbacks?
    ) {
        Log.d(TAG, "getLanguageFluencies($accountName, $params)")
        callbacks?.onLanguageFluencies(Status.SUCCESS, emptyList())
    }

    override fun getLanguageSettings(
        accountName: String?,
        settings: ClientLanguageSettings?,
        callback: IStatusCallback?
    ) {
        Log.d(TAG, "getLanguageSettings($accountName, $settings)")
        callback?.onResult(Status.SUCCESS)
    }

    override fun removeLanguageSettings(accountName: String?, callback: IStatusCallback?) {
        Log.d(TAG, "removeLanguageSettings($accountName)")
        callback?.onResult(Status.SUCCESS)
    }

}
