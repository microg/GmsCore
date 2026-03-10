/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.cast;

import android.app.Service;
import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;
import androidx.annotation.NonNull;
import com.google.android.gms.cast.framework.CastOptions;
import com.google.android.gms.cast.framework.ICastContext;
import com.google.android.gms.cast.framework.IReconnectionService;
import com.google.android.gms.cast.framework.ModuleUnavailableException;
import com.google.android.gms.cast.framework.internal.ICastDynamiteModule;
import com.google.android.gms.cast.framework.internal.IMediaRouter;
import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.dynamic.ObjectWrapper;
import com.google.android.gms.dynamite.DynamiteModule;

import java.util.Map;

public class CastDynamiteModule {
    public static ICastContext newCastContext(Context context, CastOptions castOptions, IMediaRouter mediaRouter, Map<String, IBinder> sessionProviderMap) throws ModuleUnavailableException, RemoteException {
        return getInterface(context).newCastContextImpl(ObjectWrapper.wrap(context), castOptions, mediaRouter, sessionProviderMap);
    }

    public static IReconnectionService newReconnectionService(Service service, IObjectWrapper sessionManager, IObjectWrapper discoveryManager) {
        try {
            return getInterface(service.getApplicationContext()).newReconnectionServiceImpl(ObjectWrapper.wrap(service), sessionManager, discoveryManager);
        } catch (RemoteException | ModuleUnavailableException e) {
            return null;
        }
    }

    @NonNull
    private static ICastDynamiteModule getInterface(Context context) throws ModuleUnavailableException {
        try {
            IBinder binder = DynamiteModule.load(context, DynamiteModule.PREFER_REMOTE, "com.google.android.gms.cast.framework.dynamite").instantiate("com.google.android.gms.cast.framework.internal.CastDynamiteModuleImpl");
            return ICastDynamiteModule.Stub.asInterface(binder);
        } catch (DynamiteModule.LoadingException e) {
            throw new ModuleUnavailableException(e);
        }
    }
}
