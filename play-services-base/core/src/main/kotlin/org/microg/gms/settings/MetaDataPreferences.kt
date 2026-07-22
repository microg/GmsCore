/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.settings

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager.GET_META_DATA
import android.os.Bundle

class MetaDataPreferences(private val context: Context, private val prefix: String = "") : SharedPreferences {
    private val metaData by lazy {
        runCatching { context.packageManager.getApplicationInfo(context.packageName, GET_META_DATA) }.getOrNull()?.metaData ?: Bundle.EMPTY
    }

    override fun getAll(): Map<String, *> = metaData.keySet().filter { it.startsWith(prefix) }.associate { it.substring(prefix.length) to metaData.get(it) }

    override fun getString(key: String, defValue: String?): String? = metaData.getString(prefix + key, defValue)

    override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? = metaData.getStringArray(prefix + key)?.toSet() ?: defValues

    override fun getInt(key: String?, defValue: Int): Int = metaData.getInt(prefix + key, defValue)

    override fun getLong(key: String?, defValue: Long): Long = metaData.getLong(prefix + key, defValue)

    override fun getFloat(key: String?, defValue: Float): Float = metaData.getFloat(prefix + key, defValue)

    override fun getBoolean(key: String?, defValue: Boolean): Boolean = metaData.getBoolean(prefix + key, defValue)

    override fun contains(key: String?): Boolean = metaData.containsKey(prefix + key)

    override fun edit(): SharedPreferences.Editor {
        throw UnsupportedOperationException()
    }

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
        throw UnsupportedOperationException()
    }

    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
        throw UnsupportedOperationException()
    }
}