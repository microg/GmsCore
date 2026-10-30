/*
 * Copyright (C) 2026 microG Project Team
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

package org.microg.gms.wearable;

import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;

import com.google.android.gms.common.data.DataHolder;
import com.google.android.gms.wearable.internal.AmsEntityUpdateParcelable;
import com.google.android.gms.wearable.internal.AncsNotificationParcelable;
import com.google.android.gms.wearable.internal.CapabilityInfoParcelable;
import com.google.android.gms.wearable.internal.ChannelEventParcelable;
import com.google.android.gms.wearable.internal.IWearableListener;
import com.google.android.gms.wearable.internal.MessageEventParcelable;
import com.google.android.gms.wearable.internal.NodeParcelable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class WearableListenerAdapter extends IWearableListener.Stub {
    private static final Map<String, IWearableListener> LISTENER_MAP = Collections.synchronizedMap(new HashMap<>());
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    private static String buildKey(Object listener, String typeKey) {
        return System.identityHashCode(listener) + "_" + (typeKey != null ? typeKey : "default");
    }

    public static IWearableListener getOrCreate(Object listener, String typeKey, ListenerFactory factory) {
        String key = buildKey(listener, typeKey);
        synchronized (LISTENER_MAP) {
            IWearableListener stub = LISTENER_MAP.get(key);
            if (stub == null) {
                stub = factory.create();
                LISTENER_MAP.put(key, stub);
            }
            return stub;
        }
    }

    public static IWearableListener remove(Object listener, String typeKey) {
        String key = buildKey(listener, typeKey);
        synchronized (LISTENER_MAP) {
            return LISTENER_MAP.remove(key);
        }
    }

    public static List<IWearableListener> removeAllForListener(Object listener) {
        String prefix = System.identityHashCode(listener) + "_";
        List<IWearableListener> removed = new ArrayList<>();
        synchronized (LISTENER_MAP) {
            Iterator<Map.Entry<String, IWearableListener>> iterator = LISTENER_MAP.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, IWearableListener> entry = iterator.next();
                if (entry.getKey() != null && entry.getKey().startsWith(prefix)) {
                    removed.add(entry.getValue());
                    iterator.remove();
                }
            }
        }
        return removed;
    }

    public static void postOnMainThread(Runnable runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
        } else {
            MAIN_HANDLER.post(runnable);
        }
    }

    public static void postOnMainThread(DataHolder data, Runnable runnable) {
        deliverData(data, () -> {
            Looper mainLooper = Looper.getMainLooper();
            if (mainLooper != null && Looper.myLooper() == mainLooper) {
                runnable.run();
                return true;
            }
            if (mainLooper == null) {
                return false;
            }
            return MAIN_HANDLER.post(runnable);
        });
    }

    public static void deliverData(DataHolder data, PostAttempt attempt) {
        if (data == null || attempt == null) return;
        boolean posted = false;
        try {
            posted = attempt.attempt();
        } catch (SecurityException securityException) {
            data.close();
            throw securityException;
        } finally {
            if (!posted) {
                data.close();
            }
        }
    }

    public interface PostAttempt {
        boolean attempt();
    }

    public interface ListenerFactory {
        IWearableListener create();
    }

    @Override
    public void onDataChanged(DataHolder data) throws RemoteException {
        if (data != null) {
            data.close();
        }
    }

    @Override
    public void onMessageReceived(MessageEventParcelable messageEvent) throws RemoteException {}

    @Override
    public void onPeerConnected(NodeParcelable node) throws RemoteException {}

    @Override
    public void onPeerDisconnected(NodeParcelable node) throws RemoteException {}

    @Override
    public void onConnectedNodes(List<NodeParcelable> nodes) throws RemoteException {}

    @Override
    public void onNotificationReceived(AncsNotificationParcelable notification) throws RemoteException {}

    @Override
    public void onChannelEvent(ChannelEventParcelable channelEvent) throws RemoteException {}

    @Override
    public void onConnectedCapabilityChanged(CapabilityInfoParcelable capabilityInfo) throws RemoteException {}

    @Override
    public void onEntityUpdate(AmsEntityUpdateParcelable update) throws RemoteException {}
}
