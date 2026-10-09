/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.accountsettings

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.view.View
import com.google.android.gms.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.microg.gms.auth.AuthConstants
import org.microg.gms.gcm.createGrpcClient
import java.io.IOException
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

internal const val NATIVE_ACTION_GOOGLE_HELP = 19

private const val ACCOUNT_SETTINGS_BASE_URL = "https://accountsettingsmobile-pa.googleapis.com"
private const val ACCOUNT_SETTINGS_OAUTH_SCOPE = "oauth2:https://www.googleapis.com/auth/account_settings_mobile"
private const val CLIENT_TYPE_ANDROID = 1
private const val RENDERER_NATIVE_ACTION = 2
private const val RENDERER_BROWSER = 3
private const val RENDERER_WEB_VIEW = 5
private const val SEARCH_TIMEOUT_SECONDS = 20L

object AccountSearchClient {

    suspend fun searchAccountSettings(
        context: Context,
        accountName: String,
        callingPackageName: String,
        query: String
    ): AccountSearchResponse = withContext(Dispatchers.IO) {
        val account = Account(accountName, AuthConstants.DEFAULT_ACCOUNT_TYPE)
        val token = AccountManager.get(context)
            .blockingGetAuthToken(account, ACCOUNT_SETTINGS_OAUTH_SCOPE, true)
            ?: throw IOException("Account search authorization unavailable")
        val call = createGrpcClient<AccountSettingsMobileClient>(ACCOUNT_SETTINGS_BASE_URL, token).Search()
        call.timeout.timeout(SEARCH_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        val locale = Locale.getDefault()
        call.requestMetadata = mapOf("accept-language" to if (android.os.Build.VERSION.SDK_INT >= 21) locale.toLanguageTag() else locale.language)
        call.execute(AccountSearchRequest(
            context = AccountRequestContext(
                clientType = CLIENT_TYPE_ANDROID,
                capabilities = AccountCapabilities(
                    nativeActions = listOf(AccountCapability(type = NATIVE_ACTION_GOOGLE_HELP)),
                    renderers = listOf(RENDERER_NATIVE_ACTION, RENDERER_BROWSER, RENDERER_WEB_VIEW)
                        .map { AccountCapability(type = it) },
                    themedUrls = true,
                    categorySearchIcons = true
                ),
                clientInfo = AccountClientInfo(
                    androidVersion = android.os.Build.VERSION.RELEASE,
                    androidSdkVersion = android.os.Build.VERSION.SDK_INT.toString(),
                    gmsVersion = BuildConfig.VERSION_CODE.toString()
                ),
                timeZone = TimeZone.getDefault().id,
                densityDpi = context.resources.displayMetrics.densityDpi,
                rightToLeft = context.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL,
                callingPackage = callingPackageName
            ),
            query = AccountSearchQuery(text = query)
        ))
    }
}
