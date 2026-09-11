/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.wearable.bridge;

import android.content.ComponentName;
import android.content.Context;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.microg.gms.wearable.WearableImpl;
import org.microg.gms.wearable.WearableService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Bridges phone {@link MediaController} sessions to Wear OS peers: pushes metadata/state and
 * applies control commands arriving on {@link MediaControlCommand#PATH_PREFIX}.
 */
public final class WearableMediaSessionBridge {

    private static final String TAG = "GmsWearMediaBridge";

    private static WearableMediaSessionBridge instance;

    private final Context context;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private MediaSessionManager sessionManager;
    private MediaController activeController;
    private final MediaController.Callback controllerCallback = new MediaController.Callback() {
        @Override
        public void onPlaybackStateChanged(PlaybackState state) {
            publishState();
        }

        @Override
        public void onMetadataChanged(MediaMetadata metadata) {
            publishState();
        }
    };
    private final MediaSessionManager.OnActiveSessionsChangedListener sessionsListener =
            controllers -> updateActiveController(controllers);

    private WearableMediaSessionBridge(Context context) {
        this.context = context.getApplicationContext();
    }

    public static synchronized WearableMediaSessionBridge getInstance(Context context) {
        if (instance == null) {
            instance = new WearableMediaSessionBridge(context);
        }
        return instance;
    }

    public void start() {
        handler.post(() -> {
            try {
                sessionManager = (MediaSessionManager) context.getSystemService(Context.MEDIA_SESSION_SERVICE);
                if (sessionManager == null) {
                    Log.w(TAG, "MediaSessionManager unavailable");
                    return;
                }
                ComponentName listener = new ComponentName(context, WearableNotificationListenerService.class);
                sessionManager.addOnActiveSessionsChangedListener(sessionsListener, listener);
                updateActiveController(sessionManager.getActiveSessions(listener));
            } catch (SecurityException e) {
                Log.w(TAG, "Notification listener permission required for media sessions", e);
            }
        });
    }

    public void stop() {
        handler.post(() -> {
            if (sessionManager != null) {
                try {
                    sessionManager.removeOnActiveSessionsChangedListener(sessionsListener);
                } catch (Exception e) {
                    Log.d(TAG, "removeOnActiveSessionsChangedListener", e);
                }
            }
            detachController();
            sessionManager = null;
        });
    }

    /**
     * @return {@code true} if the message was a media control command (handled or unrecognized).
     */
    public boolean handleMessage(String path, byte[] data) {
        MediaControlCommand command = MediaControlCommand.parse(path, data);
        if (command == null) {
            return MediaControlCommand.isControlPath(path);
        }
        handler.post(() -> apply(command));
        return true;
    }

    private void apply(MediaControlCommand command) {
        MediaController controller = activeController;
        if (controller == null) {
            Log.d(TAG, "No active media session for " + command.action);
            return;
        }
        MediaController.TransportControls controls = controller.getTransportControls();
        switch (command.action) {
            case PLAY:
                controls.play();
                break;
            case PAUSE:
                controls.pause();
                break;
            case TOGGLE:
                PlaybackState state = controller.getPlaybackState();
                if (state != null && state.getState() == PlaybackState.STATE_PLAYING) {
                    controls.pause();
                } else {
                    controls.play();
                }
                break;
            case NEXT:
                controls.skipToNext();
                break;
            case PREVIOUS:
                controls.skipToPrevious();
                break;
            case SEEK:
                controls.seekTo(command.seekPositionMs);
                break;
            case RATE:
                controls.setPlaybackSpeed(command.playbackRate);
                break;
        }
    }

    private void updateActiveController(List<MediaController> controllers) {
        detachController();
        if (controllers == null || controllers.isEmpty()) {
            return;
        }
        activeController = controllers.get(0);
        activeController.registerCallback(controllerCallback, handler);
        publishState();
    }

    private void detachController() {
        if (activeController != null) {
            try {
                activeController.unregisterCallback(controllerCallback);
            } catch (Exception e) {
                Log.d(TAG, "unregisterCallback", e);
            }
            activeController = null;
        }
    }

    private void publishState() {
        WearableService service = WearableService.getInstance();
        if (service == null) {
            return;
        }
        WearableImpl wearable = service.getWearableImpl();
        if (wearable == null) {
            return;
        }
        Set<String> nodes = wearable.getAllConnectedNodes();
        if (nodes.isEmpty() || activeController == null) {
            return;
        }
        byte[] payload = encodeState(activeController);
        String packageName = context.getPackageName();
        for (String nodeId : nodes) {
            wearable.sendMessage(packageName, nodeId, MediaControlCommand.STATE_PATH, payload);
        }
    }

    static byte[] encodeState(MediaController controller) {
        Map<String, String> fields = new LinkedHashMap<String, String>();
        MediaMetadata metadata = controller.getMetadata();
        PlaybackState state = controller.getPlaybackState();
        if (metadata != null) {
            fields.put("title", nullToEmpty(metadata.getString(MediaMetadata.METADATA_KEY_TITLE)));
            fields.put("artist", nullToEmpty(metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)));
            fields.put("album", nullToEmpty(metadata.getString(MediaMetadata.METADATA_KEY_ALBUM)));
            fields.put("duration", Long.toString(metadata.getLong(MediaMetadata.METADATA_KEY_DURATION)));
        }
        if (state != null) {
            fields.put("isPlaying", Boolean.toString(state.getState() == PlaybackState.STATE_PLAYING));
            fields.put("position", Long.toString(state.getPosition()));
            fields.put("playbackSpeed", Float.toString(state.getPlaybackSpeed()));
        } else {
            fields.put("isPlaying", "false");
            fields.put("position", "0");
            fields.put("playbackSpeed", "1.0");
        }
        String pkg = controller.getPackageName();
        fields.put("packageName", pkg != null ? pkg : "");
        return NotificationPayload.encode(fields);
    }

    private static String nullToEmpty(String value) {
        return value != null ? value : "";
    }
}
