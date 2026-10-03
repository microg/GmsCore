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

package com.google.android.gms.cast.framework.internal;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import androidx.mediarouter.media.MediaRouteSelector;
import androidx.mediarouter.media.MediaRouter;

import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.cast.framework.CastOptions;
import com.google.android.gms.cast.framework.IAppVisibilityListener;
import com.google.android.gms.cast.framework.ICastContext;
import com.google.android.gms.cast.framework.IDiscoveryManager;
import com.google.android.gms.cast.framework.ISessionProvider;
import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.dynamic.ObjectWrapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class CastContextImpl extends ICastContext.Stub {
    private static final String TAG = CastContextImpl.class.getSimpleName();

    private final SessionManagerImpl sessionManager;
    private final DiscoveryManagerImpl discoveryManager;

    private final Context context;
    private final CastOptions options;
    private final IMediaRouter router;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Map<String, ISessionProvider> sessionProviders = new HashMap<>();
    public ISessionProvider defaultSessionProvider;
    private String receiverApplicationId;
    private String defaultCategory;

    private MediaRouteSelector mergedSelector;
    private final List<Bundle> callbackSelectors = new ArrayList<>();
    private boolean activeDiscovery;

    // Listeners are added by the client library while it is constructing CastContext, possibly off the main thread.
    private final Set<IAppVisibilityListener> visibilityListeners = new CopyOnWriteArraySet<>();
    private final Set<Activity> startedActivities = new HashSet<>();
    private boolean applicationVisible = false;
    private Application.ActivityLifecycleCallbacks lifecycleCallbacks;

    public CastContextImpl(IObjectWrapper context, CastOptions options, IMediaRouter router, Map<String, IBinder> sessionProviders) throws RemoteException {
        this.context = (Context) ObjectWrapper.unwrap(context);
        this.options = options;
        this.router = router;
        this.receiverApplicationId = options.getReceiverApplicationId();
        setSessionProviders(sessionProviders);
        this.mergedSelector = buildSelector();
        // The client library requests the managers right after construction, possibly from another thread than the
        // main thread, so they have to exist before anything runs on the main thread.
        this.sessionManager = new SessionManagerImpl(this);
        this.discoveryManager = new DiscoveryManagerImpl(this);
        runOnMainThread(() -> {
            registerRouterCallback();
            trackApplicationVisibility();
            getSessionManagerImpl().tryResumeSavedSession();
        });
    }

    private void setSessionProviders(Map<String, IBinder> providers) {
        this.sessionProviders.clear();
        if (providers != null) {
            for (Map.Entry<String, IBinder> entry : providers.entrySet()) {
                this.sessionProviders.put(entry.getKey(), ISessionProvider.Stub.asInterface(entry.getValue()));
            }
        }
        this.defaultCategory = findDefaultCategory();
        this.defaultSessionProvider = defaultCategory == null ? null : this.sessionProviders.get(defaultCategory);
    }

    /**
     * Finds the control category of the client library's CastSession provider. Client libraries use different formats
     * for the category (recent ones append namespaces and flags, e.g. {@code CATEGORY_CAST/233637DE///ALLOW_IPV6}), so
     * the category is taken from the session providers the client passed instead of being recomputed.
     */
    private String findDefaultCategory() {
        if (TextUtils.isEmpty(receiverApplicationId)) return null;
        String fallback = CastMediaControlIntent.categoryForCast(receiverApplicationId);
        if (sessionProviders.containsKey(fallback)) return fallback;
        String prefix = CastMediaControlIntent.CATEGORY_CAST + "/";
        for (String category : sessionProviders.keySet()) {
            if (category == null || !category.startsWith(prefix)) continue;
            String rest = category.substring(prefix.length());
            int end = rest.indexOf('/');
            String applicationId = end < 0 ? rest : rest.substring(0, end);
            if (applicationId.equalsIgnoreCase(receiverApplicationId)) return category;
        }
        return fallback;
    }

    /**
     * The control category of the default (Cast) session provider. Cast routes have to advertise this category to be
     * selectable.
     */
    public String getDefaultCategory() {
        return defaultCategory;
    }

    /**
     * The session provider the client passed for the control category, or {@code null}.
     */
    public ISessionProvider getSessionProvider(String category) {
        return sessionProviders.get(category);
    }

    private MediaRouteSelector buildSelector() {
        MediaRouteSelector.Builder builder = new MediaRouteSelector.Builder();
        if (defaultCategory != null) builder.addControlCategory(defaultCategory);
        for (String category : sessionProviders.keySet()) {
            if (category != null) builder.addControlCategory(category);
        }
        return builder.build();
    }

    void runOnMainThread(Runnable runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
        } else {
            mainHandler.post(runnable);
        }
    }

    private void registerRouterCallback() {
        if (mergedSelector.isEmpty()) {
            Log.d(TAG, "No receiver application id configured, not registering for media routes");
            return;
        }
        try {
            // One callback per control category: MediaRouter only reports the routes matching a callback's selector, so
            // a selected route can be handed to the session provider of the category it was matched for.
            for (String category : mergedSelector.getControlCategories()) {
                Bundle selector = new MediaRouteSelector.Builder().addControlCategory(category).build().asBundle();
                router.registerMediaRouterCallbackImpl(selector, new CategoryRouterCallback(category));
                callbackSelectors.add(selector);
                router.addCallback(selector, getCallbackFlags());
            }
            getSessionManagerImpl().updateCastState();
        } catch (RemoteException e) {
            Log.w(TAG, "Failed to register media router callback", e);
        }
    }

    private void unregisterRouterCallback() {
        if (callbackSelectors.isEmpty()) return;
        try {
            for (Bundle selector : callbackSelectors) {
                router.removeCallback(selector);
            }
            router.clearCallbacks();
        } catch (RemoteException e) {
            Log.w(TAG, "Failed to unregister media router callback", e);
        }
        callbackSelectors.clear();
    }

    private int getCallbackFlags() {
        // Keep receiving route events while in background, request discovery while the app is visible.
        return activeDiscovery ? MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY : 0;
    }

    void setActiveDiscovery(boolean activeDiscovery) {
        runOnMainThread(() -> {
            if (this.activeDiscovery == activeDiscovery) return;
            this.activeDiscovery = activeDiscovery;
            try {
                for (Bundle selector : callbackSelectors) {
                    // MediaRouter only adds flags to an existing callback, so it has to be removed to drop the discovery flag.
                    if (!activeDiscovery) router.removeCallback(selector);
                    router.addCallback(selector, getCallbackFlags());
                }
            } catch (RemoteException e) {
                Log.w(TAG, "Failed to update media router discovery", e);
            }
        });
    }

    private void trackApplicationVisibility() {
        Context applicationContext = context.getApplicationContext();
        if (!(applicationContext instanceof Application)) {
            // Activities can't be observed, assume the app is in use.
            setApplicationVisible(true);
            return;
        }
        lifecycleCallbacks = new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityStarted(Activity activity) {
                startedActivities.add(activity);
                setApplicationVisible(true);
            }

            @Override
            public void onActivityStopped(Activity activity) {
                startedActivities.remove(activity);
                if (startedActivities.isEmpty()) setApplicationVisible(false);
            }

            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            }

            @Override
            public void onActivityResumed(Activity activity) {
            }

            @Override
            public void onActivityPaused(Activity activity) {
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
                startedActivities.remove(activity);
            }
        };
        ((Application) applicationContext).registerActivityLifecycleCallbacks(lifecycleCallbacks);
        // CastContext is created from a visible activity in practically all apps. Depending on whether it was created
        // before or after that activity's onStart, the activity may or may not be reported as started, so it isn't
        // counted: the app is in background as soon as no started activity is left.
        setApplicationVisible(true);
    }

    private void setApplicationVisible(boolean visible) {
        if (applicationVisible == visible) return;
        applicationVisible = visible;
        setActiveDiscovery(visible);
        for (IAppVisibilityListener listener : visibilityListeners) {
            try {
                if (visible) {
                    listener.onAppEnteredForeground();
                } else {
                    listener.onAppEnteredBackground();
                }
            } catch (RemoteException e) {
                Log.d(TAG, "Remote exception calling app visibility listener: " + e.getMessage());
            }
        }
    }

    @Override
    public Bundle getMergedSelectorAsBundle() throws RemoteException {
        return this.mergedSelector.asBundle();
    }

    @Override
    public void addVisibilityChangeListener(IAppVisibilityListener listener) {
        if (listener != null) visibilityListeners.add(listener);
    }

    @Override
    public void removeVisibilityChangeListener(IAppVisibilityListener listener) {
        visibilityListeners.remove(listener);
    }

    @Override
    public boolean isApplicationVisible() throws RemoteException {
        return applicationVisible;
    }

    @Override
    public SessionManagerImpl getSessionManagerImpl() {
        return this.sessionManager;
    }

    @Override
    public IDiscoveryManager getDiscoveryManagerImpl() throws RemoteException {
        return this.discoveryManager;
    }

    @Override
    public void destroy() throws RemoteException {
        runOnMainThread(() -> {
            unregisterRouterCallback();
            Context applicationContext = context.getApplicationContext();
            if (lifecycleCallbacks != null && applicationContext instanceof Application) {
                ((Application) applicationContext).unregisterActivityLifecycleCallbacks(lifecycleCallbacks);
                lifecycleCallbacks = null;
            }
            startedActivities.clear();
        });
    }

    @Override
    public void onActivityResumed(IObjectWrapper activity) throws RemoteException {
        // Deprecated by the client library, visibility is tracked using activity lifecycle callbacks.
    }

    @Override
    public void onActivityPaused(IObjectWrapper activity) throws RemoteException {
        // Deprecated by the client library, visibility is tracked using activity lifecycle callbacks.
    }

    @Override
    public void setReceiverApplicationId(String receiverApplicationId, Map sessionProvidersByCategory) throws RemoteException {
        Log.d(TAG, "setReceiverApplicationId: " + receiverApplicationId);
        runOnMainThread(() -> {
            if (TextUtils.equals(this.receiverApplicationId, receiverApplicationId)) return;
            getSessionManagerImpl().endCurrentSessionInternal(true);
            unregisterRouterCallback();
            this.receiverApplicationId = receiverApplicationId;
            setSessionProviders((Map<String, IBinder>) sessionProvidersByCategory);
            this.mergedSelector = buildSelector();
            registerRouterCallback();
        });
    }

    public Context getContext() {
        return this.context;
    }

    public IMediaRouter getRouter() {
        return this.router;
    }

    public MediaRouteSelector getMergedSelector() {
        return this.mergedSelector;
    }

    public CastOptions getOptions() {
        return this.options;
    }

    public String getReceiverApplicationId() {
        return receiverApplicationId;
    }

    @Override
    public IObjectWrapper getWrappedThis() throws RemoteException {
        return ObjectWrapper.wrap(this);
    }

    /**
     * Router callback for the routes of a single control category.
     */
    private class CategoryRouterCallback extends MediaRouterCallbackImpl {
        private final String category;

        CategoryRouterCallback(String category) {
            super(CastContextImpl.this);
            this.category = category;
        }

        @Override
        public void onRouteSelected(String routeId, Bundle extras) {
            Log.d(TAG, "onRouteSelected: " + routeId + " for " + category);
            getSessionManagerImpl().onRouteSelected(category, routeId, extras);
        }

        @Override
        public void onRouteSelectedWithRequestedRoute(String requestedRouteId, String selectedRouteId, Bundle extras) {
            Log.d(TAG, "onRouteSelected: " + selectedRouteId + " (requested " + requestedRouteId + ") for " + category);
            getSessionManagerImpl().onRouteSelected(category, selectedRouteId, extras);
        }
    }
}
