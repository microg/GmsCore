/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.finsky.assetmoduleservice

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.google.android.play.core.assetpacks.model.AssetPackStatus

private const val SESSION_MGR_TAG = "AssetModuleSessionMgr"
private const val PREFS_NAME = "AssetModuleSessionIdGenerator"
private const val PREFS_KEY_LATEST = "Latest"

data class AssetModuleSession(
    val sessionId: Int,
    val packageName: String,
    val moduleNames: Set<String>,
    val createdAtElapsedMs: Long,
    var terminal: Boolean = false
)

/**
 * Play-like session registry for asset module downloads.
 * One ever-increasing session id covers all modules in a single startDownload request.
 */
class AssetModuleSessionManager(private val context: Context) {
    private val lock = Any()
    private val sessionsById = mutableMapOf<Int, AssetModuleSession>()
    private val sessionIdsByPackage = mutableMapOf<String, MutableSet<Int>>()

    fun nextSessionId(): Int = synchronized(lock) {
        allocateSessionIdLocked()
    }

    fun createSession(packageName: String, modules: Collection<String>): AssetModuleSession {
        synchronized(lock) {
            val sessionId = allocateSessionIdLocked()
            val session = AssetModuleSession(
                sessionId = sessionId,
                packageName = packageName,
                moduleNames = modules.toSet(),
                createdAtElapsedMs = SystemClock.elapsedRealtime()
            )
            sessionsById[sessionId] = session
            sessionIdsByPackage.getOrPut(packageName) { mutableSetOf() }.add(sessionId)
            Log.d(SESSION_MGR_TAG, "createSession: package=$packageName sessionId=$sessionId modules=${session.moduleNames}")
            return session
        }
    }

    fun getSession(sessionId: Int): AssetModuleSession? = synchronized(lock) {
        sessionsById[sessionId]
    }

    fun findActiveSession(packageName: String, module: String): AssetModuleSession? = synchronized(lock) {
        val ids = sessionIdsByPackage[packageName] ?: return null
        ids.mapNotNull { sessionsById[it] }
            .filter { !it.terminal && module in it.moduleNames }
            .maxByOrNull { it.createdAtElapsedMs }
    }

    fun sessionsForPackage(packageName: String): List<AssetModuleSession> = synchronized(lock) {
        sessionIdsByPackage[packageName]
            ?.mapNotNull { sessionsById[it] }
            ?.sortedBy { it.createdAtElapsedMs }
            ?: emptyList()
    }

    fun markTerminalIfDone(sessionId: Int, downloadData: DownloadData) {
        synchronized(lock) {
            val session = sessionsById[sessionId] ?: return
            val allTerminal = session.moduleNames.all { moduleName ->
                val status = runCatching { downloadData.getModuleData(moduleName).status }.getOrNull()
                status == AssetPackStatus.COMPLETED ||
                    status == AssetPackStatus.FAILED ||
                    status == AssetPackStatus.CANCELED
            }
            if (allTerminal) {
                session.terminal = true
                Log.d(SESSION_MGR_TAG, "markTerminalIfDone: sessionId=$sessionId terminal=true")
            }
        }
    }

    private fun allocateSessionIdLocked(): Int {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val latest = sharedPreferences.getInt(PREFS_KEY_LATEST, 0) + 1
        sharedPreferences.edit().putInt(PREFS_KEY_LATEST, latest).commit()
        return latest
    }
}
