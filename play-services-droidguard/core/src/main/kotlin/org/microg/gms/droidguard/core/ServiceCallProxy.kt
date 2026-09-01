/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.droidguard.core

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Proxy

private const val TAG = "DroidGuard"

/**
 * Helper to block the call to dump() on services
 *
 * We may want to extend this later to allow for arbitrary service call interceptions
 */
@SuppressLint("PrivateApi")
object ServiceCallProxy {
    private var serviceManagerClass: Class<*>? = null
    private var getServiceMethod: Method? = null
    private var activeServices: MutableMap<String, IBinder?>? = null
    private val proxyEnabled: MutableMap<String, Boolean> = hashMapOf()
    private val originalServices: MutableMap<String, IBinder> = hashMapOf()

    init {
        try {
            serviceManagerClass = Class.forName("android.os.ServiceManager")
            getServiceMethod =
                serviceManagerClass!!.getDeclaredMethod("getService", String::class.java)
            getServiceMethod!!.isAccessible = true
            val cacheField: Field = serviceManagerClass!!.getDeclaredField("sCache")
            cacheField.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            activeServices = cacheField.get(null) as MutableMap<String, IBinder?>?
        } catch (e: Exception) {
            Log.w(TAG, "Error configuring DumpBlockingProxy", e)
        }
    }

    fun maySetBlockDumpForService(context: Context, systemServiceName: String) {
        val permissionResult =
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.DUMP)
        if (permissionResult == PackageManager.PERMISSION_DENIED) {
            setBlockDumpForService(systemServiceName, true)
        } else if (permissionResult == PackageManager.PERMISSION_GRANTED) {
            setBlockDumpForService(systemServiceName, false)
        }
    }

    fun setBlockDumpForService(systemServiceName: String, enabled: Boolean) {
        if (proxyEnabled.containsKey(systemServiceName) && proxyEnabled[systemServiceName] == enabled) return
        if (getServiceMethod == null || activeServices == null) return
        Log.d(TAG, "Configuring blocker for dump() on service $systemServiceName")
        if (enabled) {
            val originalService = try {
                getServiceMethod!!.invoke(null, systemServiceName) as IBinder
            } catch (e: Exception) {
                Log.w(TAG, e)
                return
            }
            val newService = Proxy.newProxyInstance(
                IBinder::class.java.getClassLoader(),
                arrayOf<Class<*>>(IBinder::class.java),
                { _, method, args ->
                    when (method.name) {
                        "dump" -> {
                            Log.d(TAG, "Blocking dump() call on $systemServiceName");
                            null
                        }

                        else -> method.invoke(originalService, args)
                    }
                }) as IBinder
            originalServices[systemServiceName] = originalService
            activeServices!![systemServiceName] = newService
            proxyEnabled[systemServiceName] = true
        } else if (proxyEnabled[systemServiceName] == true) {
            activeServices!![systemServiceName] = originalServices[systemServiceName]
            proxyEnabled[systemServiceName] = false
        }
    }
}