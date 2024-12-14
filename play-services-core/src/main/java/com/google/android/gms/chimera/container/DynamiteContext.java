/*
 * SPDX-FileCopyrightText: 2021, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.chimera.container;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Process;
import android.util.Log;

import androidx.annotation.RequiresApi;

import org.microg.gms.common.Constants;

import java.io.File;

import dalvik.system.PathClassLoader;

import static android.os.Build.CPU_ABI;
import static android.os.Build.SUPPORTED_32_BIT_ABIS;
import static android.os.Build.SUPPORTED_64_BIT_ABIS;
import static android.os.Build.VERSION.SDK_INT;

public class DynamiteContext extends ContextWrapper {
    private static final String TAG = "DynamiteContext";
    private DynamiteModuleInfo moduleInfo;
    private Context originalContext;
    private Context gmsContext;
    private DynamiteContext appContext;

    private ClassLoader classLoader;

    public DynamiteContext(DynamiteModuleInfo moduleInfo, Context base, Context gmsContext, DynamiteContext appContext) {
        super(base);
        this.moduleInfo = moduleInfo;
        this.originalContext = base;
        this.gmsContext = gmsContext;
        this.appContext = appContext;
    }

    @Override
    public ClassLoader getClassLoader() {
        if (classLoader == null) {
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
            classLoader = new PathClassLoader(gmsContext.getApplicationInfo().sourceDir, nativeLoaderDirs.toString(), new FilteredClassLoader(originalContext.getClassLoader(), moduleInfo.getMergedClasses(), moduleInfo.getMergedPackages()));
        }
        return classLoader;
    }

    @Override
    public String getPackageName() {
        return gmsContext.getPackageName();
    }

    @Override
    public ApplicationInfo getApplicationInfo() {
        return gmsContext.getApplicationInfo();
    }

    @Override
    public Context getApplicationContext() {
        return appContext;
    }

    @RequiresApi(24)
    @Override
    public Context createDeviceProtectedStorageContext() {
        return new DynamiteContext(moduleInfo, originalContext.createDeviceProtectedStorageContext(), gmsContext.createDeviceProtectedStorageContext(), appContext);
    }

    public static DynamiteContext create(String moduleId, Context originalContext) {
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
            return dynamiteContext;
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, e);
            return null;
        }
    }
}
