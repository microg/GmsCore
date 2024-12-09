/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.dynamite.descriptors.com.google.android.gms.ads.dynamite;

import android.content.Context;
import android.content.ContextWrapper;
import android.util.Log;
import android.webkit.WebSettings;
import androidx.annotation.Keep;

@Keep
public class ModuleDescriptor {
    public static final String MODULE_ID = "com.google.android.gms.ads.dynamite";
    public static final int MODULE_VERSION = 230500001;

    public static void init(ContextWrapper context) {
        Context baseContext = context.getBaseContext();
        WebSettings.getDefaultUserAgent(baseContext);
        Log.d("ModuleDescriptor", "init: context: " + baseContext);
    }
}
