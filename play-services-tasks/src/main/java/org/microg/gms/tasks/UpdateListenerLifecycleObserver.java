/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.tasks;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

public class UpdateListenerLifecycleObserver {
    private static WeakHashMap<Activity, WeakReference<UpdateListenerLifecycleObserver>> map = new WeakHashMap<>();
    private static boolean activityLifecycleCallbacksRegistered = false;
    private List<WeakReference<UpdateListener<?>>> list = new ArrayList<>();

    public synchronized static UpdateListenerLifecycleObserver getObserverForActivity(Activity activity) {
        WeakReference<UpdateListenerLifecycleObserver> ref = map.get(activity);
        if (ref != null) {
            UpdateListenerLifecycleObserver observer = ref.get();
            if (observer != null) {
                return observer;
            }
        }

        if (!activityLifecycleCallbacksRegistered) {
            activity.getApplication().registerActivityLifecycleCallbacks(new MyActivityLifecycleCallbacks());
            activityLifecycleCallbacksRegistered = true;
        }

        UpdateListenerLifecycleObserver newInstance = new UpdateListenerLifecycleObserver();
        map.put(activity, new WeakReference<>(newInstance));
        return newInstance;
    }

    private UpdateListenerLifecycleObserver() {
    }

    public synchronized void registerActivityStopListener(UpdateListener<?> listener) {
        list.add(new WeakReference<>(listener));
    }

    public synchronized void onStop() {
        for (WeakReference<UpdateListener<?>> ref : list) {
            UpdateListener<?> listener = ref.get();
            listener.cancel();
        }
        list.clear();
    }

    private static class MyActivityLifecycleCallbacks implements Application.ActivityLifecycleCallbacks {
        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

        }

        @Override
        public void onActivityStarted(Activity activity) {

        }

        @Override
        public void onActivityResumed(Activity activity) {

        }

        @Override
        public void onActivityPaused(Activity activity) {

        }

        @Override
        public void onActivityStopped(Activity activity) {
            WeakReference<UpdateListenerLifecycleObserver> ref = map.get(activity);
            if (ref != null) {
                UpdateListenerLifecycleObserver observer = ref.get();
                if (observer != null) {
                    observer.onStop();
                }
            }
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

        }

        @Override
        public void onActivityDestroyed(Activity activity) {

        }
    }
}
