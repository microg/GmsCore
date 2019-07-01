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
import android.content.pm.ApplicationInfo;
import android.os.Process;
import android.util.Log;

import org.conscrypt.NativeCrypto;
import org.conscrypt.OpenSSLProvider;
import org.microg.gms.common.PackageUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.Security;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

public class ProviderInstallerImpl {
    private static final String TAG = "GmsProviderInstaller";
    private static final List<String> DISABLED = Collections.singletonList("com.discord");

    public static void insertProvider(Context context) {
        String packageName = PackageUtils.packageFromProcessId(context, Process.myPid());
        Log.d(TAG, "Provider installer invoked for " + packageName);
        try {
            if (DISABLED.contains(packageName)) {
                Log.d(TAG, "Package is excluded from usage of provider installer");
                return;
            }

            OpenSSLProvider provider = null;

            try {
                provider = new OpenSSLProvider("GmsCore_OpenSSL");
            } catch (UnsatisfiedLinkError e) {
                Log.w(TAG, "Could not link conscrypt via default loader, trying manual loading");

                // TODO: Move manual loading into helper function (as it is also used in both maps implementations)
                try {
                    ApplicationInfo otherAppInfo = context.getPackageManager().getApplicationInfo(packageName, 0);

                    String primaryCpuAbi = (String) ApplicationInfo.class.getField("primaryCpuAbi").get(otherAppInfo);
                    if (primaryCpuAbi != null) {
                        String path = "lib/" + primaryCpuAbi + "/libconscrypt_jni.so";
                        File cacheFile = new File(context.createPackageContext(packageName, 0).getCacheDir().getAbsolutePath() + "/.gmscore/" + path);
                        cacheFile.getParentFile().mkdirs();
                        File apkFile = new File(context.getPackageCodePath());
                        if (!cacheFile.exists() || cacheFile.lastModified() < apkFile.lastModified()) {
                            ZipFile zipFile = new ZipFile(apkFile);
                            ZipEntry entry = zipFile.getEntry(path);
                            if (entry != null) {
                                copyInputStream(zipFile.getInputStream(entry), new FileOutputStream(cacheFile));
                            } else {
                                Log.d(TAG, "Can't load native library: " + path + " does not exist in " + apkFile);
                            }
                        }
                        Log.d(TAG, "Loading conscrypt_jni from " + cacheFile.getPath());
                        System.load(cacheFile.getAbsolutePath());

                        Class<NativeCrypto> clazz = NativeCrypto.class;
                        Field loadError = clazz.getDeclaredField("loadError");
                        loadError.setAccessible(true);
                        loadError.set(null, null);
                        Method clinit =clazz.getDeclaredMethod("clinit");
                        clinit.setAccessible(true);
                        try {
                            clinit.invoke(null);
                            provider = new OpenSSLProvider("GmsCore_OpenSSL");
                        } catch (InvocationTargetException inner) {
                            if (inner.getTargetException() instanceof UnsatisfiedLinkError) {
                                loadError.set(null, inner.getTargetException());
                            }
                        }
                    }
                } catch (Exception e2) {
                    Log.w(TAG, e2);
                }
            }

            if (provider == null) {
                Log.w(TAG, "Failed to initialize conscrypt provider");
                return;
            }

            if (Security.insertProviderAt(provider, 1) == 1) {
                Security.setProperty("ssl.SocketFactory.provider", "org.conscrypt.OpenSSLSocketFactoryImpl");
                Security.setProperty("ssl.ServerSocketFactory.provider", "org.conscrypt.OpenSSLServerSocketFactoryImpl");

                SSLContext.setDefault(SSLContext.getInstance("Default"));
                HttpsURLConnection.setDefaultSSLSocketFactory(SSLContext.getDefault().getSocketFactory());
                Log.d(TAG, "SSL provider installed");
            } else {
                Log.w(TAG, "Did not insert the new SSL provider");
            }
        } catch (Throwable e) {
            Log.w(TAG, e);
        }
    }


    private static final void copyInputStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int len;

        while ((len = in.read(buffer)) >= 0)
            out.write(buffer, 0, len);

        in.close();
        out.close();
    }
}
