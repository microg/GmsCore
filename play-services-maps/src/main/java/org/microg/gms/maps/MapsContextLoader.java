/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.maps;

import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import androidx.annotation.Nullable;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.dynamic.ObjectWrapper;
import com.google.android.gms.dynamite.DynamiteModule;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.internal.ICreator;
import com.google.android.gms.maps.model.RuntimeRemoteException;
import org.microg.gms.common.Constants;

public class MapsContextLoader {
    private static final String TAG = "MapsContextLoader";
    private static final String DYNAMITE_MODULE_DEFAULT = "com.google.android.gms.maps_dynamite";
    private static final String DYNAMITE_MODULE_LEGACY = "com.google.android.gms.maps_legacy_dynamite";
    private static final String DYNAMITE_MODULE_LATEST = "com.google.android.gms.maps_core_dynamite";

    private static Context mapsContext;
    private static ICreator creator;

    private static Context getMapsContext(Context context, @Nullable MapsInitializer.Renderer preferredRenderer) {
        if (mapsContext == null) {
            String moduleName;
            if (preferredRenderer == null) {
                moduleName = DYNAMITE_MODULE_DEFAULT;
            } else if (preferredRenderer == MapsInitializer.Renderer.LEGACY) {
                moduleName = DYNAMITE_MODULE_LEGACY;
            } else if (preferredRenderer == MapsInitializer.Renderer.LATEST) {
                moduleName = DYNAMITE_MODULE_LATEST;
            } else {
                moduleName = DYNAMITE_MODULE_DEFAULT;
            }
            Context mapsContext;
            try {
                mapsContext = DynamiteModule.load(context, DynamiteModule.PREFER_REMOTE, moduleName).getModuleContext();
            } catch (Exception e) {
                if (!moduleName.equals(DYNAMITE_MODULE_DEFAULT)) {
                    try {
                        Log.d(TAG, "Attempting to load maps_dynamite again.");
                        mapsContext = DynamiteModule.load(context, DynamiteModule.PREFER_REMOTE, DYNAMITE_MODULE_DEFAULT).getModuleContext();
                    } catch (Exception e2) {
                        Log.e(TAG, "Failed to load maps module, use pre-Chimera", e2);
                        mapsContext = GooglePlayServicesUtil.getRemoteContext(context);
                    }
                } else {
                    Log.e(TAG, "Failed to load maps module, use pre-Chimera", e);
                    mapsContext = GooglePlayServicesUtil.getRemoteContext(context);
                }
            }
            MapsContextLoader.mapsContext = mapsContext;
        }
        return mapsContext;
    }

    public static ICreator getCreator(Context context, @Nullable MapsInitializer.Renderer preferredRenderer) {
        Log.d(TAG, "preferredRenderer: " + preferredRenderer);
        if (creator == null) {
            Log.d(TAG, "Making Creator dynamically");
            try {
                Context mapsContext = getMapsContext(context, preferredRenderer);
                Class<?> clazz = mapsContext.getClassLoader().loadClass("com.google.android.gms.maps.internal.CreatorImpl");
                creator = ICreator.Stub.asInterface((IBinder) clazz.newInstance());
                creator.initV2(ObjectWrapper.wrap(mapsContext.getResources()), Constants.GMS_VERSION_CODE);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Unable to find dynamic class com.google.android.gms.maps.internal.CreatorImpl");
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Unable to call the default constructor of com.google.android.gms.maps.internal.CreatorImpl");
            } catch (InstantiationException e) {
                throw new IllegalStateException("Unable to instantiate the dynamic class com.google.android.gms.maps.internal.CreatorImpl");
            } catch (RemoteException e) {
                throw new RuntimeRemoteException(e);
            }
        }
        return creator;
    }
}
