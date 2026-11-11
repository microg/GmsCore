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

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.android.gms.wearable.internal.AncsNotificationParcelable;
import com.google.android.gms.wearable.internal.NodeParcelable;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Companion bridge for modern WearOS devices (microg/GmsCore#2843).
 * Handles notification echoing, media/remote command forwarding, call state actions,
 * and cloud-sync / storage configuration persistence so WearOS companion apps complete
 * binder handshakes and exchange notifications/media controls with paired watches.
 */
public class WearOsCompanionBridge {
    private static final String TAG = "GmsWearCompanion";
    private static final String PREFS_NAME = "wearable_companion_prefs";
    private static final String KEY_CLOUD_SYNC_ENABLED = "cloud_sync_enabled";
    private static final String KEY_CLOUD_SYNC_OPT_IN_DONE = "cloud_sync_opt_in_done";

    public static final String PATH_NOTIFICATION_BRIDGE = "/wearable/notifications/bridge";
    public static final String PATH_NOTIFICATION_ACTION = "/wearable/notifications/action";
    public static final String PATH_MEDIA_REMOTE_COMMAND = "/wearable/media/remote_command";
    public static final String PATH_TELECOM_ACTION = "/wearable/telecom/action";
    public static final String PATH_WIFI_SYNC = "/wearable/wifi/credentials_sync";

    private final Context context;
    private final WearableImpl wearable;
    private final String packageName;
    private final Map<Integer, AncsNotificationParcelable> activeNotifications = new ConcurrentHashMap<>();

    public WearOsCompanionBridge(Context context, WearableImpl wearable, String packageName) {
        this.context = context;
        this.wearable = wearable;
        this.packageName = packageName;
    }

    private SharedPreferences getPrefs() {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void setCloudSyncEnabled(boolean enabled) {
        getPrefs().edit()
                .putBoolean(KEY_CLOUD_SYNC_ENABLED, enabled)
                .putBoolean(KEY_CLOUD_SYNC_OPT_IN_DONE, true)
                .apply();
    }

    public boolean isCloudSyncEnabled() {
        return getPrefs().getBoolean(KEY_CLOUD_SYNC_ENABLED, false);
    }

    public boolean isCloudSyncOptInDone() {
        return getPrefs().getBoolean(KEY_CLOUD_SYNC_OPT_IN_DONE, true);
    }

    public int broadcastToConnectedWatches(String path, byte[] payload) {
        List<NodeParcelable> nodes = wearable.getConnectedNodesParcelableList();
        int dispatched = 0;
        if (nodes != null) {
            for (NodeParcelable node : nodes) {
                if (node != null && node.id != null) {
                    try {
                        wearable.sendMessage(packageName, node.id, path, payload);
                        dispatched++;
                    } catch (Exception e) {
                        Log.w(TAG, "Failed broadcasting to watch node " + node.id + " on " + path, e);
                    }
                }
            }
        }
        return dispatched;
    }

    public void bridgeNotification(AncsNotificationParcelable notification) {
        if (notification == null) return;
        activeNotifications.put(notification.id, notification);
        try {
            JSONObject json = new JSONObject();
            json.put("id", notification.id);
            json.put("appId", notification.appId != null ? notification.appId : packageName);
            json.put("dateTime", notification.dateTime != null ? notification.dateTime : "");
            json.put("title", notification.title != null ? notification.title : "");
            json.put("subtitle", notification.subtitle != null ? notification.subtitle : "");
            json.put("message", notification.message != null ? notification.message : "");
            json.put("displayName", notification.displayName != null ? notification.displayName : "");
            json.put("eventId", (int) notification.eventId);
            json.put("eventFlags", (int) notification.eventFlags);
            json.put("categoryId", (int) notification.categoryId);
            json.put("categoryCount", (int) notification.categoryCount);
            broadcastToConnectedWatches(
                    PATH_NOTIFICATION_BRIDGE,
                    json.toString().getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            Log.w(TAG, "Failed serializing bridged WearOS notification", e);
        }
    }

    public void performNotificationAction(int notificationId, boolean positive) {
        activeNotifications.remove(notificationId);
        try {
            JSONObject json = new JSONObject();
            json.put("notificationId", notificationId);
            json.put("action", positive ? "POSITIVE" : "NEGATIVE");
            json.put("packageName", packageName);
            broadcastToConnectedWatches(
                    PATH_NOTIFICATION_ACTION,
                    json.toString().getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            Log.w(TAG, "Failed forwarding notification action", e);
        }
    }

    public void dispatchRemoteMediaCommand(byte commandCode) {
        try {
            JSONObject json = new JSONObject();
            json.put("commandCode", (int) commandCode);
            json.put("packageName", packageName);
            broadcastToConnectedWatches(
                    PATH_MEDIA_REMOTE_COMMAND,
                    json.toString().getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            Log.w(TAG, "Failed dispatching media remote command", e);
        }
    }

    public void dispatchTelecomAction(String telecomAction) {
        try {
            JSONObject json = new JSONObject();
            json.put("telecomAction", telecomAction);
            json.put("packageName", packageName);
            broadcastToConnectedWatches(
                    PATH_TELECOM_ACTION,
                    json.toString().getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            Log.w(TAG, "Failed dispatching telecom action " + telecomAction, e);
        }
    }

    public void syncWifiCredentialsToWatches() {
        try {
            JSONObject json = new JSONObject();
            json.put("syncRequestedBy", packageName);
            json.put("timestampMs", System.currentTimeMillis());
            broadcastToConnectedWatches(
                    PATH_WIFI_SYNC,
                    json.toString().getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            Log.w(TAG, "Failed syncing Wi-Fi credentials trigger", e);
        }
    }
}
