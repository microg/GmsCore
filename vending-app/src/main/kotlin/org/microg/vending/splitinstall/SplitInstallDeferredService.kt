/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.vending.splitinstall

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.JobIntentService
import com.android.vending.VendingPreferences
import com.google.android.finsky.splitinstallservice.SplitInstallManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.microg.gms.profile.ProfileManager

private const val TAG = "SplitInstallDeferred"

@Suppress("DEPRECATION")
@RequiresApi(21)
class SplitInstallDeferredService : JobIntentService() {
    override fun onHandleWork(intent: Intent) {
        if (!VendingPreferences.isSplitInstallEnabled(this)) return
        val packageName = intent.getStringExtra("package_name") ?: return
        try {
            ProfileManager.ensureInitialized(this)
            runBlocking {
                val manager = SplitInstallManager(this@SplitInstallDeferredService)
                manager.restoreSessions()
                if (intent.hasExtra("session_id")) manager.recoverSession(intent.getIntExtra("session_id", -1))
                else if (intent.getBooleanExtra("uninstall", false)) manager.uninstall(packageName)
                else manager.installDeferred(packageName)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Deferred split request failed (${e.javaClass.simpleName})")
        }
    }

    companion object {
        internal fun enqueueRecovery(context: Context, sessionId: Int, packageName: String) {
            enqueueWork(context, SplitInstallDeferredService::class.java, 0x53504c54,
                Intent().putExtra("package_name", packageName).putExtra("session_id", sessionId))
        }

        internal fun enqueue(context: Context, packageName: String, uninstall: Boolean) {
            enqueueWork(context, SplitInstallDeferredService::class.java, 0x53504c54,
                Intent().putExtra("package_name", packageName).putExtra("uninstall", uninstall))
        }
    }
}
