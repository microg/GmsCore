/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.wearable;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.wearable.internal.NodeParcelable;
import com.google.android.gms.wearable.internal.PutDataRequest;

import org.microg.gms.common.Constants;

/**
 * Mirrors phone notifications onto connected Wear OS nodes via MessageApi paths and DataItems.
 */
public class WearableNotificationListenerService extends NotificationListenerService {
    private static final String TAG = "GmsWearNotif";

    @Override
    public void onListenerConnected() {
        try {
            startService(new Intent(this, WearableService.class));
        } catch (Exception e) {
            Log.w(TAG, "Could not start WearableService", e);
        }
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null || shouldIgnore(sbn)) {
            return;
        }
        WearableNotificationPayload payload = fromStatusBarNotification(sbn);
        dispatch(payload.encode(), WearableNotificationPayload.PATH_POSTED, payload);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        if (sbn == null || shouldIgnore(sbn)) {
            return;
        }
        WearableNotificationPayload payload = fromStatusBarNotification(sbn);
        dispatch(payload.encode(), WearableNotificationPayload.PATH_REMOVED, payload);
    }

    private boolean shouldIgnore(StatusBarNotification sbn) {
        if (sbn.isOngoing() && (sbn.getNotification().flags & Notification.FLAG_NO_CLEAR) != 0
                && (sbn.getNotification().flags & Notification.FLAG_FOREGROUND_SERVICE) != 0) {
            // Still forward ongoing; only skip our own service noise.
        }
        String pkg = sbn.getPackageName();
        return TextUtils.equals(pkg, getPackageName()) || TextUtils.equals(pkg, Constants.GMS_PACKAGE_NAME);
    }

    private void dispatch(byte[] data, String path, WearableNotificationPayload payload) {
        WearableImpl wearable = WearableService.getImpl();
        if (wearable == null || wearable.networkHandler == null) {
            Log.d(TAG, "WearableImpl not ready; dropping " + path);
            return;
        }
        if (!WearablePreferences.isNotificationsEnabled(this)) {
            return;
        }
        Context context = getApplicationContext();
        wearable.networkHandler.post(() -> {
            for (NodeParcelable node : wearable.getConnectedNodesParcelableList()) {
                wearable.sendMessage(context.getPackageName(), node.getId(), path, data);
            }
            try {
                PutDataRequest request = PutDataRequest.create(WearableNotificationPayload.dataItemPathForKey(payload.key));
                request.setData(data);
                wearable.putData(request, context.getPackageName());
            } catch (Exception e) {
                Log.w(TAG, "Failed to put notification data item", e);
            }
        });
    }

    static WearableNotificationPayload fromStatusBarNotification(StatusBarNotification sbn) {
        Notification notification = sbn.getNotification();
        Bundle extras = notification.extras;
        CharSequence title = extras != null ? extras.getCharSequence(Notification.EXTRA_TITLE) : null;
        CharSequence text = extras != null ? extras.getCharSequence(Notification.EXTRA_TEXT) : null;
        return new WearableNotificationPayload(
                sbn.getKey(),
                sbn.getPackageName(),
                title != null ? title.toString() : "",
                text != null ? text.toString() : "",
                sbn.isOngoing(),
                sbn.getId()
        );
    }
}
