/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.finsky.splitinstallservice

import android.accounts.AccountManager
import android.accounts.AuthenticatorException
import android.accounts.OperationCanceledException
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.PowerManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.pm.PackageInfoCompat
import com.android.vending.VendingPreferences
import com.android.vending.installer.canKeepAppAliveDuringFullInstall
import com.android.vending.installer.installPackages
import com.google.android.finsky.syncDeviceInfo
import io.ktor.client.plugins.ResponseException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.microg.vending.billing.AuthManager
import org.microg.vending.billing.DEFAULT_ACCOUNT_TYPE
import org.microg.vending.billing.core.HttpClient
import org.microg.vending.delivery.DeliveryException
import org.microg.vending.delivery.proto.DeliveryStatus
import org.microg.vending.delivery.requestDownloadUrls
import org.microg.vending.splitinstall.STATUS_CANCELED
import org.microg.vending.splitinstall.STATUS_CANCELING
import org.microg.vending.splitinstall.STATUS_DOWNLOADED
import org.microg.vending.splitinstall.STATUS_DOWNLOADING
import org.microg.vending.splitinstall.STATUS_FAILED
import org.microg.vending.splitinstall.STATUS_INSTALLED
import org.microg.vending.splitinstall.STATUS_INSTALLING
import org.microg.vending.splitinstall.STATUS_PENDING
import org.microg.vending.splitinstall.SplitInstallSessionManager
import org.microg.vending.splitinstall.SplitInstallCompletionService
import org.microg.vending.splitinstall.SplitInstallSessionManager.Session
import org.microg.vending.splitinstall.SplitInstallTaskService
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.IOException
import java.util.Locale
import androidx.core.content.edit

private const val TAG = "SplitInstallManager"
private const val MIN_SPLITCOMPAT_VERSION = 10300

internal const val ERROR_API_NOT_AVAILABLE = -5
internal const val ERROR_INVALID_REQUEST = -3
internal const val ERROR_SESSION_NOT_FOUND = -4
internal const val ERROR_INTERNAL = -100

internal class SplitInstallException(val errorCode: Int) : Exception()

class SplitInstallManager(private val context: Context) {
    private val httpClient = HttpClient()
    private val removals = context.getSharedPreferences("split-install-removals", Context.MODE_PRIVATE)
    private val sessionManager = SplitInstallSessionManager(context, ::enqueuePending, ::blockDeferredInstall)
    private val deferred = context.getSharedPreferences("split-install-deferred", Context.MODE_PRIVATE)

    internal suspend fun startInstall(
        packageName: String, splits: List<Bundle>, playCoreVersion: Int = 0,
        deferredRequest: DeferredRequest? = null, onStarted: (Int, Bundle) -> Unit
    ) {
        if (SDK_INT < 21) throw SplitInstallException(ERROR_API_NOT_AVAILABLE)
        validateRequest(splits)
        val modules = splits.mapNotNull { it.getString("module_name") }.distinct()
        val languages = splits.mapNotNull { it.getString("language") }.distinct()
        requestMutex.withLock {
            sessionManager.removeStaleSessions(packageName)
            val packageInfo = context.packageManager.getPackageInfo(packageName, 0)
            val version = PackageInfoCompat.getLongVersionCode(packageInfo)
            if (deferredRequest != null && (deferredRequest.version != version || deferredRequest.clientInstallTime != packageInfo.firstInstallTime)) {
                throw SplitInstallException(ERROR_SESSION_NOT_FOUND)
            }
            if (deferredRequest != null && deferredRequest != readDeferredRequest(packageName)) return
            val installed = packageInfo.splitNames?.toSet().orEmpty()
            val requestedSplits = withContext(Dispatchers.IO) {
                sessionManager.removeExpiredSessions()
                modules + languageSplits(packageName, modules, installed, languages)
            }.distinct()

            fun clearPendingRequests() {
                synchronized(deferredLock) {
                    val pending = readDeferredRequest(packageName)
                    if (pending != null && pending.version == version && pending.clientInstallTime == packageInfo.firstInstallTime) {
                        val delivered = modules.map { "module_name:$it" } + languages.map { "language:$it" }
                        saveDeferredRequest(packageName, pending.copy(splits = pending.splits - delivered.toSet(), blocked = pending.blocked - delivered.toSet()))
                    }
                }
                val pendingRemoval = removals.getStringSet(packageName, emptySet()).orEmpty()
                removals.edit {
                    putStringSet(packageName, pendingRemoval.filterNot { split ->
                        split in requestedSplits || modules.any { split.startsWith("$it.config.") }
                    }.toSet())
                }
            }

            val requested = requestedSplits.filterNot { it in installed }
            if (requested.isEmpty()) {
                clearPendingRequests()
                val state = sessionManager.newState(0, modules, languages).apply { putInt("status", STATUS_INSTALLED) }
                onStarted(0, state)
                sessionManager.sendUpdate(packageName, state)
                return
            }
            // TODO: The criteria for using SplitCompat need to be defined more clearly here.
            val useSplitCompat = deferredRequest == null && playCoreVersion >= MIN_SPLITCOMPAT_VERSION && languages.isEmpty()
            val downloadedState = withContext(Dispatchers.IO) {
                sessionManager.findDownloadedSession(packageName, version, requested, modules, languages, useSplitCompat)?.let {
                    // Renew URI grants in case the app was reinstalled.
                    sessionManager.reuseDownloadedSession(it)
                }
            }
            if (downloadedState != null) {
                clearPendingRequests()
                onStarted(downloadedState.getInt("session_id"), downloadedState)
                sessionManager.sendUpdate(packageName, downloadedState)
                scheduleCompletion()
                return
            }
            sessionManager.checkActiveSession(packageName, version, requested)
            if (deferredRequest != null && sessionManager.hasPendingDeferredRetry(packageName)) return
            val newSession = sessionManager.createSession(
                packageName, version, requested, modules, languages, useSplitCompat = useSplitCompat,
                clientInstallTime = packageInfo.firstInstallTime, isDeferred = deferredRequest != null, playCoreVersion = playCoreVersion
            )
            val sessionId = newSession.state.getInt("session_id")
            try {
                clearPendingRequests()
            } catch (e: Exception) {
                sessionManager.removeSession(newSession)
                throw e
            }
            onStarted(sessionId, Bundle(newSession.state))
            sessionManager.sendUpdate(packageName, newSession.state)
            try {
                SplitInstallTaskService.enqueueInstall(context, sessionId, packageName)
            } catch (e: Exception) {
                sessionManager.update(newSession, STATUS_FAILED) { putInt("error_code", ERROR_INTERNAL) }
            }
        }
    }

    @RequiresApi(21)
    internal suspend fun install(sessionId: Int) {
        val session = sessionManager.getSession(sessionId) ?: return
        val job = currentCoroutineContext()[Job]
        synchronized(session) {
            if (!sessionManager.isCurrent(session) || session.state.getInt("status") != STATUS_PENDING || session.job != null) return
            if (!sessionManager.isCurrentInstallation(session)) {
                sessionManager.removeSession(session)
                return
            }
            if (!VendingPreferences.isSplitInstallEnabled(context)) {
                sessionManager.update(session, STATUS_FAILED) { putInt("error_code", ERROR_API_NOT_AVAILABLE) }
                return
            }
            session.job = job
        }
        val version = session.version
        val packageName = session.packageName
        val languages = session.state.getStringArrayList("languages").orEmpty()
        try {
            withContext(Dispatchers.IO) {
                if (session.state.getBoolean("uninstall")) {
                    installNative(session, emptyList())
                    return@withContext
                }
                val components = requestComponents(packageName, version, session.requested, languages)
                val total = components.sumOf { it.size }
                val directory = File(context.cacheDir, "split-install/$packageName/$version/$sessionId")
                if (!directory.mkdirs() && !directory.isDirectory) throw IOException("Cannot create split directory")
                if (total > directory.usableSpace / 2) throw SplitInstallException(-10)
                sessionManager.update(session, STATUS_DOWNLOADING) { putLong("total_bytes_to_download", total) }
                var downloaded = 0L
                var lastUpdate = 0L
                val files = components.map { component ->
                    currentCoroutineContext().ensureActive()
                    if (!component.componentName.matches(Regex("[A-Za-z0-9_.-]+"))) throw SplitInstallException(-2)
                    val file = File(directory, "${component.componentName}.apk")
                    for (attempt in 0 until 3) {
                        currentCoroutineContext().ensureActive()
                        try {
                            file.outputStream().use { output ->
                                httpClient.download(component.url, output) { bytes ->
                                    val now = System.currentTimeMillis()
                                    if (now - lastUpdate >= 250) {
                                        sessionManager.update(session, STATUS_DOWNLOADING) { putLong("bytes_downloaded", downloaded + bytes) }
                                        lastUpdate = now
                                    }
                                }
                            }
                            if (file.length() != component.size) throw IOException("Incomplete split download")
                            break
                        } catch (e: IOException) {
                            if (attempt == 2) throw e
                        }
                    }
                    downloaded += file.length()
                    file
                }
                if (!sessionManager.isCurrentInstallation(session)) {
                    sessionManager.removeSession(session)
                    return@withContext
                }
                if (!session.useSplitCompat) {
                    installNative(session, files)
                } else {
                    // The client handles the downloaded APKs; native completion is scheduled separately.
                    sessionManager.update(session, STATUS_DOWNLOADED) {
                        putLong("bytes_downloaded", total)
                        putParcelableArrayList("split_file_intents", sessionManager.fileIntents(session))
                    }
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException || !currentCoroutineContext().isActive) {
                synchronized(session) {
                    when (session.state.getInt("status")) {
                        STATUS_CANCELING -> sessionManager.update(session, STATUS_CANCELED)
                        STATUS_PENDING, STATUS_DOWNLOADING -> try {
                            sessionManager.update(session, STATUS_PENDING)
                        } catch (failure: SplitInstallException) {
                            sessionManager.update(session, STATUS_FAILED) { putInt("error_code", failure.errorCode) }
                        }

                        else -> Unit
                    }
                }
                currentCoroutineContext().ensureActive()
                throw e
            }
            val httpStatus = (e as? ResponseException)?.response?.status?.value
            val error = when {
                e is SplitInstallException -> e.errorCode
                httpStatus in listOf(400, 404, 410) -> -2
                httpStatus in listOf(401, 403) -> -15
                e is IOException || e is ResponseException -> -6
                else -> ERROR_INTERNAL
            }
            Log.w(TAG, "Split install failed for $packageName, session $sessionId, error $error, HTTP $httpStatus (${e.javaClass.simpleName})")
            sessionManager.update(session, STATUS_FAILED) { putInt("error_code", error) }
        } finally {
            synchronized(session) {
                if (session.job === job) session.job = null
                if (session.state.getInt("status") == STATUS_CANCELING) sessionManager.update(session, STATUS_CANCELED)
            }
            enqueuePending(packageName, !session.state.getBoolean("uninstall"))
        }
    }

    internal fun failInstall(sessionId: Int) = sessionManager.failInstall(sessionId)

    private suspend fun requestComponents(packageName: String, version: Long, requested: List<String>, languages: List<String>): List<PackageComponent> {
        val accounts = AccountManager.get(context).getAccountsByType(DEFAULT_ACCOUNT_TYPE)
        var errorCode = -15
        for ((index, account) in accounts.withIndex()) {
            currentCoroutineContext().ensureActive()
            val candidate = "${index + 1}/${accounts.size}"
            val auth = try {
                AuthManager.getAuthData(context, account)
            } catch (e: AuthenticatorException) {
                currentCoroutineContext().ensureActive()
                Log.w(TAG, "Split delivery account $candidate authentication failed")
                continue
            } catch (e: OperationCanceledException) {
                currentCoroutineContext().ensureActive()
                Log.w(TAG, "Split delivery account $candidate token request canceled or timed out")
                continue
            }
            currentCoroutineContext().ensureActive()
            if (auth == null || auth.authToken.isEmpty()) {
                Log.w(TAG, "Split delivery account $candidate has no auth token")
                continue
            }
            try {
                syncDeviceInfo(context, account, auth.authToken, auth.gsfId.toLong(16))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                currentCoroutineContext().ensureActive()
                Log.w(TAG, "Device sync failed for split delivery")
            }
            currentCoroutineContext().ensureActive()
            val components = try {
                httpClient.requestDownloadUrls(context, packageName, version, auth, requested, languages = languages)
            } catch (e: DeliveryException) {
                currentCoroutineContext().ensureActive()
                if (e.status != DeliveryStatus.NOT_PURCHASED) throw e
                Log.d(TAG, "Split delivery account $candidate failed with ${e.status}")
                continue
            } catch (e: ResponseException) {
                currentCoroutineContext().ensureActive()
                val httpStatus = e.response.status.value
                when (httpStatus) {
                    400, 404, 410 -> errorCode = -2
                    401, 403 -> Unit
                    else -> throw e
                }
                Log.w(TAG, "Split delivery account $candidate failed with HTTP $httpStatus")
                continue
            }
            currentCoroutineContext().ensureActive()
            val available = requested.count { name -> components.any { it.componentName == name } }
            Log.d(TAG, "Split delivery account $candidate provides $available/${requested.size} requested splits")
            if (available == requested.size) return components
            errorCode = -2
        }
        currentCoroutineContext().ensureActive()
        throw SplitInstallException(errorCode)
    }

    @RequiresApi(21)
    private suspend fun installNative(session: Session, files: List<File>, completion: Boolean = false) {
        currentCoroutineContext().ensureActive()
        val job = currentCoroutineContext()[Job]
        val info = context.packageManager.getPackageInfo(session.packageName, 0)
        val toRemove = removals.getStringSet(session.packageName, emptySet()).orEmpty().filter { it in info.splitNames.orEmpty() }
        val fullInstall = context.canKeepAppAliveDuringFullInstall()
        val components = if (fullInstall) {
            val replaced = files.map { it.nameWithoutExtension }.toSet()
            val app = info.applicationInfo ?: throw SplitInstallException(-2)
            val retained = info.splitNames.orEmpty().zip(app.splitSourceDirs.orEmpty())
                .filterNot { (name, _) -> name in replaced || name in toRemove }.map { (_, path) -> File(path) }
            files + File(app.sourceDir) + retained
        } else files
        context.installPackages(
            session.packageName, components,
            isUpdate = fullInstall, splitInstall = true, keepFiles = true,
            splitSessionId = session.state.getInt("session_id"), removedSplits = toRemove,
            onSessionCreated = { id ->
                synchronized(session) {
                    job?.ensureActive()
                    session.job?.ensureActive()
                    if (!sessionManager.isCurrent(session) || !sessionManager.isCurrentInstallation(session)) {
                        sessionManager.removeSession(session)
                        throw CancellationException("Split install target changed")
                    }
                    val expectedStatus = when {
                        completion -> STATUS_DOWNLOADED
                        session.state.getBoolean("uninstall") -> STATUS_PENDING
                        else -> STATUS_DOWNLOADING
                    }
                    check(session.state.getInt("status") == expectedStatus)
                    if (completion) {
                        sessionManager.startCompletion(session, id)
                    } else {
                        session.installerSessionId = id
                        sessionManager.update(session, STATUS_INSTALLING) { putLong("bytes_downloaded", getLong("total_bytes_to_download")) }
                    }
                }
            })
    }

    @RequiresApi(21)
    internal fun onInstallerResult(intent: Intent) = sessionManager.onInstallerResult(intent)

    @RequiresApi(21)
    internal suspend fun uninstall(packageName: String) = requestMutex.withLock {
        if (SDK_INT < 24) return
        if (sessionManager.hasActiveSession(packageName)) return
        val info = context.packageManager.getPackageInfo(packageName, 0)
        val toRemove = removals.getStringSet(packageName, emptySet()).orEmpty().filter { it in info.splitNames.orEmpty() }
        if (toRemove.isEmpty()) return
        val session = sessionManager.createSession(
            packageName, PackageInfoCompat.getLongVersionCode(info), toRemove,
            emptyList(), emptyList(), uninstall = true, clientInstallTime = info.firstInstallTime
        )
        val id = session.state.getInt("session_id")
        try {
            SplitInstallTaskService.enqueueInstall(context, id, packageName)
        } catch (e: Exception) {
            sessionManager.update(session, STATUS_FAILED) { putInt("error_code", ERROR_INTERNAL) }
        }
    }

    internal suspend fun deferredInstall(packageName: String, splits: List<Bundle>, playCoreVersion: Int, onAccepted: () -> Unit) {
        if (SDK_INT < 21) throw SplitInstallException(ERROR_API_NOT_AVAILABLE)
        if (splits.isNotEmpty()) validateRequest(splits)
        if (splits.isNotEmpty()) {
            requestMutex.withLock {
                synchronized(deferredLock) {
                    val info = context.packageManager.getPackageInfo(packageName, 0)
                    val version = PackageInfoCompat.getLongVersionCode(info)
                    val previous = readDeferredRequest(packageName)?.takeIf { it.version == version && it.clientInstallTime == info.firstInstallTime }
                    val wanted = splits.map { bundle -> bundle.getString("module_name")?.let { "module_name:$it" } ?: "language:${bundle.getString("language")}" }.toSet()
                    saveDeferredRequest(
                        packageName, DeferredRequest(
                            version, info.firstInstallTime, playCoreVersion,
                            previous?.splits.orEmpty() + wanted, previous?.blocked.orEmpty()
                        )
                    )
                }
            }
            SplitInstallTaskService.enqueueDeferred(context, packageName)
        }
        onAccepted()
    }

    internal suspend fun installDeferred(packageName: String) {
        if (sessionManager.hasActiveSession(packageName)) return
        val request = readDeferredRequest(packageName) ?: return
        val wanted = request.splits - request.blocked
        if (wanted.isEmpty()) return
        val splits = wanted.map { value -> Bundle().apply { putString(value.substringBefore(':'), value.substringAfter(':')) } }
        startInstall(packageName, splits, request.playCoreVersion, request) { _, _ -> }
    }

    private fun blockDeferredInstall(session: Session): Boolean = synchronized(deferredLock) {
        if (!sessionManager.isCurrentInstallation(session)) return@synchronized false
        val previous = readDeferredRequest(session.packageName)
        if (previous != null && (previous.version != session.version || previous.clientInstallTime != session.clientInstallTime)) return@synchronized false
        val wanted = session.state.getStringArrayList("module_names").orEmpty().map { "module_name:$it" } +
                session.state.getStringArrayList("languages").orEmpty().map { "language:$it" }
        saveDeferredRequest(
            session.packageName, DeferredRequest(
                session.version, session.clientInstallTime,
                previous?.playCoreVersion ?: session.playCoreVersion, previous?.splits.orEmpty() + wanted, previous?.blocked.orEmpty() + wanted
            )
        )
        true
    }

    private fun readDeferredRequest(packageName: String): DeferredRequest? = synchronized(deferredLock) {
        val saved = deferred.all[packageName] ?: return@synchronized null
        val request = try {
            (saved as? String)?.let { value ->
                val json = JSONObject(value)
                fun strings(name: String): Set<String> = json.optJSONArray(name)?.let { values ->
                    (0 until values.length()).map { values.getString(it) }.toSet()
                }.orEmpty()
                DeferredRequest(
                    json.getLong("version"), json.getLong("client_install_time"), json.getInt("playcore_version_code"),
                    strings("requested"), strings("blocked")
                )
            }
        } catch (e: JSONException) {
            Log.w(TAG, "Unable to read deferred split metadata")
            null
        }
        val info = try {
            context.packageManager.getPackageInfo(packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
        if (request == null || info == null || request.version != PackageInfoCompat.getLongVersionCode(info) || request.clientInstallTime != info.firstInstallTime) {
            // Requests without an installation identity cannot be assigned to a reinstalled app.
            saveDeferredRequest(packageName, null)
            return@synchronized null
        }
        request
    }

    private fun saveDeferredRequest(packageName: String, request: DeferredRequest?) = synchronized(deferredLock) {
        val previous = deferred.all[packageName]
        val editor = deferred.edit()
        if (request == null || request.splits.isEmpty()) editor.remove(packageName)
        else editor.putString(
            packageName, JSONObject().put("version", request.version).put("client_install_time", request.clientInstallTime)
                .put("playcore_version_code", request.playCoreVersion).put("requested", JSONArray(request.splits)).put("blocked", JSONArray(request.blocked)).toString()
        )
        if (!editor.commit()) {
            val rollback = deferred.edit()
            when (previous) {
                is String -> rollback.putString(packageName, previous)
                is Set<*> -> rollback.putStringSet(packageName, previous.filterIsInstance<String>().toSet())
                else -> rollback.remove(packageName)
            }
            if (!rollback.commit()) Log.w(TAG, "Unable to restore deferred split metadata")
            throw SplitInstallException(-10)
        }
    }

    private fun enqueuePending(packageName: String, includeRemovals: Boolean = true) {
        if (SDK_INT < 21) return
        scheduleCompletion()
        if (!sessionManager.hasPendingDeferredRetry(packageName)) {
            readDeferredRequest(packageName)?.let { request ->
                if ((request.splits - request.blocked).isNotEmpty()) SplitInstallTaskService.enqueueDeferred(context, packageName)
            }
        }
        if (includeRemovals && !removals.getStringSet(packageName, emptySet()).isNullOrEmpty()) SplitInstallTaskService.enqueueRemoval(context, packageName)
    }

    internal fun restoreSessions() {
        sessionManager.restoreSessions {
            for (packageName in deferred.all.keys + removals.all.keys) enqueuePending(packageName)
        }
        scheduleCompletion()
    }

    private fun scheduleCompletion() {
        if (SDK_INT < 21 || sessionManager.downloadedSessions().isEmpty()) return
        try {
            SplitInstallCompletionService.schedule(context)
        } catch (e: RuntimeException) {
            Log.w(TAG, "Unable to schedule split completion (${e.javaClass.simpleName})")
        }
    }

    @RequiresApi(21)
    internal suspend fun completeDownloadedSessions(): Boolean = requestMutex.withLock {
        if (!VendingPreferences.isSplitInstallEnabled(context) || !SplitInstallCompletionService.canInstallSilently(context)) return@withLock false
        sessionManager.removeExpiredSessions()
        val pending = sessionManager.downloadedSessions()
        if (pending.isEmpty()) return@withLock false
        val power = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        // A restricted process list cannot establish that another app is safe to update.
        if (power.isInteractive || context.packageManager.checkPermission("android.permission.REAL_GET_TASKS", context.packageName) != PackageManager.PERMISSION_GRANTED) {
            return@withLock true
        }
        val activity = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val processes = activity.runningAppProcesses ?: return@withLock true
        val job = currentCoroutineContext().job
        for (session in pending) {
            currentCoroutineContext().ensureActive()
            val pendingRemovals = removals.getStringSet(session.packageName, emptySet()).orEmpty()
            if (session.requested.any { split -> pendingRemovals.any { split == it || split.startsWith("$it.config.") } }) continue
            if (sessionManager.hasActiveSession(session.packageName) || processes.any {
                    it.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_PERCEPTIBLE &&
                            (it.processName == session.packageName || session.packageName in it.pkgList.orEmpty())
                }) continue
            val claimed = synchronized(session) {
                if (session.job != null) false
                else {
                    session.job = job
                    true
                }
            }
            if (!claimed) continue
            try {
                val info = context.packageManager.getPackageInfo(session.packageName, 0)
                if (!sessionManager.isCurrent(session) || PackageInfoCompat.getLongVersionCode(info) != session.version ||
                    session.clientInstallTime != info.firstInstallTime
                ) continue
                if (session.requested.all { it in info.splitNames.orEmpty() }) sessionManager.finishCompletion(session)
                else {
                    val files = sessionManager.downloadedFiles(session)
                    Log.d(TAG, "Completing downloaded splits for ${session.packageName}, session ${session.state.getInt("session_id")}")
                    installNative(session, files, completion = true)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Split completion failed (${e.javaClass.simpleName})")
                sessionManager.finishCompletion(session, (e as? SplitInstallException)?.errorCode ?: ERROR_INTERNAL)
            } finally {
                synchronized(session) {
                    if (session.job === job) session.job = null
                }
            }
        }
        sessionManager.downloadedSessions().isNotEmpty()
    }

    internal suspend fun recoverSession(sessionId: Int) = sessionManager.recoverSession(sessionId)

    private fun validateRequest(splits: List<Bundle>) {
        if (splits.isEmpty() || splits.any {
                val module = it.getString("module_name")
                val language = it.getString("language")
                (module == null) == (language == null) ||
                        (module != null && !module.matches(Regex("[A-Za-z0-9_]+"))) ||
                        (language != null && !language.matches(Regex("[a-zA-Z]{2,3}(-[a-zA-Z0-9]{2,8})*")))
            }) throw SplitInstallException(ERROR_INVALID_REQUEST)
    }

    internal fun getSessionState(packageName: String, sessionId: Int): Bundle = sessionManager.getSessionState(packageName, sessionId)

    internal fun getSessionStates(packageName: String): List<Bundle> = sessionManager.getSessionStates(packageName)

    internal fun cancelInstall(packageName: String, sessionId: Int) = sessionManager.cancelInstall(packageName, sessionId)

    internal suspend fun deferredUninstall(packageName: String, splits: List<Bundle>, languages: Boolean) {
        if (SDK_INT < 21) throw SplitInstallException(ERROR_API_NOT_AVAILABLE)
        if (splits.isNotEmpty()) validateRequest(splits)
        val key = if (languages) "language" else "module_name"
        val names = splits.map { it.getString(key) ?: throw SplitInstallException(ERROR_INVALID_REQUEST) }
        if (names.any { it.isBlank() } || (languages && names.isEmpty())) throw SplitInstallException(ERROR_INVALID_REQUEST)
        if (!languages && "base" in names) throw SplitInstallException(ERROR_INVALID_REQUEST)
        val installed = context.packageManager.getPackageInfo(packageName, 0).splitNames.orEmpty().toSet()
        val toRemove = if (languages) languageSplits(packageName, emptyList(), installed, names)
        else names + installed.filter { split -> names.any { split == it || split.startsWith("$it.config.") } }
        requestMutex.withLock {
            if (!removals.edit().putStringSet(packageName, removals.getStringSet(packageName, emptySet()).orEmpty() + toRemove).commit()) {
                throw SplitInstallException(-10)
            }
        }
        SplitInstallTaskService.enqueueRemoval(context, packageName)
    }

    @RequiresApi(21)
    private fun languageSplits(packageName: String, modules: List<String>, installed: Set<String>, languages: List<String>): List<String> {
        if (languages.isEmpty()) return emptyList()
        val appInfo = context.packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
        val metadataId = appInfo.metaData?.getInt("com.android.vending.splits") ?: 0
        if (metadataId == 0) return languages.map { "config.${Locale.forLanguageTag(it).language}" }
        val resources = context.packageManager.getResourcesForApplication(appInfo)
        val result = mutableListOf<String>()
        val wantedModules = modules.toSet() + installed.filterNot { it.contains('.') } + ""
        val wantedLanguages = languages.flatMap { listOf(it, Locale.forLanguageTag(it).language) }.toSet()
        resources.getXml(metadataId).use { parser ->
            var module = ""
            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType == XmlPullParser.START_TAG) {
                    if (parser.name == "module") module = parser.getAttributeValue(null, "name").orEmpty()
                    if (parser.name == "entry" && module in wantedModules && parser.getAttributeValue(null, "key") in wantedLanguages) {
                        parser.getAttributeValue(null, "split")?.let { result.add(it) }
                    }
                }
            }
        }
        if (result.isEmpty()) throw SplitInstallException(-2)
        return result
    }

    companion object {
        private val requestMutex = Mutex()
        private val deferredLock = Any()
    }

    internal data class DeferredRequest(
        val version: Long, val clientInstallTime: Long, val playCoreVersion: Int,
        val splits: Set<String>, val blocked: Set<String> = emptySet()
    )
}
