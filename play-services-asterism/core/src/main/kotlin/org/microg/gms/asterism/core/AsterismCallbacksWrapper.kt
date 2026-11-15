/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.asterism.core

import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import com.google.android.gms.asterism.GetAsterismConsentResponse
import com.google.android.gms.asterism.SetAsterismConsentResponse
import com.google.android.gms.asterism.internal.IAsterismCallbacks
import com.google.android.gms.common.api.Status
import kotlinx.coroutines.CancellationException

private const val TAG = "AsterismCallbacks"

internal class AsterismCallbacksWrapper(private val delegate: IAsterismCallbacks) : IAsterismCallbacks {
    override fun onConsentFetched(status: Status?, response: GetAsterismConsentResponse?) =
        runRemote("onConsentFetched") { delegate.onConsentFetched(status, response) }

    override fun onConsentRegistered(status: Status?, response: SetAsterismConsentResponse?) =
        runRemote("onConsentRegistered") { delegate.onConsentRegistered(status, response) }

    override fun onIsPnvrConstellationDevice(status: Status?, isPnvrDevice: Boolean) =
        runRemote("onIsPnvrConstellationDevice") { delegate.onIsPnvrConstellationDevice(status, isPnvrDevice) }

    override fun asBinder(): IBinder = delegate.asBinder()

    private inline fun runRemote(method: String, block: () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: RemoteException) {
            Log.w(TAG, "RemoteException in $method", e)
        } catch (e: RuntimeException) {
            Log.w(TAG, "RuntimeException delivering $method", e)
        }
    }
}
