/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.chimera.context

import android.content.BroadcastReceiver
import android.content.ComponentCallbacks
import android.content.ComponentName
import android.content.Context
import android.content.ContextParams
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.content.pm.ApplicationInfo
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.Handler
import android.util.Log
import android.view.Display
import androidx.annotation.Keep
import com.google.android.chimera.BoundService
import com.google.android.chimera.DynamicBroadcastReceiver
import com.google.android.chimera.annotation.ChimeraApiVersion
import com.google.android.chimera.loader.ChimeraModuleApk

@Keep
open class ModuleContext private constructor() : ContextThemeWrapper(null) {

    companion object {
        private const val TAG = "ModuleContext"
        private val sensorServiceLock = Any()

        @JvmStatic
        fun createApkApplicationContext(
            context: Context,
            chimeraModuleApk: ChimeraModuleApk,
            resources: Resources?,
            classLoader: ClassLoader,
            fulfilledApis: Map<String, Int>,
        ): ModuleContext {
            val newModuleContext = ModuleContext()
            newModuleContext.initializeModuleContext(
                context,
                newModuleContext,
                chimeraModuleApk,
                null,
                -1,
                null,
                "apkappcontext",
                resources,
                classLoader,
                fulfilledApis,
                true
            )
            return newModuleContext
        }

        @JvmStatic
        fun createModuleApplicationContext(
            moduleContext: ModuleContext,
            moduleId: String?,
            moduleVersion: Int,
            subModuleId: String?
        ): ModuleContext {
            val attributionTag = if (subModuleId.isNullOrEmpty()) moduleId else subModuleId
            return ModuleContext().apply {
                initializeModuleContext(
                    moduleContext.containerContext,
                    this,
                    moduleContext.chimeraModuleApk,
                    moduleId,
                    moduleVersion,
                    subModuleId.takeIf { !it.isNullOrEmpty() },
                    attributionTag,
                    moduleContext.resources,
                    moduleContext.moduleClassLoader,
                    moduleContext.fulfilledApis,
                    false
                )
            }
        }

        @JvmStatic
        fun getModuleContext(context: Context): ModuleContext? {
            var currentContext = context
            while (currentContext is ContextWrapper) {
                if (currentContext is ModuleContext) {
                    return currentContext
                }
                currentContext = currentContext.baseContext
            }
            return null
        }
    }

    private lateinit var parentModuleContext: ModuleContext
    private lateinit var containerContext: Context
    private lateinit var chimeraModuleApk: ChimeraModuleApk
    private var moduleId: String? = null
    private var moduleVersion: Int = 0
    private var subModuleId: String? = null
    private var moduleResources: Resources? = null
    private lateinit var containerResources: Resources
    private lateinit var moduleClassLoader: ClassLoader
    private var fulfilledApis = HashMap<String, Int>()
    private var updateResourcesConfiguration = true

    fun initializeModuleContext(
        context: Context,
        parentModuleContext: ModuleContext,
        moduleApk: ChimeraModuleApk,
        moduleId: String?,
        moduleVersion: Int,
        subModuleId: String?,
        attributionTag: String?,
        resources: Resources?,
        classLoader: ClassLoader,
        fulfilledApis: Map<String, Int>,
        updateResourcesConfiguration: Boolean
    ) {
        val superContext: Context? = when {
            attributionTag == null -> context
            else -> {
                val modContext = getModuleContext(context)
                modContext?.createAttributionModuleContext(attributionTag)
                    ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        context.createAttributionContext(attributionTag)
                    } else {
                        null
                    }
            }
        }

        this.attachBaseContext(superContext)
        this.parentModuleContext = parentModuleContext
        this.containerContext = context
        this.chimeraModuleApk = moduleApk
        this.moduleId = moduleId
        this.moduleVersion = moduleVersion
        this.subModuleId = subModuleId
        this.moduleClassLoader = classLoader
        this.fulfilledApis = HashMap(fulfilledApis)
        this.moduleResources = resources
        this.containerResources = context.resources
        this.updateResourcesConfiguration = updateResourcesConfiguration
        if (moduleResources != null && updateResourcesConfiguration) {
            moduleResources?.updateConfiguration(containerResources.configuration, containerResources.displayMetrics)
        }
    }

    protected constructor(
        context: Context,
        moduleContext: ModuleContext,
        moduleResources: Resources?,
        updateResourcesConfiguration: Boolean
    ) : this(
        context, moduleContext,
        if (moduleContext.subModuleId == null) {
            moduleContext.moduleId
        } else {
            moduleContext.subModuleId
        },
        moduleResources, updateResourcesConfiguration
    )

    constructor(
        context: Context,
        moduleContext: ModuleContext,
        moduleId: String?,
        moduleVersion: Int,
        subModuleId: String?,
        resources: Resources?
    ) : this() {
        val attributionTag = if (subModuleId.isNullOrEmpty()) moduleId else subModuleId
        initializeModuleContext(
            context,
            moduleContext,
            moduleContext.chimeraModuleApk,
            moduleId,
            moduleVersion,
            subModuleId.takeIf { !it.isNullOrEmpty() },
            attributionTag,
            resources,
            moduleContext.moduleClassLoader,
            moduleContext.fulfilledApis,
            true
        )
    }

    protected constructor(
        context: Context,
        moduleContext: ModuleContext,
        attributionTag: String?,
        moduleResources: Resources?,
        updateResourcesConfiguration: Boolean
    ) : this() {
        initializeModuleContext(
            context,
            moduleContext.parentModuleContext,
            moduleContext.chimeraModuleApk,
            moduleContext.moduleId,
            moduleContext.moduleVersion,
            moduleContext.subModuleId,
            attributionTag,
            moduleResources,
            moduleContext.moduleClassLoader,
            moduleContext.fulfilledApis,
            updateResourcesConfiguration
        )
    }

    private fun createAttributionModuleContext(attributionTag: String?): ModuleContext {
        val ctx = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            super.createAttributionContext(attributionTag)
        } else {
            this
        }

        return ModuleContext(
            ctx,
            this,
            null,
            this.moduleResources,
            this.updateResourcesConfiguration
        )
    }

    private fun configureBroadcastReceiver(receiver: BroadcastReceiver?) {
        if (receiver is DynamicBroadcastReceiver) {
            (receiver as DynamicBroadcastReceiver).setModuleContext(this)
        }
    }

    override fun createAttributionContext(attributionTag: String?): Context {
        val tag = if (moduleId == null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                getAttributionTag()
            } else {
                attributionTag
            }
        } else {
            attributionTag
        }

        if (tag != null && tag == attributionTag) {
            Log.w(TAG, "Attribution tag: $attributionTag ignored, replaced with: $tag")
        }
        return createAttributionModuleContext(attributionTag)
    }

    override fun createConfigurationContext(config: Configuration): Context {
        if (moduleResources == null) {
            return ModuleContext(super.createConfigurationContext(config), this, null, false)
        }

        try {
            return ModuleContext(super.createConfigurationContext(config), this, chimeraModuleApk.getResources(), false)
        } catch (e: Exception) {
            throw IllegalStateException("Unable to create Resources for module $chimeraModuleApk", e)
        }
    }

    override fun createContext(contextParams: ContextParams): Context {
        return ModuleContext(super.createContext(contextParams), this, moduleResources, updateResourcesConfiguration)
    }

    override fun createDeviceProtectedStorageContext(): Context {
        return ModuleContext(super.createDeviceProtectedStorageContext(), this, moduleResources, updateResourcesConfiguration)
    }

    override fun createDisplayContext(display: Display): Context {
        if (moduleResources == null) {
            return ModuleContext(super.createDisplayContext(display), this, null, true)
        }

        try {
            return ModuleContext(super.createDisplayContext(display), this, chimeraModuleApk.getResources(), true)
        } catch (e: Exception) {
            throw RuntimeException("Failed to create module Resources", e)
        }
    }

    override fun getApplicationContext(): Context {
        return parentModuleContext
    }

    override fun getBaseContext(): Context {
        return this.containerContext
    }

    override fun getClassLoader(): ClassLoader {
        return this.moduleClassLoader
    }

    fun getContainerContext(): Context {
        return this.containerContext
    }

    fun getContainerResources(): Resources {
        return this.containerResources
    }

    fun getFulfilledApis(): Map<String, Int> {
        return this.fulfilledApis
    }

    fun getModuleApk(): ChimeraModuleApk {
        return this.chimeraModuleApk
    }

    fun getModuleId(): String? {
        return this.moduleId
    }

    fun getModuleVersion(): Int {
        return this.moduleVersion
    }

    override fun getResources(): Resources {
        return this.moduleResources ?: this.containerResources
    }

    fun getSubmoduleId(): String? {
        return this.subModuleId
    }

    override fun getSystemService(name: String): Any? {
        return when (name) {
            "sensor" -> {
                synchronized(sensorServiceLock) {
                    super.getSystemService(name)
                }
            }

            "window" -> {
                containerContext.getSystemService(name)
            }

            "user", "wifi", "connectivity" -> {
                parentModuleContext.baseContext.getSystemService(name)
            }

            else -> super.getSystemService(name)
        }
    }

    override fun registerComponentCallbacks(callbacks: ComponentCallbacks?) {
        super.getApplicationContext().registerComponentCallbacks(callbacks)
    }

    override fun registerReceiver(receiver: BroadcastReceiver?, filter: IntentFilter?): Intent? {
        configureBroadcastReceiver(receiver)
        return super.registerReceiver(receiver, filter)
    }

    override fun registerReceiver(
        receiver: BroadcastReceiver?,
        filter: IntentFilter?,
        flags: Int
    ): Intent? {
        configureBroadcastReceiver(receiver)
        return super.registerReceiver(receiver, filter, flags)
    }

    override fun registerReceiver(
        receiver: BroadcastReceiver?,
        filter: IntentFilter?,
        broadcastPermission: String?,
        scheduler: Handler?
    ): Intent? {
        configureBroadcastReceiver(receiver)
        return super.registerReceiver(receiver, filter, broadcastPermission, scheduler)
    }

    override fun registerReceiver(
        receiver: BroadcastReceiver?,
        filter: IntentFilter?,
        broadcastPermission: String?,
        scheduler: Handler?,
        flags: Int
    ): Intent? {
        configureBroadcastReceiver(receiver)
        return super.registerReceiver(receiver, filter, broadcastPermission, scheduler, flags)
    }

    override fun getPackageManager(): PackageManager {
        return ModulePackageManager(containerContext.packageManager)
    }

    override fun startService(service: Intent?): ComponentName? {
        Log.d(TAG, "startService: $service")
        if (service != null) {
            val chimIntent = resolveChimeraServiceIntent(service)
            if (chimIntent != null) return containerContext.startService(chimIntent)
        }
        return containerContext.startService(service)
    }

    override fun bindService(service: Intent, conn: ServiceConnection, flags: Int): Boolean {
        Log.d(TAG, "bindService: $service flags=$flags")
        val chimIntent = resolveChimeraServiceIntent(service)
        if (chimIntent != null) return containerContext.bindService(chimIntent, conn, flags)
        return containerContext.bindService(service, conn, flags)
    }

    private fun resolveChimeraServiceIntent(intent: Intent): Intent? {
        val action = intent.action ?: return null
        if (action.startsWith("com.google.android.chimera.")) return null // already chimera
        return BoundService.getStartIntent(containerContext, action)
    }

    override fun unregisterComponentCallbacks(callbacks: ComponentCallbacks?) {
        super.getApplicationContext().unregisterComponentCallbacks(callbacks)
    }

    override fun unregisterReceiver(receiver: BroadcastReceiver?) {
        if ((receiver is DynamicBroadcastReceiver)) {
            (receiver as DynamicBroadcastReceiver).setModuleContext(null)
        }

        super.unregisterReceiver(receiver)
    }
}
