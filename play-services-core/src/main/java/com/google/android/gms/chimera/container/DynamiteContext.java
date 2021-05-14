/*
 * SPDX-FileCopyrightText: 2021, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.chimera.container;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Process;
import android.util.Log;

import androidx.annotation.RequiresApi;

import org.microg.gms.common.Constants;

import java.io.File;

import dalvik.system.PathClassLoader;

public class DynamiteContext extends ContextWrapper {
    private static final String TAG = "DynamiteContext";
    private String moduleId;
    private Context originalContext;
    private Context gmsContext;
    private DynamiteContext appContext;

    public DynamiteContext(String moduleId, Context base, Context gmsContext, DynamiteContext appContext) {
        super(base);
        this.moduleId = moduleId;
        this.originalContext = base;
        this.gmsContext = gmsContext;
        this.appContext = appContext;
    }

    @Override
    public ClassLoader getClassLoader() {
        if (new DynamiteModuleInfo(moduleId).isMergeClassLoader()) {
            StringBuilder nativeLoaderDirs = new StringBuilder(gmsContext.getApplicationInfo().nativeLibraryDir);
            if (Build.VERSION.SDK_INT >= 23 && Process.is64Bit()) {
                for (String abi : Build.SUPPORTED_64_BIT_ABIS) {
                    nativeLoaderDirs.append(File.pathSeparator).append(gmsContext.getApplicationInfo().sourceDir).append("!/lib/").append(abi);
                }
            } else if (Build.VERSION.SDK_INT >= 21) {
                for (String abi : Build.SUPPORTED_32_BIT_ABIS) {
                    nativeLoaderDirs.append(File.pathSeparator).append(gmsContext.getApplicationInfo().sourceDir).append("!/lib/").append(abi);
                }
            } else {
                nativeLoaderDirs.append(File.pathSeparator).append(gmsContext.getApplicationInfo().sourceDir).append("!/lib/").append(Build.CPU_ABI);
            }
            return new PathClassLoader(gmsContext.getApplicationInfo().sourceDir, nativeLoaderDirs.toString(), originalContext.getClassLoader());
        } else {
            return gmsContext.getClassLoader();
        }
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
        return new DynamiteContext(moduleId, originalContext.createDeviceProtectedStorageContext(), gmsContext.createDeviceProtectedStorageContext(), appContext);
    }

    public static DynamiteContext create(String moduleId, Context originalContext) {
        try {
            Context gmsContext = originalContext.createPackageContext(Constants.GMS_PACKAGE_NAME, new DynamiteModuleInfo(moduleId).getCreatePackageOptions());
            Context originalAppContext = originalContext.getApplicationContext();
            if (originalAppContext == null || originalAppContext == originalContext) {
                return new DynamiteContext(moduleId, originalContext, gmsContext, null);
            } else {
                return new DynamiteContext(moduleId, originalContext, gmsContext, new DynamiteContext(moduleId, originalAppContext, gmsContext, null));
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, e);
            return null;
        }
    }
}
