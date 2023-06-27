/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package org.microg.gms.ads.identifier

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.google.android.gms.ads.identifier.internal.IAdvertisingIdService

class AdvertisingIdService : Service() {
    override fun onBind(intent: Intent): IBinder? {
        return AdvertisingIdServiceImpl().asBinder()
    }
}

class AdvertisingIdServiceImpl : IAdvertisingIdService.Stub() {
    override fun getAdvertisingId(): String {
        return "00000000-0000-0000-0000-000000000000"
    }

    override fun isAdTrackingLimited(defaultHint: Boolean): Boolean {
        return true
    }

    override fun generateAdvertisingId(packageName: String): String {
        return advertisingId // Ad tracking limited
    }

    override fun setAdTrackingLimited(packageName: String, limited: Boolean) {
        // Ignored, sorry :)
    }
}
