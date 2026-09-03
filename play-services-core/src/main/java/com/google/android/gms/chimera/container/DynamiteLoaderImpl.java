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

import android.content.ContentProviderClient;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.chimera.config.ChimeraConfigManager;
import com.google.android.chimera.config.ChimeraModule;
import com.google.android.chimera.config.ChimeraStorage;
import com.google.android.chimera.config.registry.DynamicModuleRegistry;
import com.google.android.gms.chimera.DynamiteContextFactory;
import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.dynamic.ObjectWrapper;
import com.google.android.gms.dynamite.IDynamiteLoader;

import java.io.File;

public class DynamiteLoaderImpl extends IDynamiteLoader.Stub {
    private static final String TAG = "GmsDynamiteLoaderImpl";

    @Override
    public IObjectWrapper createModuleContext(IObjectWrapper wrappedContext, String moduleId, int minVersion) throws RemoteException {
        return createModuleContextNoCrashUtils(wrappedContext, moduleId, minVersion);
    }

    @Override
    public IObjectWrapper createModuleContextNoCrashUtils(IObjectWrapper wrappedContext, String moduleId, int minVersion) throws RemoteException {
        Log.d(TAG, "createModuleContextNoCrashUtils: " + moduleId + " at version " + minVersion);
        return createModuleContext3NoCrashUtils(wrappedContext, moduleId, minVersion, null);
    }

    @Override
    public IObjectWrapper createModuleContext3NoCrashUtils(IObjectWrapper wrappedContext, String moduleId, int minVersion, IObjectWrapper wrappedCursor) throws RemoteException {
        Log.d(TAG, "createModuleContext3NoCrashUtils: " + moduleId + " at version " + minVersion);
        final Context originalContext = (Context) ObjectWrapper.unwrap(wrappedContext);
        if (originalContext == null) {
            Log.w(TAG, "Invalid client context");
            return ObjectWrapper.wrap(null);
        }
        if (isUnavailableDynamicModule(originalContext, moduleId)) {
            Log.d(TAG, "Dynamic module unavailable: " + moduleId);
            return ObjectWrapper.wrap(null);
        }

        if (wrappedCursor != null) {
            android.database.Cursor cursor = (android.database.Cursor) ObjectWrapper.unwrap(wrappedCursor);
            if (cursor != null && cursor.moveToFirst()) {
                int availableVersion = cursor.getInt(0);
                if (availableVersion < minVersion) {
                    Log.e(TAG, "Requested version " + minVersion + " > available " + availableVersion);
                    return ObjectWrapper.wrap(null);
                }
            }
        }

        // Cursor metadata participates in version negotiation only. APK selection is resolved from
        // ChimeraConfigManager's digest-verified record inside DynamiteContextFactory.
        return ObjectWrapper.wrap(DynamiteContextFactory.createDynamiteContext(moduleId, originalContext, null));
    }

    @Override
    public int getIDynamiteLoaderVersion() {
        return 3;
    }

    @Override
    public int getModuleVersion(IObjectWrapper wrappedContext, String moduleId) {
        return getModuleVersion2(wrappedContext, moduleId, true);
    }

    @Override
    public int getModuleVersion2(IObjectWrapper wrappedContext, String moduleId, boolean updateConfigIfRequired) {
        return getModuleVersion2NoCrashUtils(wrappedContext, moduleId, updateConfigIfRequired);
    }

    @Override
    public int getModuleVersion2NoCrashUtils(IObjectWrapper wrappedContext, String moduleId, boolean updateConfigIfRequired) {
        Log.d(TAG, "getModuleVersion2NoCrashUtils: " + moduleId + "----" + updateConfigIfRequired);

        final Context context = (Context) ObjectWrapper.unwrap(wrappedContext);
        if (context == null) {
            Log.w(TAG, "Invalid client context");
            return 0;
        }
        if (isUnavailableDynamicModule(context, moduleId)) {
            Log.d(TAG, "Dynamic module unavailable: " + moduleId);
            return 0;
        }

        // Prefer already-installed modules (dynamic chimera_manifest.pb) and report their actual
        // version (the real version imported from the bundle) before falling back to remote lookup.
        ChimeraModule installed = ChimeraConfigManager.INSTANCE.findModuleByModuleId(moduleId);
        if (installed != null && installed.installedApkPath != null
                && !installed.installedApkPath.isEmpty()
                && ChimeraStorage.INSTANCE.verifiedModuleApk(
                        context, new File(installed.installedApkPath), null, installed.apkSha256) != null) {
            int v = 0;
            try { v = installed.moduleVersion != null ? Integer.parseInt(installed.moduleVersion) : 0; }
            catch (NumberFormatException ignored) {}
            if (v > 0) {
                Log.d(TAG, "getModuleVersion2: " + moduleId + " installed v" + v + " (chimera config)");
                return v;
            }
        }

        DynamicModuleRegistry.DynamicModule moduleEntry = DynamicModuleRegistry.INSTANCE.getByModuleId(moduleId);
        if (moduleEntry != null) {
            // A feature sub-moduleId (e.g. com.google.android.gms.mlkit_docscan_detect) ships inside its parent
            // module and has no config entry of its own, so the exact findModuleByModuleId above misses it; the
            // loader may also have persisted a placeholder entry for it with version 0. Resolve the version from
            // the installed parent module (by moduleName, skipping the version-0 placeholder), matching the
            // feature-availability path (ChimeraModuleManager). Otherwise an already-present feature is reported
            // as "not installed" and the caller triggers the (now removed) install flow, breaking it on reuse.
            ChimeraModule parent = ChimeraConfigManager.INSTANCE.findInstalledModuleByName(moduleEntry.getModuleName());
            if (parent != null && parent.installedApkPath != null
                    && ChimeraStorage.INSTANCE.verifiedModuleApk(
                            context, new File(parent.installedApkPath), null, parent.apkSha256) != null) {
                int pv = 0;
                try { pv = parent.moduleVersion != null ? Integer.parseInt(parent.moduleVersion) : 0; }
                catch (NumberFormatException ignored) {}
                if (pv > 0) {
                    Log.d(TAG, "getModuleVersion2NoCrashUtils: " + moduleId + " provided by installed module "
                            + moduleEntry.getModuleName() + " v" + pv);
                    return pv;
                }
            }

            // Only report a remote version for dynamic modules that are actually available.
            // Otherwise callers skip install flow and jump directly into a missing-resource page.
            Cursor availability = null;
            try {
                availability = queryForDynamiteModule(context, moduleId, updateConfigIfRequired);
                if (availability != null && availability.moveToFirst() && availability.getInt(0) > 0) {
                    int availableVersion = availability.getInt(0);
                    Log.d(TAG, "getModuleVersion2NoCrashUtils: " + moduleId + " available at version " + availableVersion);
                    return availableVersion;
                }
            } catch (Exception e) {
                Log.w(TAG, "Failed to query module availability for " + moduleId, e);
            } finally {
                if (availability != null) {
                    availability.close();
                }
            }
            Log.d(TAG, "getModuleVersion2NoCrashUtils: " + moduleId + " not installed yet, return 0 to trigger install");
            return 0;
        }
        Log.w(TAG, "Failed to retrieve remote feature version.");

        try {
            return Class.forName("com.google.android.gms.dynamite.descriptors." + moduleId + ".ModuleDescriptor").getDeclaredField("MODULE_VERSION").getInt(null);
        } catch (Exception e) {
            Log.w(TAG, "No such module known: " + moduleId);
        }

        switch (moduleId) {
            case "com.google.android.gms.cast.framework.dynamite":
                Log.d(TAG, "returning temp fix module version for " + moduleId + ". Cast API wil not be functional!");
                return 1;
            case "com.google.android.gms.maps_dynamite":
                Log.d(TAG, "returning v1 for maps");
                return 1;
        }

        Log.d(TAG, "unimplemented Method: getModuleVersion for " + moduleId);
        return 0;
    }

    @Override
    public IObjectWrapper queryForDynamiteModuleNoCrashUtils(IObjectWrapper wrappedContext, String moduleId, boolean updateConfigIfRequired, long requestStartTime) throws RemoteException {
        final Context context = (Context) ObjectWrapper.unwrap(wrappedContext);
        if (context == null) {
            Log.w(TAG, "Invalid client Context.");
            return ObjectWrapper.wrap(null);
        }
        try {
            Cursor cursor = queryForDynamiteModule(context, moduleId, updateConfigIfRequired);

            // If ServiceProvider returned a valid cursor with version > 0, use it (Chimera module)
            if (cursor != null && cursor.moveToFirst() && cursor.getInt(0) > 0) {
                cursor.moveToPosition(-1); // reset position for caller
                return ObjectWrapper.wrap(cursor);
            }

            if (ChimeraConfigManager.INSTANCE.findModuleByModuleId(moduleId) != null
                    || DynamicModuleRegistry.INSTANCE.getByModuleId(moduleId) != null) {
                if (cursor != null) cursor.close();
                return ObjectWrapper.wrap(null);
            }

            // Fallback: build a cursor from V2 version lookup (non-Chimera modules like cast, maps, etc.)
            int version = getModuleVersion2NoCrashUtils(wrappedContext, moduleId, updateConfigIfRequired);
            if (version > 0) {
                Log.d(TAG, "queryForDynamiteModule fallback for " + moduleId + " v" + version);
                String[] columns = {"version", "apkPath", "loaderPath"};
                MatrixCursor fallbackCursor = new MatrixCursor(columns, 1);
                fallbackCursor.addRow(new Object[]{version, null, null});
                return ObjectWrapper.wrap(fallbackCursor);
            }

            return ObjectWrapper.wrap(cursor);
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving remote feature version: ", e);
            return ObjectWrapper.wrap(null);
        }
    }

    private Cursor queryForDynamiteModule(Context context, String moduleId, boolean updateConfigIfRequired) {
        Uri uri = new Uri.Builder()
                .scheme("content")
                .authority("com.google.android.gms.chimera")
                .path(updateConfigIfRequired ? "api_force_staging" : "api")
                .appendPath(moduleId)
                .appendQueryParameter("requestStartUptime", "0")
                .build();

        ContentProviderClient contentProviderClient = context.getContentResolver().acquireUnstableContentProviderClient(uri);
        if (contentProviderClient == null) {
            return null;
        }

        try {
            Cursor cursor = contentProviderClient.query(uri, null, null, null, null);
            if (cursor == null) {
                return null;
            }

            try {
                MatrixCursor matrixCursor = new MatrixCursor(cursor.getColumnNames(), cursor.getCount());

                while (cursor.moveToNext()) {
                    Object[] values = new Object[cursor.getColumnCount()];
                    for (int j = 0; j < cursor.getColumnCount(); j++) {
                        switch (cursor.getType(j)) {
                            case Cursor.FIELD_TYPE_NULL:
                                values[j] = null;
                                break;
                            case Cursor.FIELD_TYPE_INTEGER:
                                values[j] = cursor.getLong(j);
                                break;
                            case Cursor.FIELD_TYPE_FLOAT:
                                values[j] = cursor.getDouble(j);
                                break;
                            case Cursor.FIELD_TYPE_STRING:
                                values[j] = cursor.getString(j);
                                break;
                            case Cursor.FIELD_TYPE_BLOB:
                                values[j] = cursor.getBlob(j);
                                break;
                        }
                    }
                    matrixCursor.addRow(values);
                }

                return matrixCursor;
            } finally {
                cursor.close();
            }

        } catch (RemoteException ignored) {
        } finally {
            contentProviderClient.close();
        }
        return null;
    }

    private boolean isUnavailableDynamicModule(Context context, String moduleId) {
        boolean isDynamicModule = ChimeraConfigManager.INSTANCE.findModuleByModuleId(moduleId) != null
                || DynamicModuleRegistry.INSTANCE.getByModuleId(moduleId) != null;
        if (!isDynamicModule) {
            return false;
        }

        Cursor availability = null;
        try {
            availability = queryForDynamiteModule(context, moduleId, false);
            return availability == null || !availability.moveToFirst() || availability.getInt(0) <= 0;
        } catch (Exception e) {
            Log.w(TAG, "Failed to verify dynamic module availability: " + moduleId, e);
            return true;
        } finally {
            if (availability != null) {
                availability.close();
            }
        }
    }

}
