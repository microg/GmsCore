/*
 * SPDX-FileCopyrightText: 2021, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.droidguard;

import android.content.Context;
import android.media.MediaDrm;
import android.os.Build;
import android.util.Log;

import org.microg.gms.droidguard.core.FallbackCreator;
import org.microg.gms.settings.SettingsContract;

import java.util.HashMap;

/**
 * Callbacks invoked from the DroidGuard VM
 * <p>
 * We keep this file in Java to ensure ABI compatibility.
 * Methods are invoked by name from within the VM and thus must keep current name.
 */
public class GuardCallback {
    private static final String TAG = "GmsGuardCallback";
    private final Context context;
    private final String packageName;

    public GuardCallback(Context context, String packageName) {
        this.context = context;
        this.packageName = packageName;
    }

    public final String a(final byte[] array) {
        Log.d(TAG, "a[?](" + array + ")");
        return new String(FallbackCreator.create(new HashMap<>(), array, "", context, null));
    }

    // getAndroidId
    public final String b() {
        try {
            long androidId = SettingsContract.INSTANCE.getSettings(context, SettingsContract.CheckIn.INSTANCE.getContentUri(context), new String[]{SettingsContract.CheckIn.ANDROID_ID}, cursor -> cursor.getLong(0));
            Log.d(TAG, "b[getAndroidId]() = " + androidId);
            return String.valueOf(androidId);
        } catch (Throwable e) {
            Log.w(TAG, "Failed to get Android ID, fallback to random", e);
        }
        long androidId = (long) (Math.random() * Long.MAX_VALUE);
        Log.d(TAG, "b[getAndroidId]() = " + androidId + " (random)");
        return String.valueOf(androidId);
    }

    // getPackageName
    public final String c() {
        Log.d(TAG, "c[getPackageName]() = " + packageName);
        return packageName;
    }

    // closeMediaDrmSession
    public final void d(final Object mediaDrm, final byte[] sessionId) {
        Log.d(TAG, "d[closeMediaDrmSession](" + mediaDrm + ", " + sessionId + ")");
        synchronized (MediaDrmLock.LOCK) {
            if (Build.VERSION.SDK_INT >= 18) {
                ((MediaDrm) mediaDrm).closeSession(sessionId);
            }
        }
    }

    public final void e(final int task) {
        Log.d(TAG, "e[?](" + task + ")");
        // TODO: Open database
        if (task == 1) {
            // TODO
        } else if (task == 0) {
            // TODO
        }
        // TODO: Set value in database
    }
}
