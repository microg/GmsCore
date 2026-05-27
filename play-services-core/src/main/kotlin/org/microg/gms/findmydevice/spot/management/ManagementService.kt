/**
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.findmydevice.spot.management

import android.content.Context
import android.os.Parcel
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.internal.ConnectionInfo
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import com.google.android.gms.findmydevice.spot.CachedSpotDevice
import com.google.android.gms.findmydevice.spot.ChangeFindMyDeviceSettingsRequest
import com.google.android.gms.findmydevice.spot.GetCachedDevicesRequest
import com.google.android.gms.findmydevice.spot.GetCachedDevicesResponse
import com.google.android.gms.findmydevice.spot.GetFindMyDeviceSettingsRequest
import com.google.android.gms.findmydevice.spot.GetFindMyDeviceSettingsResponse
import com.google.android.gms.findmydevice.spot.GetKeychainLockScreenKnowledgeFactorSupportRequest
import com.google.android.gms.findmydevice.spot.GetOwnerKeyRequest
import com.google.android.gms.findmydevice.spot.ImportGivenOwnerKeyRequest
import com.google.android.gms.findmydevice.spot.ImportRequiredOwnerKeysRequest
import com.google.android.gms.findmydevice.spot.SetOwnerKeyRequest
import com.google.android.gms.findmydevice.spot.SyncOwnerKeyRequest
import com.google.android.gms.findmydevice.spot.internal.ISpotManagementCallbacks
import com.google.android.gms.findmydevice.spot.internal.ISpotManagementService
import org.microg.gms.BaseService
import org.microg.gms.auth.AuthPrefs
import org.microg.gms.common.GmsService
import org.microg.gms.common.PackageUtils
import org.microg.gms.findmydevice.FEATURES
import org.microg.gms.utils.warnOnTransactionIssues

private const val TAG = "ManagementService"

class ManagementService : BaseService(TAG, GmsService.FIND_MY_DEVICE_SPOT) {
    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        val packageName = PackageUtils.getAndCheckCallingPackage(this, request.packageName)
            ?: throw IllegalArgumentException("Missing package name")
        val spotManagementServiceImpl = SpotManagementServiceImpl(packageName, this, lifecycle)
        callback.onPostInitCompleteWithConnectionInfo(0, spotManagementServiceImpl.asBinder(), ConnectionInfo().apply {
            features = FEATURES
        })
    }
}

class SpotManagementServiceImpl(val packageName: String, val context: Context, override val lifecycle: Lifecycle) : ISpotManagementService.Stub(), LifecycleOwner {

    override fun getOwnerKey(callbacks: ISpotManagementCallbacks?, request: GetOwnerKeyRequest?) {
        Log.d(TAG, "Unimplement getOwnerKey: $request")
    }

    override fun setOwnerKey(callbacks: ISpotManagementCallbacks?, request: SetOwnerKeyRequest?) {
        Log.d(TAG, "Unimplement setOwnerKey: $request")
    }

    override fun importRequiredOwnerKeys(
        callbacks: ISpotManagementCallbacks?,
        request: ImportRequiredOwnerKeysRequest?
    ) {
        Log.d(TAG, "Unimplement importRequiredOwnerKeys: $request")
    }

    override fun syncOwnerKey(callbacks: ISpotManagementCallbacks?, request: SyncOwnerKeyRequest?) {
        Log.d(TAG, "Unimplement syncOwnerKey: $request")
    }

    override fun importGivenOwnerKey(
        callbacks: ISpotManagementCallbacks?,
        request: ImportGivenOwnerKeyRequest?
    ) {
        Log.d(TAG, "Unimplement importGivenOwnerKey: $request")
    }

    override fun getFindMyDeviceSettings(
        callbacks: ISpotManagementCallbacks?,
        request: GetFindMyDeviceSettingsRequest?
    ) {
        Log.d(TAG, "Unimplement getFindMyDeviceSettings: $request")
        callbacks?.onGetFindMyDeviceSettings(Status.SUCCESS, GetFindMyDeviceSettingsResponse().apply {
            isFmdEnable = AuthPrefs.allowedFindDevices(context)
            deviceStatus = 1
        })
    }

    override fun changeFindMyDeviceSettings(
        callbacks: ISpotManagementCallbacks?,
        request: ChangeFindMyDeviceSettingsRequest?
    ) {
        Log.d(TAG, "Unimplement changeFindMyDeviceSettings: $request")
    }

    override fun getKeychainLockScreenKnowledgeFactorSupport(
        callbacks: ISpotManagementCallbacks?,
        request: GetKeychainLockScreenKnowledgeFactorSupportRequest?
    ) {
        Log.d(TAG, "Unimplement getKeychainLockScreenKnowledgeFactorSupport: $request")
    }

    override fun getCachedDevices(callbacks: ISpotManagementCallbacks?, request: GetCachedDevicesRequest?) {
        Log.d(TAG, "Unimplement getCachedDevices: $request")
        callbacks?.onGetCachedDevices(Status.SUCCESS, GetCachedDevicesResponse(emptyArray<CachedSpotDevice>()))
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean =
        warnOnTransactionIssues(code, reply, flags, TAG) { super.onTransact(code, data, reply, flags) }
}