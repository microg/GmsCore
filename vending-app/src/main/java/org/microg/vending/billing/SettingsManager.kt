/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.vending.billing

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.preference.PreferenceManager

// TODO: Better name?
private const val AUTH_STATUS_KEY = "key_auth_status"

class SettingsManager(private val context: Context) {
    private val preferences: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
    }

    fun setAuthStatus(needAuth: Boolean) {
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "setAuthStatus: $needAuth")
        val editor = preferences.edit()
        editor.putBoolean(AUTH_STATUS_KEY, needAuth)
        editor.apply()
    }

    fun getAuthStatus(): Boolean {
        return preferences.getBoolean(AUTH_STATUS_KEY, true)
    }
}