/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.dynamite;

import android.content.Context;
import android.os.IBinder;
import androidx.annotation.NonNull;

public class DynamiteModule {
    @NonNull
    public static final VersionPolicy PREFER_REMOTE = null;
    @NonNull
    public static final VersionPolicy PREFER_LOCAL = null;

    public interface VersionPolicy {

    }

    public static class LoadingException extends Exception {
        public LoadingException(String message) {
            super(message);
        }

        public LoadingException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private Context remoteContext;

    private DynamiteModule(Context remoteContext) {
        this.remoteContext = remoteContext;
    }

    @NonNull
    public static DynamiteModule load(@NonNull Context context, @NonNull VersionPolicy policy, @NonNull String moduleId) throws LoadingException {
        throw new LoadingException("Not yet supported");
    }

    @NonNull
    public IBinder instantiate(@NonNull String className) throws LoadingException {
        try {
            return (IBinder) this.remoteContext.getClassLoader().loadClass(className).newInstance();
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | RuntimeException e) {
            throw new LoadingException("Failed to instantiate module class: " + className, e);
        }
    }
}
