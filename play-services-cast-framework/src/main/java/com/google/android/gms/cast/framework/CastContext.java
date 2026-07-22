/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.cast.framework;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.cast.framework.internal.IMediaRouter;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import org.microg.gms.cast.CastDynamiteModule;
import org.microg.gms.cast.CastSessionProvider;
import org.microg.gms.common.PublicApi;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

public class CastContext {
    /**
     * The metadata key to specify the fully qualified name of the {@link OptionsProvider} implementation in the
     * {@code AndroidManifest.xml}.
     */
    public static final String OPTIONS_PROVIDER_CLASS_NAME_KEY = "com.google.android.gms.cast.framework.OPTIONS_PROVIDER_CLASS_NAME";

    /**
     * Returns the shared instance of {@link CastContext}. This method must be called after {@link CastContext} is initialized through
     * {@link #getSharedInstance(Context, Executor)}. Otherwise, this method will return {@code null}.
     *
     * @throws IllegalStateException If this method is not called on the main thread.
     */
    public static CastContext getSharedInstance() {
        return sharedInstance;
    }

    /**
     * Returns a shared instance of {@link CastContext}. The shared instance will be initialized on the first time this method is called.
     *
     * @param context An application {@link Context}. If this is not an application {@link Context}, {@link Context#getApplicationContext()} will be called on
     *                the given context, to retrieve it.
     * @throws IllegalStateException If any of the following:
     *                               <ul>
     *                                   <li>This method is not called on the main thread.</li>
     *                                   <li>
     *                                       The fully qualified name of the {@link OptionsProvider} implementation is not specified as a metadata in the
     *                                       {@code AndroidManifest.xml} with key {@link #OPTIONS_PROVIDER_CLASS_NAME_KEY}.
     *                                   </li>
     *                                   <li>{@code optionsProviderClass} or its nullary constructor is not accessible.</li>
     *                                   <li>Instantiation of {@link OptionsProvider} fails for some other reason.</li>
     *                               </ul>
     * @deprecated Use {@link #getSharedInstance(Context, Executor)} instead to handle the exception when Cast SDK fails to load the internal
     * Cast module.
     */
    @Deprecated
    public static CastContext getSharedInstance(Context context) {
        if (sharedInstance == null) {
            Context appContext = context.getApplicationContext();
            OptionsProvider optionsProvider = getOptionsProvider(appContext);
            CastOptions castOptions = optionsProvider.getCastOptions(appContext);
            try {
                sharedInstance = new CastContext(appContext, castOptions, optionsProvider.getAdditionalSessionProviders(appContext));
            } catch (ModuleUnavailableException e) {
                throw new RuntimeException(e);
            }
        }
        return sharedInstance;
    }

    /**
     * Returns an asynchronous Task API call on the shared instance of {@link CastContext}. The shared instance will be initialized
     * on the first time this method is called.
     * <p>
     * Note that {@link #getSharedInstance(Context, Executor)} should be called in the {@link Activity#onCreate(Bundle)} method
     * of the activities that might display a Cast button. The Cast SDK provides {@link CastButtonFactory} to set up a Cast button.
     * <p>
     * Note that {@link ModuleUnavailableException} could be thrown when the SDK fails to load the internal Cast module. The
     * caller will get the exception from {@link Task#getException()} when the task completes.
     *
     * @param context  An application {@link Context}. If this is not an application {@link Context}, {@link Context#getApplicationContext()} will be called on
     *                 the given context, to retrieve it.
     * @param executor An {@link Executor} to load the internal Cast module.
     * @throws IllegalStateException If any of the following:
     *                               <ul>
     *                                   <li>This method is not called on the main thread.</li>
     *                                   <li>
     *                                       The fully qualified name of the {@link OptionsProvider} implementation is not specified as a metadata in the
     *                                       {@code AndroidManifest.xml} with key {@link #OPTIONS_PROVIDER_CLASS_NAME_KEY}.
     *                                   </li>
     *                                   <li>{@code optionsProviderClass} or its nullary constructor is not accessible.</li>
     *                                   <li>Instantiation of {@link OptionsProvider} fails for some other reason.</li>
     *                               </ul>
     */
    public static Task<CastContext> getSharedInstance(Context context, Executor executor) {
        if (sharedInstance != null) {
            return Tasks.forResult(sharedInstance);
        }
        Context appContext = context.getApplicationContext();
        OptionsProvider optionsProvider = getOptionsProvider(appContext);
        CastOptions castOptions = optionsProvider.getCastOptions(appContext);
        return Tasks.call(executor, () -> {
            sharedInstance = new CastContext(appContext, castOptions, optionsProvider.getAdditionalSessionProviders(appContext));
            return sharedInstance;
        });
    }

    /**
     * Returns the {@link SessionManager}, never returns {@code null}.
     *
     * @throws IllegalStateException If this method is not called on the main thread.
     */
    @NonNull
    public SessionManager getSessionManager() {
        return sessionManager;
    }

    private static volatile CastContext sharedInstance;
    private Context appContext;
    private CastOptions castOptions;
    private IMediaRouter mediaRouter;
    private List<SessionProvider> additionalSessionProviders;
    private CastSessionProvider castSessionProvider;
    private ICastContext delegate;
    private SessionManager sessionManager;
    private DiscoveryManager discoveryManager;

    private CastContext(Context appContext, CastOptions castOptions, @Nullable List<SessionProvider> additionalSessionProviders) throws ModuleUnavailableException {
        this.appContext = appContext;
        this.castOptions = castOptions;
        this.mediaRouter = null; // TODO
        this.additionalSessionProviders = additionalSessionProviders;
        this.castSessionProvider = new CastSessionProvider(appContext, castOptions);
        try {
            this.delegate = CastDynamiteModule.newCastContext(appContext, castOptions, mediaRouter, getSessionProviderMap());
            this.sessionManager = new SessionManager(appContext, delegate.getSessionManagerImpl());
            this.discoveryManager = new DiscoveryManager(appContext, delegate.getDiscoveryManagerImpl());
        } catch (RemoteException e) {
            throw new IllegalStateException("Failed to call dynamite module", e);
        }
    }

    private Map<String, IBinder> getSessionProviderMap() {
        Map<String, IBinder> map = new HashMap<>();
        if (castSessionProvider != null) {
            map.put(castSessionProvider.getCategory(), castSessionProvider.asBinder());
        }
        List<SessionProvider> list = this.additionalSessionProviders;
        if (list != null) {
            for (SessionProvider sessionProvider : list) {
                if (sessionProvider == null) throw new IllegalArgumentException("Additional SessionProvider must not be null.");
                if (sessionProvider.getCategory() == null || sessionProvider.getCategory().isEmpty())
                    throw new IllegalArgumentException("Category for SessionProvider must not be null or empty string.");
                if (map.containsKey(sessionProvider.getCategory()))
                    throw new IllegalArgumentException("SessionProvider for category " + sessionProvider.getCategory() + " already added");
                map.put(sessionProvider.getCategory(), sessionProvider.asBinder());
            }
        }
        return map;
    }

    private static OptionsProvider getOptionsProvider(Context context) {
        try {
            Bundle metaData = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA).metaData;
            String optionsProviderClassName = metaData.getString(OPTIONS_PROVIDER_CLASS_NAME_KEY);
            if (optionsProviderClassName != null) {
                return Class.forName(optionsProviderClassName).asSubclass(OptionsProvider.class).getDeclaredConstructor().newInstance();
            }
            throw new IllegalStateException("The fully qualified name of the implementation of OptionsProvider must be provided as a metadata in the AndroidManifest.xml with key com.google.android.gms.cast.framework.OPTIONS_PROVIDER_CLASS_NAME.");
        } catch (PackageManager.NameNotFoundException | ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InstantiationException |
                 InvocationTargetException | NullPointerException e) {
            throw new IllegalStateException("Failed to initialize CastContext.", e);
        }
    }

    @NonNull
    DiscoveryManager getDiscoveryManager() {
        return discoveryManager;
    }
}
