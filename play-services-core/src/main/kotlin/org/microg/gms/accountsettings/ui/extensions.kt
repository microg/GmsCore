/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.accountsettings.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.WebView
import android.widget.Toast
import com.google.android.gms.R
import org.microg.gms.accountsettings.AccountSettingsHelpParams
import org.microg.gms.accountsettings.AccountSettingsHelpUrls
import org.microg.gms.profile.Build.VERSION.SDK_INT
import java.net.URI
import androidx.core.net.toUri

private const val TAG = "AccountSettings"

internal const val SCREEN_ID_ACCOUNT_SEARCH = 10002
internal const val REQUEST_ACCOUNT_SEARCH_NAVIGATION = "accountSearchNavigation"
internal const val EXTRA_OPEN_EXTERNALLY = "extra.openExternally"
private const val URL_ACCOUNT_HELP = "https://support.google.com/accounts"

const val ACTION_BROWSE_SETTINGS = "com.google.android.gms.accountsettings.action.BROWSE_SETTINGS"
const val ACTION_MY_ACCOUNT = "com.google.android.gms.accountsettings.MY_ACCOUNT"
const val ACTION_ACCOUNT_PREFERENCES_SETTINGS = "com.google.android.gms.accountsettings.ACCOUNT_PREFERENCES_SETTINGS"
const val ACTION_PRIVACY_SETTINGS = "com.google.android.gms.accountsettings.PRIVACY_SETTINGS"
const val ACTION_SECURITY_SETTINGS = "com.google.android.gms.accountsettings.SECURITY_SETTINGS"
const val ACTION_LOCATION_SHARING = "com.google.android.gms.location.settings.LOCATION_SHARING"

const val EXTRA_CALLING_PACKAGE_NAME = "extra.callingPackageName"
const val EXTRA_IGNORE_ACCOUNT = "extra.ignoreAccount"
const val EXTRA_ACCOUNT_NAME = "extra.accountName"
const val EXTRA_SCREEN_ID = "extra.screenId"
const val EXTRA_SCREEN_OPTIONS_PREFIX = "extra.screen."
const val EXTRA_FALLBACK_URL = "extra.fallbackUrl"
const val EXTRA_FALLBACK_AUTH = "extra.fallbackAuth"
const val EXTRA_THEME_CHOICE = "extra.themeChoice"
const val EXTRA_SCREEN_MY_ACTIVITY_PRODUCT = "extra.screen.myactivityProduct"
const val EXTRA_SCREEN_KID_ONBOARDING_PARAMS = "extra.screen.kidOnboardingParams"
const val EXTRA_SCREEN_FAMILY_APP_ID = "extra.screen.family-app_id"
const val EXTRA_URL = "extra.url"

const val QUERY_WC_ACTION = "wv_action"
const val QUERY_GNOTS_ACTION = "gnotswvaction"
const val ACTION_CLOSE = "close"
const val KEY_NOTIFICATION_ID = "notificationId"

const val KEY_UPDATED_PHOTO_URL = "updatedPhotoUrl"

const val OPTION_SCREEN_FLAVOR = "screenFlavor"

enum class ResultStatus(val value: Int) {
    USER_CANCEL(1), FAILED(2), SUCCESS(3), NO_OP(4)
}

fun evaluateJavascriptCallback(webView: WebView, script: String) {
    runOnMainLooper {
        webView.evaluateJavascript(script, null)
    }
}

fun runOnMainLooper(method: () -> Unit) {
    if (Looper.myLooper() == Looper.getMainLooper()) {
        method()
    } else {
        Handler(Looper.getMainLooper()).post {
            method()
        }
    }
}

fun isGoogleAvatarUrl(url: String?): Boolean {
    if (url.isNullOrBlank()) return false
    return try {
        val uri = Uri.parse(url)
        val isGoogleHost = uri.host == "lh3.googleusercontent.com"
        val isAvatarPath = uri.path?.startsWith("/a/") == true
        val hasSizeParam = url.matches(Regex(".*=s\\d+-c-no$"))
        isGoogleHost && isAvatarPath && hasSizeParam
    } catch (e: Exception) {
        false
    }
}

fun Context.isNightMode(): Boolean {
    val nightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    return nightMode == Configuration.UI_MODE_NIGHT_YES
}

fun Activity.finishActivity() {
    if (SDK_INT >= 21) finishAndRemoveTask() else finish()
}

internal fun String.isBrowsableWebUrl(): Boolean = runCatching { URI(this) }.getOrNull()?.let {
    (it.scheme.equals("https", true) || it.scheme.equals("http", true)) && !it.host.isNullOrBlank()
} == true

internal fun String.isAllowedWebUrl(allowedPrefixes: Set<String> = ALLOWED_WEB_PREFIXES): Boolean {
    val uri = runCatching { URI(this) }.getOrNull() ?: return false
    if (!uri.scheme.equals("https", true) || uri.userInfo != null || (uri.port != -1 && uri.port != 443)) return false
    if (uri.path?.split('/')?.any { it == "." || it == ".." } == true) return false
    return allowedPrefixes.any {
        val allowed = URI(it)
        uri.host.equals(allowed.host, true) && (uri.path.ifEmpty { "/" }).let { path ->
            val allowedPath = allowed.path.ifEmpty { "/" }
            path == allowedPath || path.startsWith(if (allowedPath.endsWith('/')) allowedPath else "$allowedPath/")
        }
    }
}

internal fun Context.getThemedUrl(url: String?, themeUrls: AccountSettingsHelpUrls?): String? =
    (if (isNightMode()) themeUrls?.darkThemeUrl else null)?.takeIf(String::isNotBlank)
        ?: themeUrls?.defaultUrl?.takeIf(String::isNotBlank) ?: url?.takeIf(String::isNotBlank)

internal fun Context.getHelpUrl(params: AccountSettingsHelpParams): String =
    listOfNotNull(
        if (isNightMode()) params.themeUrls?.darkThemeUrl else null,
        params.themeUrls?.defaultUrl,
        params.url,
        params.fallbackUrl
    ).firstOrNull(String::isBrowsableWebUrl) ?: URL_ACCOUNT_HELP

internal fun MainActivity.openUrl(url: String?, accountName: String?, callingPackage: String, openExternally: Boolean = false) {
    if (url.isNullOrBlank() || !url.isBrowsableWebUrl() || isFinishing || isDestroyed) return
    val uri = url.toUri()
    if (!openExternally && url.isAllowedWebUrl()) {
        startActivity(createScreenIntent(accountName, callingPackage).putExtra(EXTRA_URL, url))
    } else {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE))
        } catch (e: Exception) {
            Log.w(TAG, "Unable to open account link: ${e.javaClass.simpleName}")
            Toast.makeText(this, R.string.account_settings_link_unavailable, Toast.LENGTH_SHORT).show()
        }
    }
}

internal fun MainActivity.openScreen(screenId: Int, options: Bundle, accountName: String?, callingPackage: String) {
    if (isFinishing || isDestroyed) return
    if (screenId != SCREEN_ID_ACCOUNT_SEARCH && screenId !in SCREEN_ID_TO_URL) {
        Toast.makeText(this, R.string.account_settings_link_unavailable, Toast.LENGTH_SHORT).show()
        return
    }
    startActivity(createScreenIntent(accountName, callingPackage).putExtras(options).putExtra(EXTRA_SCREEN_ID, screenId))
}
