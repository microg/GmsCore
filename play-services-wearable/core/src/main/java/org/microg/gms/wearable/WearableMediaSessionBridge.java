/*
 * SPDX-FileCopyrightText: 2024, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.wearable;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Bridges media playback info from the phone's active media sessions to connected Wear OS watches.
 * <p>
 * Monitors all active {@link android.media.session.MediaSession} instances on the device
 * and pushes metadata (track title, artist, album) and playback state (playing/paused,
 * position) to the WearableImpl data layer. Also listens via WearableImpl for incoming
 * control commands from the watch and forwards them to MediaController.TransportControls.
 * <p>
 * Started automatically by {@link WearableService} when the wearable system is initialized.
 */
public class WearableMediaSessionBridge extends android.app.Service {

    private static final String TAG = "GmsWearMedia";

    /** Path prefix for media state data items. */
    private static final String WEAR_PATH_MEDIA = "/wearable/media";

    /** Path for media state items sent phone -> watch. */
    private static final String PATH_MEDIA_STATE = WEAR_PATH_MEDIA + "/state";

    /** Path prefix for incoming control commands watch -> phone. */
    private static final String PATH_MEDIA_CONTROL = WEAR_PATH_MEDIA + "/control";

    private MediaSessionManager mediaSessionManager;
    private MediaController activeMediaController;
    private final Set<MediaController> activeControllers = new HashSet<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private WearableImpl wearable;

    // -------------------------------------------------------------------------
    // Service lifecycle
    // -------------------------------------------------------------------------

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "MediaSessionBridge created");

        // Get reference to WearableImpl via static accessor on WearableService
        this.wearable = WearableService.getWearableImpl();

        mediaSessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);

        // Register media button receiver to re-evaluate sessions on hardware button press
        IntentFilter filter = new IntentFilter(Intent.ACTION_MEDIA_BUTTON);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(mediaButtonReceiver, filter, RECEIVER_EXPORTED);
        } else {
            registerReceiver(mediaButtonReceiver, filter);
        }

        // Start monitoring active media sessions immediately
        monitorActiveSessions();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // When started from WearableService, get a reference to WearableImpl
        if (intent != null && intent.hasExtra("wearable")) {
            // WearableImpl reference is passed through the service - we access it via singleton
        }
        return START_STICKY;
    }

    /**
     * Sets the WearableImpl instance. Called by WearableService after creation.
     */
    public void setWearable(WearableImpl impl) {
        this.wearable = impl;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "MediaSessionBridge destroyed");
        try {
            unregisterReceiver(mediaButtonReceiver);
        } catch (Exception ignored) {
        }
        unregisterCallbacks();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // -------------------------------------------------------------------------
    // Session monitoring
    // -------------------------------------------------------------------------

    private void monitorActiveSessions() {
        if (mediaSessionManager == null) return;

        List<MediaController> controllers = mediaSessionManager.getActiveSessions(null);
        registerCallbacks(controllers);
    }

    private synchronized void registerCallbacks(List<MediaController> controllers) {
        unregisterCallbacks();
        for (MediaController controller : controllers) {
            Log.d(TAG, "Monitoring media session from: " + controller.getPackageName());
            MediaController.Callback callback = createCallback(controller);
            controller.registerCallback(callback, mainHandler);
            activeControllers.add(controller);

            PlaybackState state = controller.getPlaybackState();
            if (state != null && state.getState() == PlaybackState.STATE_PLAYING) {
                setActiveController(controller);
            }
        }

        if (activeMediaController == null && !controllers.isEmpty()) {
            setActiveController(controllers.get(0));
        }

        if (activeMediaController != null) {
            pushMediaState(activeMediaController);
        }
    }

    private void unregisterCallbacks() {
        activeControllers.clear();
        activeMediaController = null;
    }

    private MediaController.Callback createCallback(final MediaController controller) {
        return new MediaController.Callback() {
            @Override
            public void onSessionDestroyed() {
                Log.d(TAG, "Session destroyed: " + controller.getPackageName());
                if (controller == activeMediaController) {
                    pickNextActiveSession(controller);
                }
                activeControllers.remove(controller);
            }

            @Override
            public void onPlaybackStateChanged(PlaybackState state) {
                if (state == null) return;
                if (state.getState() == PlaybackState.STATE_PLAYING) {
                    setActiveController(controller);
                }
                if (controller == activeMediaController) {
                    pushMediaState(controller);
                }
            }

            @Override
            public void onMetadataChanged(MediaMetadata metadata) {
                if (metadata == null) return;
                if (activeMediaController == null) {
                    setActiveController(controller);
                }
                pushMediaState(controller);
            }

            public void onSessionReady() {
                if (activeMediaController == null) {
                    setActiveController(controller);
                }
                pushMediaState(controller);
            }
        };
    }

    private void pickNextActiveSession(MediaController exclude) {
        List<MediaController> controllers = mediaSessionManager.getActiveSessions(null);
        for (MediaController c : controllers) {
            if (c != exclude && c.getPlaybackState() != null) {
                setActiveController(c);
                pushMediaState(c);
                return;
            }
        }
        activeMediaController = null;
    }

    private synchronized void setActiveController(MediaController controller) {
        if (controller != activeMediaController) {
            Log.d(TAG, "Active media controller: " + controller.getPackageName());
            activeMediaController = controller;
        }
    }

    // -------------------------------------------------------------------------
    // Push media state to data layer (phone -> watch)
    // -------------------------------------------------------------------------

    private void pushMediaState(MediaController controller) {
        if (controller == null || wearable == null) return;

        try {
            // Build data item containing media state
            DataItemInternal dataItem = new DataItemInternal(
                    wearable.getLocalNodeId(), PATH_MEDIA_STATE);

            // Metadata
            MediaMetadata metadata = controller.getMetadata();
            if (metadata != null) {
                putString(dataItem, "title",
                        metadata.getString(MediaMetadata.METADATA_KEY_TITLE));
                putString(dataItem, "artist",
                        metadata.getString(MediaMetadata.METADATA_KEY_ARTIST));
                putString(dataItem, "album",
                        metadata.getString(MediaMetadata.METADATA_KEY_ALBUM));
                putString(dataItem, "albumArtist",
                        metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST));
                putLong(dataItem, "duration",
                        metadata.getLong(MediaMetadata.METADATA_KEY_DURATION));
                putInt(dataItem, "trackNumber",
                        (int) metadata.getLong(MediaMetadata.METADATA_KEY_TRACK_NUMBER));
                putInt(dataItem, "totalTrackCount",
                        (int) metadata.getLong(MediaMetadata.METADATA_KEY_NUM_TRACKS));

                // Album art
                Bitmap albumArt = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
                if (albumArt != null) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    albumArt.compress(Bitmap.CompressFormat.WEBP, 80, baos);
                    dataItem.data = baos.toByteArray();
                }
            }

            // Playback state
            PlaybackState playbackState = controller.getPlaybackState();
            if (playbackState != null) {
                int state = playbackState.getState();
                putBool(dataItem, "active", true);
                putBool(dataItem, "isPlaying", state == PlaybackState.STATE_PLAYING);
                putInt(dataItem, "playbackState", state);
                putLong(dataItem, "position", playbackState.getPosition());
                putLong(dataItem, "actions", playbackState.getActions());
            } else {
                putBool(dataItem, "active", true);
                putBool(dataItem, "isPlaying", false);
            }

            putString(dataItem, "packageName", controller.getPackageName());

            // Push via WearableImpl (internal data layer)
            DataItemRecord record = wearable.putDataItem(
                    "com.google.android.gms",
                    "media_bridge",
                    wearable.getLocalNodeId(),
                    dataItem);
            wearable.syncRecordToAll(record);

            Log.d(TAG, "Media state pushed for " + controller.getPackageName());
        } catch (Exception e) {
            Log.w(TAG, "Failed to push media state", e);
        }
    }

    // -------------------------------------------------------------------------
    // Handle control commands from the watch
    // -------------------------------------------------------------------------

    /**
     * Called by WearableServiceImpl when a message is received from the watch.
     * Route media control commands to the active media session.
     */
    public void handleMessage(String path, byte[] data) {
        if (!path.startsWith(PATH_MEDIA_CONTROL)) return;

        if (activeMediaController == null) {
            Log.w(TAG, "No active media session to control");
            return;
        }

        MediaController.TransportControls controls = activeMediaController.getTransportControls();
        if (controls == null) return;

        String command = path.substring(PATH_MEDIA_CONTROL.length());
        if (command.startsWith("/")) command = command.substring(1);

        Log.d(TAG, "Media control command: " + command);

        try {
            switch (command) {
                case "play":
                    controls.play();
                    break;
                case "pause":
                    controls.pause();
                    break;
                case "toggle":
                    PlaybackState ps = activeMediaController.getPlaybackState();
                    if (ps != null && ps.getState() == PlaybackState.STATE_PLAYING) {
                        controls.pause();
                    } else {
                        controls.play();
                    }
                    break;
                case "stop":
                    controls.stop();
                    break;
                case "next":
                    controls.skipToNext();
                    break;
                case "previous":
                    controls.skipToPrevious();
                    break;
                case "fast-forward":
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        controls.fastForward();
                    } else {
                        PlaybackState p = activeMediaController.getPlaybackState();
                        if (p != null) controls.seekTo(p.getPosition() + 15000);
                    }
                    break;
                case "rewind":
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        controls.rewind();
                    } else {
                        PlaybackState p = activeMediaController.getPlaybackState();
                        if (p != null) controls.seekTo(Math.max(0, p.getPosition() - 15000));
                    }
                    break;
                case "seek":
                    if (data != null && data.length >= 8) {
                        controls.seekTo(bytesToLong(data));
                    }
                    break;
                case "rate":
                    if (data != null && data.length >= 4) {
                        controls.setPlaybackSpeed(bytesToFloat(data));
                    }
                    break;
                default:
                    Log.w(TAG, "Unknown media control command: " + command);
            }

            pushMediaState(activeMediaController);
        } catch (Exception e) {
            Log.w(TAG, "Failed to execute media control: " + command, e);
        }
    }

    // -------------------------------------------------------------------------
    // DataItem helpers (avoid using public DataMap API)
    // -------------------------------------------------------------------------

    private static void putString(DataItemInternal item, String key, String value) {
        if (value != null && item.data == null) {
            // Store as key=value in data bytes, simple text format
            String entry = key + "=" + value + "\n";
            byte[] existing = item.data;
            if (existing == null) {
                item.data = entry.getBytes();
            } else {
                byte[] combined = new byte[existing.length + entry.getBytes().length];
                System.arraycopy(existing, 0, combined, 0, existing.length);
                System.arraycopy(entry.getBytes(), 0, combined, existing.length, entry.getBytes().length);
                item.data = combined;
            }
        }
    }

    private static void putBool(DataItemInternal item, String key, boolean value) {
        putString(item, key, Boolean.toString(value));
    }

    private static void putInt(DataItemInternal item, String key, int value) {
        putString(item, key, Integer.toString(value));
    }

    private static void putLong(DataItemInternal item, String key, long value) {
        putString(item, key, Long.toString(value));
    }

    // -------------------------------------------------------------------------
    // Media button broadcast receiver
    // -------------------------------------------------------------------------

    private final BroadcastReceiver mediaButtonReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
                monitorActiveSessions();
            }
        }
    };

    // -------------------------------------------------------------------------
    // Helper: byte[] -> primitive conversion
    // -------------------------------------------------------------------------

    private static long bytesToLong(byte[] bytes) {
        long value = 0;
        for (int i = 0; i < Math.min(8, bytes.length); i++) {
            value = (value << 8) | (bytes[i] & 0xFF);
        }
        return value;
    }

    private static float bytesToFloat(byte[] bytes) {
        return Float.intBitsToFloat(
                (bytes[0] & 0xFF) << 24 |
                (bytes[1] & 0xFF) << 16 |
                (bytes[2] & 0xFF) << 8 |
                (bytes[3] & 0xFF)
        );
    }
}
