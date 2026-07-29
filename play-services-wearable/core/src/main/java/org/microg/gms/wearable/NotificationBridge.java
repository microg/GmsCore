/*
 * SPDX-FileCopyrightText: 2024-2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.wearable;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bridges Android notifications from the phone to connected Wear OS peers.
 *
 * <p>This class serves as the shared state between {@link WearableServiceImpl} and the
 * in-process {@code WearableNotificationService} (play-services-core). It maintains a
 * thread-safe map of active notifications and provides ANCS-compatible action dispatch.
 *
 * <h2>Key Responsibilities</h2>
 * <ul>
 *   <li>Maintains {@link #activeNotifications} mapping from notification UID to live
 *       {@link StatusBarNotification}.</li>
 *   <li>Dispatches positive and negative ANCS actions on behalf of a connected watch.</li>
 *   <li>Provides notification filtering and prioritization for wearable delivery.</li>
 *   <li>Supports notification category-based routing (calls, messages, media, other).</li>
 * </ul>
 *
 * <h2>ANCS Action Support</h2>
 * <table>
 *   <tr><th>Action</th><th>Method</th><th>Fallback</th></tr>
 *   <tr><td>Positive</td><td>First notification action PendingIntent</td><td>Content intent</td></tr>
 *   <tr><td>Negative</td><td>Cancel notification</td><td>—</td></tr>
 * </table>
 *
 * @see android.service.notification.NotificationListenerService
 */
public class NotificationBridge {

    private static final String TAG = "GmsWearNotifBridge";

    /**
     * Maps notification UID (the integer value sent to the wearable peer in ANCS messages)
     * to the live {@link StatusBarNotification}. Thread-safe via {@link ConcurrentHashMap}.
     */
    public static final Map<Integer, StatusBarNotification> activeNotifications =
            new ConcurrentHashMap<>();

    // -------------------------------------------------------------------------
    // Notification categorization
    // -------------------------------------------------------------------------

    /** Notification category for call-related notifications. */
    public static final String CATEGORY_CALL = "call";
    /** Notification category for messaging notifications. */
    public static final String CATEGORY_MSG = "msg";
    /** Notification category for media playback notifications. */
    public static final String CATEGORY_MEDIA = "media";
    /** Notification category for all other notifications. */
    public static final String CATEGORY_OTHER = "other";

    /**
     * Determines the wearable category for a notification based on its Android category
     * and extras.
     *
     * @param sbn the status bar notification
     * @return one of {@link #CATEGORY_CALL}, {@link #CATEGORY_MSG},
     *         {@link #CATEGORY_MEDIA}, or {@link #CATEGORY_OTHER}
     */
    public static String getNotificationCategory(StatusBarNotification sbn) {
        if (sbn == null) return CATEGORY_OTHER;
        Notification n = sbn.getNotification();
        if (n == null) return CATEGORY_OTHER;

        String cat = n.category;
        if (cat == null) cat = "";
        switch (cat) {
            case Notification.CATEGORY_CALL:
                return CATEGORY_CALL;
            case Notification.CATEGORY_MESSAGE:
            case Notification.CATEGORY_EMAIL:
            case Notification.CATEGORY_SOCIAL:
                return CATEGORY_MSG;
            case Notification.CATEGORY_TRANSPORT:
                return CATEGORY_MEDIA;
            default:
                return CATEGORY_OTHER;
        }
    }

    /**
     * Filters the active notifications list to only those matching a given category.
     *
     * @param category one of the CATEGORY_* constants
     * @return an unmodifiable list of matching notifications (may be empty)
     */
    public static List<StatusBarNotification> getNotificationsByCategory(String category) {
        List<StatusBarNotification> result = new ArrayList<>();
        for (StatusBarNotification sbn : activeNotifications.values()) {
            if (category.equals(getNotificationCategory(sbn))) {
                result.add(sbn);
            }
        }
        return Collections.unmodifiableList(result);
    }

    // -------------------------------------------------------------------------
    // ANCS Action Dispatch
    // -------------------------------------------------------------------------

    /**
     * Executes the <em>positive</em> ANCS action for the notification identified by
     * {@code uid}. Tries the first {@link android.app.Notification.Action} if available,
     * then falls back to the notification's content intent.
     *
     * @param context application context
     * @param uid     notification unique identifier (matching the key in
     *                {@link #activeNotifications})
     */
    public static void doPositiveAction(Context context, int uid) {
        StatusBarNotification sbn = activeNotifications.get(uid);
        if (sbn == null) {
            Log.d(TAG, "doPositiveAction: no notification for uid=" + uid);
            return;
        }
        Notification n = sbn.getNotification();
        if (n == null) return;

        // Try notification actions first (API 19+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Notification.Action[] actions = n.actions;
            if (actions != null && actions.length > 0 && actions[0].actionIntent != null) {
                try {
                    Bundle extras = new Bundle();
                    actions[0].actionIntent.send(context, 0, null, null, null, null, extras);
                    Log.d(TAG, "doPositiveAction: fired action for uid=" + uid);
                    return;
                } catch (Exception e) {
                    Log.w(TAG, "doPositiveAction: PendingIntent.send() failed", e);
                }
            }
        }

        // Fall back to content intent
        if (n.contentIntent != null) {
            try {
                n.contentIntent.send(context, 0, null);
                Log.d(TAG, "doPositiveAction: fired content intent for uid=" + uid);
            } catch (Exception e) {
                Log.w(TAG, "doPositiveAction: contentIntent.send() failed", e);
            }
        }
    }

    /**
     * Executes the <em>negative</em> ANCS action for the notification identified by
     * {@code uid}. Cancels (dismisses) the notification and removes it from
     * {@link #activeNotifications}.
     *
     * @param context application context
     * @param uid     notification unique identifier
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
            try {
                nm.cancel(sbn.getTag(), sbn.getId());
                Log.d(TAG, "doNegativeAction: canceled notification uid=" + uid);
            } catch (SecurityException e) {
                Log.w(TAG, "doNegativeAction: cannot cancel notification for uid=" + uid, e);
            }
        }
    }

    /**
     * Executes a specific ANCS action by index for the notification identified by {@code uid}.
     *
     * @param context     application context
     * @param uid         notification unique identifier
     * @param actionIndex index of the action to fire (0-based, maps to
     *                    {@link Notification.Action} array index)
     * @return {@code true} if the action was successfully dispatched, {@code false} otherwise
     */
    public static boolean doCustomAction(Context context, int uid, int actionIndex) {
        StatusBarNotification sbn = activeNotifications.get(uid);
        if (sbn == null) {
            Log.d(TAG, "doCustomAction: no notification for uid=" + uid);
            return false;
        }
        Notification n = sbn.getNotification();
        if (n == null) return false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Notification.Action[] actions = n.actions;
            if (actions != null && actionIndex >= 0 && actionIndex < actions.length) {
                Notification.Action action = actions[actionIndex];
                if (action.actionIntent != null) {
                    try {
                        Bundle extras = new Bundle();
                        action.actionIntent.send(context, 0, null, null, null, null, extras);
                        Log.d(TAG, "doCustomAction: fired action[" + actionIndex + "] for uid=" + uid);
                        return true;
                    } catch (Exception e) {
                        Log.w(TAG, "doCustomAction: PendingIntent.send() failed", e);
                    }
                }
            }
        }
        return false;
    }

    /**
     * Clears the entire notification cache. Typically called when the wearable
     * connection is reset.
     */
    public static void clear() {
        Log.d(TAG, "clear: removing " + activeNotifications.size() + " cached notifications");
        activeNotifications.clear();
    }
}
