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

package com.google.android.gms.common.security;

import android.content.Context;
import android.os.Process;
import android.util.Log;

import org.conscrypt.OpenSSLProvider;
import org.microg.gms.common.PackageUtils;

import java.security.Security;
import java.util.Collections;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

public class ProviderInstallerImpl {
    private static final String TAG = "GmsProviderInstaller";
    private static final List<String> DISABLED = Collections.singletonList("com.discord");

    public static void insertProvider(Context context) {
        try {
            String packageName = PackageUtils.packageFromProcessId(context, Process.myPid());
            Log.d(TAG, "Provider installer invoked for " + packageName);
            if (DISABLED.contains(packageName)) {
                Log.d(TAG, "Package is excluded from usage of provider installer");
            } else if (Security.insertProviderAt(new OpenSSLProvider("GmsCore_OpenSSL"), 1) == 1) {
                Security.setProperty("ssl.SocketFactory.provider", "org.conscrypt.OpenSSLSocketFactoryImpl");
                Security.setProperty("ssl.ServerSocketFactory.provider", "org.conscrypt.OpenSSLServerSocketFactoryImpl");

                SSLContext.setDefault(SSLContext.getInstance("Default"));
                HttpsURLConnection.setDefaultSSLSocketFactory(SSLContext.getDefault().getSocketFactory());
                Log.d(TAG, "SSL provider installed");
            } else {
                Log.w(TAG, "Did not insert the new SSL provider");
            }
        } catch (Exception e) {
            Log.w(TAG, e);
        }
    }
}
