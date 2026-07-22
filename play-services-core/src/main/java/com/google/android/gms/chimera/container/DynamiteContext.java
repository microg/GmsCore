/*
 * SPDX-FileCopyrightText: 2021, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.chimera.container;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.ApplicationInfo;

import androidx.annotation.RequiresApi;

import com.google.android.gms.chimera.DynamiteContextFactory;

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
            classLoader = DynamiteContextFactory.createClassLoader(moduleInfo, gmsContext, originalContext);
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
}
