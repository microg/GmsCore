/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.chimera.component

import android.content.Context
import android.os.Build
import com.google.android.chimera.config.ModuleDownloadRegistry
import org.json.JSONArray
import org.json.JSONObject

/** Device-protected hand-off state used to recover download/import flows after the :ui process dies. */
object PendingModuleRequestStore {
    private const val PREFS = "chimera_pending_module_request"
    private const val KEY_REQUESTS = "requests"
    private const val MAX_REQUEST_AGE_MILLIS = 24L * 60 * 60 * 1000
    private const val MAX_FUTURE_SKEW_MILLIS = 5L * 60 * 1000

    data class PendingRequest(
        val requestId: String,
        val features: List<ModuleDownloadRegistry.RequestedFeature>,
        val currentCatalogId: String,
        val componentClassName: String?,
        val afterAuthorizationAction: Int,
        val taskId: Int,
        val downloadTargetSelected: Boolean = false,
        val createdAtMillis: Long = System.currentTimeMillis(),
    )

    @Synchronized
    fun save(context: Context, request: PendingRequest) {
        if (request.requestId.isEmpty() || request.features.isEmpty()) return
        val requests = readValidRequests(context)
            .filterNot { it.requestId == request.requestId }
            .plus(request)
        writeRequests(context, requests)
    }

    @Synchronized
    fun load(context: Context, requestId: String): PendingRequest? {
        if (requestId.isEmpty()) return null
        return readValidRequests(context).firstOrNull { it.requestId == requestId }
    }

    @Synchronized
    fun loadAll(context: Context): List<PendingRequest> {
        return readValidRequests(context).sortedByDescending(PendingRequest::createdAtMillis)
    }

    @Synchronized
    fun markDownloadTargetSelected(context: Context, requestId: String): Boolean {
        if (requestId.isEmpty()) return false
        var found = false
        val requests = readValidRequests(context).map { request ->
            if (request.requestId == requestId) {
                found = true
                request.copy(downloadTargetSelected = true)
            } else {
                request
            }
        }
        if (found) writeRequests(context, requests)
        return found
    }

    @Synchronized
    fun remove(context: Context, requestId: String) {
        if (requestId.isEmpty()) return
        writeRequests(context, readValidRequests(context).filterNot { it.requestId == requestId })
    }

    private fun readValidRequests(context: Context): List<PendingRequest> {
        val prefs = preferences(context)
        val encoded = prefs.getString(KEY_REQUESTS, null) ?: run {
            // Purge the legacy single-request schema when upgrading.
            if (prefs.all.isNotEmpty()) prefs.edit().clear().commit()
            return emptyList()
        }
        val now = System.currentTimeMillis()
        val requests = runCatching {
            val array = JSONArray(encoded)
            buildList {
                for (index in 0 until array.length()) {
                    decodeRequest(array.optJSONObject(index) ?: continue)?.let(::add)
                }
            }
        }.getOrElse {
            prefs.edit().clear().commit()
            return emptyList()
        }
        val valid = requests.filter { request ->
            request.createdAtMillis > 0L &&
                    request.createdAtMillis <= now + MAX_FUTURE_SKEW_MILLIS &&
                    now - request.createdAtMillis <= MAX_REQUEST_AGE_MILLIS
        }
        if (valid.size != requests.size) writeRequests(context, valid)
        return valid
    }

    private fun decodeRequest(value: JSONObject): PendingRequest? {
        val requestId = value.optString("requestId").takeIf(String::isNotEmpty) ?: return null
        val featureArray = value.optJSONArray("features") ?: return null
        val features = buildList {
            for (index in 0 until featureArray.length()) {
                val feature = featureArray.optJSONObject(index) ?: continue
                val name = feature.optString("name").takeIf(String::isNotEmpty) ?: continue
                add(
                    ModuleDownloadRegistry.RequestedFeature(
                        name = name,
                        minVersion = feature.optLong("minVersion", 0L),
                    )
                )
            }
        }
        if (features.isEmpty()) return null
        return PendingRequest(
            requestId = requestId,
            features = features,
            currentCatalogId = value.optString("currentCatalogId"),
            componentClassName = value.optString("componentClassName")
                .takeIf(String::isNotEmpty),
            afterAuthorizationAction = value.optInt(
                "afterAuthorizationAction",
                ModuleDownloadActivity.ACTION_DOWNLOAD_MODULE,
            ),
            taskId = value.optInt("taskId", -1),
            downloadTargetSelected = value.optBoolean("downloadTargetSelected", false),
            createdAtMillis = value.optLong("createdAtMillis", 0L),
        )
    }

    private fun writeRequests(context: Context, requests: List<PendingRequest>) {
        val prefs = preferences(context)
        if (requests.isEmpty()) {
            prefs.edit().clear().commit()
            return
        }
        val array = JSONArray()
        requests.forEach { request ->
            val features = JSONArray()
            request.features.forEach { feature ->
                features.put(
                    JSONObject()
                        .put("name", feature.name)
                        .put("minVersion", feature.minVersion)
                )
            }
            array.put(
                JSONObject()
                    .put("requestId", request.requestId)
                    .put("features", features)
                    .put("currentCatalogId", request.currentCatalogId)
                    .put("componentClassName", request.componentClassName.orEmpty())
                    .put("afterAuthorizationAction", request.afterAuthorizationAction)
                    .put("taskId", request.taskId)
                    .put("downloadTargetSelected", request.downloadTargetSelected)
                    .put("createdAtMillis", request.createdAtMillis)
            )
        }
        prefs.edit().clear().putString(KEY_REQUESTS, array.toString()).commit()
    }

    private fun preferences(context: Context) =
        (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.createDeviceProtectedStorageContext()
        } else {
            context
        }).getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
