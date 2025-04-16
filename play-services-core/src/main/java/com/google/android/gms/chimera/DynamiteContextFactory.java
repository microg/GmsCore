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

import com.google.android.gms.chimera.container.DynamiteContext;
import com.google.android.gms.chimera.container.DynamiteModuleInfo;
import com.google.android.gms.chimera.container.FilteredClassLoader;

import org.microg.gms.common.Constants;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import dalvik.system.PathClassLoader;

public class DynamiteContextFactory {
    private static final String TAG = "DynamiteContextFactory";
    private static final Map<String, DynamiteContext> sContextCache = new WeakHashMap<>();
    // WeakHashMap cannot be used, and there is a high probability that it will be recycled, causing ClassLoader to be rebuilt
    private static final Map<String, ClassLoader> sClassLoaderCache = new HashMap<>();

    public static DynamiteContext createDynamiteContext(String moduleId, Context originalContext) {
        if (originalContext == null) {
            Log.w(TAG, "create <DynamiteContext> Original context is null");
            return null;
        }
        String cacheKey = moduleId + "-" + originalContext.getPackageName();
        synchronized (sContextCache) {
            DynamiteContext cached = sContextCache.get(cacheKey);
            if (cached != null) {
                Log.d(TAG, "Using cached DynamiteContext for cacheKey: " + cacheKey);
                return cached;
            }
        }
        try {
            DynamiteModuleInfo moduleInfo = new DynamiteModuleInfo(moduleId);
            Context gmsContext = originalContext.createPackageContext(Constants.GMS_PACKAGE_NAME, 0);
            Context originalAppContext = originalContext.getApplicationContext();

            DynamiteContext dynamiteContext;
            if (originalAppContext == null || originalAppContext == originalContext) {
                dynamiteContext = new DynamiteContext(moduleInfo, originalContext, gmsContext, null);
            } else {
                dynamiteContext = new DynamiteContext(moduleInfo, originalContext, gmsContext, new DynamiteContext(moduleInfo, originalAppContext, gmsContext, null));
            }
            moduleInfo.init(dynamiteContext);

            synchronized (sContextCache) {
                sContextCache.put(cacheKey, dynamiteContext);
            }
            Log.d(TAG, "Created and cached a new DynamiteContext for cacheKey: " + cacheKey);
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
        ClassLoader classLoader = new PathClassLoader(gmsContext.getApplicationInfo().sourceDir, nativeLoaderDirs.toString(), new FilteredClassLoader(originalContext.getClassLoader(), moduleInfo.getMergedClasses(), moduleInfo.getMergedPackages()));
        synchronized (sClassLoaderCache) {
            sClassLoaderCache.put(cacheKey, classLoader);
        }
        Log.d(TAG, "Created and cached a new ClassLoader for cacheKey: " + cacheKey + " ClassLoader: " + classLoader.hashCode());
        return classLoader;
    }
}

