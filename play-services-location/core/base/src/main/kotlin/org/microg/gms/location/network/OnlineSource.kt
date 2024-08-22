/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location.network

import android.net.Uri
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import org.microg.gms.location.LocationSettings
import org.microg.gms.location.base.BuildConfig

fun parseOnlineSources(string: String): List<OnlineSource> = JSONArray(string).let { array ->
    (0 until array.length()).map { parseOnlineSource(array.getJSONObject(it)) }.also { Log.d("Location", "parseOnlineSources: ${it.joinToString()}") }
}

fun parseOnlineSource(json: JSONObject): OnlineSource {
    val id = json.getString("id")
    val url = json.optString("url").takeIf { it.isNotBlank() }
    val host = json.optString("host").takeIf { it.isNotBlank() } ?: runCatching { Uri.parse(url).host }.getOrNull()
    val name = json.optString("name").takeIf { it.isNotBlank() } ?: host
    return OnlineSource(
        id = id,
        name = name,
        url = url,
        host = host,
        terms = json.optString("terms").takeIf { it.isNotBlank() }?.let { runCatching { Uri.parse(it) }.getOrNull() },
        suggested = json.optBoolean("suggested", false),
        import = json.optBoolean("import", false),
        allowContribute = json.optBoolean("allowContribute", false),
    )
}

data class OnlineSource(
    val id: String,
    val name: String? = null,
    val url: String? = null,
    val host: String? = null,
    val terms: Uri? = null,
    /**
     * Show suggested flag
     */
    val suggested: Boolean = false,
    /**
     * If set, automatically import from custom URL if host matches (is the same domain suffix)
     */
    val import: Boolean = false,
    val allowContribute: Boolean = false,
) {
    companion object {
        /**
         * Entry to allow configuring a custom URL
         */
        val ID_CUSTOM = "custom"

        /**
         * Legacy compatibility
         */
        val ID_DEFAULT = "default"

        val ALL: List<OnlineSource> = BuildConfig.ONLINE_SOURCES
    }
}

val LocationSettings.onlineSource: OnlineSource?
    get() {
        val id = onlineSourceId
        if (id != null) {
            val source = OnlineSource.ALL.firstOrNull { it.id == id }
            if (source != null) return source
        }
        val endpoint = customEndpoint
        if (endpoint != null) {
            val endpointHostSuffix = runCatching { "." + Uri.parse(endpoint).host }.getOrNull()
            if (endpointHostSuffix != null) {
                for (source in OnlineSource.ALL) {
                    if (source.import && endpointHostSuffix.endsWith("." + source.host)) {
                        return source
                    }
                }
            }
            val customSource = OnlineSource.ALL.firstOrNull { it.id == OnlineSource.ID_CUSTOM }
            if (customSource != null && customSource.import) {
                return customSource
            }
        }
        if (OnlineSource.ALL.size == 1) return OnlineSource.ALL.single()
        return OnlineSource.ALL.firstOrNull { it.id == OnlineSource.ID_DEFAULT }
    }

val LocationSettings.effectiveEndpoint: String?
    get() {
        val source = onlineSource ?: return null
        if (source.id == OnlineSource.ID_CUSTOM) return customEndpoint
        return source.url
    }