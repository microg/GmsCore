/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.vending.splitinstall

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.pm.PackageInfoCompat
import com.google.android.finsky.splitinstallservice.SplitInstallException
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

private const val TAG = "SplitInstallRemovals"

internal class SplitInstallRemovalStore(private val context: Context) {
    private val preferences = context.getSharedPreferences("split-install-removals", Context.MODE_PRIVATE)

    val packages: Set<String> get() = preferences.all.keys

    fun read(packageName: String): RemovalRequest? = synchronized(lock) {
        val value = preferences.all[packageName] ?: return@synchronized null
        val request = try {
            (value as? String)?.let {
                val json = JSONObject(it)
                val requested = json.getJSONArray("requested")
                RemovalRequest(json.getLong("version"), json.getLong("client_install_time"),
                    (0 until requested.length()).map { requested.getString(it) }.toSet())
            }
        } catch (e: JSONException) {
            null
        }
        val info = try {
            context.packageManager.getPackageInfo(packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
        if (request == null || info == null || request.version != PackageInfoCompat.getLongVersionCode(info) || request.clientInstallTime != info.firstInstallTime) {
            save(packageName, null)
            return@synchronized null
        }
        request
    }

    fun save(packageName: String, request: RemovalRequest?) = synchronized(lock) {
        val previous = preferences.all[packageName]
        val editor = preferences.edit()
        if (request == null || request.splits.isEmpty()) editor.remove(packageName)
        else editor.putString(packageName, JSONObject().put("version", request.version).put("client_install_time", request.clientInstallTime)
            .put("requested", JSONArray(request.splits)).toString())
        if (!editor.commit()) {
            val rollback = preferences.edit()
            when (previous) {
                is String -> rollback.putString(packageName, previous)
                is Set<*> -> rollback.putStringSet(packageName, previous.filterIsInstance<String>().toSet())
                else -> rollback.remove(packageName)
            }
            if (!rollback.commit()) Log.w(TAG, "Unable to restore split removal metadata")
            throw SplitInstallException(-10)
        }
    }

    data class RemovalRequest(val version: Long, val clientInstallTime: Long, val splits: Set<String>)

    companion object {
        private val lock = Any()
    }
}
