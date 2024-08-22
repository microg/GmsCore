/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.finsky.splitinstallservice

import android.content.Intent
import android.os.IBinder
import androidx.lifecycle.LifecycleService
import com.google.android.play.core.splitinstall.protocol.ISplitInstallService
import org.microg.gms.profile.ProfileManager

class SplitInstallService : LifecycleService() {
    private var mService: ISplitInstallService? = null

    override fun onCreate() {
        super.onCreate()
        ProfileManager.ensureInitialized(this)
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        if (mService == null) {
            mService = SplitInstallServiceImpl(this.applicationContext)
        }
        return mService as IBinder?
    }
}
