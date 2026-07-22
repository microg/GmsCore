/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location.provider

import android.app.Service
import android.content.Intent
import android.os.IBinder
import java.io.FileDescriptor
import java.io.PrintWriter

class GeocodeProviderService : Service() {
    private var bound: Boolean = false
    private var provider: OpenStreetMapNominatimGeocodeProvider? = null

    override fun onBind(intent: Intent?): IBinder? {
        if (provider == null) {
            provider = OpenStreetMapNominatimGeocodeProvider(this)
        }
        bound = true
        return provider?.binder
    }

    override fun dump(fd: FileDescriptor?, writer: PrintWriter?, args: Array<out String>?) {
        provider?.dump(writer)
    }
}