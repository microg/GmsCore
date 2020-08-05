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
import android.content.ContextWrapper;
import android.content.pm.PackageManager;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.dynamic.ObjectWrapper;
import com.google.android.gms.dynamite.IDynamiteLoader;

import org.microg.gms.common.Constants;

public class DynamiteLoaderImpl extends IDynamiteLoader.Stub {
    private static final String TAG = "GmsDynamiteLoaderImpl";

    @Override
    public IObjectWrapper createModuleContext(IObjectWrapper wrappedContext, String moduleId, int minVersion) throws RemoteException {
        Log.d(TAG, "unimplemented Method: createModuleContext for " + moduleId + " at version " + minVersion + ", returning gms context");
        final Context context = (Context) ObjectWrapper.unwrap(wrappedContext);
        try {
            return ObjectWrapper.wrap(new ContextWrapper(context.createPackageContext(Constants.GMS_PACKAGE_NAME, Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY)) {
                @Override
                public Context getApplicationContext() {
                    return context;
                }
            });
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "returning null instead", e);
            return null;
        }
    }

    @Override
    public int getModuleVersion(IObjectWrapper context, String moduleId) throws RemoteException {
        return getModuleVersion2(context, moduleId, true);
    }

    @Override
    public int getModuleVersion2(IObjectWrapper context, String moduleId, boolean updateConfigIfRequired) throws RemoteException {
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
}
