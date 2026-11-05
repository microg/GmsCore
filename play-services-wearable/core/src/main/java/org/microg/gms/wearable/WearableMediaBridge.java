/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.wearable;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Handler;
import android.os.Build;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.google.android.gms.wearable.DataMap;

import org.microg.gms.wearable.media.MediaCommandParser;
import org.microg.gms.wearable.media.MediaPublication;
import org.microg.wearable.WearableConnection;

import java.util.List;
import java.util.concurrent.Semaphore;

/** Bridges a phone MediaSession to the Wear OS media controls Data Layer item. */
public class WearableMediaBridge extends NotificationListenerService {
    private static final String TAG = "WearMediaBridge";
    private static final String MEDIA_PACKAGE = "com.google.android.wearable.media.sessions";
    // SHA-1 package identities from official Wear OS 5, 5.1, 6.0, and 6.1 emulator images.
    // A Data Layer item is visible only to the matching package identity on the watch.
    private static final String[] MEDIA_SIGNATURES = {
            "b50e7e83ecba1f422ecd8b1bf94d597e337250bd", // Wear OS 5 and 5.1
            "aff374c3b0ebf541e4171baab41f2823e7430b70"  // Wear OS 6.0 and 6.1
    };
    private static final String MEDIA_PATH = "/mediacontrols/mediacontrols";
    private static final String COMMAND_PACKAGE = "com.google.android.apps.wear.companion";
    private static volatile WearableMediaBridge listener;
    private static volatile WearableImpl wearable;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private MediaSessionManager sessionManager;
    private volatile MediaController selected;
    private volatile boolean allowed;
    private final MediaPublication publication = new MediaPublication();
    private final Semaphore pendingCommands = new Semaphore(16);
    private final MediaSessionManager.OnActiveSessionsChangedListener sessionsChanged = this::selectSession;
    private final MediaController.Callback controllerCallback = new MediaController.Callback() {
        @Override public void onPlaybackStateChanged(PlaybackState state) { publish(); }
        @Override public void onMetadataChanged(MediaMetadata metadata) { publish(); }
        @Override public void onSessionDestroyed() { refresh(); }
    };

    static void attach(WearableImpl implementation) {
        wearable = implementation;
        WearableMediaBridge current = listener;
        if (current != null) current.handler.post(current::refresh);
        else implementation.networkHandler.post(() -> {
            if (wearable == implementation && listener == null) clearState(implementation);
        });
    }

    static void detach(WearableImpl implementation) {
        if (wearable == implementation) wearable = null;
        WearableMediaBridge current = listener;
        if (current != null) current.publication.cancel(implementation);
    }

    static boolean dispatch(String packageName, String signature, String path, String peerNodeId,
                            WearableConnection connection, byte[] payload) {
        WearableImpl implementation = wearable;
        WearableMediaBridge current = listener;
        if (implementation == null || current == null || peerNodeId == null
                || !current.allowed || !implementation.isCurrentConnection(peerNodeId, connection)
                || !implementation.matchesInstalledSignature(packageName, signature)
                || !COMMAND_PACKAGE.equals(packageName) || !MEDIA_PATH.equals(path)) return false;
        MediaCommandParser.Command command = MediaCommandParser.parse(payload);
        final MediaController selected = current.selected;
        if (command == null || selected == null || !current.pendingCommands.tryAcquire()) return false;
        boolean posted = current.handler.post(() -> {
            try {
            if (wearable != implementation || listener != current || !current.allowed
                    || !current.hasAccess() || current.selected != selected
                    || !implementation.isCurrentConnection(peerNodeId, connection)
                    || !implementation.matchesInstalledSignature(packageName, signature)) return;
            MediaController.TransportControls controls = selected.getTransportControls();
            switch (command) {
                case PLAY: controls.play(); break;
                case PAUSE: controls.pause(); break;
                case NEXT: controls.skipToNext(); break;
                case PREVIOUS: controls.skipToPrevious(); break;
            }
            } catch (SecurityException e) {
                current.onListenerDisconnected();
            } finally {
                current.pendingCommands.release();
            }
        });
        if (!posted) current.pendingCommands.release();
        return true;
    }

    @Override public void onListenerConnected() {
        allowed = true;
        listener = this;
        sessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
        if (sessionManager != null) {
            try {
                sessionManager.addOnActiveSessionsChangedListener(sessionsChanged,
                        new ComponentName(this, WearableMediaBridge.class), handler);
            } catch (SecurityException e) {
                allowed = false;
                Log.w(TAG, "Notification access is required for media controls");
            }
        }
        refresh();
    }

    @Override public void onListenerDisconnected() {
        allowed = false;
        if (listener == this) listener = null;
        if (sessionManager != null) sessionManager.removeOnActiveSessionsChangedListener(sessionsChanged);
        if (selected != null) selected.unregisterCallback(controllerCallback);
        selected = null;
        publication.clear();
        WearableImpl implementation = wearable;
        if (implementation != null) implementation.networkHandler.post(() -> {
            if (wearable == implementation && listener == null) clearState(implementation);
        });
    }

    @Override public void onDestroy() {
        onListenerDisconnected();
        super.onDestroy();
    }

    private static void clearState(WearableImpl implementation) {
        for (String signature : MEDIA_SIGNATURES) {
            implementation.deleteBridgeData(MEDIA_PACKAGE, signature, MEDIA_PATH);
        }
    }

    static void clearUnauthorizedState(Context context, WearableImpl implementation) {
        if (!hasAccess(context)) clearState(implementation);
    }

    static boolean canSynchronize(Context context, DataItemRecord record) {
        return record.deleted || !MEDIA_PACKAGE.equals(record.packageName)
                || !MEDIA_PATH.equals(record.dataItem.path) || hasAccess(context);
    }

    @Override public void onNotificationPosted(StatusBarNotification notification) {
        if (hasAccess() && isMediaNotification(notification)) refresh();
    }

    @Override public void onNotificationRemoved(StatusBarNotification notification) {
        if (hasAccess() && isMediaNotification(notification)) refresh();
    }

    private static boolean isMediaNotification(StatusBarNotification notification) {
        if (notification == null || notification.getNotification() == null) return false;
        Notification payload = notification.getNotification();
        return Notification.CATEGORY_TRANSPORT.equals(payload.category)
                || (payload.extras != null && payload.extras.containsKey(Notification.EXTRA_MEDIA_SESSION));
    }

    private void refresh() {
        if (sessionManager == null || !hasAccess()) return;
        try {
            selectSession(sessionManager.getActiveSessions(new ComponentName(this, WearableMediaBridge.class)));
        } catch (SecurityException e) {
            Log.w(TAG, "Cannot read active media sessions without notification access");
            onListenerDisconnected();
        }
    }

    private boolean hasAccess() {
        if (!allowed) return false;
        return hasAccess(this);
    }

    private static boolean hasAccess(Context context) {
        if (Build.VERSION.SDK_INT < 27) {
            String enabled = Settings.Secure.getString(context.getContentResolver(), "enabled_notification_listeners");
            ComponentName expected = new ComponentName(context, WearableMediaBridge.class);
            if (enabled != null) for (String flattened : enabled.split(":")) {
                if (expected.equals(ComponentName.unflattenFromString(flattened))) return true;
            }
            return false;
        }
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        return manager != null && manager.isNotificationListenerAccessGranted(
                new ComponentName(context, WearableMediaBridge.class));
    }

    private void selectSession(List<MediaController> sessions) {
        if (!hasAccess()) return;
        MediaController next = null;
        if (sessions != null) {
            for (MediaController candidate : sessions) {
                if (!hasAccess()) return;
                PlaybackState state = candidate.getPlaybackState();
                if (state == null || state.getState() == PlaybackState.STATE_NONE
                        || state.getState() == PlaybackState.STATE_ERROR) continue;
                if (next == null) next = candidate;
                if (state != null && state.getState() == PlaybackState.STATE_PLAYING) {
                    next = candidate;
                    break;
                }
            }
        }
        if (selected != next) {
            if (selected != null) selected.unregisterCallback(controllerCallback);
            selected = next;
            if (selected != null) selected.registerCallback(controllerCallback, handler);
        }
        publish();
    }

    private void publish() {
        WearableImpl implementation = wearable;
        if (implementation == null || !hasAccess()) return;
        MediaController controller = selected;
        MediaMetadata metadata = controller != null ? controller.getMetadata() : null;
        PlaybackState state = controller != null ? controller.getPlaybackState() : null;
        DataMap map = new DataMap();
        map.putBoolean("mediacontrols.media_notification_active", controller != null);
        map.putBoolean("mediacontrols.playing", state != null && state.getState() == PlaybackState.STATE_PLAYING);
        map.putLong("elapsed_time", SystemClock.elapsedRealtime());
        if (controller != null) {
            String packageName = controller.getPackageName();
            map.putString("mediacontrols.package_name", packageName);
            try {
                CharSequence label = getPackageManager().getApplicationLabel(
                        getPackageManager().getApplicationInfo(packageName, 0));
                map.putString("mediacontrols.application_label", bounded(label));
            } catch (Exception e) {
                map.putString("mediacontrols.application_label", packageName);
            }
        }
        if (metadata != null) {
            CharSequence title = metadata.getText(MediaMetadata.METADATA_KEY_TITLE);
            CharSequence artist = metadata.getText(MediaMetadata.METADATA_KEY_ARTIST);
            map.putString("mediacontrols.title", bounded(title));
            map.putString("mediacontrols.artist", bounded(artist));
            map.putLong("mediacontrols.duration", metadata.getLong(MediaMetadata.METADATA_KEY_DURATION));
        }
        if (state != null) {
            map.putLong("mediacontrols.position", state.getPosition());
            map.putLong("mediacontrols.position_update_time", state.getLastPositionUpdateTime());
            map.putFloat("mediacontrols.playback_speed", state.getPlaybackSpeed());
            long actions = state.getActions();
            int flags = 0;
            if ((actions & (PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE | PlaybackState.ACTION_PLAY_PAUSE)) != 0) flags |= 8;
            if ((actions & PlaybackState.ACTION_SKIP_TO_PREVIOUS) != 0) flags |= 1;
            if ((actions & PlaybackState.ACTION_SKIP_TO_NEXT) != 0) flags |= 128;
            map.putInt("mediacontrols.transport_flags", flags);
        }
        if (!publication.offer(implementation, map.toByteArray())) return;
        boolean posted = implementation.networkHandler.post(() -> {
            final byte[] stateBytes = publication.take(implementation);
            if (stateBytes == null || wearable != implementation || listener != this || !hasAccess()) return;
            DataItemInternal item = new DataItemInternal(implementation.getLocalNodeId(), MEDIA_PATH);
            item.data = stateBytes;
            for (String signature : MEDIA_SIGNATURES) {
                implementation.syncRecordToAll(implementation.putDataItem(MEDIA_PACKAGE, signature,
                        implementation.getLocalNodeId(), item));
            }
        });
        if (!posted) publication.cancel(implementation);
    }

    private static String bounded(CharSequence value) {
        if (value == null) return "";
        return value.subSequence(0, Math.min(value.length(), 256)).toString();
    }
}
