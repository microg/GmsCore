/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.tflite.dynamite;

import android.content.Context;
import android.os.Binder;
import android.os.Process;
import android.os.RemoteException;

import androidx.annotation.Keep;

import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.dynamic.ObjectWrapper;
import com.google.android.gms.tflite.dynamite.internal.CustomerInfo;

import org.microg.gms.common.PackageUtils;

@Keep
public final class TfLiteDynamiteLoaderImpl extends ITfLiteDynamiteLoader.Stub {
    @Override
    public long loadTfLiteLibrary(IObjectWrapper wrappedContext, CustomerInfo customerInfo) throws RemoteException {
        checkClient(wrappedContext, customerInfo);
        try {
            return TfLiteNativeApi.getApi();
        } catch (UnsatisfiedLinkError e) {
            RemoteException failure = new RemoteException("Error loading TFLite native library");
            failure.initCause(e);
            throw failure;
        }
    }

    static void checkClient(IObjectWrapper wrappedContext, CustomerInfo customerInfo) {
        // Native addresses are valid only in the process that loads the module.
        if (Binder.getCallingPid() != Process.myPid()) {
            throw new SecurityException("TensorFlow Lite must be loaded in the calling process");
        }
        Context context = ObjectWrapper.unwrapTyped(wrappedContext, Context.class);
        if (context == null || customerInfo == null || customerInfo.packageName == null) {
            throw new IllegalArgumentException("Context and customer package are required");
        }
        String packageName = PackageUtils.getAndCheckCallingPackage(context, customerInfo.packageName);
        if (!customerInfo.packageName.equals(packageName)) {
            throw new SecurityException("Cannot verify the TensorFlow Lite client package");
        }
    }
}
