/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location.provider

import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.*
import android.os.Build.VERSION.SDK_INT
import org.microg.gms.location.network.NetworkLocationService.Companion.ACTION_REPORT_LOCATION
import org.microg.gms.location.network.NetworkLocationService.Companion.EXTRA_LOCATION
import java.io.FileDescriptor
import java.io.PrintWriter

class NetworkLocationProviderService : Service() {
    private lateinit var handlerThread: HandlerThread
    private lateinit var handler: Handler
    private var bound: Boolean = false
    private var provider: GenericLocationProvider? = null

    override fun onCreate() {
        super.onCreate()
        handlerThread = HandlerThread(NetworkLocationProviderService::class.java.simpleName)
        handlerThread.start()
        handler = Handler(handlerThread.looper)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Binder.getCallingUid() == Process.myUid() && intent?.action == ACTION_REPORT_LOCATION) {
            handler.post {
                val location = intent.getParcelableExtra<Location>(EXTRA_LOCATION)
                if (location != null) {
                    provider?.reportLocationToSystem(location)
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        bound = true
        if (provider == null) {
            provider = when {
                // TODO: Migrate to Tiramisu provider. Not yet required thanks to backwards compat
                // SDK_INT >= 33 ->
                SDK_INT >= 31 ->
                    NetworkLocationProviderPreTiramisu(this)

                else ->
                    @Suppress("DEPRECATION")
                    NetworkLocationProviderPreTiramisu(this, Unit)
            }
            provider?.enable()
        }
        return provider?.getBinder()
    }

    override fun dump(fd: FileDescriptor, writer: PrintWriter, args: Array<out String>) {
        writer.println("Bound: $bound")
        provider?.dump(writer)
    }

    override fun onDestroy() {
        if (SDK_INT >= 18) handlerThread.looper.quitSafely()
        else handlerThread.looper.quit()
        provider?.disable()
        provider = null
        bound = false
        super.onDestroy()
    }
}