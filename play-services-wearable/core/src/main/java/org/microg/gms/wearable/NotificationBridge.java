/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.wearable;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared bridge between {@link WearableServiceImpl} and the in-process
 * {@code WearableNotificationService} (play-services-core).
 * <p>
 * {@code WearableNotificationService} populates {@link #activeNotifications}
 * so that ANCS action requests from a Wear OS peer can be dispatched to the
 * correct Android notification.
 */
public class NotificationBridge {

    private static final String TAG = "GmsWearNotifBridge";

    /**
     * Maps notification UID (the value sent to the wearable peer) to the live
     * {@link StatusBarNotification}.  Entries are added/removed by the
     * {@code WearableNotificationService} running in the same process.
     */
    public static final Map<Integer, StatusBarNotification> activeNotifications =
            new ConcurrentHashMap<>();

    /**
     * Executes the <em>positive</em> ANCS action for {@code uid}: fires the first
     * {@link android.app.Notification.Action} on the notification if one exists.
     */
    public static void doPositiveAction(Context context, int uid) {
        StatusBarNotification sbn = activeNotifications.get(uid);
        if (sbn == null) {
            Log.d(TAG, "doPositiveAction: no notification for uid=" + uid);
            return;
        }
        Notification n = sbn.getNotification();
        if (n == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Notification.Action[] actions = n.actions;
            if (actions != null && actions.length > 0 && actions[0].actionIntent != null) {
                try {
                    actions[0].actionIntent.send(context, 0, null);
                } catch (Exception e) {
                    Log.w(TAG, "doPositiveAction: PendingIntent.send() failed", e);
                }
                return;
            }
        }
        // No action available — fall back to content intent
        if (n.contentIntent != null) {
            try {
                n.contentIntent.send(context, 0, null);
            } catch (Exception e) {
                Log.w(TAG, "doPositiveAction: contentIntent.send() failed", e);
            }
        }
    }

    /**
     * Executes the <em>negative</em> ANCS action for {@code uid}: dismisses / cancels
     * the notification.
     */
    public static void doNegativeAction(Context context, int uid) {
        StatusBarNotification sbn = activeNotifications.get(uid);
        if (sbn == null) {
            Log.d(TAG, "doNegativeAction: no notification for uid=" + uid);
            return;
        }
        NotificationManager nm =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                nm.cancel(sbn.getTag(), sbn.getId());
            }
        }
        activeNotifications.remove(uid);
    }
}
