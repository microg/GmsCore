/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation.core

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import com.google.android.gms.auth.appcert.IAppCertService
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

internal const val APP_CERT_SERVICE_ACTION = "com.google.android.gms.auth.be.appcert.AppCertService"
private const val TAG = "SpatulaHeaderProvider"
private const val BIND_TIMEOUT_SECONDS = 60L
private const val CACHE_TTL_MS = 30L * 60L * 1000L

internal interface SpatulaHeaderProvider {
    fun getSpatulaHeader(packageName: String): String?
}

// Bind AppCertService instead of depending on play-services-core.
internal class AppCertSpatulaHeaderProvider(
    private val context: Context
) : SpatulaHeaderProvider {
    @Volatile
    private var cachedHeader: String? = null

    @Volatile
    private var cachedAtElapsedMs: Long = 0L

    private val lock = Any()

    override fun getSpatulaHeader(packageName: String): String? {
        val cached = cachedHeader
        if (!cached.isNullOrBlank() && !isCacheExpired()) {
            return cached
        }
        synchronized(lock) {
            val lockedCache = cachedHeader
            if (!lockedCache.isNullOrBlank() && !isCacheExpired()) {
                return lockedCache
            }
            val header = fetchFromAppCertService(packageName)
            if (!header.isNullOrBlank()) {
                cachedHeader = header
                cachedAtElapsedMs = SystemClock.elapsedRealtime()
            }
            return header
        }
    }

    private fun isCacheExpired(): Boolean {
        return SystemClock.elapsedRealtime() - cachedAtElapsedMs >= CACHE_TTL_MS
    }

    private fun fetchFromAppCertService(packageName: String): String? {
        val serviceQueue = LinkedBlockingQueue<IAppCertService>(1)
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                service?.let { serviceQueue.offer(IAppCertService.Stub.asInterface(it)) }
            }

            override fun onServiceDisconnected(name: ComponentName?) = Unit
        }
        val intent = Intent(APP_CERT_SERVICE_ACTION).setPackage(context.packageName)
        val bound = try {
            context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        } catch (e: Exception) {
            Log.w(TAG, "Unable to bind AppCertService", e)
            false
        }
        if (!bound) return null

        return try {
            val service = serviceQueue.poll(BIND_TIMEOUT_SECONDS, TimeUnit.SECONDS) ?: return null
            service.getSpatulaHeader(packageName)
        } catch (e: Exception) {
            Log.w(TAG, "AppCertService.getSpatulaHeader failed", e)
            null
        } finally {
            runCatching { context.unbindService(connection) }
        }
    }
}
