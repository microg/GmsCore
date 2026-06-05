/*
 * SPDX-FileCopyrightText: 2024, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.wearable;

import android.app.Notification;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

/**
 * NotificationListenerService that mirrors phone notifications to connected Wear OS watches.
 * <p>
 * Captures all posted notifications and forwards them to the watch via the internal
 * WearableImpl data layer.
 */
public class WearableNotificationListenerService extends NotificationListenerService {

    private static final String TAG = "GmsWearNotif";

    private static final String WEAR_PATH_PREFIX = "/wearable/notification/";

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Log.d(TAG, "Notification posted: " + sbn.getPackageName() + " / " + sbn.getId());
        forwardNotificationToWear(sbn);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn, RankingMap rankingMap) {
        onNotificationPosted(sbn);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        Log.d(TAG, "Notification removed: " + sbn.getPackageName() + " / " + sbn.getId());
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn, RankingMap rankingMap) {
        onNotificationRemoved(sbn);
    }

    private void forwardNotificationToWear(StatusBarNotification sbn) {
        try {
            Notification notification = sbn.getNotification();
            if (notification == null) return;

            String notificationId = sbn.getPackageName() + ":" + sbn.getId() + ":" + sbn.getTag();
            String path = WEAR_PATH_PREFIX + notificationId.replace(":", "/");

            // Build a simple text-format data payload
            StringBuilder sb = new StringBuilder();

            // Basic notification data
            appendField(sb, "package", sbn.getPackageName());
            appendField(sb, "id", String.valueOf(sbn.getId()));
            appendField(sb, "postTime", String.valueOf(sbn.getPostTime()));
            appendField(sb, "isOngoing", String.valueOf(sbn.isOngoing()));
            appendField(sb, "isClearable", String.valueOf(sbn.isClearable()));

            // Extract notification content
            Bundle extras = notification.extras;
            if (extras != null) {
                CharSequence title = extras.getCharSequence(Notification.EXTRA_TITLE);
                CharSequence text = extras.getCharSequence(Notification.EXTRA_TEXT);
                CharSequence subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT);
                CharSequence info = extras.getCharSequence(Notification.EXTRA_INFO_TEXT);
                CharSequence summary = extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT);

                if (title != null) appendField(sb, "title", title.toString());
                if (text != null) appendField(sb, "text", text.toString());
                if (subText != null) appendField(sb, "subText", subText.toString());
                if (info != null) appendField(sb, "infoText", info.toString());
                if (summary != null) appendField(sb, "summaryText", summary.toString());

                int smallIconRes = extras.getInt(Notification.EXTRA_SMALL_ICON);
                appendField(sb, "smallIcon", String.valueOf(smallIconRes));

                appendField(sb, "category", notification.category);
                appendField(sb, "priority", String.valueOf(notification.priority));

                // Notification actions
                Notification.Action[] actions = notification.actions;
                if (actions != null) {
                    for (int i = 0; i < Math.min(actions.length, 3); i++) {
                        String actionTitle = actions[i].title != null ? actions[i].title.toString() : "";
                        appendField(sb, "action_" + i, actionTitle);
                    }
                    appendField(sb, "actionCount", String.valueOf(Math.min(actions.length, 3)));
                }
            }

            byte[] data = sb.toString().getBytes();

            // Push through WearableImpl if available
            WearableImpl wearable = getWearableImpl();
            if (wearable != null) {
                DataItemInternal dataItem = new DataItemInternal(wearable.getLocalNodeId(), path);
                dataItem.data = data;
                DataItemRecord record = wearable.putDataItem(
                        "com.google.android.gms",
                        "notif_bridge",
                        wearable.getLocalNodeId(),
                        dataItem);
                wearable.syncRecordToAll(record);
                Log.d(TAG, "Notification forwarded to watch: " + notificationId);
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to forward notification to wear", e);
        }
    }

    private static void appendField(StringBuilder sb, String key, String value) {
        if (value == null) return;
        // Simple escaping: replace \n with \\n
        sb.append(key).append("=").append(value.replace("\n", "\\n")).append("\n");
    }

    /**
     * Gets the WearableImpl instance. In microG, this is accessible via the
     * WearableService. Falls back to a static reference if one was set.
     */
    private WearableImpl getWearableImpl() {
        // The WearableImpl is held by WearableService. Since the notification
        // listener runs in the same process, we can access it via the static holder.
        return WearableService.getWearableImpl();
    }
}
