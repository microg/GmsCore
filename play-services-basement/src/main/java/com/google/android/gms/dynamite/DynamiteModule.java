/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.dynamite;

import android.content.Context;
import android.database.Cursor;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.NonNull;

import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.dynamic.ObjectWrapper;

import java.lang.reflect.Field;
import java.util.Objects;

public class DynamiteModule {
    private static final String TAG = "DynamiteModule";

    public static final int NONE = 0;
    public static final int LOCAL = -1;
    public static final int REMOTE = 1;

    private static IDynamiteLoader sCachedLoader;

    private final Context moduleContext;

    private DynamiteModule(Context moduleContext) {
        this.moduleContext = moduleContext;
    }

    public Context getModuleContext() {
        return moduleContext;
    }

    public interface VersionPolicy {
        interface IVersions {
            int getLocalVersion(@NonNull Context context, @NonNull String moduleId);
            int getRemoteVersion(@NonNull Context context, @NonNull String moduleId, boolean forceStaging) throws LoadingException;

            IVersions Default = new IVersions() {
                @Override
                public int getLocalVersion(@NonNull Context context, @NonNull String moduleId) {
                    return DynamiteModule.getLocalVersion(context, moduleId);
                }

                @Override
                public int getRemoteVersion(@NonNull Context context, @NonNull String moduleId, boolean forceStaging) throws LoadingException {
                    return DynamiteModule.getRemoteVersion(context, moduleId, forceStaging);
                }
            };
        }

        class SelectionResult {
            public int localVersion = 0;
            public int remoteVersion = 0;
            public int selection = NONE;
        }

        SelectionResult selectModule(@NonNull Context context, @NonNull String moduleId, @NonNull IVersions versions) throws LoadingException;
    }

    public static class LoadingException extends Exception {
        public LoadingException(String message) { super(message); }
        public LoadingException(String message, Throwable cause) { super(message, cause); }
    }

    @NonNull
    public static final VersionPolicy PREFER_REMOTE = (context, moduleId, versions) -> {
        VersionPolicy.SelectionResult r = new VersionPolicy.SelectionResult();
        r.remoteVersion = versions.getRemoteVersion(context, moduleId, false);
        if (r.remoteVersion != 0) {
            r.selection = REMOTE;
        } else {
            r.localVersion = versions.getLocalVersion(context, moduleId);
            if (r.localVersion != 0) r.selection = LOCAL;
        }
        return r;
    };

    @NonNull
    public static final VersionPolicy PREFER_LOCAL = (context, moduleId, versions) -> {
        VersionPolicy.SelectionResult r = new VersionPolicy.SelectionResult();
        r.localVersion = versions.getLocalVersion(context, moduleId);
        if (r.localVersion != 0) {
            r.selection = LOCAL;
        } else {
            r.remoteVersion = versions.getRemoteVersion(context, moduleId, false);
            if (r.remoteVersion != 0) r.selection = REMOTE;
        }
        return r;
    };

    public static int getLocalVersion(@NonNull Context context, @NonNull String moduleId) {
        try {
            ClassLoader cl = context.getApplicationContext().getClassLoader();
            Class<?> clazz = cl.loadClass("com.google.android.gms.dynamite.descriptors." + moduleId + ".ModuleDescriptor");
            Field idF = clazz.getDeclaredField("MODULE_ID");
            Field verF = clazz.getDeclaredField("MODULE_VERSION");
            if (!Objects.equals(idF.get(null), moduleId)) {
                Log.e(TAG, "Module descriptor id '" + idF.get(null) + "' didn't match expected id '" + moduleId + "'");
                return 0;
            }
            return verF.getInt(null);
        } catch (ClassNotFoundException e) {
            Log.w(TAG, "Local module descriptor class for " + moduleId + " not found.");
            return 0;
        } catch (Exception e) {
            Log.e(TAG, "Failed to load module descriptor class.", e);
            return 0;
        }
    }

    public static int getRemoteVersion(@NonNull Context context, @NonNull String moduleId) {
        return getRemoteVersion(context, moduleId, false);
    }

    public static int getRemoteVersion(@NonNull Context context, @NonNull String moduleId, boolean forceStaging) {
        try {
            IDynamiteLoader loader = getIDynamiteLoader(context);
            if (loader == null) {
                Log.w(TAG, "Failed to create IDynamiteLoader for version check");
                return 0;
            }
            return loader.getModuleVersion2NoCrashUtils(ObjectWrapper.wrap(context), moduleId, forceStaging);
        } catch (Exception e) {
            Log.w(TAG, "Failed to retrieve remote module version: " + e.getMessage());
            return 0;
        }
    }

    @NonNull
    public static DynamiteModule load(@NonNull Context context, @NonNull VersionPolicy policy, @NonNull String moduleId) throws LoadingException {
        Context app = context.getApplicationContext();
        if (app == null) throw new LoadingException("null application Context");

        try {
            VersionPolicy.SelectionResult result = policy.selectModule(context, moduleId, VersionPolicy.IVersions.Default);
            Log.i(TAG, "Considering local " + moduleId + ":" + result.localVersion + " / remote " + moduleId + ":" + result.remoteVersion);

            switch (result.selection) {
                case NONE:
                    throw new LoadingException("No acceptable module " + moduleId + " found. local=" + result.localVersion + " remote=" + result.remoteVersion);
                case LOCAL:
                    Log.i(TAG, "Selected local version of " + moduleId);
                    return new DynamiteModule(context);
                case REMOTE:
                    return loadRemoteModule(context, moduleId, result.remoteVersion);
                default:
                    throw new LoadingException("VersionPolicy returned invalid code:" + result.selection);
            }
        } catch (LoadingException loadingException) {
            throw loadingException;
        } catch (Throwable e) {
            throw new LoadingException("Failed to load remote module.", e);
        }
    }

    /**
     * Load a remote module via IDynamiteLoader.
     * The IDynamiteLoader is loaded from GMS's APK into this process via createPackageContext.
     * It then creates the module context using ChimeraModuleLdr which handles cross-process
     * APK delivery via ContentProvider when direct file access is unavailable.
     */
    private static DynamiteModule loadRemoteModule(Context context, String moduleId, int minVersion) throws LoadingException {
        IDynamiteLoader loader = getIDynamiteLoader(context);
        if (loader == null) {
            throw new LoadingException("Failed to create IDynamiteLoader from GmsCore");
        }

        try {
            int loaderVersion = loader.getIDynamiteLoaderVersion();
            Log.i(TAG, "IDynamiteLoader version: " + loaderVersion + " for " + moduleId);

            IObjectWrapper wrappedContext = ObjectWrapper.wrap(context);
            IObjectWrapper wrappedResult;

            if (loaderVersion >= 3) {
                // V3: query first, then create context with cursor
                IObjectWrapper wrappedCursor = loader.queryForDynamiteModuleNoCrashUtils(
                        wrappedContext, moduleId, false, 0L);
                Cursor cursor = (Cursor) ObjectWrapper.unwrap(wrappedCursor);
                try {
                    if (cursor != null && cursor.moveToFirst()) {
                        int availableVersion = cursor.getInt(0);
                        if (availableVersion < minVersion) {
                            Log.w(TAG, "Available version " + availableVersion + " < requested " + minVersion);
                        }
                    }
                    wrappedResult = loader.createModuleContext3NoCrashUtils(
                            wrappedContext, moduleId, minVersion, wrappedCursor);
                } finally {
                    if (cursor != null) cursor.close();
                }
            } else if (loaderVersion >= 2) {
                wrappedResult = loader.createModuleContextNoCrashUtils(
                        wrappedContext, moduleId, minVersion);
            } else {
                wrappedResult = loader.createModuleContext(
                        wrappedContext, moduleId, minVersion);
            }

            Context moduleCtx = (Context) ObjectWrapper.unwrap(wrappedResult);
            if (moduleCtx == null) {
                throw new LoadingException("Failed to load remote module " + moduleId);
            }
            Log.i(TAG, "Remote module loaded: " + moduleId + " classLoader=" + moduleCtx.getClassLoader());
            return new DynamiteModule(moduleCtx);
        } catch (LoadingException le) {
            throw le;
        } catch (Exception e) {
            throw new LoadingException("Failed to load remote module " + moduleId, e);
        }
    }

    /**
     * Get IDynamiteLoader by loading DynamiteLoaderImpl from GMS's APK into this process.
     * Uses CONTEXT_INCLUDE_CODE to load GMS code, similar to Google's real implementation.
     */
    private static synchronized IDynamiteLoader getIDynamiteLoader(Context context) {
        if (sCachedLoader != null) return sCachedLoader;
        try {
            // Load DynamiteLoaderImpl from GMS package into this process
            // Flag 3 = CONTEXT_INCLUDE_CODE | CONTEXT_IGNORE_SECURITY
            Context gmsContext = context.createPackageContext(
                    "com.google.android.gms",
                    Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
            IBinder binder = (IBinder) gmsContext.getClassLoader()
                    .loadClass("com.google.android.gms.chimera.container.DynamiteLoaderImpl")
                    .newInstance();
            final IDynamiteLoader loader = IDynamiteLoader.Stub.asInterface(binder);
            if (loader != null) {
                sCachedLoader = loader;
                return loader;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to load IDynamiteLoader from GmsCore: " + e.getMessage());
        }
        return null;
    }

    @NonNull
    public IBinder instantiate(@NonNull String className) throws LoadingException {
        try {
            return (IBinder) this.moduleContext.getClassLoader().loadClass(className).newInstance();
        } catch (Throwable e) {
            throw new LoadingException("Failed to instantiate module class: " + className, e);
        }
    }
}
