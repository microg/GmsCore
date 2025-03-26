/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.finsky.splitinstallservice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.android.vending.VendingPreferences
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.play.core.splitinstall.protocol.ISplitInstallService
import com.google.android.play.core.splitinstall.protocol.ISplitInstallServiceCallback
import kotlinx.coroutines.launch
import org.microg.gms.profile.ProfileManager

private const val TAG = "SplitInstallService"

class SplitInstallService : LifecycleService() {

    private lateinit var splitInstallManager: SplitInstallManager

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        Log.d(TAG, "onBind: ")
        ProfileManager.ensureInitialized(this)
        splitInstallManager = SplitInstallManager(this)
        return SplitInstallServiceImpl(splitInstallManager, this, lifecycle).asBinder()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "onUnbind: ")
        splitInstallManager.release()
        return super.onUnbind(intent)
    }
}

class SplitInstallServiceImpl(private val installManager: SplitInstallManager, private val context: Context, override val lifecycle: Lifecycle) : ISplitInstallService.Stub(),
    LifecycleOwner {

    override fun startInstall(pkg: String, splits: List<Bundle>, bundle0: Bundle, callback: ISplitInstallServiceCallback) {
        Log.d(TAG, "Method <startInstall> Called by package: $pkg")
        if (VendingPreferences.isSplitInstallEnabled(context)) {
            lifecycleScope.launch {
                val installStatus = installManager.splitInstallFlow(pkg, splits)
                Log.d(TAG, "startInstall: installStatus -> $installStatus")
                callback.onStartInstall(CommonStatusCodes.SUCCESS, Bundle())
            }
        } else {
            Log.w(TAG, "refusing to perform split installation for $pkg as the service is disabled")
            callback.onStartInstall(CommonStatusCodes.ERROR, Bundle())
        }
    }

    override fun completeInstalls(pkg: String, sessionId: Int, bundle0: Bundle, callback: ISplitInstallServiceCallback) {
        Log.d(TAG, "Method (completeInstalls) called but not implement by package -> $pkg")
    }

    override fun cancelInstall(pkg: String, sessionId: Int, callback: ISplitInstallServiceCallback) {
        Log.d(TAG, "Method (cancelInstall) called but not implement by package -> $pkg")
    }

    override fun getSessionState(pkg: String, sessionId: Int, callback: ISplitInstallServiceCallback) {
        Log.d(TAG, "Method (getSessionState) called but not implement by package -> $pkg")
    }

    override fun getSessionStates(pkg: String, callback: ISplitInstallServiceCallback) {
        Log.d(TAG, "Method (getSessionStates) called but not implement by package -> $pkg")
        callback.onGetSessionStates(ArrayList<Bundle>(1))
    }

    override fun splitRemoval(pkg: String, splits: List<Bundle>, callback: ISplitInstallServiceCallback) {
        Log.d(TAG, "Method (splitRemoval) called but not implement by package -> $pkg")
    }

    override fun splitDeferred(pkg: String, splits: List<Bundle>, bundle0: Bundle, callback: ISplitInstallServiceCallback) {
        Log.d(TAG, "Method (splitDeferred) called but not implement by package -> $pkg")
        callback.onDeferredInstall(Bundle())
    }

    override fun getSessionState2(pkg: String, sessionId: Int, callback: ISplitInstallServiceCallback) {
        Log.d(TAG, "Method (getSessionState2) called but not implement by package -> $pkg")
    }

    override fun getSessionStates2(pkg: String, callback: ISplitInstallServiceCallback) {
        Log.d(TAG, "Method (getSessionStates2) called but not implement by package -> $pkg")
    }

    override fun getSplitsAppUpdate(pkg: String, callback: ISplitInstallServiceCallback) {
        Log.d(TAG, "Method (getSplitsAppUpdate) called but not implement by package -> $pkg")
    }

    override fun completeInstallAppUpdate(pkg: String, callback: ISplitInstallServiceCallback) {
        Log.d(TAG, "Method (completeInstallAppUpdate) called but not implement by package -> $pkg")
    }

    override fun languageSplitInstall(pkg: String, splits: List<Bundle>, bundle0: Bundle, callback: ISplitInstallServiceCallback) {
        Log.d(TAG, "Method <languageSplitInstall> Called by package: $pkg")
    }

    override fun languageSplitUninstall(pkg: String, splits: List<Bundle>, callback: ISplitInstallServiceCallback) {
        Log.d(TAG, "Method (languageSplitUninstall) called but not implement by package -> $pkg")
    }

}
