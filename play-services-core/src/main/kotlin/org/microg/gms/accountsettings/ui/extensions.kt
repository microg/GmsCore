/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.accountsettings.ui

import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.webkit.WebView

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