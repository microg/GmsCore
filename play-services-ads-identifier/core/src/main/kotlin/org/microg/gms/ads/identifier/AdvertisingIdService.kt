/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package org.microg.gms.ads.identifier

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.core.os.bundleOf
import com.google.android.gms.ads.identifier.internal.IAdvertisingIdService
import org.microg.gms.common.GooglePackagePermission
import org.microg.gms.common.PackageUtils
import java.util.UUID

const val TAG = "AdvertisingId"
const val EMPTY_AD_ID = "00000000-0000-0000-0000-000000000000"

class AdvertisingIdService : Service() {
    override fun onBind(intent: Intent): IBinder? {
        return AdvertisingIdServiceImpl(this).asBinder()
    }
}

class MemoryAdvertisingIdConfiguration(context: Context) : AdvertisingIdConfiguration(context) {
    override val adTrackingLimitedPerApp: MutableMap<Int, Boolean> = hashMapOf()
    override var adTrackingLimitedGlobally: Boolean = true
    override var debugLogging: Boolean = false
    override var adId: String = EMPTY_AD_ID
    override var debugAdId: String = EMPTY_AD_ID

    init {
        resetAdvertisingId()
    }
}

abstract class AdvertisingIdConfiguration(private val context: Context) {
    abstract val adTrackingLimitedPerApp: MutableMap<Int, Boolean>
    abstract var adTrackingLimitedGlobally: Boolean
    abstract var debugLogging: Boolean
    abstract var adId: String
    abstract var debugAdId: String

    fun isAdTrackingLimitedForApp(uid: Int): Boolean {
        if (adTrackingLimitedGlobally) return true
        return adTrackingLimitedPerApp[uid] ?: false
    }

    fun resetAdvertisingId(): String {
        adId = UUID.randomUUID().toString()
        debugAdId = UUID.randomUUID().toString().dropLast(12) + "10ca1ad1abe1"
        return if (debugLogging) debugAdId else adId
    }

    fun getAdvertisingIdForApp(uid: Int): String {
        if (isAdTrackingLimitedForApp(uid)) return EMPTY_AD_ID
        try {
            val packageNames = context.packageManager.getPackagesForUid(uid) ?: return EMPTY_AD_ID
            for (packageName in packageNames) {
                val applicationInfo = context.packageManager.getApplicationInfo(packageName, 0)
                if (applicationInfo.targetSdkVersion > 33) {
                    if (context.packageManager.checkPermission("com.google.android.gms.permission.AD_ID", packageName) == PackageManager.PERMISSION_DENIED) {
                        throw SecurityException("Permission not granted")
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Permission check failed", e)
            return EMPTY_AD_ID
        }
        val adId = if (debugLogging) debugAdId else adId
        return adId.ifEmpty { resetAdvertisingId() }
    }
}

class AdvertisingIdServiceImpl(private val context: Context) : IAdvertisingIdService.Stub() {
    private val configuration = MemoryAdvertisingIdConfiguration(context)

    override fun getAdvertisingId(): String {
        return configuration.getAdvertisingIdForApp(Binder.getCallingUid())
    }

    override fun isAdTrackingLimited(ignored: Boolean): Boolean {
        return configuration.isAdTrackingLimitedForApp(Binder.getCallingUid())
    }

    override fun resetAdvertisingId(packageName: String): String {
        PackageUtils.checkPackageUid(context, packageName, getCallingUid())
        PackageUtils.assertGooglePackagePermission(context, GooglePackagePermission.AD_ID)
        return configuration.resetAdvertisingId()
    }

    override fun setAdTrackingLimitedGlobally(packageName: String, limited: Boolean) {
        PackageUtils.checkPackageUid(context, packageName, getCallingUid())
        PackageUtils.assertGooglePackagePermission(context, GooglePackagePermission.AD_ID)
        configuration.adTrackingLimitedGlobally = limited
    }

    override fun setDebugLoggingEnabled(packageName: String, enabled: Boolean): String {
        PackageUtils.checkPackageUid(context, packageName, getCallingUid())
        PackageUtils.assertGooglePackagePermission(context, GooglePackagePermission.AD_ID)
        configuration.debugLogging = enabled
        return advertisingId
    }

    override fun isDebugLoggingEnabled(): Boolean {
        return configuration.debugLogging
    }

    override fun isAdTrackingLimitedGlobally(): Boolean {
        PackageUtils.assertGooglePackagePermission(context, GooglePackagePermission.AD_ID)
        return configuration.adTrackingLimitedGlobally
    }

    override fun setAdTrackingLimitedForApp(uid: Int, limited: Boolean) {
        PackageUtils.assertGooglePackagePermission(context, GooglePackagePermission.AD_ID)
        configuration.adTrackingLimitedPerApp[uid] = limited
    }

    override fun resetAdTrackingLimitedForApp(uid: Int) {
        PackageUtils.assertGooglePackagePermission(context, GooglePackagePermission.AD_ID)
        configuration.adTrackingLimitedPerApp.remove(uid)
    }

    override fun getAllAppsLimitedAdTrackingConfiguration(): Bundle {
        PackageUtils.assertGooglePackagePermission(context, GooglePackagePermission.AD_ID)
        return bundleOf(*configuration.adTrackingLimitedPerApp.map { it.key.toString() to it.value }.toTypedArray())
    }

    override fun getAdvertisingIdForApp(uid: Int): String {
        PackageUtils.assertGooglePackagePermission(context, GooglePackagePermission.AD_ID)
        return configuration.getAdvertisingIdForApp(uid)
    }
}
