/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.finsky.splitinstallservice

import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.microg.vending.billing.AuthManager
import org.microg.vending.billing.core.HttpClient
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
import org.microg.vending.splitinstall.SplitInstallSessionManager.Session
import org.microg.vending.splitinstall.SplitInstallTaskService
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
    private val sessionManager = SplitInstallSessionManager(context, ::enqueuePending)
    private val deferred = context.getSharedPreferences("split-install-deferred", Context.MODE_PRIVATE)

    internal suspend fun startInstall(packageName: String, splits: List<Bundle>, playCoreVersion: Int = 0, onStarted: (Int, Bundle) -> Unit) {
        if (SDK_INT < 21) throw SplitInstallException(ERROR_API_NOT_AVAILABLE)
        validateRequest(splits)
        val modules = splits.mapNotNull { it.getString("module_name") }.distinct()
        val languages = splits.mapNotNull { it.getString("language") }.distinct()
        requestMutex.withLock {
            val packageInfo = context.packageManager.getPackageInfo(packageName, 0)
            val version = PackageInfoCompat.getLongVersionCode(packageInfo)
            val installed = packageInfo.splitNames?.toSet().orEmpty()
            val requestedSplits = withContext(Dispatchers.IO) {
                sessionManager.removeExpiredSessions()
                modules + languageSplits(packageName, modules, installed, languages)
            }.distinct()
            fun clearPendingRemovals() {
                val pendingRemoval = removals.getStringSet(packageName, emptySet()).orEmpty()
                removals.edit {
                    putStringSet(packageName, pendingRemoval.filterNot { split ->
                        split in requestedSplits || modules.any { split.startsWith("$it.config.") }
                    }.toSet())
                }
            }
            val requested = requestedSplits.filterNot { it in installed }
            if (requested.isEmpty()) {
                clearPendingRemovals()
                val state = sessionManager.newState(0, modules, languages).apply { putInt("status", STATUS_INSTALLED) }
                onStarted(0, state)
                sessionManager.sendUpdate(packageName, state)
                return
            }
            // TODO: The criteria for using SplitCompat need to be defined more clearly here.
            val useSplitCompat = withContext(Dispatchers.IO) {
                playCoreVersion >= MIN_SPLITCOMPAT_VERSION && !canInstallPackages(packageName) &&
                    languages.isEmpty()
            }
            val downloadedSession = withContext(Dispatchers.IO) {
                sessionManager.findDownloadedSession(packageName, version, requested, useSplitCompat)
            }
            if (downloadedSession != null) {
                clearPendingRemovals()
                // Renew URI grants in case the app was reinstalled.
                val state = withContext(Dispatchers.IO) {
                    Bundle(downloadedSession.state).apply {
                        putParcelableArrayList("split_file_intents", sessionManager.fileIntents(downloadedSession))
                    }
                }
                onStarted(state.getInt("session_id"), state)
                sessionManager.sendUpdate(packageName, state)
                return
            }
            sessionManager.checkActiveSession(packageName, version, requested)
            val newSession = sessionManager.createSession(packageName, version, requested, modules, languages, useSplitCompat = useSplitCompat)
            val sessionId = newSession.state.getInt("session_id")
            clearPendingRemovals()
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
                val auth = AuthManager.getAuthData(context)
                if (auth?.authToken.isNullOrEmpty()) throw SplitInstallException(-15)
                auth!!
                AccountManager.get(context).accounts.firstOrNull { it.name == auth.email }?.let { account ->
                    try {
                        syncDeviceInfo(context, account, auth.authToken, auth.gsfId.toLong(16))
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.w(TAG, "Device sync failed for split delivery")
                    }
                }
                val components = httpClient.requestDownloadUrls(context, packageName, version, auth, session.requested, languages = languages)
                if (!session.requested.all { name -> components.any { it.componentName == name } }) throw SplitInstallException(-2)
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
                if (PackageInfoCompat.getLongVersionCode(context.packageManager.getPackageInfo(packageName, 0)) != version) {
                    throw SplitInstallException(-2)
                }
                if (!session.useSplitCompat) {
                    installNative(session, files)
                } else {
                    // SplitCompat reports completion after verifying and loading the APKs itself.
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
                session.job = null
                if (session.state.getInt("status") == STATUS_CANCELING) sessionManager.update(session, STATUS_CANCELED)
            }
            enqueuePending(packageName, !session.state.getBoolean("uninstall"))
        }
    }

    private fun canInstallPackages(packageName: String): Boolean {
        if (context.checkCallingOrSelfPermission(android.Manifest.permission.INSTALL_PACKAGES) == PackageManager.PERMISSION_GRANTED) return true
        return SDK_INT >= 31 && context.packageManager.getInstallSourceInfo(packageName).installingPackageName == context.packageName &&
            context.checkCallingOrSelfPermission(android.Manifest.permission.UPDATE_PACKAGES_WITHOUT_USER_ACTION) == PackageManager.PERMISSION_GRANTED
    }

    internal fun failInstall(sessionId: Int) = sessionManager.failInstall(sessionId)

    @RequiresApi(21)
    private suspend fun installNative(session: Session, files: List<File>) {
        currentCoroutineContext().ensureActive()
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
        context.installPackages(session.packageName, components,
            isUpdate = fullInstall, splitInstall = true, keepFiles = true,
            splitSessionId = session.state.getInt("session_id"), removedSplits = toRemove,
            onSessionCreated = { id ->
                synchronized(session) {
                    session.job?.ensureActive()
                    session.installerSessionId = id
                    sessionManager.update(session, STATUS_INSTALLING) { putLong("bytes_downloaded", getLong("total_bytes_to_download")) }
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
        val session = sessionManager.createSession(packageName, PackageInfoCompat.getLongVersionCode(info), toRemove,
            emptyList(), emptyList(), uninstall = true)
        val id = session.state.getInt("session_id")
        try {
            SplitInstallTaskService.enqueueInstall(context, id, packageName)
        } catch (e: Exception) {
            sessionManager.update(session, STATUS_FAILED) { putInt("error_code", ERROR_INTERNAL) }
        }
    }

    internal suspend fun deferredInstall(packageName: String, splits: List<Bundle>, onAccepted: () -> Unit) {
        if (SDK_INT < 21) throw SplitInstallException(ERROR_API_NOT_AVAILABLE)
        if (splits.isNotEmpty()) validateRequest(splits)
        if (splits.isNotEmpty()) {
            requestMutex.withLock {
                val wanted = splits.map { bundle -> bundle.getString("module_name")?.let { "module_name:$it" } ?: "language:${bundle.getString("language")}" }
                if (!deferred.edit().putStringSet(packageName, deferred.getStringSet(packageName, emptySet()).orEmpty() + wanted).commit()) {
                    throw SplitInstallException(-10)
                }
            }
            SplitInstallTaskService.enqueueDeferred(context, packageName)
        }
        onAccepted()
    }

    internal suspend fun installDeferred(packageName: String) {
        if (sessionManager.hasActiveSession(packageName)) return
        val wanted = deferred.getStringSet(packageName, emptySet()).orEmpty().toSet()
        if (wanted.isEmpty()) return
        val splits = wanted.map { value -> Bundle().apply { putString(value.substringBefore(':'), value.substringAfter(':')) } }
        startInstall(packageName, splits) { _, _ ->
            deferred.edit().putStringSet(packageName, deferred.getStringSet(packageName, emptySet()).orEmpty() - wanted).commit()
        }
    }

    private fun enqueuePending(packageName: String, includeRemovals: Boolean = true) {
        if (SDK_INT < 21) return
        if (!deferred.getStringSet(packageName, emptySet()).isNullOrEmpty()) SplitInstallTaskService.enqueueDeferred(context, packageName)
        if (includeRemovals && !removals.getStringSet(packageName, emptySet()).isNullOrEmpty()) SplitInstallTaskService.enqueueRemoval(context, packageName)
    }

    internal fun restoreSessions() = sessionManager.restoreSessions {
        for (packageName in deferred.all.keys + removals.all.keys) enqueuePending(packageName)
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
    }
}
