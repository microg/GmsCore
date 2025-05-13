/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.finsky.services

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleService

private const val TAG = "PlayGearheadService"

class PlayGearheadService : LifecycleService() {

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        Log.d(TAG, "onBind")
        return PlayGearheadServiceImpl(this, lifecycle).asBinder()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "onUnbind")
        return super.onUnbind(intent)
    }
}

private class PlayGearheadServiceImpl(
    private val context: Context, override val lifecycle: Lifecycle
) : IPlayGearheadService.Stub(), LifecycleOwner {

    override fun isPackageInstalledByPlayCheck(pkgName: String?): Bundle {
        Log.d(TAG, "isPackageInstalledByPlayCheck: pkgName: $pkgName")
        return Bundle().apply { putBoolean("Finsky.IsValid", true) }
    }
}
