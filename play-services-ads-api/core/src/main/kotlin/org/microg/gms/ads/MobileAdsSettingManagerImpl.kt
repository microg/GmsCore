/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package org.microg.gms.ads

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Parcel
import android.os.RemoteException
import android.util.Log
import com.google.android.gms.ads.internal.AdapterStatusParcel
import com.google.android.gms.ads.internal.RequestConfigurationParcel
import com.google.android.gms.ads.internal.client.IMobileAdsSettingManager
import com.google.android.gms.ads.internal.client.IOnAdInspectorClosedListener
import com.google.android.gms.ads.internal.initialization.IInitializationCallback
import com.google.android.gms.ads.internal.mediation.client.IAdapterCreator
import com.google.android.gms.dynamic.IObjectWrapper
import org.microg.gms.utils.warnOnTransactionIssues

private const val TAG = "AdsSettingManager"

class MobileAdsSettingManagerImpl(private val context: Context?) : IMobileAdsSettingManager.Stub() {
    private var initialized = false
    private val initializationCallbacks = mutableListOf<IInitializationCallback>()
    private var muted = false
    private var volume = 1.0f

    override fun initialize() {
        Log.d(TAG, "initialize")
        if (initialized) return
        initialized = true
        for (callback in initializationCallbacks) {
            runCatching { callback.onInitialized(emptyList()) }
        }
    }

    override fun setAppVolume(volume: Float) {
        this.volume = volume
    }

    override fun fetchAppSettings(appId: String?) {
        fetchAppSettingsV2(appId, null)
    }

    override fun setAppMuted(muted: Boolean) {
        this.muted = muted
    }

    override fun openDebugMenu(context: IObjectWrapper?, adUnitId: String?) {
        Log.d(TAG, "openDebugMenu($adUnitId)")
    }

    override fun fetchAppSettingsV2(appId: String?, runnable: IObjectWrapper?) {
        Log.d(TAG, "fetchAppSettings($appId)")
    }

    override fun getAdVolume(): Float {
        return volume
    }

    override fun isAdMuted(): Boolean {
        return muted
    }

    override fun getVersionString(): String {
        return ""
    }

    override fun registerRtbAdapter(className: String?) {
        Log.d(TAG, "registerRtbAdapter($className)")
    }

    override fun setAdapterCreator(iAdapterCreator: IAdapterCreator?) {
        Log.d(TAG, "Not yet implemented: setAdapterCreator")
    }

    override fun addInitializationCallback(callback: IInitializationCallback?) {
        Log.d(TAG, "addInitializationCallback")
        callback?.let { initializationCallbacks.add(it) }
    }

    override fun getAdapterStatus(): List<AdapterStatusParcel> {
        Log.d(TAG, "getAdapterStatus")
        return arrayListOf(AdapterStatusParcel("com.google.android.gms.ads.MobileAds", true, 0, "Dummy"))
    }

    override fun setRequestConfiguration(configuration: RequestConfigurationParcel?) {
        Log.d(TAG, "setRequestConfiguration")
    }

    override fun disableMediationAdapterInitialization() {
        Log.d(TAG, "disableMediationAdapterInitialization")
    }

    override fun openAdInspector(listener: IOnAdInspectorClosedListener?) {
        Log.d(TAG, "openAdInspector")
    }

    override fun enableSameAppKey(enabled: Boolean) {
        Log.d(TAG, "enableSameAppKey($enabled)")
    }

    override fun setPlugin(plugin: String?) {
        Log.d(TAG, "setPlugin($plugin)")
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean = warnOnTransactionIssues(code, reply, flags, TAG) { super.onTransact(code, data, reply, flags) }
}