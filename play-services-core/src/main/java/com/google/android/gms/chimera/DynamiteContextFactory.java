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
import java.util.Map;
import java.util.WeakHashMap;

import dalvik.system.PathClassLoader;

public class DynamiteContextFactory {
    private static final String TAG = "DynamiteContextFactory";
    private static final Map<Context, DynamiteContext> sContextCache = new WeakHashMap<>();
    private static final Map<DynamiteModuleInfo, ClassLoader> sClassLoaderCache = new WeakHashMap<>();

    public static DynamiteContext createDynamiteContext(String moduleId, Context originalContext) {
        if (originalContext == null) {
            Log.w(TAG, "create <DynamiteContext> Original context is null");
            return null;
        }
        synchronized (sContextCache) {
            DynamiteContext cached = sContextCache.get(originalContext);
            if (cached != null) {
                Log.d(TAG, "Using cached DynamiteContext for original context: " + originalContext);
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
                sContextCache.put(originalContext, dynamiteContext);
            }
            Log.d(TAG, "Created and cached a new DynamiteContext for original context: " + originalContext);
            return dynamiteContext;
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, e);
            return null;
        }
    }

    public static ClassLoader createClassLoader(DynamiteModuleInfo moduleInfo, Context gmsContext, Context originalContext) {
        if (moduleInfo == null) {
            Log.w(TAG, "create <ClassLoader> moduleInfo is null");
            return null;
        }
        synchronized (sClassLoaderCache) {
            ClassLoader cached = sClassLoaderCache.get(moduleInfo);
            if (cached != null) {
                Log.d(TAG, "Using cached ClassLoader for moduleInfo: " + moduleInfo.getModuleId());
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
            sClassLoaderCache.put(moduleInfo, classLoader);
        }
        Log.d(TAG, "Created and cached a new ClassLoader for moduleInfo: " + moduleInfo.getModuleId());
        return classLoader;
    }
}

