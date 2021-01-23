/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.common.security;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.Process;
import android.util.Log;

import androidx.annotation.Keep;

import com.google.android.gms.org.conscrypt.Conscrypt;
import com.google.android.gms.org.conscrypt.NativeCrypto;

import org.microg.gms.common.PackageUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.Provider;
import java.security.Security;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

import static com.google.android.gms.security.ProviderInstaller.PROVIDER_NAME;

@Keep
public class ProviderInstallerImpl {
    private static final String TAG = "GmsProviderInstaller";
    private static final List<String> DISABLED = Collections.unmodifiableList(Arrays.asList("com.bankid.bus"));

    private static final Object lock = new Object();
    private static Provider provider;

    private static String getRealSelfPackageName(Context context) {
        String packageName = PackageUtils.packageFromProcessId(context, Process.myPid());
        if (packageName != null && packageName.contains(".")) return packageName;
        try {
            Method getBasePackageName = Context.class.getDeclaredMethod("getBasePackageName");
            packageName = (String) getBasePackageName.invoke(context);
            if (packageName != null) return packageName;
        } catch (Exception e) {

        }
        if (Build.VERSION.SDK_INT >= 29) {
            return context.getOpPackageName();
        }
        Context applicationContext = context.getApplicationContext();
        if (applicationContext != null) {
            return applicationContext.getPackageName();
        }
        return context.getPackageName();
    }

    @Keep
    public static void insertProvider(Context context) {
        String packageName = getRealSelfPackageName(context);
        try {
            if (DISABLED.contains(packageName)) {
                Log.d(TAG, "Package " + packageName + " is excluded from usage of provider installer");
                return;
            }
            if (Security.getProvider(PROVIDER_NAME) != null) {
                Log.d(TAG, "Provider already inserted in " + packageName);
                return;
            }

            synchronized (lock) {
                initProvider(context, packageName);

                if (provider == null) {
                    Log.w(TAG, "Failed to initialize Conscrypt");
                    return;
                }

                int res = Security.insertProviderAt(provider, 1);
                if (res == 1) {
                    Security.setProperty("ssl.SocketFactory.provider", "com.google.android.gms.org.conscrypt.OpenSSLSocketFactoryImpl");
                    Security.setProperty("ssl.ServerSocketFactory.provider", "com.google.android.gms.org.conscrypt.OpenSSLServerSocketFactoryImpl");

                    SSLContext sslContext = SSLContext.getInstance("Default");
                    SSLContext.setDefault(sslContext);
                    HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());

                    Log.d(TAG, "Installed default security provider " + PROVIDER_NAME);
                } else {
                    throw new SecurityException("Failed to install security provider " + PROVIDER_NAME + ", result: " + res);
                }
            }
        } catch (Throwable e) {
            Log.w(TAG, e);
        }
    }

    private static void initProvider(Context context, String packageName) {
        Log.d(TAG, "Initializing provider for " + packageName);

        try {
            provider = Conscrypt.newProviderBuilder().setName(PROVIDER_NAME).defaultTlsProtocol("TLSv1.2").build();
        } catch (UnsatisfiedLinkError e) {
            Log.w(TAG, "Could not link conscrypt via default loader, trying manual loading");

            try {
                loadConscryptDirect(context, packageName);
                provider = Conscrypt.newProviderBuilder().setName(PROVIDER_NAME).defaultTlsProtocol("TLSv1.2").build();
            } catch (Exception e2) {
                Log.w(TAG, e2);
            }
        }
    }

    private static void loadConscryptDirect(Context context, String packageName) throws Exception {
        ApplicationInfo otherAppInfo = context.getPackageManager().getApplicationInfo(packageName, 0);

        // TODO: Move manual loading into helper function (as it is also used in both maps implementations)
        String primaryCpuAbi = (String) ApplicationInfo.class.getField("primaryCpuAbi").get(otherAppInfo);
        if (primaryCpuAbi != null) {
            String path = "lib/" + primaryCpuAbi + "/libconscrypt_gmscore_jni.so";
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
            Log.d(TAG, "Loading conscrypt_gmscore_jni from " + cacheFile.getPath());
            System.load(cacheFile.getAbsolutePath());

            Class<NativeCrypto> clazz = NativeCrypto.class;

            Field loadError = clazz.getDeclaredField("loadError");
            loadError.setAccessible(true);
            loadError.set(null, null);

            Method clinit = clazz.getDeclaredMethod("clinit");
            clinit.setAccessible(true);

            Method get_cipher_names = clazz.getDeclaredMethod("get_cipher_names", String.class);
            get_cipher_names.setAccessible(true);

            Method cipherSuiteToJava = clazz.getDeclaredMethod("cipherSuiteToJava", String.class);
            cipherSuiteToJava.setAccessible(true);

            Method EVP_has_aes_hardware = clazz.getDeclaredMethod("EVP_has_aes_hardware");
            EVP_has_aes_hardware.setAccessible(true);

            Field f = clazz.getDeclaredField("SUPPORTED_TLS_1_2_CIPHER_SUITES_SET");
            f.setAccessible(true);

            Set<String> SUPPORTED_TLS_1_2_CIPHER_SUITES_SET = (Set<String>) f.get(null);
            f = clazz.getDeclaredField("SUPPORTED_LEGACY_CIPHER_SUITES_SET");
            f.setAccessible(true);

            Set<String> SUPPORTED_LEGACY_CIPHER_SUITES_SET = (Set<String>) f.get(null);
            f = clazz.getDeclaredField("SUPPORTED_TLS_1_2_CIPHER_SUITES");
            f.setAccessible(true);

            try {
                clinit.invoke(null);

                String[] allCipherSuites = (String[]) get_cipher_names.invoke(null, "ALL:!DHE");
                int size = allCipherSuites.length;

                String[] SUPPORTED_TLS_1_2_CIPHER_SUITES = new String[size / 2 + 2];
                for (int i = 0; i < size; i += 2) {
                    String cipherSuite = (String) cipherSuiteToJava.invoke(null, allCipherSuites[i]);

                    SUPPORTED_TLS_1_2_CIPHER_SUITES[i / 2] = cipherSuite;
                    SUPPORTED_TLS_1_2_CIPHER_SUITES_SET.add(cipherSuite);

                    SUPPORTED_LEGACY_CIPHER_SUITES_SET.add(allCipherSuites[i + 1]);
                }
                SUPPORTED_TLS_1_2_CIPHER_SUITES[size / 2] = "TLS_EMPTY_RENEGOTIATION_INFO_SCSV";
                SUPPORTED_TLS_1_2_CIPHER_SUITES[size / 2 + 1] = "TLS_FALLBACK_SCSV";
                f.set(null, SUPPORTED_TLS_1_2_CIPHER_SUITES);

                f = clazz.getDeclaredField("HAS_AES_HARDWARE");
                f.setAccessible(true);
                f.set(null, (int) EVP_has_aes_hardware.invoke(null) == 1);

            } catch (InvocationTargetException inner) {
                if (inner.getTargetException() instanceof UnsatisfiedLinkError) {
                    loadError.set(null, inner.getTargetException());
                }
            }
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
