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

package org.microg.gms.wearable;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class RpcHelper {
    private final Map<String, RpcConnectionState> rpcStateMap = new HashMap<String, RpcConnectionState>();
    private final SharedPreferences preferences;
    private final Context context;

    private final ConcurrentMap<String, PendingRpcListener> rpcListeners = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, PendingDataSyncListener> dataSyncListeners = new ConcurrentHashMap<>();

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public RpcHelper(Context context) {
        this.context = context;
        this.preferences = context.getSharedPreferences("wearable.rpc_service.settings", 0);
    }

    public static int combineId(int generation, int requestId) {
        // magic numbers, need to find out what exactly is 527 and 31
        return ((generation + 527) * 31) + requestId;
    }

    private static String rpcListenerKey(String peerNodeId, int requestId) {
        return peerNodeId + ":" + requestId;
    }

    private String getRpcConnectionId(String packageName, String targetNodeId, String path) {
        String mode = "lo";
        if (packageName.equals("com.google.android.wearable.app") && path.startsWith("/s3"))
            mode = "hi";
        return targetNodeId + ":" + mode;
    }

    public RpcHelper.RpcConnectionState useConnectionState(String packageName, String targetNodeId, String path) {
        String rpcConnectionId = getRpcConnectionId(packageName, targetNodeId, path);
        synchronized (rpcStateMap) {
            if (!rpcStateMap.containsKey(rpcConnectionId)) {
                int g = preferences.getInt(rpcConnectionId, 1)+1;
                preferences.edit().putInt(rpcConnectionId, g).apply();
                rpcStateMap.put(rpcConnectionId, new RpcConnectionState(g));
            }
            RpcHelper.RpcConnectionState res = rpcStateMap.get(rpcConnectionId);
            res.lastRequestId++;
            return res.freeze();
        }
    }

    public static class RpcConnectionState {
        public int generation;
        public int lastRequestId;

        public RpcConnectionState(int generation) {
            this.generation = generation;
        }

        public RpcConnectionState freeze() {
            RpcConnectionState res = new RpcConnectionState(generation);
            res.lastRequestId = lastRequestId;
            return res;
        }
    }

    public void addResponseListener(String peerNodeId, int requestId, long timeoutMs,
                                    RpcResponseCallback onResponse, RpcTimeoutCallback onTimeout) {
        final String key = rpcListenerKey(peerNodeId, requestId);
        final PendingRpcListener l = new PendingRpcListener(onResponse, onTimeout);
        l.timeoutTask = () -> { if (rpcListeners.remove(key, l)) onTimeout.onTimeout(); };

        PendingRpcListener old = rpcListeners.put(key, l);
        if (old != null) {
            mainHandler.removeCallbacks(old.timeoutTask);
            mainHandler.post(old.timeout::onTimeout);
        }
        mainHandler.postDelayed(l.timeoutTask, timeoutMs);
    }

    public boolean deliverRpcResponse(String peerNodeId, int senderRequestId, @Nullable byte[] data) {
        PendingRpcListener l = rpcListeners.remove(rpcListenerKey(peerNodeId, senderRequestId));
        if (l == null) return false;
        mainHandler.removeCallbacks(l.timeoutTask);
        l.callback.onResponse(data);
        return true;
    }

    public void removeResponseListener(String peerNodeId, int requestId) {
        PendingRpcListener l = rpcListeners.remove(rpcListenerKey(peerNodeId, requestId));
        if (l != null) mainHandler.removeCallbacks(l.timeoutTask);
    }

    public void failPeer(String peerNodeId) {
        final String prefix = peerNodeId + ":";
        for (Map.Entry<String, PendingRpcListener> e : rpcListeners.entrySet()) {
            if (e.getKey().startsWith(prefix) && rpcListeners.remove(e.getKey(), e.getValue())) {
                mainHandler.removeCallbacks(e.getValue().timeoutTask);
                final PendingRpcListener dead = e.getValue();
                mainHandler.post(dead.timeout::onTimeout);
            }
        }
    }


    public interface RpcResponseCallback {
        void onResponse(@Nullable byte[] data);
    }

    public interface RpcTimeoutCallback {
        void onTimeout();
    }

    private static final class PendingRpcListener {
        final RpcResponseCallback callback;
        final RpcTimeoutCallback timeout;
        volatile Runnable timeoutTask;

        PendingRpcListener(RpcResponseCallback rc, RpcTimeoutCallback tc) {
            this.callback = rc;
            this.timeout = tc;
        }
    }

    public void addDataSyncListener(String nodeId, String trackerId,
                                    long timeoutMs, DataSyncResponseCallback onReached,
                                    DataSyncTimeoutCallback onTimeout) {
        String key = nodeId + ":" + trackerId;
        dataSyncListeners.put(key, new PendingDataSyncListener(onReached, onTimeout));
        mainHandler.postDelayed(() -> {
            if (dataSyncListeners.remove(key) != null) {
                onTimeout.onTimeout();
            }
        }, timeoutMs);
    }

    public void cancelDataSyncListener(String nodeId, String trackerId) {
        dataSyncListeners.remove(nodeId + ":" + trackerId);
    }

    public boolean deliverDataSyncResponse(String peerNodeId, String trackerId,
                                           long reachedSeqId) {
        String key = peerNodeId + ":" + trackerId;
        PendingDataSyncListener listener = dataSyncListeners.remove(key);
        if (listener == null) return false;
        listener.callback.onReached(reachedSeqId);
        return true;
    }

    public interface DataSyncResponseCallback {
        void onReached(long reachedSeqId);
    }

    public interface DataSyncTimeoutCallback {
        void onTimeout();
    }

    private static final class PendingDataSyncListener {
        final DataSyncResponseCallback callback;
        final DataSyncTimeoutCallback timeout;

        PendingDataSyncListener(DataSyncResponseCallback cb, DataSyncTimeoutCallback to) {
            this.callback = cb;
            this.timeout = to;
        }
    }


}
