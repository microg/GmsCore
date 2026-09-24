/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.chimera;

import static android.os.Build.CPU_ABI;
import static android.os.Build.SUPPORTED_32_BIT_ABIS;
import static android.os.Build.SUPPORTED_64_BIT_ABIS;
import static android.os.Build.VERSION.SDK_INT;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Process;
import android.util.Log;

import com.google.android.chimera.config.ChimeraStorage;
import com.google.android.chimera.loader.ChimeraModuleLdr;
import com.google.android.chimera.config.registry.DynamicModuleRegistry;
import com.google.android.gms.chimera.container.DynamiteContext;
import com.google.android.gms.chimera.container.DynamiteModuleInfo;
import com.google.android.gms.chimera.container.FilteredClassLoader;

import org.microg.gms.common.Constants;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

import dalvik.system.PathClassLoader;

public class DynamiteContextFactory {
    private static final String TAG = "DynamiteContextFactory";
    private static final Map<String, Context> sContextCache = new WeakHashMap<>();
    // Must not use WeakHashMap here: entries would likely be reclaimed, forcing the ClassLoader to be rebuilt
    private static final Map<String, ClassLoader> sClassLoaderCache = new HashMap<>();

    public static void clearCacheForModule(String moduleId) {
        if (moduleId == null || moduleId.isEmpty()) return;
        String prefix = moduleId + "-";
        removeCacheEntries(sContextCache, prefix);
        removeCacheEntries(sClassLoaderCache, prefix);
        Log.d(TAG, "Cleared Dynamite caches for moduleId: " + moduleId);
    }

    private static void removeCacheEntries(Map<String, ?> cache, String prefix) {
        synchronized (cache) {
            Iterator<String> iterator = cache.keySet().iterator();
            while (iterator.hasNext()) {
                if (iterator.next().startsWith(prefix)) iterator.remove();
            }
        }
    }

    /**
     * The third argument remains for the upstream-compatible call shape. It is deliberately not
     * trusted: actual module selection and cache identity come from the verified local config.
     */
    public static Context createDynamiteContext(String moduleId, Context originalContext, String ignoredLoaderPath) {
        Log.d(TAG, "create <DynamiteContext> moduleId: " + moduleId);
        if (originalContext == null) {
            Log.w(TAG, "create <DynamiteContext> Original context is null");
            return null;
        }
        try {
            DynamiteModuleInfo moduleInfo = new DynamiteModuleInfo(moduleId);
            Context gmsContext = originalContext.createPackageContext(Constants.GMS_PACKAGE_NAME, 0);
            Context originalAppContext = originalContext.getApplicationContext();

            Context dynamiteContext;
            if (originalAppContext == null || originalAppContext == originalContext) {
                dynamiteContext = new DynamiteContext(moduleInfo, originalContext, gmsContext, null);
            } else {
                dynamiteContext = new DynamiteContext(moduleInfo, originalContext, gmsContext, new DynamiteContext(moduleInfo, originalAppContext, gmsContext, null));
            }
            moduleInfo.init(dynamiteContext);
            Log.d(TAG, "init " + moduleInfo.getModuleId());
            // Module downloads may be written by another process. Refresh cached manifest
            // before resolving module metadata in this process.
            try {
                com.google.android.chimera.config.ChimeraConfigManager.INSTANCE.reload();
            } catch (Exception ignored) {
            }
            String lookupModuleId = moduleInfo.getModuleId();
            // Prefer the installed module's moduleName/version (from chimera_manifest.pb); fall back to the registry when not installed
            com.google.android.chimera.config.ChimeraModule installedForCtx =
                    com.google.android.chimera.config.ChimeraConfigManager.INSTANCE.findModuleByModuleId(lookupModuleId);
            DynamicModuleRegistry.DynamicModule moduleEntry = DynamicModuleRegistry.INSTANCE.getByModuleId(lookupModuleId);
            // A replacement can reuse the same module ID and version. The persisted digest, rather than a
            // caller-supplied loader path, therefore separates cache entries across verified artifacts.
            String cacheIdentity = "builtin";
            if (installedForCtx != null) {
                String installedPath = installedForCtx.installedApkPath;
                if (installedPath == null || ChimeraStorage.INSTANCE.verifiedModuleApk(
                        originalContext, new File(installedPath), null, installedForCtx.apkSha256) == null) {
                    Log.w(TAG, "No verified dynamic module available for " + lookupModuleId);
                    return null;
                }
                String installedDigest = installedForCtx.apkSha256;
                cacheIdentity = installedDigest != null && !installedDigest.isEmpty()
                        ? installedDigest : "unverified";
            }
            String cacheKey = lookupModuleId + "-" + originalContext.getPackageName() + "-" + cacheIdentity;
            synchronized (sContextCache) {
                Context cached = sContextCache.get(cacheKey);
                if (cached != null) {
                    Log.d(TAG, "Using cached DynamiteContext for moduleId: " + lookupModuleId);
                    return cached;
                }
            }

            if (installedForCtx != null || moduleEntry != null) {
                String moduleName = (installedForCtx != null && installedForCtx.moduleName != null
                        && !installedForCtx.moduleName.isEmpty())
                        ? installedForCtx.moduleName
                        : (moduleEntry != null ? moduleEntry.getModuleName() : "");
                int moduleVersion;
                if (installedForCtx != null && installedForCtx.moduleVersion != null) {
                    int pv = 0;
                    try {
                        pv = Integer.parseInt(installedForCtx.moduleVersion);
                    } catch (NumberFormatException ignored) {
                    }
                    moduleVersion = pv;
                } else {
                    // installedForCtx is null for a feature sub-moduleId (e.g. mlkit_docscan_detect) that has no
                    // config entry of its own. Inherit the installed parent module's version (resolved by
                    // moduleName) instead of persisting 0 below: a 0 here gets written into the config entry and
                    // then makes every later getModuleVersion2(sub-moduleId) report "not installed", wrongly
                    // triggering the (now removed) install flow and breaking the feature on its next use.
                    com.google.android.chimera.config.ChimeraModule parent = moduleName.isEmpty() ? null
                            : com.google.android.chimera.config.ChimeraConfigManager.INSTANCE.findInstalledModuleByName(moduleName);
                    int pv = 0;
                    if (parent != null && parent.moduleVersion != null) {
                        try {
                            pv = Integer.parseInt(parent.moduleVersion);
                        } catch (NumberFormatException ignored) {
                        }
                    }
                    moduleVersion = pv;
                }

                // The loader resolves only configuration records whose artifact has passed integrity checks.
                Context moduleData = ChimeraModuleLdr.INSTANCE.loadModule(
                        dynamiteContext, lookupModuleId, moduleName, moduleVersion);
                if (moduleData != null) {
                    dynamiteContext = moduleData;
                    Log.d(TAG, "Module loaded via ChimeraModuleLdr: " + moduleInfo.getModuleId());
                } else {
                    Log.w(TAG, "No verified dynamic module available for " + moduleInfo.getModuleId());
                }
            } else {
                Log.d(TAG, "No DynamicModuleRegistry entry for " + moduleInfo.getModuleId());
            }
            Log.d(TAG, "DC createClassLoader " + moduleInfo.getModuleId() + " ClassLoader: " + dynamiteContext.getClassLoader());
            synchronized (sContextCache) {
                sContextCache.put(cacheKey, dynamiteContext);
            }
            Log.d(TAG, "Created and cached a new DynamiteContext for moduleId: " + lookupModuleId);
            return dynamiteContext;
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, e);
            return null;
        }
    }

    public static ClassLoader createClassLoader(DynamiteModuleInfo moduleInfo, Context gmsContext, Context originalContext) {
        String cacheKey = moduleInfo.getModuleId() + "-" + originalContext.getPackageName();
        synchronized (sClassLoaderCache) {
            ClassLoader cached = sClassLoaderCache.get(cacheKey);
            if (cached != null) {
                Log.d(TAG, "Using cached ClassLoader for cacheKey: " + cacheKey + " cached: " + cached.hashCode());
                return cached;
            }
        }
        StringBuilder nativeLoaderDirs = new StringBuilder(gmsContext.getApplicationInfo().nativeLibraryDir);
        if (SDK_INT >= 23 && Process.is64Bit()) {
            for (String abi : SUPPORTED_64_BIT_ABIS) {
                nativeLoaderDirs.append(File.pathSeparator).append(gmsContext.getApplicationInfo().sourceDir).append("!/lib/").append(abi);
            }
        } else if (SDK_INT >= 21) {
            for (String abi : SUPPORTED_32_BIT_ABIS) {
                nativeLoaderDirs.append(File.pathSeparator).append(gmsContext.getApplicationInfo().sourceDir).append("!/lib/").append(abi);
            }
        } else {
            nativeLoaderDirs.append(File.pathSeparator).append(gmsContext.getApplicationInfo().sourceDir).append("!/lib/").append(CPU_ABI);
        }
        Collection<String> mergedClasses = moduleInfo.getMergedClasses();
        Collection<String> mergedPackages = moduleInfo.getMergedPackages();

        // Some module descriptors do not publish merge allow-lists. In that case,
        // using FilteredClassLoader blocks non-boot dependencies (e.g. obfuscated
        // host classes like m38.*), causing module activity class resolution to fail.
        ClassLoader parent = originalContext.getClassLoader();
        if (!mergedClasses.isEmpty() || !mergedPackages.isEmpty()) {
            parent = new FilteredClassLoader(parent, mergedClasses, mergedPackages);
        } else {
            Log.d(TAG, "No merged class/package allow-list for " + moduleInfo.getModuleId() + ", using unfiltered parent ClassLoader");
        }

        ClassLoader classLoader = createSelfFirstClassLoader(gmsContext.getApplicationInfo().sourceDir, nativeLoaderDirs.toString(), parent);
        synchronized (sClassLoaderCache) {
            sClassLoaderCache.put(cacheKey, classLoader);
        }
        Log.d(TAG, "Created and cached a new ClassLoader for cacheKey: " + cacheKey + " ClassLoader: " + classLoader.hashCode());
        return classLoader;
    }

    // Dynamite modules run inside the *client's* process. With a parent-first PathClassLoader, any class the
    // client also bundles — notably official @SafeParcelable data classes like GoogleCertificatesLookupQuery or
    // measurement's InitializationParams/ScionActivityInfo — resolves to the client's official version while the
    // module's service code is ours, causing ABI crashes (NoSuchFieldError on CREATOR). Loading self-first makes
    // module-bundled classes (our data classes, present in the GMS APK) load from the module, while host-only
    // classes (obfuscated m38.* etc., absent from our APK) still fall through to the client. Official GMS doesn't
    // need this — its module code and the client's data classes come from the same build, ABI-identical regardless
    // of load order; microG's data classes differ, so explicit self-first isolation is required. This makes every
    // built-in dynamite service module (googlecertificates, measurement, ...) safe at once, replacing per-module
    // MERGED_CLASSES workarounds and covering future modules automatically.
    private static ClassLoader createSelfFirstClassLoader(String dexPath, String librarySearchPath, ClassLoader parent) {
        if (SDK_INT >= 27) { // DelegateLastClassLoader (self-first) available since API 27 (Android 8.1)
            return new dalvik.system.DelegateLastClassLoader(dexPath, librarySearchPath, parent);
        }
        // Pre-27 fallback: keep parent-first. Such old devices rarely run client SDKs that bundle the conflicting
        // official data classes, so the residual risk is negligible.
        return new PathClassLoader(dexPath, librarySearchPath, parent);
    }
}

