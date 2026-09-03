/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.chimera.android

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.AssetManager
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.os.StrictMode
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import com.google.android.chimera.InstanceProvider
import com.google.android.chimera.component.BaseActivityProxy
import com.google.android.chimera.component.ChimeraComponentProxy
import com.google.android.chimera.component.ChimeraFallbackActImpl
import com.google.android.chimera.component.ChimeraModuleContextProvider
import com.google.android.chimera.component.ChimeraPermissionActImpl
import com.google.android.chimera.component.ChimeraProxyCallback
import com.google.android.chimera.component.ContainerApk
import com.google.android.chimera.config.ChimeraApkManifestReader
import com.google.android.chimera.config.ChimeraConfigManager
import com.google.android.chimera.config.ChimeraModuleBootstrap
import com.google.android.chimera.config.DynamicModuleSettings
import com.google.android.chimera.config.ModuleDownloadRegistry
import com.google.android.chimera.context.GmsContextWrapper
import com.google.android.chimera.context.ModuleContext
import com.google.android.chimera.loader.ChimeraModuleLdr
import com.google.android.chimera.util.ChimeraResource
import com.google.android.chimera.util.ChimeraViewCreator
import java.lang.reflect.Constructor
import java.lang.reflect.Method

open class ChimeraActivityProxy : BaseActivityProxy(), IChimeraActivityProxy, ChimeraModuleContextProvider {
    companion object {
        private const val TAG = "ChimeraActivityProxy"
        private const val REQUEST_MODULE_PERMISSION = 0x4348
        private const val STATE_MODULE = "_chimera_module_state"
        private const val STATE_FEATURE_REQUEST = "_chimera_attempt_ftr_req"
    }

    var hasFeatureRequest = false
    private var isAttachingBaseContext = false
    private var permissionRedirectLaunched = false
    private var activityImpl: Activity? = null
    private var layoutInflater: LayoutInflater? = null
    private lateinit var containerClassLoader: ClassLoader
    private lateinit var moduleClassLoader: ClassLoader
    private lateinit var currentClassLoader: ClassLoader

    private enum class DynamicComponentResolution {
        LOADED,
        PERMISSION_REQUIRED,
        UNAVAILABLE
    }

    private fun resolveDynamicComponent(): DynamicComponentResolution {
        val currentClassName = javaClass.name
        if (!DynamicModuleSettings.isAvailable(this)) {
            Log.d(TAG, "Dynamic modules unavailable for $currentClassName")
            return DynamicComponentResolution.UNAVAILABLE
        }
        val oldPolicy = StrictMode.allowThreadDiskWrites()
        try {
            val route = ChimeraConfigManager.findComponentByComponentName(currentClassName)
            if (route == null) {
                Log.w(TAG, "Chimera component route not found for $currentClassName (prefix=${ChimeraConfigManager.getChimeraPrefix()})")
                return DynamicComponentResolution.UNAVAILABLE
            }

            val module = ChimeraConfigManager.findModuleByComponent(currentClassName)
            if (module == null) {
                Log.w(TAG, "Chimera module not found for $currentClassName")
                return DynamicComponentResolution.UNAVAILABLE
            }
            val routeModuleId = route.moduleId.orEmpty()
            val verifiedRoute = routeModuleId.isNotEmpty() &&
                ChimeraApkManifestReader.readVerifiedCapabilities(this, module).any { capability ->
                    capability.moduleId == routeModuleId &&
                        capability.activityBindings.any { binding ->
                            binding.containerName == route.containerName &&
                                binding.moduleChimeraName == route.moduleChimeraName
                        }
                }
            if (!verifiedRoute) {
                Log.w(TAG, "Chimera activity route is not declared by the verified module APK: $currentClassName")
                return DynamicComponentResolution.UNAVAILABLE
            }

            // Permission is part of the module's execution precondition. Check it before creating
            // the module ClassLoader or invoking ModuleApi initialization so revoked permissions
            // cannot run module code while the authorization page is being shown.
            val requestedFeatureNames =
                ModuleDownloadRegistry.requestedFeatureNamesForActivity(this, currentClassName)
            if (ModuleDownloadRegistry.hasMissingPermissions(this, requestedFeatureNames)) {
                Log.w(TAG, "Required permission missing for installed module: $currentClassName")
                return DynamicComponentResolution.PERMISSION_REQUIRED
            }

            val moduleVersion = module.moduleVersion?.toIntOrNull() ?: 0
            val moduleData = try {
                ChimeraModuleLdr.loadModule(this, routeModuleId, module.moduleName, moduleVersion)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load module for $currentClassName, falling back", e)
                null
            }
            if (moduleData == null) {
                Log.w(TAG, "Module not available for $currentClassName, will use fallback")
                return DynamicComponentResolution.UNAVAILABLE
            }

            var method: Method? = null
            var constructor: Constructor<*>? = null
            try {
                val instanceProviderClass = moduleData.classLoader.loadClass("${ChimeraConfigManager.getChimeraPrefix()}${route.moduleChimeraName}").asSubclass(InstanceProvider::class.java)

                try {
                    constructor = instanceProviderClass.getConstructor()
                } catch (_: NoSuchMethodException) {
                    try {
                        method = instanceProviderClass.getDeclaredMethod("provideInstance")
                    } catch (_: NoSuchMethodException) {
                    }
                }

                val callback = createInstance(
                    Activity::class.java, constructor, arrayOf(), method
                ) as? ChimeraProxyCallback ?: return DynamicComponentResolution.UNAVAILABLE

                ChimeraComponentProxy.bindComponentProxy(this, this, callback, moduleData)
                Log.d(TAG, "instanceProviderClass: $instanceProviderClass")
                return DynamicComponentResolution.LOADED
            } catch (e: ClassNotFoundException) {
                Log.w(TAG, "instanceProviderClass error: $e", e)
            } catch (e: NoClassDefFoundError) {
                Log.w(TAG, "instanceProviderClass dependency missing: ${e.message}", e)
            }

            return DynamicComponentResolution.UNAVAILABLE
        } finally {
            StrictMode.setThreadPolicy(oldPolicy)
        }
    }

    private fun createInstance(clazz: Class<*>, constructor: Constructor<*>?, arguments: Array<Any?>, method: Method?): Any? {
        val instanceProvider = runCatching {
            constructor?.let {
                constructor.newInstance(*arguments) as InstanceProvider
            } ?: method?.let {
                method.invoke(null, null) as? InstanceProvider
            }
        }.getOrElse {
            Log.w(TAG, "Failed to instantiate: provideInstance() returned null")
            return null
        }

        if (instanceProvider == null) {
            Log.w(TAG, "Failed to instantiate: provideInstance() returned null")
            return null
        }

        return try {
            clazz.cast(instanceProvider.getChimeraImpl())
        } catch (e: ClassCastException) {
            Log.w(TAG, "Failed to cast to $clazz")
            return null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        hasFeatureRequest = savedInstanceState?.getBoolean(STATE_FEATURE_REQUEST) ?: true

        if (!hasActivityImpl) {
            Log.w(TAG, "onCreate: module not loaded, falling back to container lifecycle")
            super.onCreate(savedInstanceState)
            return
        }

        val moduleState = extractModuleState(savedInstanceState)
        setBundleClassLoader(moduleState)
        getChimeraActivity().public_onCreate(moduleState)
    }

    override fun onResume() {
        val currentImpl = activityImpl
        val isLoadedDynamicImpl = currentImpl != null &&
            currentImpl !is ChimeraFallbackActImpl &&
            currentImpl !is ChimeraPermissionActImpl
        if (isLoadedDynamicImpl && !DynamicModuleSettings.isAvailable(this)) {
            Log.w(TAG, "Dynamic modules were disabled while ${javaClass.name} was stopped")
            // Complete the platform lifecycle without dispatching into disabled module code.
            super.platform_onResume()
            super.platform_finish()
            return
        }
        if (!permissionRedirectLaunched && isLoadedDynamicImpl) {
            val currentClassName = javaClass.name
            val requestedFeatureNames =
                ModuleDownloadRegistry.requestedFeatureNamesForActivity(this, currentClassName)
            if (ModuleDownloadRegistry.hasMissingPermissions(this, requestedFeatureNames)) {
                val permissionIntent = ModuleDownloadRegistry.createModulePermissionIntent(
                    this,
                    requestedFeatureNames
                )
                if (permissionIntent != null) {
                    permissionRedirectLaunched = true
                    // Satisfy the platform lifecycle without resuming module code after permission revocation.
                    super.platform_onResume()
                    super.platform_startActivityForResult(permissionIntent, REQUEST_MODULE_PERMISSION)
                    return
                }
                Log.w(TAG, "Unable to create permission gate for installed module: $currentClassName")
            }
        }
        super.onResume()
    }

    override fun createContextWrapper(proxy: Any?, context: Context): Context {
        return GmsContextWrapper(context)
    }

    override fun attachBaseContext(newBase: Context) {
        try {
            isAttachingBaseContext = true
            super.attachBaseContext(newBase)
            ChimeraModuleBootstrap.ensureInitialized(newBase)
            runCatching { ChimeraConfigManager.reload() }
            when (resolveDynamicComponent()) {
                DynamicComponentResolution.LOADED -> Unit
                DynamicComponentResolution.PERMISSION_REQUIRED ->
                    bindContainerActivityImpl(newBase, getChimeraPermissionActImpl())
                DynamicComponentResolution.UNAVAILABLE ->
                    bindContainerActivityImpl(newBase, getChimeraFallbackActImpl())
            }
        } finally {
            isAttachingBaseContext = false
        }
    }

    private fun bindContainerActivityImpl(context: Context, callback: ChimeraProxyCallback) {
        val applicationContext = context.applicationContext
        val moduleContext = ModuleContext(
            context,
            ModuleContext.createApkApplicationContext(
                applicationContext,
                ContainerApk(context),
                null,
                applicationContext.classLoader,
                emptyMap()
            ),
            "",
            -1,
            null,
            null,
        )
        ChimeraComponentProxy.bindComponentProxy(this, this, callback, moduleContext)
    }

    open fun getChimeraFallbackActImpl(): ChimeraFallbackActImpl {
        return ChimeraFallbackActImpl()
    }

    open fun getChimeraPermissionActImpl(): ChimeraPermissionActImpl {
        return ChimeraPermissionActImpl()
    }

    override fun getChimeraActivity(): Activity {
        return activityImpl ?: throw IllegalStateException("Activity impl has not been set!")
    }

    private val hasActivityImpl: Boolean get() = activityImpl != null

    override fun clearFeatureRequest() {
        hasFeatureRequest = false
    }

    override fun hasFeatureRequest(): Boolean {
        return hasFeatureRequest
    }

    override fun getTheme(): Resources.Theme {
        return if (isAttachingBaseContext || !hasActivityImpl) {
            super.getTheme()
        } else {
            getChimeraActivity().theme
        }
    }

    override fun setTheme(resid: Int) {
        if (!hasActivityImpl) {
            super.setTheme(resid)
            return
        }
        val newResId = findModuleThemeResId(getChimeraActivity(), resid)
        super.setTheme(newResId)
        getChimeraActivity().setTheme(newResId)
    }

    private fun findModuleThemeResId(activity: Activity, resId: Int): Int {
        if (resId == 0) return 0

        var currentResId = resId
        val visited = mutableSetOf<Int>()

        while (!visited.contains(currentResId)) {
            try {
                val moduleResId = ChimeraResource.getResourceId(
                    moduleClassLoader, activity.resources, super.getResources(), currentResId
                )
                if (moduleResId != 0) return moduleResId
            } catch (_: Resources.NotFoundException) {
            }

            visited.add(currentResId)
            currentResId = getThemeFallback(currentResId)
        }

        Log.w(TAG, "Failed to find module theme for container theme: $resId (tried: $visited)")
        return 0
    }

    protected open fun getThemeFallback(themeResId: Int): Int = themeResId

    override fun platform_getReferrer(): Uri? {
        val referrer = super.platform_getReferrer()
        Log.d(TAG, "platform_getReferrer: $referrer, callingPackage=$callingPackage, callingActivity=$callingActivity")

        if (referrer != null) return referrer

        val intentReferrer = intent?.getParcelableExtra<Uri>("android.intent.extra.REFERRER")
        if (intentReferrer != null) {
            Log.d(TAG, "platform_getReferrer: using intent extra referrer: $intentReferrer")
            return intentReferrer
        }

        val referrerName = intent?.getStringExtra("android.intent.extra.REFERRER_NAME")
        if (referrerName != null) {
            Log.d(TAG, "platform_getReferrer: using intent extra referrer name: $referrerName")
            return Uri.parse(referrerName)
        }

        val pkg = callingPackage
        if (pkg != null) {
            val uri = Uri.parse("android-app://$pkg")
            Log.d(TAG, "platform_getReferrer: constructed from callingPackage: $uri")
            return uri
        }

        Log.w(TAG, "platform_getReferrer: no referrer available")
        return null
    }

    override fun getAssets(): AssetManager {
        return this.resources.assets
    }

    override fun getClassLoader(): ClassLoader {
        return if (this.isAttachingBaseContext || !this::currentClassLoader.isInitialized || !hasActivityImpl) {
            super.getClassLoader()
        } else {
            this.currentClassLoader
        }
    }

    override fun getResources(): Resources {
        return if (this.isAttachingBaseContext || !hasActivityImpl) super.getResources() else this.getChimeraActivity().resources
    }

    override fun getSystemService(serviceName: String): Any? {
        if (!isAttachingBaseContext && hasActivityImpl && "layout_inflater".equals(serviceName)) {
            if (layoutInflater == null) {
                layoutInflater = (super.getSystemService(serviceName) as LayoutInflater).cloneInContext(getChimeraActivity())
            }
            return layoutInflater
        }
        return super.getSystemService(serviceName)
    }

    override fun platform_onCreateView(parent: View?, name: String, context: Context, attrs: AttributeSet): View? {
        if ("fragment" == name) {
            Log.w(TAG, "Chimera does not support inflating fragments via XML at this time.")
            return null
        }
        if (this::moduleClassLoader.isInitialized) {
            val view = ChimeraViewCreator.createView(moduleClassLoader, context, name, attrs)
            if (view != null) return view
        }
        return super.platform_onCreateView(parent, name, context, attrs)
    }

    override fun platform_onCreateView(name: String, context: Context, attrs: AttributeSet): View? {
        if ("fragment" == name) {
            Log.w(TAG, "Chimera does not support inflating fragments via XML at this time.")
            return null
        }
        if (this::moduleClassLoader.isInitialized) {
            val view = ChimeraViewCreator.createView(moduleClassLoader, context, name, attrs)
            if (view != null) return view
        }
        return super.platform_onCreateView(name, context, attrs)
    }

    @SuppressLint("MissingSuperCall")
    override fun onSaveInstanceState(outState: Bundle) {
        if (!hasActivityImpl) return
        val moduleState = Bundle()
        getChimeraActivity().public_onSaveInstanceState(moduleState)
        outState.putBundle(STATE_MODULE, moduleState)
        outState.putBoolean(STATE_FEATURE_REQUEST, hasFeatureRequest)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        if (!hasActivityImpl) {
            super.platform_onRestoreInstanceState(Bundle()); return
        }
        val moduleState = extractModuleState(savedInstanceState)
        if (moduleState == null) {
            super.platform_onRestoreInstanceState(Bundle())
            return
        }
        getChimeraActivity().public_onRestoreInstanceState(moduleState)
    }

    @SuppressLint("MissingSuperCall")
    override fun onPostCreate(savedInstanceState: Bundle?) {
        if (!hasActivityImpl) {
            super.onPostCreate(savedInstanceState)
            return
        }
        // The module implementation calls platform_onPostCreate() through its default superclass.
        // Calling BaseActivityProxy.onPostCreate() here would dispatch to the module once already,
        // then the explicit call below would dispatch a second time.
        val moduleState = if (savedInstanceState != null) extractModuleState(savedInstanceState) else null
        getChimeraActivity().public_onPostCreate(moduleState)
    }

    private fun extractModuleState(savedInstanceState: Bundle?): Bundle? {
        if (savedInstanceState == null) return null
        return savedInstanceState.getBundle(STATE_MODULE)
    }

    private fun setBundleClassLoader(bundle: Bundle?) {
        if (bundle != null && this::moduleClassLoader.isInitialized) {
            bundle.classLoader = moduleClassLoader
        }
    }

    private fun setIntentClassLoader(intent: Intent?) {
        if (intent != null && this::moduleClassLoader.isInitialized) {
            intent.setExtrasClassLoader(moduleClassLoader)
        }
    }

    override fun platform_getIntent(): Intent? {
        val intent = super.platform_getIntent()
        setIntentClassLoader(intent)
        return intent
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_MODULE_PERMISSION) {
            permissionRedirectLaunched = false
            if (resultCode == android.app.Activity.RESULT_OK) {
                // Keep this Activity record so callingPackage and the caller's result target are preserved.
                super.platform_recreate()
            } else {
                super.platform_finish()
            }
            return
        }
        if (!hasActivityImpl) return
        val activity = getChimeraActivity()
        if (data != null) {
            setIntentClassLoader(data)
            if (data.hasExtra("_chimera_fallback_only") && activity !is ChimeraFallbackActImpl) {
                return
            }
        }
        activity.public_onActivityResult(requestCode, resultCode, data)
    }

    override fun onNewIntent(intent: Intent?) {
        if (!hasActivityImpl) return
        setIntentClassLoader(intent)
        getChimeraActivity().public_onNewIntent(intent)
    }

    @Suppress("DEPRECATION")
    override fun platform_overridePendingTransition(enterAnim: Int, exitAnim: Int) {
        if (this::containerClassLoader.isInitialized && hasActivityImpl) {
            val containerRes = super.getResources()
            val moduleRes = getChimeraActivity().resources
            super.platform_overridePendingTransition(
                ChimeraResource.getResourceId(containerClassLoader, containerRes, moduleRes, enterAnim), ChimeraResource.getResourceId(containerClassLoader, containerRes, moduleRes, exitAnim)
            )
        } else {
            super.platform_overridePendingTransition(enterAnim, exitAnim)
        }
    }

    override fun createModuleContext(module: Any?, moduleClass: Class<*>?, context: Context): Context {
        return createContextWrapper(module, context)
    }

    override fun setProxyWrapper(proxy: Any?, context: Context) {
        check(activityImpl == null) { "Activity impl has been set!" }
        activityImpl = proxy as Activity
        activityProxyWrapper = proxy
        moduleClassLoader = context.classLoader
        checkNotNull(javaClass.classLoader) { "Container ClassLoader not been set!" }
        containerClassLoader = javaClass.classLoader
        currentClassLoader = moduleClassLoader
    }

    override fun setProxyWrapper(moduleName: String?, proxy: Any?, context: Context) {
        this.setProxyWrapper(proxy, context)
    }
}
