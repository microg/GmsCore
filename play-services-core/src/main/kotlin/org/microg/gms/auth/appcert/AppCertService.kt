/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth.appcert

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.Parcel
import android.util.Log
import com.google.android.gms.auth.appcert.IAppCertService
import kotlinx.coroutines.runBlocking
import org.microg.gms.common.PackageUtils
import org.microg.gms.utils.warnOnTransactionIssues

private const val TAG = "AppCertService"

class AppCertService : Service() {
    override fun onBind(intent: Intent): IBinder {
        Log.d(TAG, "onBind: $intent")
        return AppCertServiceImpl(this).asBinder()
    }
}

class AppCertServiceImpl(private val context: Context) : IAppCertService.Stub() {
    private val manager = AppCertManager(context)

    override fun fetchDeviceKey(): Boolean {
        PackageUtils.assertExtendedAccess(context)
        return runBlocking { manager.fetchDeviceKey() }
    }

    override fun getSpatulaHeader(packageName: String): String? {
        PackageUtils.assertExtendedAccess(context)
        return runBlocking { manager.getSpatulaHeader(packageName) }
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean = warnOnTransactionIssues(code, reply, flags) { super.onTransact(code, data, reply, flags) }
}
