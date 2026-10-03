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

import java.util.HashMap;
import java.util.Map;

public class RpcHelper {
    private final Map<String, RpcConnectionState> rpcStateMap = new HashMap<String, RpcConnectionState>();
    private final Map<String, RpcConnectionState> unorderedStateMap = new HashMap<String, RpcConnectionState>();
    private final SharedPreferences preferences;
    private final Context context;

    public RpcHelper(Context context) {
        this.context = context;
        this.preferences = context.getSharedPreferences("wearable.rpc_service.settings", 0);
    }

    private String getRpcConnectionId(String packageName, String targetNodeId, String path, int priority) {
        if (priority < 0 || priority > 1) throw new IllegalArgumentException("Invalid RPC priority");
        String mode = priority == 1 ? "hi" : "lo";
        if (packageName.equals("com.google.android.wearable.app") && path.startsWith("/s3"))
            mode = "voice";
        return targetNodeId + ":" + mode;
    }

    public RpcHelper.RpcConnectionState useConnectionState(String packageName, String targetNodeId, String path) {
        return useConnectionState(packageName, targetNodeId, path, 0);
    }

    public RpcHelper.RpcConnectionState useConnectionState(String packageName, String targetNodeId,
                                                           String path, int priority) {
        String rpcConnectionId = getRpcConnectionId(packageName, targetNodeId, path, priority);
        synchronized (rpcStateMap) {
            if (!rpcStateMap.containsKey(rpcConnectionId)) {
                int previous = preferences.getInt(rpcConnectionId, 1);
                // Older versions charged high-priority frames to lo and voice frames to hi.
                // Start beyond those persisted generations so peers discard the old ordering state.
                if (rpcConnectionId.endsWith(":hi")) {
                    previous = Math.max(previous, preferences.getInt(targetNodeId + ":lo", 1));
                } else if (rpcConnectionId.endsWith(":voice")) {
                    previous = Math.max(previous, preferences.getInt(targetNodeId + ":hi", 1));
                }
                int g = previous + 1;
                preferences.edit().putInt(rpcConnectionId, g).apply();
                rpcStateMap.put(rpcConnectionId, new RpcConnectionState(g));
            }
            RpcHelper.RpcConnectionState res = rpcStateMap.get(rpcConnectionId);
            res.lastRequestId++;
            return res.freeze();
        }
    }

    public RpcConnectionState useUnorderedConnectionState(String packageName, String targetNodeId, String path) {
        String connectionId = getRpcConnectionId(packageName, targetNodeId, path, 0);
        synchronized (unorderedStateMap) {
            RpcConnectionState state = unorderedStateMap.get(connectionId);
            if (state == null) {
                // Generation zero bypasses peer reordering and must not consume ordered RPC IDs.
                state = new RpcConnectionState(0);
                unorderedStateMap.put(connectionId, state);
            }
            state.lastRequestId++;
            return state.freeze();
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
}
