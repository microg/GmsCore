/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.rcs

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Locale

private const val RCS_POLICY_TAG = "RcsPolicyConfig"
private const val POLICY_FILE_NAME = "rcs_policy_overrides.json"

internal data class CompletionRowKey(
    val token: String,
    val code: Int
)

internal data class RcsPolicyConfig(
    val enableMinimalCompletion: Boolean,
    val messagesClients: Set<String>,
    val completionRows: Set<CompletionRowKey>
) {
    companion object {
        private val DEFAULT_MESSAGES_CLIENTS = setOf(
            "com.google.android.apps.messaging",
            "com.samsung.android.messaging"
        )
        private val DEFAULT_COMPLETION_ROWS = setOf(
            CompletionRowKey(token = "com.google.android.gms.rcs.iprovisioning", code = 1),
            CompletionRowKey(token = "com.google.android.gms.rcs.iprovisioning", code = 2),
            CompletionRowKey(token = "com.google.android.gms.rcs.iprovisioning", code = 1001)
        )

        fun defaults(): RcsPolicyConfig {
            return RcsPolicyConfig(
                enableMinimalCompletion = true,
                messagesClients = DEFAULT_MESSAGES_CLIENTS,
                completionRows = DEFAULT_COMPLETION_ROWS
            )
        }
    }
}

internal object RcsPolicyConfigStore {
    @Volatile
    private var cachedConfig: RcsPolicyConfig = RcsPolicyConfig.defaults()
    @Volatile
    private var cachedMtimeMs: Long = -1L
    @Volatile
    private var lastAttemptMs: Long = 0L
    private const val RELOAD_INTERVAL_MS = 3_000L

    fun current(context: Context): RcsPolicyConfig {
        val now = System.currentTimeMillis()
        if (now - lastAttemptMs < RELOAD_INTERVAL_MS) return cachedConfig
        synchronized(this) {
            val refreshedNow = System.currentTimeMillis()
            if (refreshedNow - lastAttemptMs < RELOAD_INTERVAL_MS) return cachedConfig
            lastAttemptMs = refreshedNow
            reloadIfChanged(context)
            return cachedConfig
        }
    }

    private fun reloadIfChanged(context: Context) {
        val policyFile = File(context.filesDir, POLICY_FILE_NAME)
        if (!policyFile.exists()) {
            if (cachedMtimeMs != -1L) {
                cachedConfig = RcsPolicyConfig.defaults()
                cachedMtimeMs = -1L
                Log.i(RCS_POLICY_TAG, "policy_config reset_to_defaults")
            }
            return
        }
        val mtime = policyFile.lastModified()
        if (mtime == cachedMtimeMs) return
        val parsed = parsePolicy(policyFile.readText())
        cachedConfig = parsed ?: RcsPolicyConfig.defaults()
        cachedMtimeMs = mtime
        Log.i(
            RCS_POLICY_TAG,
            "policy_config reloaded completion=${cachedConfig.enableMinimalCompletion} clients=${cachedConfig.messagesClients.size} rows=${cachedConfig.completionRows.size}"
        )
    }

    private fun parsePolicy(jsonText: String): RcsPolicyConfig? {
        return runCatching {
            val root = JSONObject(jsonText)
            val defaults = RcsPolicyConfig.defaults()
            val completion = root.optBoolean("enableMinimalCompletion", defaults.enableMinimalCompletion)
            val clients = parseClients(root.optJSONArray("messagesClients"), defaults.messagesClients)
            val rows = parseRows(root.optJSONArray("completionRows"), defaults.completionRows)
            RcsPolicyConfig(
                enableMinimalCompletion = completion,
                messagesClients = clients,
                completionRows = rows
            )
        }.onFailure {
            Log.w(RCS_POLICY_TAG, "policy_config parse_failed: ${it.message}")
        }.getOrNull()
    }

    private fun parseClients(source: JSONArray?, fallback: Set<String>): Set<String> {
        if (source == null) return fallback
        val values = mutableSetOf<String>()
        for (index in 0 until source.length()) {
            val candidate = source.optString(index).orEmpty().trim().lowercase(Locale.US)
            if (candidate.isNotEmpty()) values += candidate
        }
        return if (values.isNotEmpty()) values else fallback
    }

    private fun parseRows(source: JSONArray?, fallback: Set<CompletionRowKey>): Set<CompletionRowKey> {
        if (source == null) return fallback
        val values = mutableSetOf<CompletionRowKey>()
        for (index in 0 until source.length()) {
            val row = source.optJSONObject(index) ?: continue
            val token = row.optString("token").orEmpty().trim().lowercase(Locale.US)
            val code = row.optInt("code", Int.MIN_VALUE)
            if (token.isNotEmpty() && code != Int.MIN_VALUE) {
                values += CompletionRowKey(token = token, code = code)
            }
        }
        return if (values.isNotEmpty()) values else fallback
    }
}
