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
import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;

import androidx.mediarouter.media.MediaRouteSelector;

import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.cast.framework.CastOptions;
import com.google.android.gms.cast.framework.IAppVisibilityListener;
import com.google.android.gms.cast.framework.ICastContext;
import com.google.android.gms.cast.framework.IDiscoveryManager;
import com.google.android.gms.cast.framework.ISessionProvider;
import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.dynamic.ObjectWrapper;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Module side of {@code CastContext}.
 * <p>
 * This is loaded into the process of the client app and wires the three things the client library can not do on its
 * own: route discovery (through the client's {@link IMediaRouter}), starting a session once a Cast route is selected
 * and reporting the cast state.
 */
public class CastContextImpl extends ICastContext.Stub {
    private static final String TAG = "GmsCastContext";

    private final Context context;
    private final IMediaRouter router;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Map<String, ISessionProvider> sessionProviders = new LinkedHashMap<>();
    private final ListenerRegistry<IAppVisibilityListener> visibilityListeners = new ListenerRegistry<>(TAG, IAppVisibilityListener::getThisObject);
    private final SessionManagerImpl sessionManager;
    private final DiscoveryManagerImpl discoveryManager;

    private CastOptions options;
    private String receiverApplicationId;
    private ISessionProvider defaultSessionProvider;
    private MediaRouteSelector mergedSelector = MediaRouteSelector.EMPTY;
    private Bundle registeredSelectorBundle;

    // Activities of the client app that are currently started, used to derive app visibility
    private final Set<Activity> startedActivities = Collections.newSetFromMap(new WeakHashMap<>());
    private boolean lifecycleObserved = false;
    private boolean visibleBeforeObserving = false;
    private boolean applicationVisible = false;

    public CastContextImpl(IObjectWrapper context, CastOptions options, IMediaRouter router, Map<String, IBinder> sessionProviders) throws RemoteException {
        this.context = (Context) ObjectWrapper.unwrap(context);
        this.options = options;
        this.router = router;
        this.receiverApplicationId = options == null ? null : options.getReceiverApplicationId();
        this.sessionManager = new SessionManagerImpl(this);
        this.discoveryManager = new DiscoveryManagerImpl(this);

        setSessionProviders(sessionProviders);
        registerRouterCallback();
        observeAppVisibility();
        // The client may create us on a background thread, while its MediaRouter has to be used on the main thread
        this.mainHandler.post(this.sessionManager::resumeSavedSession);
    }

    private void setSessionProviders(Map<?, ?> providers) {
        this.sessionProviders.clear();
        if (providers != null) {
            for (Map.Entry<?, ?> entry : providers.entrySet()) {
                if (entry.getKey() instanceof String && entry.getValue() instanceof IBinder) {
                    this.sessionProviders.put((String) entry.getKey(), ISessionProvider.Stub.asInterface((IBinder) entry.getValue()));
                }
            }
        }

        String defaultCategory = receiverApplicationId == null ? null : CastMediaControlIntent.categoryForCast(receiverApplicationId);
        this.defaultSessionProvider = defaultCategory == null ? null : this.sessionProviders.get(defaultCategory);
        if (this.defaultSessionProvider == null && defaultCategory != null) {
            // The client may key its providers by the full category, which has namespaces and flags appended
            for (Map.Entry<String, ISessionProvider> entry : this.sessionProviders.entrySet()) {
                if (entry.getKey().startsWith(defaultCategory)) {
                    this.defaultSessionProvider = entry.getValue();
                    break;
                }
            }
        }
        if (this.defaultSessionProvider == null && !this.sessionProviders.isEmpty()) {
            this.defaultSessionProvider = this.sessionProviders.values().iterator().next();
        }

        // Only routes that one of the session providers can handle are of interest. Adding generic categories here would
        // make the cast button offer routes that we can't start a session on.
        MediaRouteSelector.Builder builder = new MediaRouteSelector.Builder();
        for (String category : this.sessionProviders.keySet()) {
            builder.addControlCategory(category);
        }
        if (defaultCategory != null) {
            builder.addControlCategory(defaultCategory);
        }
        this.mergedSelector = builder.build();
    }

    /**
     * Registers our callback with the MediaRouter of the client. Without this the module never learns about route
     * selection and thus never starts a session.
     */
    private void registerRouterCallback() {
        try {
            if (registeredSelectorBundle != null) {
                router.clearCallbacks();
            }
            registeredSelectorBundle = mergedSelector.asBundle();
            router.registerMediaRouterCallbackImpl(registeredSelectorBundle, new MediaRouterCallbackImpl(this));
            discoveryManager.onSelectorChanged();
        } catch (Exception e) {
            Log.w(TAG, "Failed to register media router callback", e);
        }
    }

    private void observeAppVisibility() {
        try {
            ActivityManager.RunningAppProcessInfo info = new ActivityManager.RunningAppProcessInfo();
            ActivityManager.getMyMemoryState(info);
            visibleBeforeObserving = info.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE;
        } catch (Exception e) {
            visibleBeforeObserving = true;
        }

        Context applicationContext = context == null ? null : context.getApplicationContext();
        if (applicationContext instanceof Application) {
            ((Application) applicationContext).registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
                @Override
                public void onActivityStarted(Activity activity) {
                    startedActivities.add(activity);
                    updateAppVisibility();
                }

                @Override
                public void onActivityStopped(Activity activity) {
                    lifecycleObserved = true;
                    startedActivities.remove(activity);
                    updateAppVisibility();
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
                }
            });
        } else {
            // Can't observe, so assume we're visible. Discovery is passive, so this only costs some multicast traffic.
            Log.w(TAG, "Application context is not an Application, assuming app is always visible");
            visibleBeforeObserving = true;
        }
        updateAppVisibility();
    }

    private void updateAppVisibility() {
        // Until we have seen an activity stop, there might be activities that were started before we began observing.
        boolean visible = !startedActivities.isEmpty() || (!lifecycleObserved && visibleBeforeObserving);
        if (visible == applicationVisible) return;
        applicationVisible = visible;
        Log.d(TAG, "Application visibility changed: " + visible);

        discoveryManager.onAppVisibilityChanged();

        this.visibilityListeners.forEach("app visibility listener", listener -> {
            if (visible) {
                listener.onAppEnteredForeground();
            } else {
                listener.onAppEnteredBackground();
            }
        });
    }

    @Override
    public Bundle getMergedSelectorAsBundle() throws RemoteException {
        return this.mergedSelector.asBundle();
    }

    @Override
    public void addVisibilityChangeListener(IAppVisibilityListener listener) {
        this.visibilityListeners.add(listener);
    }

    @Override
    public void removeVisibilityChangeListener(IAppVisibilityListener listener) {
        this.visibilityListeners.remove(listener);
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

    public DiscoveryManagerImpl getDiscoveryManager() {
        return this.discoveryManager;
    }

    @Override
    public void destroy() throws RemoteException {
        // Deprecated in the client library, CastContext is a process-wide singleton.
        Log.d(TAG, "unimplemented Method: destroy");
    }

    @Override
    public void onActivityResumed(IObjectWrapper activity) throws RemoteException {
        // Only invoked by old client libraries. We observe the activity lifecycle ourselves.
    }

    @Override
    public void onActivityPaused(IObjectWrapper activity) throws RemoteException {
        // Only invoked by old client libraries. We observe the activity lifecycle ourselves.
    }

    @Override
    public void setReceiverApplicationId(String receiverApplicationId, Map sessionProvidersByCategory) throws RemoteException {
        Log.d(TAG, "setReceiverApplicationId: " + receiverApplicationId);
        if (receiverApplicationId == null || receiverApplicationId.equals(this.receiverApplicationId)) return;
        this.receiverApplicationId = receiverApplicationId;
        setSessionProviders(sessionProvidersByCategory);
        registerRouterCallback();
    }

    @Override
    public boolean hasActivityInRecents() throws RemoteException {
        try {
            if (Build.VERSION.SDK_INT >= 21) {
                ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                return activityManager != null && !activityManager.getAppTasks().isEmpty();
            }
            return applicationVisible;
        } catch (Exception e) {
            return applicationVisible;
        }
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

    public Bundle getRegisteredSelectorBundle() {
        return this.registeredSelectorBundle;
    }

    public CastOptions getOptions() {
        return this.options;
    }

    public String getReceiverApplicationId() {
        return this.receiverApplicationId;
    }

    public ISessionProvider getDefaultSessionProvider() {
        return this.defaultSessionProvider;
    }

    public boolean isAppVisible() {
        return this.applicationVisible;
    }

    public Handler getMainHandler() {
        return this.mainHandler;
    }

    @Override
    public IObjectWrapper getWrappedThis() throws RemoteException {
        return ObjectWrapper.wrap(this);
    }
}
