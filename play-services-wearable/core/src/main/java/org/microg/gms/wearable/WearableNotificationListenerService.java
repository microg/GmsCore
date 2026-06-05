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

import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.net.URI;

/**
 * NotificationListenerService that mirrors phone notifications to connected Wear OS watches.
 * <p>
 * Captures all posted notifications and forwards them to the watch via the Wearable Data Layer.
 * The watch receives the notification data and displays it as a native Wear OS notification.
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
        removeNotificationFromWear(sbn);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn, RankingMap rankingMap) {
        onNotificationRemoved(sbn);
    }

    private void forwardNotificationToWear(StatusBarNotification sbn) {
        try {
            Notification notification = sbn.getNotification();
            if (notification == null) return;

            // Build data map for the notification
            String notificationId = sbn.getPackageName() + ":" + sbn.getId() + ":" + sbn.getTag();

            PutDataRequest request = PutDataRequest.create(WEAR_PATH_PREFIX + notificationId.replace(":", "/"));

            // Add basic notification data
            request.getDataMap().putString("package", sbn.getPackageName());
            request.getDataMap().putInt("id", sbn.getId());
            request.getDataMap().putLong("postTime", sbn.getPostTime());
            request.getDataMap().putBoolean("isOngoing", sbn.isOngoing());
            request.getDataMap().putBoolean("isClearable", sbn.isClearable());

            // Extract notification content
            Bundle extras = notification.extras;
            if (extras != null) {
                CharSequence title = extras.getCharSequence(Notification.EXTRA_TITLE);
                CharSequence text = extras.getCharSequence(Notification.EXTRA_TEXT);
                CharSequence subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT);
                CharSequence info = extras.getCharSequence(Notification.EXTRA_INFO_TEXT);
                CharSequence summary = extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT);

                if (title != null) request.getDataMap().putString("title", title.toString());
                if (text != null) request.getDataMap().putString("text", text.toString());
                if (subText != null) request.getDataMap().putString("subText", subText.toString());
                if (info != null) request.getDataMap().putString("infoText", info.toString());
                if (summary != null) request.getDataMap().putString("summaryText", summary.toString());

                // Add small icon as asset
                int smallIconRes = extras.getInt(Notification.EXTRA_SMALL_ICON);
                request.getDataMap().putInt("smallIcon", smallIconRes);

                // Add notification category and priority
                request.getDataMap().putString("category", notification.category);
                request.getDataMap().putInt("priority", notification.priority);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    request.getDataMap().putInt("badgeIconType", notification.getBadgeIconType());
                }

                // Add notification actions
                Notification.Action[] actions = notification.actions;
                if (actions != null) {
                    for (int i = 0; i < Math.min(actions.length, 3); i++) {
                        String actionTitle = actions[i].title != null ? actions[i].title.toString() : "";
                        request.getDataMap().putString("action_" + i, actionTitle);
                    }
                    request.getDataMap().putInt("actionCount", Math.min(actions.length, 3));
                }
            }

            // Send to watch via Wearable Data Layer
            // This is done asynchronously
            com.google.android.gms.wearable.Wearable.DataApi.putDataItem(
                Wearable.getClient(this),
                request.toDataMap()
            ).await();

            Log.d(TAG, "Notification forwarded to watch: " + notificationId);
        } catch (Exception e) {
            Log.w(TAG, "Failed to forward notification to wear", e);
        }
    }

    private void removeNotificationFromWear(StatusBarNotification sbn) {
        try {
            String notificationId = sbn.getPackageName() + ":" + sbn.getId() + ":" + sbn.getTag();
            String path = WEAR_PATH_PREFIX + notificationId.replace(":", "/");

            // Delete the data item to remove the notification from the watch
            com.google.android.gms.wearable.Wearable.DataApi.deleteDataItems(
                Wearable.getClient(this),
                new android.net.Uri.Builder()
                    .scheme("wear")
                    .path(path)
                    .build()
            ).await();
        } catch (Exception e) {
            Log.w(TAG, "Failed to remove notification from wear", e);
        }
    }
}
