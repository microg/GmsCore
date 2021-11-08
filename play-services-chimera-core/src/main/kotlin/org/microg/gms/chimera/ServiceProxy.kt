/*
 * SPDX-FileCopyrightText: 2021, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package org.microg.gms.chimera

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import com.google.android.chimera.Service.ProxyCallbacks
import android.os.IBinder
import com.google.android.chimera.Service
import java.io.FileDescriptor
import java.io.PrintWriter

abstract class ServiceProxy(private val loader: ServiceLoader) : android.app.Service(), ProxyCallbacks {
    private var actualService: Service? = null
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)

        if (actualService == null) {
            val service = loader.loadService(base)
            actualService = service
            service.setProxy(this, this)
        }
    }

    override fun dump(fs: FileDescriptor, writer: PrintWriter, args: Array<String>) {
        if (actualService != null) {
            actualService!!.publicDump(fs, writer, args)
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return if (actualService != null) {
            actualService!!.onBind(intent)
        } else null
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        if (actualService != null) {
            actualService!!.onConfigurationChanged(newConfig)
        }
    }

    override fun onCreate() {
        if (actualService != null) {
            actualService!!.onCreate()
        }
    }

    override fun onDestroy() {
        if (actualService != null) {
            actualService!!.onDestroy()
        }
    }

    override fun onLowMemory() {
        if (actualService != null) {
            actualService!!.onLowMemory()
        }
    }

    override fun onRebind(intent: Intent) {
        if (actualService != null) {
            if (intent != null) intent.setExtrasClassLoader(actualService!!.classLoader)
            actualService!!.onRebind(intent)
        }
    }

    override fun onStart(intent: Intent, startId: Int) {
        if (actualService != null) {
            if (intent != null) intent.setExtrasClassLoader(actualService!!.classLoader)
            actualService!!.onStart(intent, startId)
        } else {
            stopSelf(startId)
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        return if (actualService != null) {
            if (intent != null) intent.setExtrasClassLoader(actualService!!.classLoader)
            actualService!!.onStartCommand(intent, flags, startId)
        } else {
            super.onStartCommand(intent, flags, startId)
        }
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        if (actualService != null) {
            if (rootIntent != null) rootIntent.setExtrasClassLoader(actualService!!.classLoader)
            actualService!!.onTaskRemoved(rootIntent)
        }
    }

    override fun onTrimMemory(level: Int) {
        if (actualService != null) {
            actualService!!.onTrimMemory(level)
        }
    }

    override fun onUnbind(intent: Intent): Boolean {
        return if (actualService != null) {
            if (intent != null) intent.setExtrasClassLoader(actualService!!.classLoader)
            actualService!!.onUnbind(intent)
        } else {
            false
        }
    }

    override fun superOnCreate() {
        super.onCreate()
    }

    override fun superOnDestroy() {
        super.onDestroy()
    }

    override fun superOnStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }

    override fun superStopSelf() {
        super.stopSelf()
    }

    override fun superStopSelf(startId: Int) {
        super.stopSelf(startId)
    }

    override fun superStopSelfResult(startId: Int): Boolean {
        return super.stopSelfResult(startId)
    }
}
