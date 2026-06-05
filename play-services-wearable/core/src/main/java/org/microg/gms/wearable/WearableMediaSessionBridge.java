/*
 * SPDX-FileCopyrightText: 2024, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.wearable;

import android.app.Service;
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
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.data.DataMap;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.ByteArrayOutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Bridges media playback info from the phone's active media sessions to connected Wear OS watches.
 * <p>
 * Monitors all active {@link android.media.session.MediaSession} instances on the device
 * and pushes metadata (track title, artist, album) and playback state (playing/paused,
 * position) to the wearable data layer. Also listens for incoming control commands from the
 * watch and forwards them to the appropriate {@link MediaController.TransportControls}.
 * <p>
 * Started automatically by {@link WearableService} when the wearable system is initialized.
 */
public class WearableMediaSessionBridge extends Service
        implements GoogleApiClient.ConnectionCallbacks,
        MessageApi.MessageListener, DataApi.DataListener {

    private static final String TAG = "GmsWearMedia";

    /** Data layer path for media state updates sent phone → watch. */
    private static final String PATH_MEDIA_STATE = "/wearable/media/state";

    /** Data layer path prefix for incoming control commands watch → phone. */
    private static final String PATH_MEDIA_CONTROL = "/wearable/media/control";

    private MediaSessionManager mediaSessionManager;
    private final Set<MediaController> activeControllers = new HashSet<>();
    private MediaController activeMediaController;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private GoogleApiClient googleApiClient;

    // -------------------------------------------------------------------------
    // Service lifecycle
    // -------------------------------------------------------------------------

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "MediaSessionBridge created");

        mediaSessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);

        // Register media button receiver to re-evaluate sessions on hardware button press
        IntentFilter filter = new IntentFilter(Intent.ACTION_MEDIA_BUTTON);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(mediaButtonReceiver, filter, RECEIVER_EXPORTED);
        } else {
            registerReceiver(mediaButtonReceiver, filter);
        }

        // Build the GoogleApiClient to communicate via Wearable Data Layer
        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .build();
        googleApiClient.connect();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Sticky service: if killed, restart automatically to keep monitoring
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "MediaSessionBridge destroyed");
        try {
            unregisterReceiver(mediaButtonReceiver);
        } catch (Exception ignored) {
        }
        unregisterCallbacks();
        if (googleApiClient != null) {
            if (googleApiClient.isConnected()) {
                Wearable.MessageApi.removeListener(googleApiClient, this);
                Wearable.DataApi.removeListener(googleApiClient, this);
                googleApiClient.disconnect();
            }
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; // Not a bindable service
    }

    // -------------------------------------------------------------------------
    // GoogleApiClient connection
    // -------------------------------------------------------------------------

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "GoogleApiClient connected, registering listeners");

        // Listen for incoming messages from the watch
        Wearable.MessageApi.addListener(googleApiClient, this);

        // Listen for data layer changes (e.g., watch requesting state refresh)
        Wearable.DataApi.addListener(googleApiClient, this);

        // Start monitoring active media sessions
        monitorActiveSessions();
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.w(TAG, "GoogleApiClient connection suspended: " + cause);
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

            // If this session is playing, promote it to active immediately
            PlaybackState state = controller.getPlaybackState();
            if (state != null && state.getState() == PlaybackState.STATE_PLAYING) {
                setActiveController(controller);
            }
        }

        // Fallback: use the most recent session if none is active
        if (activeMediaController == null && !controllers.isEmpty()) {
            setActiveController(controllers.get(0));
        }

        // Push initial state
        if (activeMediaController != null) {
            pushMediaState(activeMediaController);
        } else {
            pushEmptyState();
        }
    }

    private void unregisterCallbacks() {
        for (MediaController controller : activeControllers) {
            // MediaController.Callback is weakly referenced, it will be GC'd
        }
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

            @Override
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
        pushEmptyState();
    }

    private synchronized void setActiveController(MediaController controller) {
        if (controller != activeMediaController) {
            Log.d(TAG, "Active media controller: " + controller.getPackageName());
            activeMediaController = controller;
        }
    }

    // -------------------------------------------------------------------------
    // Push media state to data layer (phone → watch)
    // -------------------------------------------------------------------------

    private void pushEmptyState() {
        try {
            PutDataRequest request = PutDataRequest.create(PATH_MEDIA_STATE);
            request.getDataMap().putBoolean("active", false);
            Wearable.DataApi.putDataItem(googleApiClient, request).await();
        } catch (Exception e) {
            Log.w(TAG, "Failed to push empty media state", e);
        }
    }

    private void pushMediaState(MediaController controller) {
        if (controller == null || googleApiClient == null || !googleApiClient.isConnected()) {
            return;
        }

        try {
            PutDataRequest request = PutDataRequest.create(PATH_MEDIA_STATE);

            // Metadata
            MediaMetadata metadata = controller.getMetadata();
            if (metadata != null) {
                request.getDataMap().putString("title",
                        metadata.getString(MediaMetadata.METADATA_KEY_TITLE));
                request.getDataMap().putString("artist",
                        metadata.getString(MediaMetadata.METADATA_KEY_ARTIST));
                request.getDataMap().putString("album",
                        metadata.getString(MediaMetadata.METADATA_KEY_ALBUM));
                request.getDataMap().putString("albumArtist",
                        metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST));
                request.getDataMap().putLong("duration",
                        metadata.getLong(MediaMetadata.METADATA_KEY_DURATION));
                request.getDataMap().putInt("trackNumber",
                        metadata.getInt(MediaMetadata.METADATA_KEY_TRACK_NUMBER));
                request.getDataMap().putInt("totalTrackCount",
                        metadata.getInt(MediaMetadata.METADATA_KEY_NUM_TRACKS));
                request.getDataMap().putString("genre",
                        metadata.getString(MediaMetadata.METADATA_KEY_GENRE));
                request.getDataMap().putString("composer",
                        metadata.getString(MediaMetadata.METADATA_KEY_COMPOSER));

                // Album art as compressed byte array
                Bitmap albumArt = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
                if (albumArt != null) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    albumArt.compress(Bitmap.CompressFormat.WEBP, 80, baos);
                    request.getDataMap().putByteArray("albumArt", baos.toByteArray());
                }
            }

            // Playback state
            PlaybackState playbackState = controller.getPlaybackState();
            if (playbackState != null) {
                int state = playbackState.getState();
                request.getDataMap().putBoolean("active", true);
                request.getDataMap().putBoolean("isPlaying", state == PlaybackState.STATE_PLAYING);
                request.getDataMap().putInt("playbackState", state);
                request.getDataMap().putLong("position", playbackState.getPosition());
                request.getDataMap().putFloat("playbackSpeed", playbackState.getPlaybackSpeed());
                request.getDataMap().putLong("lastUpdateTime",
                        playbackState.getLastPositionUpdateTime());
                request.getDataMap().putLong("actions", playbackState.getActions());
            } else {
                request.getDataMap().putBoolean("active", true);
                request.getDataMap().putBoolean("isPlaying", false);
            }

            request.getDataMap().putString("packageName", controller.getPackageName());

            Wearable.DataApi.putDataItem(googleApiClient, request).await();
            Log.d(TAG, "Media state pushed for " + controller.getPackageName());

        } catch (Exception e) {
            Log.w(TAG, "Failed to push media state", e);
        }
    }

    // -------------------------------------------------------------------------
    // Handle incoming messages from the watch (watch → phone)
    // -------------------------------------------------------------------------

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        String path = messageEvent.getPath();
        Log.d(TAG, "Message received: " + path);

        if (!path.startsWith(PATH_MEDIA_CONTROL)) {
            return;
        }

        if (activeMediaController == null) {
            Log.w(TAG, "No active media session to control");
            return;
        }

        MediaController.TransportControls controls = activeMediaController.getTransportControls();
        if (controls == null) return;

        // Extract command from the trailing path segment
        String command = path.substring(PATH_MEDIA_CONTROL.length());
        if (command.startsWith("/")) command = command.substring(1);

        byte[] data = messageEvent.getData();
        Log.d(TAG, "Media control command: " + command);

        try {
            switch (command) {
                case "play":
                    controls.play();
                    break;
                case "pause":
                    controls.pause();
                    break;
                case "play-pause":
                case "toggle":
                    PlaybackState state = activeMediaController.getPlaybackState();
                    if (state != null && state.getState() == PlaybackState.STATE_PLAYING) {
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
                        PlaybackState ps = activeMediaController.getPlaybackState();
                        if (ps != null) controls.seekTo(ps.getPosition() + 15000);
                    }
                    break;
                case "rewind":
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        controls.rewind();
                    } else {
                        PlaybackState ps = activeMediaController.getPlaybackState();
                        if (ps != null) controls.seekTo(Math.max(0, ps.getPosition() - 15000));
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

            // Push the updated state back to the watch
            pushMediaState(activeMediaController);
        } catch (Exception e) {
            Log.w(TAG, "Failed to execute media control: " + command, e);
        }
    }

    // -------------------------------------------------------------------------
    // Handle data events (e.g., watch requests state refresh)
    // -------------------------------------------------------------------------

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                String path = event.getDataItem().getUri().getPath();
                DataMap dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();

                // Watch requests a state refresh
                if ("/wearable/media/refresh".equals(path)) {
                    if (activeMediaController != null) {
                        pushMediaState(activeMediaController);
                    } else {
                        pushEmptyState();
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Media button broadcast receiver
    // -------------------------------------------------------------------------

    private final BroadcastReceiver mediaButtonReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
                // Re-evaluate active sessions on hardware button press
                monitorActiveSessions();
            }
        }
    };

    // -------------------------------------------------------------------------
    // Helper: byte[] → primitive conversion
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
