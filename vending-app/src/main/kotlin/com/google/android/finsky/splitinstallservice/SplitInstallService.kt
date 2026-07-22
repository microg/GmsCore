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

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return SplitInstallServiceImpl(splitInstallManager, this, lifecycle).asBinder()
    }

    override fun onDestroy() {
        splitInstallManager.release()
        super.onDestroy()
    }
}

class SplitInstallServiceImpl(private val installManager: SplitInstallManager, private val context: Context, override val lifecycle: Lifecycle) : ISplitInstallService.Stub(),
    LifecycleOwner {

    override fun startInstall(targetPackage: String, splits: List<Bundle>, bundle0: Bundle, callback: ISplitInstallServiceCallback) {
        val packageName = PackageUtils.getAndCheckCallingPackage(context, targetPackage)!!
        Log.d(TAG, "startInstall(${splits.joinToString()}) called for $targetPackage")
        if (VendingPreferences.isSplitInstallEnabled(context)) {
            lifecycleScope.launch {
                val installStatus = installManager.splitInstallFlow(packageName, splits)
                Log.d(TAG, "startInstall result $installStatus for $targetPackage")
                runCatching { callback.onStartInstall(CommonStatusCodes.SUCCESS, Bundle()) }
            }
        } else {
            Log.w(TAG, "startInstall rejected for $packageName, service is disabled")
            runCatching { callback.onStartInstall(CommonStatusCodes.ERROR, Bundle()) }
        }
    }

    override fun completeInstalls(targetPackage: String, sessionId: Int, bundle0: Bundle, callback: ISplitInstallServiceCallback) {
        val packageName = PackageUtils.getAndCheckCallingPackage(context, targetPackage)!!
        Log.w(TAG, "completeInstalls($sessionId) called for $packageName, but is not implemented")
    }

    override fun cancelInstall(targetPackage: String, sessionId: Int, callback: ISplitInstallServiceCallback) {
        val packageName = PackageUtils.getAndCheckCallingPackage(context, targetPackage)!!
        Log.w(TAG, "cancelInstall($sessionId) called for $packageName, but is not implemented")
    }

    override fun getSessionState(targetPackage: String, sessionId: Int, callback: ISplitInstallServiceCallback) {
        val packageName = PackageUtils.getAndCheckCallingPackage(context, targetPackage)!!
        Log.w(TAG, "getSessionState($sessionId) called for $packageName, but is not implemented")
    }

    override fun getSessionStates(targetPackage: String, callback: ISplitInstallServiceCallback) {
        val packageName = PackageUtils.getAndCheckCallingPackage(context, targetPackage)!!
        Log.w(TAG, "getSessionStates() called for $packageName, but is not implemented")
        runCatching { callback.onGetSessionStates(ArrayList<Bundle>(1)) }
    }

    override fun splitRemoval(targetPackage: String, splits: List<Bundle>, callback: ISplitInstallServiceCallback) {
        val packageName = PackageUtils.getAndCheckCallingPackage(context, targetPackage)!!
        Log.w(TAG, "splitRemoval(${splits.joinToString()}) called for $packageName, but is not implemented")
    }

    override fun splitDeferred(targetPackage: String, splits: List<Bundle>, bundle0: Bundle, callback: ISplitInstallServiceCallback) {
        val packageName = PackageUtils.getAndCheckCallingPackage(context, targetPackage)!!
        Log.w(TAG, "splitDeferred(${splits.joinToString()}) called for $packageName, but is not implemented")
        runCatching { callback.onDeferredInstall(Bundle()) }
    }

    override fun getSessionState2(targetPackage: String, sessionId: Int, callback: ISplitInstallServiceCallback) {
        val packageName = PackageUtils.getAndCheckCallingPackage(context, targetPackage)!!
        Log.w(TAG, "getSessionState2($sessionId) called for $packageName, but is not implemented")
    }

    override fun getSessionStates2(targetPackage: String, callback: ISplitInstallServiceCallback) {
        val packageName = PackageUtils.getAndCheckCallingPackage(context, targetPackage)!!
        Log.w(TAG, "getSessionStates2() called for $packageName, but is not implemented")
    }

    override fun getSplitsAppUpdate(targetPackage: String, callback: ISplitInstallServiceCallback) {
        val packageName = PackageUtils.getAndCheckCallingPackage(context, targetPackage)!!
        Log.w(TAG, "getSessionStates2() called for $packageName, but is not implemented")
    }

    override fun completeInstallAppUpdate(targetPackage: String, callback: ISplitInstallServiceCallback) {
        val packageName = PackageUtils.getAndCheckCallingPackage(context, targetPackage)!!
        Log.w(TAG, "completeInstallAppUpdate() called for $packageName, but is not implemented")
    }

    override fun languageSplitInstall(targetPackage: String, splits: List<Bundle>, bundle0: Bundle, callback: ISplitInstallServiceCallback) {
        val packageName = PackageUtils.getAndCheckCallingPackage(context, targetPackage)!!
        Log.w(TAG, "languageSplitInstall(${splits.joinToString()}) called for $packageName, but is not implemented")
    }

    override fun languageSplitUninstall(targetPackage: String, splits: List<Bundle>, callback: ISplitInstallServiceCallback) {
        val packageName = PackageUtils.getAndCheckCallingPackage(context, targetPackage)!!
        Log.w(TAG, "languageSplitUninstall(${splits.joinToString()}) called for $packageName, but is not implemented")
    }

}
