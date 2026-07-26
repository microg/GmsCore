/*
 * Copyright (C) 2025 microG Project Team
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

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Bridges Android notifications from the phone to paired WearOS devices.
 * This service runs as a NotificationListenerService, captures incoming
 * notifications, and forwards them to the wearable via the Data Layer API.
 */
public class NotificationBridgeService extends NotificationListenerService {
    private static final String TAG = "GmsWearNotifBridge";
    private static final String PATH_NOTIFICATION = "/notifications";
    private static final String PATH_NOTIFICATION_ACTION = "/notification_action";
    private static final String KEY_PACKAGE = "package";
    private static final String KEY_TITLE = "title";
    private static final String KEY_TEXT = "text";
    private static final String KEY_SUB_TEXT = "subText";
    private static final String KEY_POST_TIME = "postTime";
    private static final String KEY_ID = "notificationId";
    private static final String KEY_TAG = "tag";
    private static final String KEY_GROUP = "group";
    private static final String KEY_ACTION_INDEX = "actionIndex";
    private static final String KEY_ACTION = "action";

    private GoogleApiClient googleApiClient;
    private ExecutorService executor;
    private Handler mainHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "NotificationBridgeService created");
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
        connectGoogleApiClient();
    }

    private synchronized void connectGoogleApiClient() {
        if (googleApiClient == null || !googleApiClient.isConnected()) {
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Wearable.API)
                    .build();
            googleApiClient.connect();
        }
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return;
        Log.d(TAG, "Notification posted: " + sbn.getPackageName() + " / " + sbn.getId());

        Notification notification = sbn.getNotification();
        if (notification == null) return;

        // Don't mirror our own notifications
        if (getPackageName().equals(sbn.getPackageName())) return;

        executor.execute(() -> {
            try {
                connectGoogleApiClient();
                if (!googleApiClient.isConnected()) return;
                forwardNotificationToWearable(sbn, notification);
            } catch (Exception e) {
                Log.w(TAG, "Failed to forward notification", e);
            }
        });
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return;
        Log.d(TAG, "Notification removed: " + sbn.getPackageName() + " / " + sbn.getId());
        executor.execute(() -> {
            try {
                connectGoogleApiClient();
                if (!googleApiClient.isConnected()) return;
                removeNotificationFromWearable(sbn);
            } catch (Exception e) {
                Log.w(TAG, "Failed to remove notification from wearable", e);
            }
        });
    }

    private void forwardNotificationToWearable(StatusBarNotification sbn, Notification notification) {
        String packageName = sbn.getPackageName();
        int notificationId = sbn.getId();
        String tag = sbn.getTag();
        long postTime = sbn.getPostTime();

        CharSequence title = null;
        CharSequence text = null;
        CharSequence subText = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            Bundle extras = notification.extras;
            if (extras != null) {
                title = extras.getCharSequence(Notification.EXTRA_TITLE);
                text = extras.getCharSequence(Notification.EXTRA_TEXT);
                subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT);
            }
        }

        String group = notification.getGroup();

        // Build DataMap
        String path = PATH_NOTIFICATION + "/" + packageName + "/" + notificationId;
        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(path);
        DataMap dataMap = putDataMapRequest.getDataMap();
        dataMap.putString(KEY_PACKAGE, packageName);
        dataMap.putString(KEY_TITLE, title != null ? title.toString() : "");
        dataMap.putString(KEY_TEXT, text != null ? text.toString() : "");
        dataMap.putString(KEY_SUB_TEXT, subText != null ? subText.toString() : "");
        dataMap.putLong(KEY_POST_TIME, postTime);
        dataMap.putInt(KEY_ID, notificationId);
        dataMap.putString(KEY_TAG, tag);
        if (group != null) {
            dataMap.putString(KEY_GROUP, group);
        }

        // Send via DataApi
        com.google.android.gms.common.api.PendingResult<com.google.android.gms.wearable.DataApi.DataItemResult> result =
                Wearable.DataApi.putDataItem(googleApiClient, putDataMapRequest.asPutDataRequest());
        result.await();
        Log.d(TAG, "Notification forwarded to wearable: " + packageName + "/" + notificationId);
    }

    private void removeNotificationFromWearable(StatusBarNotification sbn) {
        String path = PATH_NOTIFICATION + "/" + sbn.getPackageName() + "/" + sbn.getId();
        com.google.android.gms.common.api.PendingResult<com.google.android.gms.wearable.DataApi.DeleteDataItemsResult> result =
                Wearable.DataApi.deleteDataItems(googleApiClient,
                        new android.net.Uri.Builder().scheme("wear").path(path).build());
        result.await();
        Log.d(TAG, "Notification removed from wearable: " + path);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "NotificationBridgeService destroyed");
        if (googleApiClient != null && googleApiClient.isConnected()) {
            googleApiClient.disconnect();
        }
        if (executor != null) {
            executor.shutdown();
        }
    }

    /**
     * Handle notification action invocations from the wearable.
     * Called when a user taps an action button on a notification from the watch.
     */
    public static void handleNotificationAction(Context context, String packageName, int notificationId,
                                                  String tag, int actionIndex) {
        Intent intent = new Intent();
        intent.setPackage(packageName);
        // This is a simplified approach - full implementation would need
        // to reconstruct the PendingIntent from the original notification
        Log.d(TAG, "Notification action for " + packageName + ": id=" + notificationId
                + ", tag=" + tag + ", action=" + actionIndex);
    }
}