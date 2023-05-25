/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.dynamite;

import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import androidx.annotation.NonNull;

import java.lang.reflect.Field;
import java.util.Objects;

public class DynamiteModule {
    private static final String TAG = "DynamiteModule";

    public static final int NONE = 0;
    public static final int LOCAL = -1;
    public static final int REMOTE = 1;

    @NonNull
    public static final VersionPolicy PREFER_REMOTE = (context, moduleId, versions) -> {
        VersionPolicy.SelectionResult result = new VersionPolicy.SelectionResult();
        result.remoteVersion = versions.getRemoteVersion(context, moduleId, true);
        if (result.remoteVersion != 0) {
            result.selection = REMOTE;
        } else {
            result.localVersion = versions.getLocalVersion(context, moduleId);
            if (result.localVersion != 0) {
                result.selection = LOCAL;
            }
        }
        return result;
    };
    @NonNull
    public static final VersionPolicy PREFER_LOCAL = (context, moduleId, versions) -> {
        VersionPolicy.SelectionResult result = new VersionPolicy.SelectionResult();
        result.localVersion = versions.getLocalVersion(context, moduleId);
        if (result.localVersion != 0) {
            result.selection = LOCAL;
        } else {
            result.remoteVersion = versions.getRemoteVersion(context, moduleId, true);
            if (result.remoteVersion != 0) {
                result.selection = REMOTE;
            }
        }
        return result;
    };

    public interface VersionPolicy {
        interface IVersions {
            /* renamed from: zza */
            int getLocalVersion(@NonNull Context context, @NonNull String moduleId);

            /* renamed from: zzb */
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
        public LoadingException(String message) {
            super(message);
        }

        public LoadingException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private Context moduleContext;

    private DynamiteModule(Context moduleContext) {
        this.moduleContext = moduleContext;
    }

    public Context getModuleContext() {
        return moduleContext;
    }

    public static int getLocalVersion(@NonNull Context context, @NonNull String moduleId) {
        try {
            ClassLoader classLoader = context.getApplicationContext().getClassLoader();
            Class<?> clazz = classLoader.loadClass("com.google.android.gms.dynamite.descriptors." + moduleId + ".ModuleDescriptor");
            Field moduleIdField = clazz.getDeclaredField("MODULE_ID");
            Field moduleVersionField = clazz.getDeclaredField("MODULE_VERSION");
            if (!Objects.equals(moduleIdField.get(null), moduleId)) {
                Log.e(TAG, "Module descriptor id '" + moduleIdField.get(null) + "' didn't match expected id '" + moduleId + "'");
                return 0;
            }
            return moduleVersionField.getInt(null);
        } catch (ClassNotFoundException e) {
            Log.w(TAG, "Local module descriptor class for" + moduleId + " not found.");
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
        Log.e(TAG, "Remote modules not yet supported");
        return 0;
    }

    @NonNull
    public static DynamiteModule load(@NonNull Context context, @NonNull VersionPolicy policy, @NonNull String moduleId) throws LoadingException {
        Context applicationContext = context.getApplicationContext();
        if (applicationContext == null) throw new LoadingException("null application Context", null);
        try {
            VersionPolicy.SelectionResult result = policy.selectModule(context, moduleId, VersionPolicy.IVersions.Default);
            Log.i(TAG, "Considering local module " + moduleId + ":" + result.localVersion + " and remote module " + moduleId + ":" + result.remoteVersion);
            switch (result.selection) {
                case NONE:
                    throw new LoadingException("No acceptable module " + moduleId + " found. Local version is " + result.localVersion + " and remote version is " + result.remoteVersion + ".");
                case LOCAL:
                    Log.i(TAG, "Selected local version of " + moduleId);
                    return new DynamiteModule(context);
                case REMOTE:
                    throw new UnsupportedOperationException();
                default:
                    throw new LoadingException("VersionPolicy returned invalid code:" + result.selection);
            }
        } catch (LoadingException loadingException) {
            throw loadingException;
        } catch (Throwable e) {
            throw new LoadingException("Failed to load remote module.", e);
        }
    }

    @NonNull
    public IBinder instantiate(@NonNull String className) throws LoadingException {
        try {
            return (IBinder) this.moduleContext.getClassLoader().loadClass(className).newInstance();
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | RuntimeException e) {
            throw new LoadingException("Failed to instantiate module class: " + className, e);
        }
    }
}
