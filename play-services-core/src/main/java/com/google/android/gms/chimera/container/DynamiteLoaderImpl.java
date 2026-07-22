/*
 * Copyright (C) 2013-2017 microG Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.gms.chimera.container;

import android.content.Context;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.chimera.DynamiteContextFactory;
import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.dynamic.ObjectWrapper;
import com.google.android.gms.dynamite.IDynamiteLoader;

public class DynamiteLoaderImpl extends IDynamiteLoader.Stub {
    private static final String TAG = "GmsDynamiteLoaderImpl";

    @Override
    public IObjectWrapper createModuleContext(IObjectWrapper wrappedContext, String moduleId, int minVersion) throws RemoteException {
        // We don't have crash utils, so just forward
        return createModuleContextV2(wrappedContext, moduleId, minVersion);
    }

    @Override
    public IObjectWrapper createModuleContextV2(IObjectWrapper wrappedContext, String moduleId, int minVersion) throws RemoteException {
        Log.d(TAG, "createModuleContext for " + moduleId + " at version " + minVersion);
        final Context originalContext = (Context) ObjectWrapper.unwrap(wrappedContext);
        return ObjectWrapper.wrap(DynamiteContextFactory.createDynamiteContext(moduleId, originalContext));
    }

    @Override
    public IObjectWrapper createModuleContextV3(IObjectWrapper wrappedContext, String moduleId, int minVersion, IObjectWrapper wrappedCursor) throws RemoteException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getIDynamiteLoaderVersion() throws RemoteException {
        return 2;
    }

    @Override
    public int getModuleVersion(IObjectWrapper wrappedContext, String moduleId) throws RemoteException {
        return getModuleVersion2(wrappedContext, moduleId, true);
    }

    @Override
    public int getModuleVersion2(IObjectWrapper wrappedContext, String moduleId, boolean updateConfigIfRequired) throws RemoteException {
        // We don't have crash utils, so just forward
        return getModuleVersionV2(wrappedContext, moduleId, updateConfigIfRequired);
    }

    @Override
    public int getModuleVersionV2(IObjectWrapper wrappedContext, String moduleId, boolean updateConfigIfRequired) throws RemoteException {
        final Context context = (Context) ObjectWrapper.unwrap(wrappedContext);
        if (context == null) {
            Log.w(TAG, "Invalid client context");
            return 0;
        }

        try {
            return Class.forName("com.google.android.gms.dynamite.descriptors." + moduleId + ".ModuleDescriptor").getDeclaredField("MODULE_VERSION").getInt(null);
        } catch (Exception e) {
            Log.w(TAG, "No such module known: " + moduleId);
        }

        if (moduleId.equals("com.google.android.gms.firebase_database")) {
            Log.d(TAG, "returning temp fix module version for " + moduleId + ". Firebase Database will not be functional!");
            return com.google.android.gms.dynamite.descriptors.com.google.android.gms.firebase_database.ModuleDescriptor.MODULE_VERSION;
        }
        if (moduleId.equals("com.google.android.gms.googlecertificates")) {
            return com.google.android.gms.dynamite.descriptors.com.google.android.gms.googlecertificates.ModuleDescriptor.MODULE_VERSION;
        }
        if (moduleId.equals("com.google.android.gms.cast.framework.dynamite")) {
            Log.d(TAG, "returning temp fix module version for " + moduleId + ". Cast API wil not be functional!");
            return 1;
        }

        if (moduleId.equals("com.google.android.gms.maps_dynamite")) {
            Log.d(TAG, "returning v1 for maps");
            return 1;
        }

        Log.d(TAG, "unimplemented Method: getModuleVersion for " + moduleId);
        return 0;
    }

    @Override
    public IObjectWrapper getModuleVersionV3(IObjectWrapper wrappedContext, String moduleId, boolean updateConfigIfRequired, long requestStartTime) throws RemoteException {
        throw new UnsupportedOperationException();
    }
}
