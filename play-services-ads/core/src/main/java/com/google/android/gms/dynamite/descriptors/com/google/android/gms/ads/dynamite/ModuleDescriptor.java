/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.dynamite.descriptors.com.google.android.gms.ads.dynamite;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.annotation.Keep;

import java.util.Locale;

import static android.os.Build.DEVICE;
import static android.os.Build.DISPLAY;
import static android.os.Build.VERSION.RELEASE;

@Keep
public class ModuleDescriptor {
    public static final String MODULE_ID = "com.google.android.gms.ads.dynamite";
    public static final int MODULE_VERSION = 230500001;
    private static final String TAG = "AdsDynamiteModule";

    /**
     * The ads module might try to access the user agent, requiring initialization on the main thread,
     * which may result in deadlocks when invoked from any other thread. This only happens with microG,
     * because we don't use the highly privileged SELinux Sandbox that regular Play Services uses
     * (which allows apps to read the user-agent from Play Services instead of the WebView). To prevent
     * the issue we pre-emptively write a user agent in the local storage of the app.
     */
    public static void init(Context context) {
        do {
            try {
                injectUserAgentSharedPreference(context);
            } catch (Exception e) {
            }
            if (context instanceof ContextWrapper) {
                Context baseContext = ((ContextWrapper) context).getBaseContext();
                if (context == baseContext) break;
                context = baseContext;
            } else {
                break;
            }
        } while (context != null);
    }

    /**
     * @return A user-agent representing a browser on the current device.
     */
    private static String buildDefaultUserAgent() {
        StringBuilder sb = new StringBuilder();
        sb.append("Mozilla/5.0 (Linux; U; Android");
        if (RELEASE != null) sb.append(" ").append(RELEASE);
        sb.append("; ").append(Locale.getDefault());
        if (DEVICE != null) {
            sb.append("; ").append(DEVICE);
            if (DISPLAY != null) sb.append(" Build/").append(DISPLAY);
        }
        sb.append(") AppleWebKit/533 Version/4.0 Safari/533");
        return sb.toString();
    }

    @SuppressLint("ApplySharedPref")
    private static void injectUserAgentSharedPreference(Context context) {
        SharedPreferences preferences = context.getSharedPreferences("admob_user_agent", Context.MODE_PRIVATE);
        if (!preferences.contains("user_agent")) {
            preferences.edit().putString("user_agent", buildDefaultUserAgent()).commit();
            Log.d(TAG, "Injected admob_user_agent into package " + context.getPackageName());
        }
    }
}
