/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.finsky.splitinstallservice

import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.android.vending.VendingPreferences
import com.google.android.play.core.splitinstall.protocol.ISplitInstallService
import com.google.android.play.core.splitinstall.protocol.ISplitInstallServiceCallback
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.microg.gms.common.PackageUtils
import org.microg.gms.profile.ProfileManager

private const val TAG = "SplitInstallService"

class SplitInstallService : LifecycleService() {
    private lateinit var splitInstallManager: SplitInstallManager

    override fun onCreate() {
        super.onCreate()
        ProfileManager.ensureInitialized(this)
        splitInstallManager = SplitInstallManager(this)
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return SplitInstallServiceImpl(splitInstallManager, this, lifecycle).asBinder()
    }

    internal fun execute(block: suspend () -> Unit) {
        lifecycleScope.launch(Dispatchers.IO) { block() }
    }
}

class SplitInstallServiceImpl(
    private val installManager: SplitInstallManager,
    private val context: SplitInstallService,
    override val lifecycle: Lifecycle
) : ISplitInstallService.Stub(), LifecycleOwner {
    private fun execute(method: String, targetPackage: String, callback: ISplitInstallServiceCallback, block: suspend (String) -> Unit) {
        val packageName = try {
            PackageUtils.getAndCheckCallingPackage(context, targetPackage)
        } catch (e: SecurityException) {
            null
        }
        Log.d(TAG, "$method: packageName=$packageName")
        if (packageName == null) {
            error(callback, -7)
            return
        }
        if (!VendingPreferences.isSplitInstallEnabled(context)) {
            error(callback, ERROR_API_NOT_AVAILABLE)
            return
        }
        context.execute {
            try {
                installManager.restoreSessions()
                block(packageName)
            } catch (e: CancellationException) {
                throw e
            } catch (e: SplitInstallException) {
                error(callback, e.errorCode)
            } catch (e: Exception) {
                Log.w(TAG, "Split install request failed for $packageName (${e.javaClass.simpleName})")
                error(callback, ERROR_INTERNAL)
            }
        }
    }

    private fun error(callback: ISplitInstallServiceCallback, code: Int) {
        runCatching { callback.onError(Bundle().apply { putInt("error_code", code) }) }
    }

    override fun startInstall(targetPackage: String, splits: List<Bundle>, bundle0: Bundle, callback: ISplitInstallServiceCallback) =
        execute("startInstall(splitCount=${splits.size})", targetPackage, callback) { packageName ->
            installManager.startInstall(packageName, splits, bundle0.getInt("playcore_version_code")) { id, state ->
                runCatching { callback.onStartInstall(id, state) }
            }
        }

    override fun completeInstalls(targetPackage: String, sessionId: Int, bundle0: Bundle, callback: ISplitInstallServiceCallback) =
        execute("completeInstalls(sessionId=$sessionId)", targetPackage, callback) { packageName ->
            val state = installManager.getSessionState(packageName, sessionId)
            runCatching { callback.onInstallCompleted(sessionId, state) }
        }

    override fun cancelInstall(targetPackage: String, sessionId: Int, bundle0: Bundle, callback: ISplitInstallServiceCallback) =
        execute("cancelInstall(sessionId=$sessionId)", targetPackage, callback) { packageName ->
            installManager.cancelInstall(packageName, sessionId)
            runCatching { callback.onCancelInstall(sessionId, Bundle()) }
        }

    override fun getSessionState(targetPackage: String, sessionId: Int, callback: ISplitInstallServiceCallback) =
        execute("getSessionState(sessionId=$sessionId)", targetPackage, callback) { packageName ->
            val state = installManager.getSessionState(packageName, sessionId)
            runCatching { callback.onGetSessionState(sessionId, state) }
        }

    override fun getSessionStates(targetPackage: String, callback: ISplitInstallServiceCallback) =
        execute("getSessionStates", targetPackage, callback) { packageName ->
            val states = installManager.getSessionStates(packageName)
            runCatching { callback.onGetSessionStates(states) }
        }

    override fun splitRemoval(targetPackage: String, splits: List<Bundle>, bundle0: Bundle, callback: ISplitInstallServiceCallback) =
        execute("splitRemoval(splitCount=${splits.size})", targetPackage, callback) { packageName ->
            installManager.deferredUninstall(packageName, splits, languages = false)
            runCatching { callback.onDeferredUninstall(Bundle()) }
        }

    override fun splitDeferred(targetPackage: String, splits: List<Bundle>, bundle0: Bundle, callback: ISplitInstallServiceCallback) =
        execute("splitDeferred(splitCount=${splits.size})", targetPackage, callback) { packageName ->
            if (splits.any { it.getString("module_name") == null }) throw SplitInstallException(ERROR_INVALID_REQUEST)
            installManager.deferredInstall(packageName, splits, bundle0.getInt("playcore_version_code", 0)) {
                runCatching { callback.onDeferredInstall(Bundle()) }
            }
        }

    override fun getSessionState2(targetPackage: String, sessionId: Int, bundle0: Bundle, callback: ISplitInstallServiceCallback) =
        execute("getSessionState2(sessionId=$sessionId)", targetPackage, callback) { packageName ->
            val state = installManager.getSessionState(packageName, sessionId)
            runCatching { callback.onGetSessionState(sessionId, state) }
        }

    override fun getSessionStates2(targetPackage: String, bundle0: Bundle, callback: ISplitInstallServiceCallback) =
        execute("getSessionStates2", targetPackage, callback) { packageName ->
            val states = installManager.getSessionStates(packageName)
            runCatching { callback.onGetSessionStates(states) }
        }

    override fun getSplitsAppUpdate(targetPackage: String, bundle0: Bundle, callback: ISplitInstallServiceCallback) =
        execute("getSplitsAppUpdate", targetPackage, callback) { error(callback, ERROR_API_NOT_AVAILABLE) }

    override fun completeInstallAppUpdate(targetPackage: String, bundle0: Bundle, callback: ISplitInstallServiceCallback) =
        execute("completeInstallAppUpdate", targetPackage, callback) { error(callback, ERROR_API_NOT_AVAILABLE) }

    override fun languageSplitInstall(targetPackage: String, splits: List<Bundle>, bundle0: Bundle, callback: ISplitInstallServiceCallback) =
        execute("languageSplitInstall(splitCount=${splits.size})", targetPackage, callback) { packageName ->
            if (splits.isEmpty() || splits.any { it.getString("language") == null }) throw SplitInstallException(ERROR_INVALID_REQUEST)
            installManager.deferredInstall(packageName, splits, bundle0.getInt("playcore_version_code", 0)) {
                runCatching { callback.onDeferredLanguageInstall(Bundle()) }
            }
        }

    override fun languageSplitUninstall(targetPackage: String, splits: List<Bundle>, bundle0: Bundle, callback: ISplitInstallServiceCallback) =
        execute("languageSplitUninstall(splitCount=${splits.size})", targetPackage, callback) { packageName ->
            installManager.deferredUninstall(packageName, splits, languages = true)
            runCatching { callback.onDeferredLanguageUninstall(Bundle()) }
        }
}
