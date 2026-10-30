/*
 * SPDX-FileCopyrightText: 2026 e foundation
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.vending.enterprise

import android.accounts.Account
import android.content.Context

class WorkAccountCreationTimes(context: Context) {

    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun recordIfAbsent(account: Account) {
        if (!preferences.contains(account.name)) {
            preferences.edit().putLong(account.name, System.currentTimeMillis()).apply()
        }
    }

    fun creationTimeMillisOf(account: Account): Long? =
        if (preferences.contains(account.name)) preferences.getLong(account.name, 0) else null

    fun clear() {
        preferences.edit().clear().apply()
    }

    companion object {
        private const val PREFERENCES_NAME = "work_account_creation_times"
    }
}
