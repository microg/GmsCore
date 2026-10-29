/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.vending.splitinstall

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.PendingIntentCompat
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import com.android.vending.installer.InstallReceiver
import com.android.vending.installer.SESSION_ID
import com.android.vending.installer.SessionResultReceiver
import com.android.vending.installer.VENDING_INSTALL_DELETE_ACTION
import com.google.android.finsky.splitinstallservice.ERROR_API_NOT_AVAILABLE
import com.google.android.finsky.splitinstallservice.ERROR_INTERNAL
import com.google.android.finsky.splitinstallservice.ERROR_INVALID_REQUEST
import com.google.android.finsky.splitinstallservice.ERROR_SESSION_NOT_FOUND
import com.google.android.finsky.splitinstallservice.SplitInstallException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import org.microg.vending.enterprise.Downloading
import org.microg.vending.ui.notifyInstallPrompt
import org.microg.vending.ui.notifySplitInstallProgress
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import androidx.core.content.edit

private const val TAG = "SplitInstallSessions"
private const val ACTION_UPDATE_SERVICE = "com.google.android.play.core.splitinstall.receiver.SplitInstallUpdateIntentService"
internal const val STATUS_PENDING = 1
internal const val STATUS_DOWNLOADING = 2
internal const val STATUS_DOWNLOADED = 3
internal const val STATUS_INSTALLING = 4
internal const val STATUS_INSTALLED = 5
internal const val STATUS_FAILED = 6
internal const val STATUS_CANCELED = 7
internal const val STATUS_REQUIRES_USER_CONFIRMATION = 8
internal const val STATUS_CANCELING = 9
private const val INSTALLER_RESULT_TIMEOUT_MILLIS = 30_000L
private const val SESSION_RETENTION_MILLIS = 24 * 60 * 60 * 1000L
private const val NOTIFICATION_CLEANUP_PREFIX = "notification-cleanup:"
private val INSTALLER_ACTIVE_STATUSES = setOf(STATUS_INSTALLING, STATUS_REQUIRES_USER_CONFIRMATION)
private val ACTIVE_STATUSES = setOf(STATUS_PENDING, STATUS_DOWNLOADING, STATUS_INSTALLING, STATUS_REQUIRES_USER_CONFIRMATION, STATUS_CANCELING)

internal class SplitInstallSessionManager(
    private val context: Context,
    private val onSessionFinished: (String, Boolean) -> Unit,
    private val onDeferredInstallBlocked: (Session) -> Boolean
) {
    private val savedSessions = context.getSharedPreferences("split-install-sessions", Context.MODE_PRIVATE)

    fun getSession(sessionId: Int): Session? = sessions[sessionId]

    fun isCurrent(session: Session): Boolean = sessions[session.state.getInt("session_id")] === session

    fun isCurrentInstallation(session: Session): Boolean = installedPackage(session.packageName)?.let {
        PackageInfoCompat.getLongVersionCode(it) == session.version && session.clientInstallTime == it.firstInstallTime
    } == true

    fun removeStaleSessions(packageName: String) {
        sessions.values.filter { it.packageName == packageName }.forEach { session ->
            synchronized(session) {
                if (!isCurrent(session)) return@synchronized
                val info = installedPackage(packageName)
                if (info == null || PackageInfoCompat.getLongVersionCode(info) != session.version ||
                    (session.clientInstallTime != info.firstInstallTime &&
                            (!session.useSplitCompat || session.state.getInt("status") != STATUS_DOWNLOADED))
                ) removeSession(session)
                else if (SDK_INT >= 21 && session.clientInstallTime != info.firstInstallTime && session.installerSessionId != -1 && !session.completionBlocked) {
                    blockCompletion(session)
                }
            }
        }
    }

    fun hasActiveSession(packageName: String): Boolean {
        removeStaleSessions(packageName)
        return sessions.values.any { it.packageName == packageName && isActive(it) }
    }

    fun hasPendingDeferredRetry(packageName: String): Boolean =
        sessions.values.any { it.packageName == packageName && it.deferredRetryPending }

    private fun isInstallerActive(session: Session): Boolean = session.state.getInt("status") in INSTALLER_ACTIVE_STATUSES ||
            (session.useSplitCompat && session.state.getInt("status") == STATUS_DOWNLOADED && session.installerSessionId != -1 && !session.completionBlocked)

    private fun isActive(session: Session): Boolean = session.state.getInt("status") in ACTIVE_STATUSES || isInstallerActive(session)

    fun downloadedSessions(): List<Session> = sessions.values.filter {
        it.useSplitCompat && it.state.getInt("status") == STATUS_DOWNLOADED && !it.completionBlocked && isCurrentInstallation(it)
    }

    fun downloadedFiles(session: Session): List<File> {
        val files = directory(session).listFiles().orEmpty().filter { it.extension == "apk" }
        if (files.isEmpty() || files.any { it.length() == 0L } ||
            files.sumOf { it.length() } != session.state.getLong("total_bytes_to_download")) throw SplitInstallException(-6)
        return files
    }

    fun reuseDownloadedSession(session: Session): Bundle? = synchronized(session) {
        if (!isCurrent(session) || session.state.getInt("status") != STATUS_DOWNLOADED ||
            (session.installerSessionId != -1 && !session.completionBlocked)
        ) return@synchronized null
        try {
            downloadedFiles(session)
        } catch (e: SplitInstallException) {
            return@synchronized null
        }
        val info = context.packageManager.getPackageInfo(session.packageName, 0)
        if (PackageInfoCompat.getLongVersionCode(info) != session.version) return@synchronized null
        val state = Bundle(session.state).apply { putParcelableArrayList("split_file_intents", fileIntents(session)) }
        val previous = session.clientInstallTime
        val previousInstallerId = session.installerSessionId
        val previousDeadline = session.installerResultDeadline
        val previousBlocked = session.completionBlocked
        val previousDeliveredAt = session.lastDeliveredAt
        session.clientInstallTime = info.firstInstallTime
        session.lastDeliveredAt = System.currentTimeMillis()
        // An explicit delivery supersedes the previous background installation attempt.
        session.installerSessionId = -1
        session.installerResultDeadline = 0
        if (session.clientInstallTime != previous) {
            session.completionBlocked = false
        }
        try {
            saveSession(session)
        } catch (e: Exception) {
            session.clientInstallTime = previous
            session.installerSessionId = previousInstallerId
            session.installerResultDeadline = previousDeadline
            session.completionBlocked = previousBlocked
            session.lastDeliveredAt = previousDeliveredAt
            runCatching { saveSession(session) }
                .onFailure { Log.w(TAG, "Unable to restore split session after a failed reuse") }
            throw e
        }
        state
    }

    fun startCompletion(session: Session, installerId: Int) = synchronized(session) {
        check(isCurrent(session) && session.state.getInt("status") == STATUS_DOWNLOADED && session.installerSessionId == -1 && !session.completionBlocked)
        session.installerSessionId = installerId
        try {
            saveSession(session)
        } catch (e: Exception) {
            session.installerSessionId = -1
            throw e
        }
    }

    fun finishCompletion(session: Session, error: Int = 0) = synchronized(session) {
        if (!isCurrent(session)) return@synchronized
        update(session, if (error == 0) STATUS_INSTALLED else STATUS_FAILED, notifyClient = false) { putInt("error_code", error) }
        directory(session).deleteRecursively()
        onSessionFinished(session.packageName, true)
    }

    @RequiresApi(21)
    private fun blockCompletion(session: Session, inferred: Boolean = false) {
        session.completionBlocked = true
        if (!inferred) session.installerResultDeadline = 0
        try {
            saveSession(session)
        } catch (e: SplitInstallException) {
            Log.w(TAG, "Unable to persist blocked split completion (error ${e.errorCode})")
        }
        abandonInstallerSession(session)
    }

    fun checkActiveSession(packageName: String, version: Long, requested: List<String>) {
        removeStaleSessions(packageName)
        sessions.values.firstOrNull {
            it.packageName == packageName && it.version == version && isActive(it)
        }?.let {
            throw SplitInstallException(if (it.requested.intersect(requested.toSet()).isNotEmpty()) -8 else -1)
        }
    }

    suspend fun cancelPendingDeferredInstalls(packageName: String, onCancel: (Session) -> Boolean) {
        for (session in sessions.values) {
            val job = synchronized(session) {
                if (session.packageName != packageName || !session.isDeferred || !isCurrent(session) || !isCurrentInstallation(session) ||
                    session.installerSessionId != -1 || session.state.getInt("status") !in listOf(STATUS_PENDING, STATUS_DOWNLOADING)
                ) return@synchronized null
                if (!onCancel(session)) return@synchronized null
                update(session, STATUS_CANCELING)
                session.job.also {
                    if (it != null) it.cancel()
                    else {
                        update(session, STATUS_CANCELED)
                        onSessionFinished(session.packageName, true)
                    }
                }
            }
            // Let the old writer close its installer session before accepting another request.
            job?.join()
        }
    }

    fun findDownloadedSession(
        packageName: String, version: Long, requested: List<String>, modules: List<String>,
        languages: List<String>, useSplitCompat: Boolean
    ): Session? =
        sessions.values.firstOrNull { session ->
            useSplitCompat && session.useSplitCompat && session.packageName == packageName && session.version == version &&
                    session.state.getInt("status") == STATUS_DOWNLOADED && (session.installerSessionId == -1 || session.completionBlocked) &&
                    session.state.getStringArrayList("module_names").orEmpty().toSet() == modules.toSet() &&
                    session.state.getStringArrayList("languages").orEmpty().toSet() == languages.toSet() &&
                    requested.all { it in session.requested } &&
                    directory(session).listFiles()?.sumOf { it.length() } == session.state.getLong("total_bytes_to_download")
        }

    fun createSession(
        packageName: String,
        version: Long,
        requested: List<String>,
        modules: List<String>,
        languages: List<String>,
        useSplitCompat: Boolean = false,
        uninstall: Boolean = false,
        clientInstallTime: Long,
        isDeferred: Boolean = false,
        playCoreVersion: Int = 0
    ): Session {
        val id = nextSessionId.incrementAndGet()
        val state = newState(id, modules, languages).apply { if (uninstall) putBoolean("uninstall", true) }
        val session = Session(
            packageName, version, requested, state, useSplitCompat = useSplitCompat,
            isDeferred = isDeferred, playCoreVersion = playCoreVersion
        )
        session.clientInstallTime = clientInstallTime
        sessions[id] = session
        try {
            saveSession(session)
        } catch (e: Exception) {
            sessions.remove(id)
            savedSessions.edit(commit = true) { remove(id.toString()) }
            throw e
        }
        return session
    }

    internal fun failInstall(sessionId: Int) {
        sessions[sessionId]?.let { update(it, STATUS_FAILED) { putInt("error_code", ERROR_INTERNAL) } }
    }

    fun fileIntents(session: Session): ArrayList<Intent> = ArrayList(
        directory(session).listFiles().orEmpty().filter { it.extension == "apk" }.map { file ->
            val splitId = file.nameWithoutExtension
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.splitinstall.files", file)
            context.grantUriPermission(session.packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putExtra("split_id", splitId)
                putExtra("module_name", if (splitId.startsWith("config.")) "" else splitId.substringBefore(".config."))
                putExtra("version_code", session.version.toInt())
            }
        })

    private fun confirmation(session: Session): PendingIntent? = session.confirmationIntent?.let { uri ->
        PendingIntentCompat.getActivity(
            context, session.state.getInt("session_id"),
            Intent.parseUri(uri, Intent.URI_INTENT_SCHEME), PendingIntent.FLAG_UPDATE_CURRENT, false
        )
    }

    @RequiresApi(21)
    internal fun onInstallerResult(intent: Intent) {
        val session = sessions[intent.getIntExtra(SessionResultReceiver.KEY_SPLIT_SESSION_ID, -1)] ?: return
        synchronized(session) {
            if (sessions[session.state.getInt("session_id")] !== session) return
            val installerId = intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1)
            if (session.installerSessionId != installerId || session.packageName != intent.getStringExtra(SessionResultReceiver.KEY_PACKAGE_NAME)) return
            val notifyId = intent.getIntExtra(SessionResultReceiver.KEY_NOTIFY_ID, -1)
            if (!isCurrentInstallation(session)) {
                if (!session.useSplitCompat || session.state.getInt("status") != STATUS_DOWNLOADED) removeSession(session)
                return
            }
            if (session.useSplitCompat) {
                val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)
                if (status == PackageInstaller.STATUS_SUCCESS) {
                    finishCompletion(session)
                    return
                }
                if (session.state.getInt("status") != STATUS_DOWNLOADED) return
                when (status) {
                    // Background completion must never ask the user to install the same module again.
                    PackageInstaller.STATUS_PENDING_USER_ACTION -> blockCompletion(session)
                    PackageInstaller.STATUS_FAILURE_ABORTED -> if (!session.completionBlocked || session.installerResultDeadline != 0L) {
                        finishCompletion(session, ERROR_INTERNAL)
                    }

                    else -> finishCompletion(
                        session,
                        if (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1) == PackageInstaller.STATUS_FAILURE_STORAGE) -10 else ERROR_INTERNAL
                    )
                }
                return
            }
            if (session.state.getInt("status") == STATUS_INSTALLED) {
                cancelNotification(notifyId)
                return
            }
            if (session.deferredRetryPending && intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1) != PackageInstaller.STATUS_SUCCESS) {
                blockDeferredSession(session)
                return
            }
            if (session.state.getInt("status") == STATUS_CANCELED &&
                intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1) == PackageInstaller.STATUS_FAILURE_ABORTED
            ) {
                update(session, STATUS_CANCELED, notifyClient = false) { putInt("error_code", 0) }
                cancelNotification(notifyId)
                return
            }
            when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)) {
                PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                    if (session.state.getInt("status") in listOf(STATUS_CANCELED, STATUS_CANCELING, STATUS_FAILED)) return
                    if (session.isDeferred) {
                        blockDeferredSession(session)
                        return
                    }
                    val action = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT) ?: return
                    action.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                    session.confirmationIntent = action.toUri(Intent.URI_INTENT_SCHEME)
                    val pendingIntent = confirmation(session) ?: return
                    try {
                        update(session, STATUS_REQUIRES_USER_CONFIRMATION) { putParcelable("user_confirmation_intent", pendingIntent) }
                    } catch (e: SplitInstallException) {
                        update(session, STATUS_FAILED) { putInt("error_code", e.errorCode) }
                        abandonInstallerSession(session)
                        return@synchronized
                    }
                    val cancelIntent = PendingIntentCompat.getBroadcast(
                        context, installerId,
                        Intent(context, InstallReceiver::class.java).setAction(VENDING_INSTALL_DELETE_ACTION).putExtra(SESSION_ID, installerId),
                        PendingIntent.FLAG_UPDATE_CURRENT, false
                    )!!
                    context.notifyInstallPrompt(session.packageName, notifyId, pendingIntent, cancelIntent)
                    return
                }

                PackageInstaller.STATUS_SUCCESS -> {
                    session.deferredRetryPending = false
                    update(session, STATUS_INSTALLED) { putInt("error_code", 0) }
                }

                PackageInstaller.STATUS_FAILURE_ABORTED -> if (session.state.getInt("status") != STATUS_FAILED || session.installerResultDeadline != 0L) {
                    update(session, STATUS_CANCELED) { putInt("error_code", 0) }
                }

                else -> update(session, STATUS_FAILED) {
                    putInt("error_code", if (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1) == PackageInstaller.STATUS_FAILURE_STORAGE) -10 else ERROR_INTERNAL)
                }
            }
            cancelNotification(notifyId)
            // A running worker will schedule pending work after cancellation cleanup.
            if (session.state.getInt("status") == STATUS_CANCELED && session.job != null) return
        }
        onSessionFinished(session.packageName, !session.state.getBoolean("uninstall"))
    }

    @RequiresApi(21)
    private fun blockDeferredSession(session: Session) {
        val wasCanceled = session.state.getInt("status") == STATUS_CANCELED
        val wasActive = isActive(session)
        session.deferredRetryPending = true
        try {
            if (!wasCanceled) update(session, STATUS_REQUIRES_USER_CONFIRMATION, notifyClient = false)
            if (!onDeferredInstallBlocked(session)) {
                removeSession(session)
                return
            }
            session.deferredRetryPending = false
        } catch (e: Exception) {
            Log.w(TAG, "Unable to pause deferred split installation (${e.javaClass.simpleName})")
        } finally {
            try {
                if (isCurrentInstallation(session)) update(session, STATUS_CANCELED, notifyClient = !wasCanceled)
                else removeSession(session)
            } finally {
                abandonInstallerSession(session)
            }
        }
        if ((wasActive || !session.deferredRetryPending) && session.job == null) onSessionFinished(session.packageName, true)
    }

    internal fun restoreSessions(onRestored: () -> Unit) = synchronized(sessions) {
        if (SDK_INT < 21) return@synchronized
        val notificationsToCancel = mutableSetOf<Int>()
        try {
            restoreSessions(onRestored, notificationsToCancel)
        } finally {
            cancelRestoredNotifications(notificationsToCancel)
        }
    }

    @RequiresApi(21)
    private fun restoreSessions(onRestored: () -> Unit, notificationsToCancel: MutableSet<Int>) {
        if (restored) {
            sessions.values.forEach {
                if (installedVersion(it.packageName) != it.version ||
                    ((!it.useSplitCompat || it.state.getInt("status") != STATUS_DOWNLOADED) && !isCurrentInstallation(it))
                ) removeSession(it, notificationsToCancel)
                else if (it.useSplitCompat && it.state.getInt("status") == STATUS_DOWNLOADED) restoreDownloadedSession(it, notificationsToCancel)
                else restoreInstallerSession(it, notificationsToCancel = notificationsToCancel)
            }
            return
        }
        nextSessionId.set(maxOf(nextSessionId.get(), savedSessions.getInt("next-session-id", 0)))
        for ((key, value) in savedSessions.all) {
            val id = key.toIntOrNull() ?: continue
            var restoredSession: Session? = null
            try {
                val json = JSONObject(value as String)
                val state = newState(id, json.strings("module_names"), json.strings("languages"))
                for (name in listOf("status", "error_code")) state.putInt(name, json.getInt(name))
                for (name in listOf("bytes_downloaded", "total_bytes_to_download")) state.putLong(name, json.getLong(name))
                state.putBoolean("uninstall", json.optBoolean("uninstall"))
                val session = Session(
                    json.getString("package_name"), json.getLong("version"), json.strings("requested"), state,
                    json.getLong("created_at"), json.optBoolean("use_splitcompat", state.getInt("status") == STATUS_DOWNLOADED),
                    json.optBoolean("deferred"), json.optInt("playcore_version_code")
                )
                restoredSession = session
                session.installerSessionId = json.optInt("installer_session_id", -1)
                session.installerResultDeadline = json.optLong("installer_result_deadline")
                session.lastDeliveredAt = json.optLong("last_delivered_at", session.createdAt)
                session.completionBlocked = json.optBoolean("completion_blocked")
                session.deferredRetryPending = json.optBoolean("deferred_retry_pending")
                session.confirmationIntent = json.optString("confirmation_intent").takeIf { it.isNotEmpty() }
                sessions[id] = session
                nextSessionId.set(maxOf(nextSessionId.get(), id))
                if (installedVersion(session.packageName) != session.version) {
                    removeSession(session, notificationsToCancel)
                    continue
                }
                val installTime = context.packageManager.getPackageInfo(session.packageName, 0).firstInstallTime
                session.clientInstallTime = json.optLong("client_install_time", if (installTime <= session.createdAt) installTime else 0L)
                if (session.clientInstallTime != installTime && (!session.useSplitCompat || state.getInt("status") != STATUS_DOWNLOADED)) {
                    removeSession(session, notificationsToCancel)
                    continue
                }
                if (session.deferredRetryPending) {
                    blockDeferredSession(session)
                    continue
                }
                when (state.getInt("status")) {
                    STATUS_PENDING, STATUS_DOWNLOADING -> {
                        state.putInt("status", STATUS_PENDING)
                        saveSession(session)
                        SplitInstallTaskService.enqueueInstall(context, id, session.packageName)
                    }

                    STATUS_DOWNLOADED -> {
                        restoreDownloadedSession(session, notificationsToCancel, restoring = true)
                    }

                    STATUS_INSTALLING, STATUS_REQUIRES_USER_CONFIRMATION ->
                        restoreInstallerSession(session, restoring = true, notificationsToCancel = notificationsToCancel)

                    STATUS_CANCELING, STATUS_CANCELED, STATUS_FAILED -> {
                        abandonInstallerSession(session, notificationsToCancel)
                        if (state.getInt("status") == STATUS_CANCELING) update(session, STATUS_CANCELED)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Unable to restore split session $id (${e.javaClass.simpleName})")
                val session = restoredSession
                if (session == null) {
                    savedSessions.edit(commit = true) { remove(key) }
                } else {
                    update(session, STATUS_FAILED, notifyClient = !session.useSplitCompat || session.state.getInt("status") != STATUS_DOWNLOADED) {
                        putInt("error_code", (e as? SplitInstallException)?.errorCode ?: ERROR_INTERNAL)
                    }
                }
            }
        }
        restored = true
        removeExpiredSessions(notificationsToCancel)
        onRestored()
    }

    @RequiresApi(21)
    private fun restoreDownloadedSession(session: Session, notificationsToCancel: MutableSet<Int>, restoring: Boolean = false) = synchronized(session) {
        val info = installedPackage(session.packageName)
        if (info == null || PackageInfoCompat.getLongVersionCode(info) != session.version) {
            removeSession(session, notificationsToCancel)
            return@synchronized
        }
        if (session.clientInstallTime != info.firstInstallTime) {
            if (session.installerSessionId != -1 && !session.completionBlocked) blockCompletion(session)
            return@synchronized
        }
        if (session.requested.all { it in info.splitNames.orEmpty() } && (session.installerSessionId == -1 || session.completionBlocked)) {
            finishCompletion(session)
        } else {
            if (session.installerSessionId != -1 && !session.completionBlocked) {
                restoreInstallerSession(session, restoring = restoring, notificationsToCancel = notificationsToCancel)
                if (isInstallerActive(session)) return@synchronized
            }
            if (restoring && session.state.getInt("status") == STATUS_DOWNLOADED) {
                try {
                    downloadedFiles(session)
                    session.state = Bundle(session.state).apply { putParcelableArrayList("split_file_intents", fileIntents(session)) }
                } catch (e: SplitInstallException) {
                    finishCompletion(session, e.errorCode)
                    abandonInstallerSession(session)
                }
            }
        }
    }

    private fun cancelRestoredNotifications(notificationIds: Set<Int>) = synchronized(notificationLock) {
        val pending = savedSessions.all.filterKeys { it.startsWith(NOTIFICATION_CLEANUP_PREFIX) }.mapNotNull { (key, value) ->
            key.removePrefix(NOTIFICATION_CLEANUP_PREFIX).toIntOrNull()?.let { it to (value == true) }
        }.toMap().filterKeys { sessions[it]?.state?.getInt("status") !in ACTIVE_STATUSES }
        val candidates = notificationIds + pending.keys
        if (candidates.isEmpty()) return@synchronized
        val activeIds = if (SDK_INT >= 23) {
            try {
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.activeNotifications.filter { it.packageName == context.packageName && it.tag == null && it.id in candidates }
                    .map { it.id }.toSet()
            } catch (e: RuntimeException) {
                Log.w(TAG, "Unable to query split notifications (${e.javaClass.simpleName})")
                candidates
            }
        } else candidates
        val toCancel = activeIds + pending.filterValues { it }.keys
        toCancel.forEach { cancelNotification(it) }
        val absent = pending.keys - toCancel
        if (absent.isNotEmpty() && !savedSessions.edit().apply {
                absent.forEach { remove("$NOTIFICATION_CLEANUP_PREFIX$it") }
            }.commit()) {
            Log.w(TAG, "Unable to persist split notification cleanup")
        }
    }

    private fun cancelNotification(id: Int) = synchronized(notificationLock) {
        val key = "$NOTIFICATION_CLEANUP_PREFIX$id"
        try {
            NotificationManagerCompat.from(context).cancel(id)
        } catch (e: RuntimeException) {
            Log.w(TAG, "Unable to cancel split notification $id (${e.javaClass.simpleName})")
            if (!savedSessions.edit().putBoolean(key, true).commit()) {
                Log.w(TAG, "Unable to persist split notification cleanup")
            }
            return@synchronized
        }
        if (savedSessions.contains(key) && !savedSessions.edit().remove(key).commit()) {
            Log.w(TAG, "Unable to persist split notification cleanup")
        }
    }

    @RequiresApi(21)
    private fun restoreInstallerSession(
        session: Session,
        expiredDeadline: Long = 0,
        restoring: Boolean = false,
        notificationsToCancel: MutableSet<Int>? = null
    ) = synchronized(session) {
        if (sessions[session.state.getInt("session_id")] !== session) return@synchronized
        if (!isInstallerActive(session) && !session.deferredRetryPending) return@synchronized
        try {
            if (!isCurrentInstallation(session)) {
                removeSession(session, notificationsToCancel)
                return@synchronized
            }
            if (session.deferredRetryPending || (session.isDeferred && session.state.getInt("status") == STATUS_REQUIRES_USER_CONFIRMATION)) {
                blockDeferredSession(session)
                return@synchronized
            }
            if (session.state.getInt("status") == STATUS_REQUIRES_USER_CONFIRMATION && session.job == null &&
                session.createdAt < System.currentTimeMillis() - SESSION_RETENTION_MILLIS
            ) {
                // Unanswered confirmations must not outlive the session retention period.
                val info = context.packageManager.getPackageInfo(session.packageName, 0)
                val completed = if (session.state.getBoolean("uninstall")) session.requested.none { it in info.splitNames.orEmpty() }
                else session.requested.all { it in info.splitNames.orEmpty() }
                update(session, if (completed) STATUS_INSTALLED else STATUS_CANCELED)
                abandonInstallerSession(session, notificationsToCancel)
                onSessionFinished(session.packageName, !session.state.getBoolean("uninstall"))
                return@synchronized
            }
            val installer = context.packageManager.packageInstaller
            val info = installer.getSessionInfo(session.installerSessionId)
            val isSealed = info?.let(::isInstallerSessionSealed)
            if (info != null && isSealed != false) {
                if (isSealed == null && session.job == null) {
                    if (session.installerStateRecoveryId != info.sessionId || session.installerResultDeadline != 0L) {
                        if (session.installerStateRecoveryId != info.sessionId) {
                            Log.w(TAG, "Unable to determine split installer submission state; scheduling a recheck")
                        }
                        try {
                            session.installerStateRecoveryId = -1
                            val recoveryPending = deferInstallerRecovery(session, expiredDeadline, restoring, throwOnSchedulingFailure = true)
                            // Only a successfully scheduled or completed recheck consumes this attempt.
                            session.installerStateRecoveryId = info.sessionId
                            if (!recoveryPending) {
                                session.installerResultDeadline = 0
                                saveSession(session)
                                Log.w(TAG, "Split installer submission state remains unknown; keeping the native session")
                            }
                        } catch (e: Exception) {
                            session.installerResultDeadline = 0
                            Log.w(TAG, "Unable to recheck split installer submission state (${e.javaClass.simpleName})")
                        }
                    }
                } else if (session.installerResultDeadline != 0L) {
                    session.installerResultDeadline = 0
                    saveSession(session)
                }
                if (restoring) {
                    confirmation(session)?.let { pendingIntent ->
                        session.state = Bundle(session.state).apply { putParcelable("user_confirmation_intent", pendingIntent) }
                    }
                }
                return@synchronized
            }
            // A local installer may still be preparing to commit this session.
            if (info != null && session.job != null) return@synchronized
            if (info != null) installer.openSession(info.sessionId).use { it.abandon() }
            val installed = context.packageManager.getPackageInfo(session.packageName, 0)
            val names = installed.splitNames.orEmpty().toSet()
            val completed = info == null && PackageInfoCompat.getLongVersionCode(installed) == session.version &&
                    if (session.state.getBoolean("uninstall")) session.requested.none { it in names } else session.requested.all { it in names }
            if (info == null && session.installerSessionId != -1 && !completed) {
                if (deferInstallerRecovery(session, expiredDeadline, restoring)) return@synchronized
                if (session.useSplitCompat) blockCompletion(session, inferred = true)
                else update(session, STATUS_FAILED, inferredFailure = true) { putInt("error_code", ERROR_INTERNAL) }
            } else {
                update(session, if (completed) STATUS_INSTALLED else STATUS_FAILED, notifyClient = !session.useSplitCompat) {
                    if (!completed) putInt("error_code", ERROR_INTERNAL)
                }
            }
            if (session.useSplitCompat && session.state.getInt("status") != STATUS_DOWNLOADED) directory(session).deleteRecursively()
            onSessionFinished(session.packageName, !session.state.getBoolean("uninstall"))
        } catch (e: Exception) {
            Log.w(TAG, "Unable to restore split installer session (${e.javaClass.simpleName})")
            update(session, STATUS_FAILED, inferredFailure = session.installerResultDeadline != 0L, notifyClient = !session.useSplitCompat) {
                putInt("error_code", (e as? SplitInstallException)?.errorCode ?: ERROR_INTERNAL)
            }
        }
    }

    @RequiresApi(21)
    @SuppressLint("PrivateApi")
    private fun isInstallerSessionSealed(info: PackageInstaller.SessionInfo): Boolean? {
        if (SDK_INT >= 26) return info.isSealed
        return try {
            PackageInstaller.SessionInfo::class.java.getField("sealed").getBoolean(info)
        } catch (_: Exception) {
            null
        }
    }

    @RequiresApi(21)
    private fun deferInstallerRecovery(
        session: Session, expiredDeadline: Long, restoring: Boolean, throwOnSchedulingFailure: Boolean = false
    ): Boolean {
        val scheduleRecovery = session.installerResultDeadline == 0L || restoring
        if (session.installerResultDeadline == 0L) {
            session.installerResultDeadline = System.currentTimeMillis() + INSTALLER_RESULT_TIMEOUT_MILLIS
            try {
                saveSession(session)
            } catch (e: Exception) {
                session.installerResultDeadline = 0
                throw e
            }
        }
        val recoveryScheduled = try {
            if (scheduleRecovery) {
                SplitInstallDeferredService.enqueueRecovery(context, session.state.getInt("session_id"), session.packageName)
            }
            true
        } catch (e: Exception) {
            if (throwOnSchedulingFailure) throw e
            Log.w(TAG, "Unable to schedule split session recovery (${e.javaClass.simpleName})")
            false
        }
        return recoveryScheduled && session.installerResultDeadline != expiredDeadline &&
                System.currentTimeMillis() < session.installerResultDeadline
    }

    internal suspend fun recoverSession(sessionId: Int) {
        if (SDK_INT < 21) return
        val session = sessions[sessionId] ?: return
        val deadline = synchronized(session) {
            if (!isInstallerActive(session)) return
            session.installerResultDeadline
        }
        if (deadline == 0L) return
        var remaining = (deadline - System.currentTimeMillis()).coerceIn(0, INSTALLER_RESULT_TIMEOUT_MILLIS)
        val waitUntil = SystemClock.elapsedRealtime() + remaining
        while (remaining > 0) {
            delay(minOf(remaining, 1_000L))
            if (synchronized(session) { session.installerResultDeadline != deadline || !isInstallerActive(session) }) return
            remaining = waitUntil - SystemClock.elapsedRealtime()
        }
        restoreInstallerSession(session, deadline)
    }

    private fun JSONObject.strings(key: String): List<String> {
        val values = getJSONArray(key)
        return (0 until values.length()).map { values.getString(it) }
    }

    private fun saveSession(session: Session, notificationCleanup: Boolean = false) = synchronized(session) {
        val state = session.state
        if (sessions[state.getInt("session_id")] !== session) return@synchronized
        val json = JSONObject().put("package_name", session.packageName).put("version", session.version)
            .put("requested", JSONArray(session.requested)).put("created_at", session.createdAt)
            .put("last_delivered_at", session.lastDeliveredAt)
            .put("use_splitcompat", session.useSplitCompat)
            .put("deferred", session.isDeferred).put("playcore_version_code", session.playCoreVersion)
            .put("completion_blocked", session.completionBlocked)
            .put("deferred_retry_pending", session.deferredRetryPending)
            .put("client_install_time", session.clientInstallTime)
            .put("module_names", JSONArray(state.getStringArrayList("module_names").orEmpty()))
            .put("languages", JSONArray(state.getStringArrayList("languages").orEmpty()))
            .put("installer_session_id", session.installerSessionId).put("confirmation_intent", session.confirmationIntent)
            .put("installer_result_deadline", session.installerResultDeadline)
            .put("uninstall", state.getBoolean("uninstall"))
        for (name in listOf("session_id", "status", "error_code")) json.put(name, state.getInt(name))
        for (name in listOf("bytes_downloaded", "total_bytes_to_download")) json.put(name, state.getLong(name))
        synchronized(notificationLock) {
            val editor = savedSessions.edit().putInt("next-session-id", nextSessionId.get()).putString(state.getInt("session_id").toString(), json.toString())
            if (notificationCleanup) editor.putBoolean("$NOTIFICATION_CLEANUP_PREFIX${state.getInt("session_id")}", true)
            if (!editor.commit()) throw SplitInstallException(-10)
        }
    }

    private fun directory(session: Session) = File(context.cacheDir, "split-install/${session.packageName}/${session.version}/${session.state.getInt("session_id")}")

    private fun clientState(session: Session): Bundle = synchronized(session) {
        Bundle(session.state).apply {
            if (getInt("status") == STATUS_DOWNLOADED) {
                downloadedFiles(session)
                putParcelableArrayList("split_file_intents", fileIntents(session))
            }
        }
    }

    internal fun getSessionState(packageName: String, sessionId: Int): Bundle = clientState(findSession(packageName, sessionId))

    internal fun getSessionStates(packageName: String): List<Bundle> {
        val info = installedPackage(packageName) ?: return emptyList()
        val version = PackageInfoCompat.getLongVersionCode(info)
        return sessions.values.filter {
            it.packageName == packageName && it.version == version && !it.state.getBoolean("uninstall") &&
                    it.clientInstallTime == info.firstInstallTime
        }.map { clientState(it) }
    }

    internal fun cancelInstall(packageName: String, sessionId: Int) {
        if (SDK_INT < 21) throw SplitInstallException(ERROR_API_NOT_AVAILABLE)
        val session = findSession(packageName, sessionId)
        synchronized(session) {
            if (session.state.getInt("status") == STATUS_CANCELED) {
                abandonInstallerSession(session)
                return
            }
            if (session.state.getInt("status") !in listOf(STATUS_PENDING, STATUS_DOWNLOADING, STATUS_REQUIRES_USER_CONFIRMATION)) throw SplitInstallException(ERROR_INVALID_REQUEST)
            update(session, STATUS_CANCELING)
            val job = session.job
            job?.cancel() ?: update(session, STATUS_CANCELED)
            abandonInstallerSession(session)
            if (job == null) onSessionFinished(session.packageName, !session.state.getBoolean("uninstall"))
        }
    }

    @RequiresApi(21)
    private fun abandonInstallerSession(session: Session, notificationsToCancel: MutableSet<Int>? = null) {
        if (session.installerSessionId != -1) runCatching {
            val installer = context.packageManager.packageInstaller
            if (installer.getSessionInfo(session.installerSessionId)?.appPackageName == session.packageName) {
                installer.openSession(session.installerSessionId).use { it.abandon() }
            }
        }.onFailure { Log.w(TAG, "Unable to abandon split installer session (${it.javaClass.simpleName})") }
        val id = session.state.getInt("session_id")
        if (notificationsToCancel != null && session.state.getInt("status") !in ACTIVE_STATUSES) notificationsToCancel.add(id)
        else cancelNotification(id)
    }

    private fun findSession(packageName: String, sessionId: Int): Session {
        val info = installedPackage(packageName) ?: throw SplitInstallException(ERROR_SESSION_NOT_FOUND)
        return sessions[sessionId]?.takeIf {
            it.packageName == packageName && it.version == PackageInfoCompat.getLongVersionCode(info) && !it.state.getBoolean("uninstall") &&
                    it.clientInstallTime == info.firstInstallTime
        } ?: throw SplitInstallException(ERROR_SESSION_NOT_FOUND)
    }

    private fun installedVersion(packageName: String): Long? = installedPackage(packageName)?.let { PackageInfoCompat.getLongVersionCode(it) }

    private fun installedPackage(packageName: String): PackageInfo? = try {
        context.packageManager.getPackageInfo(packageName, 0)
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }

    fun update(session: Session, status: Int, inferredFailure: Boolean = false, notifyClient: Boolean = true, edit: Bundle.() -> Unit = {}) {
        synchronized(session) {
            if (sessions[session.state.getInt("session_id")] !== session) return
            if (status == STATUS_FAILED && session.state.getInt("status") in listOf(STATUS_INSTALLED, STATUS_CANCELED, STATUS_CANCELING)) return
            if (session.state.getInt("status") == STATUS_CANCELING && status in listOf(STATUS_DOWNLOADING, STATUS_INSTALLING, STATUS_DOWNLOADED)) return
            if (!inferredFailure && status in listOf(STATUS_INSTALLED, STATUS_FAILED, STATUS_CANCELED)) session.installerResultDeadline = 0
            val previousState = session.state
            session.state = Bundle(session.state).apply {
                putInt("status", status)
                edit()
                if (status != STATUS_DOWNLOADED) remove("split_file_intents")
                if (status != STATUS_REQUIRES_USER_CONFIRMATION) remove("user_confirmation_intent")
            }
            try {
                saveSession(session, notificationCleanup = status >= STATUS_DOWNLOADED)
            } catch (e: SplitInstallException) {
                if (status !in listOf(STATUS_INSTALLED, STATUS_FAILED, STATUS_CANCELED)) {
                    session.state = previousState
                    runCatching { saveSession(session) }
                        .onFailure { Log.w(TAG, "Unable to restore split session after a failed save (error ${e.errorCode})") }
                    throw e
                }
                Log.w(TAG, "Unable to persist split session ${session.state.getInt("session_id")} (error ${e.errorCode})")
            }
            Log.d(TAG, "Session ${session.state.getInt("session_id")} for ${session.packageName}: status=$status")
            if (notifyClient) sendUpdate(session.packageName, session.state)
            if (status == STATUS_DOWNLOADING) {
                context.notifySplitInstallProgress(
                    session.packageName, session.state.getInt("session_id"),
                    Downloading(session.state.getLong("bytes_downloaded"), session.state.getLong("total_bytes_to_download"))
                )
            } else if (status >= STATUS_DOWNLOADED &&
                (status != STATUS_REQUIRES_USER_CONFIRMATION || previousState.getInt("status") != status)
            ) {
                // Keep repeated confirmation updates from alerting the user again.
                cancelNotification(session.state.getInt("session_id"))
            }
        }
    }

    fun sendUpdate(packageName: String, state: Bundle) {
        if (state.getBoolean("uninstall")) return
        context.sendBroadcast(Intent(ACTION_UPDATE_SERVICE).apply {
            setPackage(packageName)
            putExtra("session_state", Bundle(state))
            addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        })
    }

    fun newState(id: Int, modules: List<String>, languages: List<String>) = Bundle().apply {
        putInt("session_id", id)
        putInt("status", STATUS_PENDING)
        putInt("error_code", 0)
        putLong("bytes_downloaded", 0)
        putLong("total_bytes_to_download", 0)
        putStringArrayList("module_names", ArrayList(modules))
        putStringArrayList("languages", ArrayList(languages))
    }

    fun removeExpiredSessions() {
        val notificationsToCancel = mutableSetOf<Int>()
        try {
            removeExpiredSessions(notificationsToCancel)
        } finally {
            cancelRestoredNotifications(notificationsToCancel)
        }
    }

    private fun removeExpiredSessions(notificationsToCancel: MutableSet<Int>) {
        val cutoff = System.currentTimeMillis() - SESSION_RETENTION_MILLIS
        for (session in sessions.values) {
            synchronized(session) {
                if (SDK_INT >= 21 && session.job == null && session.state.getInt("status") == STATUS_REQUIRES_USER_CONFIRMATION && session.createdAt < cutoff) {
                    restoreInstallerSession(session, notificationsToCancel = notificationsToCancel)
                }
                if (session.job == null && !isActive(session) &&
                    maxOf(session.createdAt, session.lastDeliveredAt, session.installerResultDeadline) < cutoff
                ) {
                    if (!session.deferredRetryPending) removeSession(session, notificationsToCancel)
                    else if (session.state.getInt("status") == STATUS_CANCELED) directory(session).deleteRecursively()
                }
            }
        }
        File(context.cacheDir, "split-install").listFiles().orEmpty().forEach { packageDir ->
            packageDir.listFiles().orEmpty().forEach { versionDir ->
                versionDir.listFiles().orEmpty().filter { it.isDirectory && it.lastModified() < cutoff && it.name.toIntOrNull() !in sessions.keys }
                    .forEach { it.deleteRecursively() }
            }
        }
    }

    fun removeSession(session: Session, notificationsToCancel: MutableSet<Int>? = null) = synchronized(session) {
        val id = session.state.getInt("session_id")
        if (!sessions.remove(id, session)) return@synchronized
        session.job?.cancel()
        synchronized(notificationLock) {
            val key = "$NOTIFICATION_CLEANUP_PREFIX$id"
            val direct = savedSessions.getBoolean(key, false) || session.state.getInt("status") in ACTIVE_STATUSES
            if (!savedSessions.edit().remove(id.toString()).putBoolean(key, direct).commit()) {
                Log.w(TAG, "Unable to persist split session removal")
            }
        }
        if (SDK_INT >= 21) abandonInstallerSession(session, notificationsToCancel)
        session.state.getParcelableArrayList<Intent>("split_file_intents")?.forEach { intent ->
            intent.data?.let { context.revokeUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        }
        directory(session).deleteRecursively()
    }

    class Session(
        val packageName: String, val version: Long, val requested: List<String>, @Volatile var state: Bundle,
        val createdAt: Long = System.currentTimeMillis(), val useSplitCompat: Boolean = false,
        val isDeferred: Boolean = false, val playCoreVersion: Int = 0
    ) {
        @Volatile
        var job: Job? = null
        @Volatile
        var installerSessionId = -1
        @Volatile
        var installerResultDeadline = 0L
        var installerStateRecoveryId = -1
        @Volatile
        var completionBlocked = false
        @Volatile
        var deferredRetryPending = false
        @Volatile
        var clientInstallTime = 0L
        @Volatile
        var lastDeliveredAt = createdAt
        var confirmationIntent: String? = null
    }

    companion object {
        private val notificationLock = Any()
        private val nextSessionId = AtomicInteger((System.currentTimeMillis() / 1000).toInt() and 0x3fffffff)
        private val sessions = ConcurrentHashMap<Int, Session>()
        private var restored = false
    }
}
