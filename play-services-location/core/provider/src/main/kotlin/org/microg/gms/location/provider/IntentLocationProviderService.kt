/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location.provider

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.Binder
import android.os.Build.VERSION.SDK_INT
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Process
import android.util.Log
import com.android.location.provider.ProviderPropertiesUnbundled
import com.android.location.provider.ProviderRequestUnbundled
import java.io.FileDescriptor
import java.io.PrintWriter

abstract class IntentLocationProviderService : Service() {
    private lateinit var handlerThread: HandlerThread
    private lateinit var handler: Handler
    private var bound: Boolean = false
    private var provider: GenericLocationProvider? = null

    override fun onCreate() {
        super.onCreate()
        handlerThread = HandlerThread(this.javaClass.simpleName)
        handlerThread.start()
        handler = Handler(handlerThread.looper)
    }

    abstract fun requestIntentUpdated(currentRequest: ProviderRequestUnbundled?, pendingIntent: PendingIntent)

    abstract fun stopIntentUpdated(pendingIntent: PendingIntent)

    abstract fun extractLocation(intent: Intent): Location?

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Binder.getCallingUid() == Process.myUid() && intent?.action == ACTION_REPORT_LOCATION) {
            handler.post {
                val location = extractLocation(intent)
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
                    IntentLocationProviderPreTiramisu(this, properties)

                else ->
                    @Suppress("DEPRECATION")
                    (IntentLocationProviderPreTiramisu(this, properties, Unit))
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

    abstract val minIntervalMillis: Long
    abstract val minReportMillis: Long
    abstract val properties: ProviderPropertiesUnbundled
    abstract val providerName: String
}