/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.dynamite.descriptors.com.google.android.gms.ads.dynamite;

import android.content.Context;
import android.content.ContextWrapper;
import android.os.Build;
import android.util.Log;
import android.webkit.WebSettings;
import androidx.annotation.Keep;

import java.io.File;

@Keep
public class ModuleDescriptor {
    private static final String TAG = "ModuleDescriptor";
    public static final String MODULE_ID = "com.google.android.gms.ads.dynamite";
    public static final int MODULE_VERSION = 230500001;

    /**
     * The ads module might try to access the user agent, requiring initialization on the main thread,
     * which may result in deadlocks when invoked from any other thread. This only happens with microG,
     * because we don't use the highly privileged SELinux Sandbox that regular Play Services uses
     * (which allows apps to read the user-agent from Play Services instead of the WebView). To prevent
     * the issue we pre-emptively initialize the WebView.
     */
    public static void init(Context context) {
        if (context instanceof ContextWrapper) {
            context = ((ContextWrapper) context).getBaseContext();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            File webViewDataDir = new File(context.getDataDir(), "app_webview");
            if (webViewDataDir.exists()) {
                Log.d(TAG, "WebView data directory is initialized");
                WebSettings.getDefaultUserAgent(context);
            } else {
                Log.d(TAG, "WebView data directory is not initialized.");
            }
        }
    }
}
