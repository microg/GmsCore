/*
 * Copyright (C) 2013-2025 microG Project Team
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
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * Service to handle notification syncing between phone and WearOS devices
 */
public class WearableNotificationSync extends NotificationListenerService {
    private static final String TAG = "WearNotificationSync";
    
    private WearableImpl wearable;
    private AudioManager audioManager;
    private Map<String, StatusBarNotification> activeNotifications = new HashMap<>();

    @Override
    public void onCreate() {
        super.onCreate();
        wearable = new WearableImpl(this, null, null);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        Log.d(TAG, "WearableNotificationSync service created");
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);
        
        if (shouldSyncNotification(sbn)) {
            syncNotificationToWearable(sbn);
            activeNotifications.put(sbn.getKey(), sbn);
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        super.onNotificationRemoved(sbn);
        
        if (activeNotifications.containsKey(sbn.getKey())) {
            removeNotificationFromWearable(sbn);
            activeNotifications.remove(sbn.getKey());
        }
    }

    private boolean shouldSyncNotification(StatusBarNotification sbn) {
        Notification notification = sbn.getNotification();
        String packageName = sbn.getPackageName();
        
        // Don't sync our own notifications
        if (packageName.equals("org.microg.gms") || packageName.equals("com.google.android.gms")) {
            return false;
        }
        
        // Don't sync system notifications that aren't user-facing
        if ((notification.flags & Notification.FLAG_LOCAL_ONLY) != 0) {
            return false;
        }
        
        // Only sync notifications that would normally be shown to the user
        if (notification.priority < Notification.PRIORITY_LOW) {
            return false;
        }
        
        return true;
    }

    private void syncNotificationToWearable(StatusBarNotification sbn) {
        try {
            Notification notification = sbn.getNotification();
            
            // Create a wearable-friendly notification bundle
            Bundle wearableData = new Bundle();
            wearableData.putString("package", sbn.getPackageName());
            wearableData.putString("title", getNotificationTitle(notification));
            wearableData.putString("text", getNotificationText(notification));
            wearableData.putLong("timestamp", sbn.getPostTime());
            wearableData.putString("key", sbn.getKey());
            wearableData.putInt("id", sbn.getId());
            
            // Add actions if available
            if (notification.actions != null) {
                String[] actionTitles = new String[notification.actions.length];
                for (int i = 0; i < notification.actions.length; i++) {
                    actionTitles[i] = notification.actions[i].title.toString();
                }
                wearableData.putStringArray("actions", actionTitles);
            }
            
            // Send to connected wearable devices
            wearable.sendNotificationToWearables(wearableData);
            
            Log.d(TAG, "Synced notification from " + sbn.getPackageName() + " to wearables");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to sync notification to wearable", e);
        }
    }

    private void removeNotificationFromWearable(StatusBarNotification sbn) {
        try {
            Bundle removalData = new Bundle();
            removalData.putString("action", "remove");
            removalData.putString("key", sbn.getKey());
            removalData.putString("package", sbn.getPackageName());
            
            wearable.sendNotificationToWearables(removalData);
            
            Log.d(TAG, "Removed notification " + sbn.getKey() + " from wearables");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to remove notification from wearable", e);
        }
    }

    private String getNotificationTitle(Notification notification) {
        Bundle extras = notification.extras;
        if (extras != null) {
            CharSequence title = extras.getCharSequence(Notification.EXTRA_TITLE);
            if (title != null) {
                return title.toString();
            }
        }
        return "Notification";
    }

    private String getNotificationText(Notification notification) {
        Bundle extras = notification.extras;
        if (extras != null) {
            CharSequence text = extras.getCharSequence(Notification.EXTRA_TEXT);
            if (text != null) {
                return text.toString();
            }
            CharSequence bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT);
            if (bigText != null) {
                return bigText.toString();
            }
        }
        return "";
    }

    /**
     * Handle media control requests from wearables
     */
    public void handleMediaControl(String action) {
        try {
            Intent mediaIntent = new Intent(action);
            sendBroadcast(mediaIntent);
            
            // Also try to control via AudioManager for system-level controls
            switch (action) {
                case "PLAY_PAUSE":
                    // Toggle play/pause - this is device dependent
                    audioManager.dispatchMediaKeyEvent(
                        new android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, 
                        android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));
                    audioManager.dispatchMediaKeyEvent(
                        new android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, 
                        android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));
                    break;
                    
                case "NEXT_TRACK":
                    audioManager.dispatchMediaKeyEvent(
                        new android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, 
                        android.view.KeyEvent.KEYCODE_MEDIA_NEXT));
                    audioManager.dispatchMediaKeyEvent(
                        new android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, 
                        android.view.KeyEvent.KEYCODE_MEDIA_NEXT));
                    break;
                    
                case "PREVIOUS_TRACK":
                    audioManager.dispatchMediaKeyEvent(
                        new android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, 
                        android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS));
                    audioManager.dispatchMediaKeyEvent(
                        new android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, 
                        android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS));
                    break;
                    
                case "VOLUME_UP":
                    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, 
                        AudioManager.ADJUST_RAISE, 0);
                    break;
                    
                case "VOLUME_DOWN":
                    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, 
                        AudioManager.ADJUST_LOWER, 0);
                    break;
            }
            
            Log.d(TAG, "Handled media control: " + action);
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to handle media control: " + action, e);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (wearable != null) {
            wearable.stop();
        }
        Log.d(TAG, "WearableNotificationSync service destroyed");
    }
}