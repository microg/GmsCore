/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.wearable.bridge;

import android.app.Notification;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import org.microg.gms.wearable.WearableImpl;
import org.microg.gms.wearable.WearableService;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Mirrors phone notifications to connected Wear OS peers over the Wearable message transport.
 *
 * <p>Requires the user to enable microG as a notification listener. Ongoing / local-only
 * notifications are skipped.
 */
public class WearableNotificationListenerService extends NotificationListenerService {

    private static final String TAG = "GmsWearNotifBridge";

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null || shouldSkip(sbn)) {
            return;
        }
        WearableImpl wearable = wearable();
        if (wearable == null) {
            return;
        }
        Notification notification = sbn.getNotification();
        Bundle extras = notification.extras;
        String title = extras != null ? extras.getString(Notification.EXTRA_TITLE, "") : "";
        CharSequence textCs = extras != null ? extras.getCharSequence(Notification.EXTRA_TEXT) : null;
        String text = textCs != null ? textCs.toString() : "";
        List<String> actions = new ArrayList<String>();
        if (notification.actions != null) {
            for (Notification.Action action : notification.actions) {
                if (action.title != null) {
                    actions.add(action.title.toString());
                }
            }
        }
        NotificationPayload payload = new NotificationPayload(
                sbn.getKey(),
                sbn.getPackageName(),
                sbn.getId(),
                title,
                text,
                notification.category,
                notification.priority,
                (notification.flags & Notification.FLAG_ONGOING_EVENT) != 0,
                actions);
        broadcast(wearable, NotificationPayload.PATH_POSTED, payload.toBytes());
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        if (sbn == null) {
            return;
        }
        WearableImpl wearable = wearable();
        if (wearable == null) {
            return;
        }
        broadcast(
                wearable,
                NotificationPayload.PATH_REMOVED,
                NotificationPayload.removedBytes(sbn.getKey(), sbn.getPackageName(), sbn.getId()));
    }

    private static boolean shouldSkip(StatusBarNotification sbn) {
        if (sbn.isOngoing()) {
            return true;
        }
        Notification notification = sbn.getNotification();
        if (notification == null) {
            return true;
        }
        if ((notification.flags & Notification.FLAG_LOCAL_ONLY) != 0) {
            return true;
        }
        // Do not echo microG's own wearable bridge traffic.
        String pkg = sbn.getPackageName();
        return pkg != null && pkg.contains("android.gms") && sbn.getTag() != null
                && sbn.getTag().startsWith("wearable");
    }

    private WearableImpl wearable() {
        WearableService service = WearableService.getInstance();
        return service != null ? service.getWearableImpl() : null;
    }

    private void broadcast(WearableImpl wearable, String path, byte[] data) {
        Set<String> nodes = wearable.getAllConnectedNodes();
        if (nodes.isEmpty()) {
            return;
        }
        String packageName = getPackageName();
        for (String nodeId : nodes) {
            int requestId = wearable.sendMessage(packageName, nodeId, path, data);
            if (requestId < 0) {
                Log.d(TAG, "Failed to send " + path + " to " + nodeId);
            }
        }
    }
}
