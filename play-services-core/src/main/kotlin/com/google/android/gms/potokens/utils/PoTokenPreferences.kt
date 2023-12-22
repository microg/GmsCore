/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.potokens.utils

import android.content.Context
import android.content.SharedPreferences
import com.android.volley.toolbox.Volley
import org.microg.gms.droidguard.core.HandleProxyFactory

class PoTokenPreferences(context: Context) {

    private val sp: SharedPreferences =
        context.getSharedPreferences("com.google.android.gms.potokens", Context.MODE_PRIVATE)

    fun save(key: String, value: Any?) {
        when (value) {
            is String -> {
                sp.edit().putString(key, value).apply()
            }

            is Int -> {
                sp.edit().putInt(key, value).apply()
            }
        }
    }

    fun getString(key: String, defValue: String?): String? {
        return sp.getString(key, defValue)
    }

    fun getInt(key: String, defValue: Int): Int {
        return sp.getInt(key, defValue)
    }

    companion object {
        @Volatile
        private var instance: PoTokenPreferences? = null
        fun get(context: Context): PoTokenPreferences {
            return instance ?: synchronized(this) {
                instance ?: PoTokenPreferences(context).also { instance = it }
            }
        }
    }
}