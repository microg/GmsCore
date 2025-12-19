/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.common

import android.accounts.Account
import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.MODE_PRIVATE
import androidx.core.content.edit
import org.microg.gms.auth.AuthConstants

class AccountUtils(val context: Context) {

    private val prefs = context.getSharedPreferences("common.selected_account_prefs", MODE_PRIVATE)

    companion object {
        private const val TYPE = "selected_account_type:"
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: AccountUtils? = null
        fun get(context: Context): AccountUtils = instance ?: synchronized(this) {
            instance ?: AccountUtils(context.applicationContext).also { instance = it }
        }
    }

    fun saveSelectedAccount(packageName: String, account: Account?) {
        if (account != null) {
            prefs.edit(true) {
                putString(packageName, account.name)
                putString(TYPE.plus(packageName), account.type)
            }
        }
    }

    fun getSelectedAccount(packageName: String): Account? {
        val name = prefs.getString(packageName, null) ?: return null
        val type = prefs.getString(TYPE.plus(packageName), AuthConstants.DEFAULT_ACCOUNT_TYPE) ?: return null
        return Account(name, type)
    }

    fun removeSelectedAccount(packageName: String) {
        prefs.edit {
            remove(packageName)
            remove(TYPE.plus(packageName))
        }
    }
}