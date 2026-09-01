/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.common.security;

import android.content.Context;
import android.content.pm.ApplicationInfo;
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

import static android.os.Build.VERSION.SDK_INT;
import static com.google.android.gms.security.ProviderInstaller.PROVIDER_NAME;

/**
 * This is the old entry point, the new one is at {@link com.google.android.gms.providerinstaller.ProviderInstallerImpl}
 */
@Keep
public class ProviderInstallerImpl {
    private static final String TAG = "ProviderInstaller";

    @Keep
    public static void insertProvider(Context context) {
        com.google.android.gms.providerinstaller.ProviderInstallerImpl.insertProvider(context);
    }

    @Keep
    public void reportRequestStats(Context context, long elapsedRealtimeBeforeLoad, long elapsedRealtimeAfterLoad) {
        com.google.android.gms.providerinstaller.ProviderInstallerImpl.reportRequestStats(context, elapsedRealtimeBeforeLoad, elapsedRealtimeAfterLoad);
    }
}
