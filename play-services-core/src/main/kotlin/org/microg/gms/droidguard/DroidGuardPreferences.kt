/*
 * SPDX-FileCopyrightText: 2021, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.droidguard

import android.content.Context
import android.content.SharedPreferences
import java.io.File

class DroidGuardPreferences(private val context: Context) {
    @Suppress("DEPRECATION")
    private val preferences by lazy { context.getSharedPreferences("droidguard", Context.MODE_PRIVATE) }
    private val systemDefaultPreferences by lazy {
        try {
            Context::class.java.getDeclaredMethod("getSharedPreferences", File::class.java, Int::class.javaPrimitiveType).invoke(context, File("/system/etc/microg.xml"), Context.MODE_PRIVATE) as SharedPreferences
        } catch (ignored: Exception) {
            null
        }
    }
    private var editing: Boolean = false
    private val updates: MutableMap<String, Any?> = hashMapOf()

    var mode: Mode
        get() = try {
            getSettingsString(PREF_DROIDGUARD_MODE)?.let { Mode.valueOf(it) } ?: Mode.Connector
        } catch (e: Exception) {
            Mode.Connector
        }
        set(value) {
            if (editing) updates[PREF_DROIDGUARD_MODE] = value.name
        }

    var networkServerUrl: String?
        get() = getSettingsString(PREF_DROIDGUARD_NETWORK_SERVER_URL)
        set(value) {
            if (editing) updates[PREF_DROIDGUARD_NETWORK_SERVER_URL] = value
        }

    private fun getSettingsString(key: String): String? {
        return systemDefaultPreferences?.getString(key, null) ?: preferences.getString(key, null)
    }

    fun edit(commands: DroidGuardPreferences.() -> Unit) {
        editing = true
        commands(this)
        preferences.edit().also {
            for ((k, v) in updates) {
                when (v) {
                    is String -> it.putString(k, v)
                    null -> it.remove(k)
                }
            }
        }.apply()
        editing = false
    }

    enum class Mode {
        Disabled,
        Connector,
        Network
    }

    companion object {
        const val PREF_DROIDGUARD_MODE = "droidguard_mode"
        const val PREF_DROIDGUARD_NETWORK_SERVER_URL = "droidguard_network_server_url"
    }
}

