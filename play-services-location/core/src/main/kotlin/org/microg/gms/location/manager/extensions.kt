/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location.manager

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Build.VERSION.SDK_INT
import android.os.Process
import android.util.Log
import androidx.core.app.AppOpsManagerCompat
import androidx.core.content.getSystemService
import com.google.android.gms.common.Feature
import com.google.android.gms.location.*
import com.google.android.gms.location.internal.ClientIdentity
import com.google.android.gms.location.internal.IFusedLocationProviderCallback
import org.microg.gms.common.PackageUtils
import org.microg.gms.location.GranularityUtil

const val TAG = "LocationManager"

internal val FEATURES = arrayOf(
    Feature("name_ulr_private", 1),
    Feature("driving_mode", 6),
    Feature("name_sleep_segment_request", 1),
    Feature("support_context_feature_id", 1),
    Feature("get_current_location", 2),
    Feature("get_last_activity_feature_id", 1),
    Feature("get_last_location_with_request", 1),
    Feature("set_mock_mode_with_callback", 1),
    Feature("set_mock_location_with_callback", 1),
    Feature("inject_location_with_callback", 1),
    Feature("location_updates_with_callback", 1),
    Feature("user_service_developer_features", 1),
    Feature("user_service_location_accuracy", 1),
    Feature("user_service_safety_and_emergency", 1),

    Feature("use_safe_parcelable_in_intents", 1)
)

fun ILocationListener.asCallback(): ILocationCallback {
    return object : ILocationCallback.Stub() {
        override fun onLocationResult(result: LocationResult) {
            for (location in result.locations) {
                onLocationChanged(location)
            }
        }

        override fun onLocationAvailability(availability: LocationAvailability) = Unit
        override fun cancel() = this@asCallback.cancel()
    }
}

fun ILocationCallback.redirectCancel(fusedCallback: IFusedLocationProviderCallback?): ILocationCallback {
    if (fusedCallback == null) return this
    return object : ILocationCallback.Stub() {
        override fun onLocationResult(result: LocationResult) = this@redirectCancel.onLocationResult(result)
        override fun onLocationAvailability(availability: LocationAvailability) = this@redirectCancel.onLocationAvailability(availability)
        override fun cancel() = fusedCallback.cancel()
    }
}

fun ClientIdentity.isGoogle(context: Context) = PackageUtils.isGooglePackage(context, packageName)

fun ClientIdentity.isSelfProcess() = pid == Process.myPid()

fun Context.granularityFromPermission(clientIdentity: ClientIdentity): @Granularity Int = when (PackageManager.PERMISSION_GRANTED) {
    packageManager.checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, clientIdentity.packageName) -> Granularity.GRANULARITY_FINE
    packageManager.checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION, clientIdentity.packageName) -> Granularity.GRANULARITY_COARSE
    else -> Granularity.GRANULARITY_PERMISSION_LEVEL
}

fun LocationRequest.verify(context: Context, clientIdentity: ClientIdentity) {
    GranularityUtil.checkValidGranularity(granularity)
    if (isBypass) {
        val permission = if (SDK_INT >= 33) "android.permission.LOCATION_BYPASS" else Manifest.permission.WRITE_SECURE_SETTINGS
        if (context.checkPermission(permission, clientIdentity.pid, clientIdentity.uid) != PackageManager.PERMISSION_GRANTED) {
            throw SecurityException("Caller must hold $permission for location bypass")
        }
    }
    if (impersonation != null) {
        Log.w(TAG, "${clientIdentity.packageName} wants to impersonate ${impersonation!!.packageName}. Ignoring.")
    }

}

fun checkAppOpFromEffectiveGranularity(effectiveGranularity: @Granularity Int) = when (effectiveGranularity) {
    Granularity.GRANULARITY_FINE -> AppOpsManager.OPSTR_FINE_LOCATION
    Granularity.GRANULARITY_COARSE -> AppOpsManager.OPSTR_COARSE_LOCATION
    else -> throw IllegalArgumentException()
}

fun persistAppOpsFromEffectiveGranularity(effectiveGranularity: @Granularity Int) = when (effectiveGranularity) {
    Granularity.GRANULARITY_FINE -> listOf(AppOpsManager.OPSTR_MONITOR_LOCATION, AppOpsManager.OPSTR_MONITOR_HIGH_POWER_LOCATION)
    Granularity.GRANULARITY_COARSE -> listOf(AppOpsManager.OPSTR_MONITOR_LOCATION)
    else -> throw IllegalArgumentException()
}

fun getEffectiveGranularity(requestGranularity: @Granularity Int, permissionGranularity: @Granularity Int) = when {
    requestGranularity == Granularity.GRANULARITY_PERMISSION_LEVEL && permissionGranularity == Granularity.GRANULARITY_PERMISSION_LEVEL -> Granularity.GRANULARITY_FINE
    requestGranularity == Granularity.GRANULARITY_PERMISSION_LEVEL -> permissionGranularity
    else -> requestGranularity
}

fun Context.noteAppOpForEffectiveGranularity(
    clientIdentity: ClientIdentity,
    effectiveGranularity: @Granularity Int,
    message: String? = null
): Boolean {
    return try {
        val op = checkAppOpFromEffectiveGranularity(effectiveGranularity)
        noteAppOp(op, clientIdentity, message)
    } catch (e: Exception) {
        Log.w(TAG, "Could not notify appops", e)
        true
    }
}

fun Context.checkAppOpForEffectiveGranularity(clientIdentity: ClientIdentity, effectiveGranularity: @Granularity Int): Boolean {
    return try {
        val op = checkAppOpFromEffectiveGranularity(effectiveGranularity)
        checkAppOp(op, clientIdentity)
    } catch (e: Exception) {
        Log.w(TAG, "Could not check appops", e)
        true
    }
}

fun Context.startAppOpForEffectiveGranularity(clientIdentity: ClientIdentity, effectiveGranularity: @Granularity Int): Boolean {
    return try {
        val ops = persistAppOpsFromEffectiveGranularity(effectiveGranularity)
        startAppOps(ops, clientIdentity)
    } catch (e: Exception) {
        Log.w(TAG, "Could not start appops", e)
        true
    }
}

fun Context.finishAppOpForEffectiveGranularity(clientIdentity: ClientIdentity, effectiveGranularity: @Granularity Int) {
    try {
        val ops = persistAppOpsFromEffectiveGranularity(effectiveGranularity)
        finishAppOps(ops, clientIdentity)
    } catch (e: Exception) {
        Log.w(TAG, "Could not finish appops", e)
    }
}

private fun Context.checkAppOp(
    op: String,
    clientIdentity: ClientIdentity
) = try {
    if (SDK_INT >= 29) {
        getSystemService<AppOpsManager>()?.unsafeCheckOpNoThrow(op, clientIdentity.uid, clientIdentity.packageName) == AppOpsManager.MODE_ALLOWED
    } else {
        getSystemService<AppOpsManager>()?.checkOpNoThrow(op, clientIdentity.uid, clientIdentity.packageName) == AppOpsManager.MODE_ALLOWED
    }
} catch (e: SecurityException) {
    true
}

fun Context.startAppOps(
    ops: List<String>,
    clientIdentity: ClientIdentity,
    message: String? = null
) = ops.all { startAppOp(it, clientIdentity, message) }

fun Context.startAppOp(
    op: String,
    clientIdentity: ClientIdentity,
    message: String? = null
) = try {
    if (SDK_INT >= 30 && clientIdentity.attributionTag != null) {
        getSystemService<AppOpsManager>()?.startOpNoThrow(op, clientIdentity.uid, clientIdentity.packageName, clientIdentity.attributionTag!!, message)
    } else {
        getSystemService<AppOpsManager>()?.startOpNoThrow(op, clientIdentity.uid, clientIdentity.packageName)
    }
} catch (e: SecurityException) {
    if (SDK_INT >= 31) {
        getSystemService<AppOpsManager>()?.startProxyOpNoThrow(op, clientIdentity.uid, clientIdentity.packageName, clientIdentity.attributionTag, message)
    } else {
        AppOpsManager.MODE_ALLOWED
    }
} == AppOpsManager.MODE_ALLOWED

fun Context.finishAppOps(
    ops: List<String>,
    clientIdentity: ClientIdentity
) = ops.forEach { finishAppOp(it, clientIdentity) }

fun Context.finishAppOp(
    op: String,
    clientIdentity: ClientIdentity
) {
    try {
        if (SDK_INT >= 30 && clientIdentity.attributionTag != null) {
            getSystemService<AppOpsManager>()?.finishOp(op, clientIdentity.uid, clientIdentity.packageName, clientIdentity.attributionTag!!)
        } else {
            getSystemService<AppOpsManager>()?.finishOp(op, clientIdentity.uid, clientIdentity.packageName)
        }
    } catch (e: SecurityException) {
        if (SDK_INT >= 31) {
            getSystemService<AppOpsManager>()?.finishProxyOp(op, clientIdentity.uid, clientIdentity.packageName, clientIdentity.attributionTag)
        }
    }
}

private fun Context.noteAppOp(
    op: String,
    clientIdentity: ClientIdentity,
    message: String? = null
) = try {
    if (SDK_INT >= 30) {
        getSystemService<AppOpsManager>()
            ?.noteOpNoThrow(op, clientIdentity.uid, clientIdentity.packageName, clientIdentity.attributionTag, message) == AppOpsManager.MODE_ALLOWED
    } else {
        AppOpsManagerCompat.noteOpNoThrow(this, op, clientIdentity.uid, clientIdentity.packageName) == AppOpsManager.MODE_ALLOWED
    }
} catch (e: SecurityException) {
    if (Binder.getCallingUid() == clientIdentity.uid) {
        AppOpsManagerCompat.noteProxyOpNoThrow(this, op, clientIdentity.packageName) == AppOpsManager.MODE_ALLOWED
    } else if (SDK_INT >= 29) {
        getSystemService<AppOpsManager>()
            ?.noteProxyOpNoThrow(op, clientIdentity.packageName, clientIdentity.uid) == AppOpsManager.MODE_ALLOWED
    } else {
        true
    }
}