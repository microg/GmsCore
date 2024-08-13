/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.common.internal;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class Preconditions {
    
    @NonNull
    public static <T> T checkNotNull(@Nullable T value) {
        if (value == null) {
            throw new NullPointerException("null reference");
        } else {
            return value;
        }
    }

    
    public static String checkNotEmpty(String value) {
        if (TextUtils.isEmpty(value)) {
            throw new IllegalArgumentException("Given String is empty or null");
        } else {
            return value;
        }
    }

    
    public static String checkNotEmpty(String value, Object error) {
        if (TextUtils.isEmpty(value)) {
            throw new IllegalArgumentException(String.valueOf(error));
        } else {
            return value;
        }
    }

    
    @NonNull
    public static <T> T checkNotNull(T value, Object error) {
        if (value == null) {
            throw new NullPointerException(String.valueOf(error));
        } else {
            return value;
        }
    }

    
    public static int checkNotZero(int value, Object error) {
        if (value == 0) {
            throw new IllegalArgumentException(String.valueOf(error));
        } else {
            return value;
        }
    }

    
    public static int checkNotZero(int value) {
        if (value == 0) {
            throw new IllegalArgumentException("Given Integer is zero");
        } else {
            return value;
        }
    }

    
    public static long checkNotZero(long value, Object error) {
        if (value == 0L) {
            throw new IllegalArgumentException(String.valueOf(error));
        } else {
            return value;
        }
    }

    
    public static long checkNotZero(long value) {
        if (value == 0L) {
            throw new IllegalArgumentException("Given Long is zero");
        } else {
            return value;
        }
    }

    
    public static void checkState(boolean value) {
        if (!value) {
            throw new IllegalStateException();
        }
    }

    
    public static void checkState(boolean value, Object error) {
        if (!value) {
            throw new IllegalStateException(String.valueOf(error));
        }
    }

    
    public static void checkState(boolean value, String key, Object... data) {
        if (!value) {
            throw new IllegalStateException(String.format(key, data));
        }
    }

    
    public static void checkArgument(boolean value, Object data) {
        if (!value) {
            throw new IllegalArgumentException(String.valueOf(data));
        }
    }

    
    public static void checkArgument(boolean value, String key, Object... data) {
        if (!value) {
            throw new IllegalArgumentException(String.format(key, data));
        }
    }

    
    public static void checkArgument(boolean value) {
        if (!value) {
            throw new IllegalArgumentException();
        }
    }

    private Preconditions() {
        throw new AssertionError("Uninstantiable");
    }

    
    public static void checkMainThread(String value) {
        if (Looper.getMainLooper() != Looper.myLooper()) {
            throw new IllegalStateException(value);
        }
    }

    
    public static void checkNotMainThread() {
        checkNotMainThread("Must not be called on the main application thread");
    }

    
    public static void checkNotMainThread(String value) {
        if (Looper.getMainLooper() != Looper.myLooper()) {
            throw new IllegalStateException(value);
        }
    }

    
    public static void checkHandlerThread(Handler handler) {
        checkHandlerThread(handler, "Must be called on the handler thread");
    }

    public static void checkHandlerThread(Handler handler, String msg) {
        if (Looper.myLooper() != handler.getLooper()) {
            throw new IllegalStateException(msg);
        }
    }
}
