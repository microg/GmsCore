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

    /** Path on which the wearable peer sends notification-control commands. */
    public static final String NOTIFICATION_COMMAND_PATH = "/wearable/notification/command";

    /** Positive / content action (first Notification.Action or contentIntent). */
    public static final byte CMD_POSITIVE = 1;

    /** Negative action: dismiss / cancel the notification. */
    public static final byte CMD_NEGATIVE = 2;

    /**
     * Maps notification UID (the value sent to the wearable peer) to the live
     * {@link StatusBarNotification}.  Entries are added/removed by the
     * {@code WearableNotificationService} running in the same process.
     */
    public static final Map<Integer, StatusBarNotification> activeNotifications =
            new ConcurrentHashMap<>();


    /**
     * Dispatches a notification-control command received from the watch.
     *
     * <p>Payload format:
     * <pre>
     *   byte action  1 = positive, 2 = negative
     *   int  uid     notification uid previously assigned by WearableNotificationService
     * </pre>
     */
    public static void handleCommand(Context context, byte[] data) {
        if (data == null || data.length < 5) {
            Log.w(TAG, "handleCommand: empty or short payload");
            return;
        }
        try {
            java.io.DataInputStream dis =
                    new java.io.DataInputStream(new java.io.ByteArrayInputStream(data));
            byte action = dis.readByte();
            int uid = dis.readInt();
            Log.d(TAG, "handleCommand: action=" + action + ", uid=" + uid);
            switch (action) {
                case CMD_POSITIVE:
                    doPositiveAction(context, uid);
                    break;
                case CMD_NEGATIVE:
                    doNegativeAction(context, uid);
                    break;
                default:
                    Log.w(TAG, "handleCommand: unknown action=" + action);
            }
        } catch (Exception e) {
            Log.e(TAG, "handleCommand: failed to parse payload", e);
        }
    }

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
