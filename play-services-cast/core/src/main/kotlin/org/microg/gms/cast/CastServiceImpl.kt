/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.cast

import android.os.Bundle
import android.os.RemoteException
import android.util.Log
import com.google.android.gms.cast.RequestItem
import com.google.android.gms.cast.internal.IBundleCallback
import com.google.android.gms.cast.internal.ICastService
import com.google.android.gms.common.Feature
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.api.internal.IStatusCallback

private const val TAG = "GmsCastService"

/** Features answered by [CastServiceImpl] (service id CAST_API) */
@JvmField
val CAST_SERVICE_FEATURES = arrayOf(
    Feature("module_flag_control", 1L),
    Feature("analytics_proto_enum_translation", 1L),
    Feature("integer_to_integer_map", 1L),
)

/** Features answered by [CastDeviceControllerImpl] (service id CAST) */
@JvmField
val CAST_DEVICE_CONTROLLER_FEATURES = arrayOf(
    // connect() / addListener() / removeListener()
    Feature("cxless_client_minimal", 1L),
)

/**
 * Configuration side of the Cast API. Clients use it for feature flags and lookup tables; none are set, so every
 * lookup returns an empty bundle and the client keeps its built-in defaults.
 */
class CastServiceImpl(private val packageName: String?) : ICastService.Stub() {
    override fun broadcastPrecacheMessageLegacy(callback: IStatusCallback?, arg2: Array<out String>?, precacheData: String?) =
        callback.reply(Status.SUCCESS)

    override fun broadcastPrecacheMessage(callback: IStatusCallback?, arg2: Array<out String>?, precacheData: String?, requestItems: MutableList<RequestItem>?) =
        callback.reply(Status.SUCCESS)

    override fun getCxLessStatus(callback: IStatusCallback?) = callback.reply(Status.SUCCESS)

    override fun getFeatureFlags(callback: IBundleCallback?, flags: Array<out String>?) = callback.reply(Bundle())

    override fun getCastStatusCodeDictionary(callback: IBundleCallback?, dictionaries: Array<out String>?) = callback.reply(Bundle())

    override fun getIntegerMaps(callback: IBundleCallback?, keys: Array<out String>?) = callback.reply(Bundle())

    private fun IStatusCallback?.reply(status: Status) {
        try {
            this?.onResult(status)
        } catch (e: RemoteException) {
            Log.w(TAG, "Client $packageName died", e)
        }
    }

    private fun IBundleCallback?.reply(bundle: Bundle) {
        try {
            this?.onBundle(bundle)
        } catch (e: RemoteException) {
            Log.w(TAG, "Client $packageName died", e)
        }
    }
}
