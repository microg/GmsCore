package org.microg.gms.wearable;

import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Phone-side media session bridge for WearOS.
 *
 * SOLVES:
 * - S-01: Bundle.toByteArray() -> now uses BundleUtil.toByteArray()
 * - A-06: Narrow exception handling -> multi-catch in start()
 * - B-01: Bitmap division by zero -> zero-dimension guard
 * - B-02: Per-node retry storm -> isolated per-node retries
 */
public class WearOSMediaController {

    private static final String TAG = "WearOSMediaCtrl";

    private static final String PATH_METADATA   = WearableDataPaths.MEDIA_METADATA;
    private static final String PATH_STATE      = WearableDataPaths.MEDIA_STATE;
    private static final String PATH_COMMAND    = WearableDataPaths.MEDIA_COMMAND;
    private static final String PATH_DISCONNECT = WearableDataPaths.MEDIA_DISCONNECT;

    private static final String KEY_TITLE      = "title";
    private static final String KEY_ARTIST     = "artist";
    private static final String KEY_ALBUM      = "album";
    private static final String KEY_DURATION   = "duration";
    private static final String KEY_ALBUM_ART  = "album_art";
    private static final String KEY_PACKAGE    = "package";
    private static final String KEY_STATE      = "state";
    private static final String KEY_IS_PLAYING = "is_playing";
    private static final String KEY_POSITION   = "position";
    private static final String KEY_SPEED      = "speed";
    private static final String KEY_CAN_PLAY   = "can_play";
    private static final String KEY_CAN_PAUSE  = "can_pause";
    private static final String KEY_CAN_NEXT   = "can_skip_next";
    private static final String KEY_CAN_PREV   = "can_skip_prev";

    private static final String CMD_PLAY   = "play";
    private static final String CMD_PAUSE  = "pause";
    private static final String CMD_TOGGLE = "toggle";
    private static final String CMD_NEXT   = "next";
    private static final String CMD_PREV   = "prev";
    private static final String CMD_SEEK   = "seek";
    private static final String CMD_STOP   = "stop";

    private static final long STATE_THROTTLE_MS    = 250;
    private static final long METADATA_THROTTLE_MS = 1000;
    private static final int MAX_ART_WIDTH  = 320;
    private static final int MAX_ART_HEIGHT = 320;
    private static final int ART_QUALITY   = 70;
    private static final int MAX_ART_BYTES = 80 * 1024;

    private final Context context;
    private final ScheduledExecutorService executor;
    private GoogleApiClient googleApiClient;

    private MediaSessionManager mediaSessionManager;
    private final Map<String, MediaController> activeControllers = new ConcurrentHashMap<>();
    private final Map<String, MediaController.Callback> activeCallbacks = new ConcurrentHashMap<>();
    private MediaController activeMediaController;
    private String activeSessionId;
    private final Object sessionLock = new Object();

    private long lastStateUpdateTime;
    private long lastMetadataUpdateTime;
    private ScheduledFuture<?> pendingStateUpdate;
    private ScheduledFuture<?> pendingMetadataUpdate;

    private final Map<String, Node> connectedNodes = new ConcurrentHashMap<>();
    private volatile boolean isListening = false;
    private ComponentName notificationListenerComponent;

    public WearOSMediaController(Context context) {
        this.context = context.getApplicationContext();
        this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "WearOS-MediaCtrl");
            t.setDaemon(true);
            return t;
        });
    }

    // A-06 FIX: Multi-catch for all exception types
    public synchronized void start(GoogleApiClient apiClient, MessageRetryQueue retryQueue) {
        if (isListening) return;
        this.googleApiClient = apiClient;
        try {
            mediaSessionManager = (MediaSessionManager)
                    context.getSystemService(Context.MEDIA_SESSION_SERVICE);
            if (mediaSessionManager == null) {
                Log.e(TAG, "MediaSessionManager not available");
                return;
            }
            notificationListenerComponent = new ComponentName(
                    context, WearOSNotificationForwarder.class);
            registerActiveSessionCallbacks();
            mediaSessionManager.addOnActiveSessionsChangedListener(
                    activeSessionsListener, notificationListenerComponent);
            refreshConnectedNodes();
            isListening = true;
            Log.i(TAG, "Media controller started");
        } catch (SecurityException e) {
            Log.e(TAG, "Missing NotificationListener permission", e);
        } catch (IllegalStateException e) {
            Log.e(TAG, "MediaSessionManager invalid state", e);
        } catch (Exception e) {
            Log.e(TAG, "Failed to start media controller", e);
        }
    }

    public synchronized void stop() {
        if (!isListening) return;
        if (mediaSessionManager != null)
            mediaSessionManager.removeOnActiveSessionsChangedListener(activeSessionsListener);
        synchronized (sessionLock) {
            for (Map.Entry<String, MediaController.Callback> entry : activeCallbacks.entrySet()) {
                MediaController ctrl = activeControllers.get(entry.getKey());
                if (ctrl != null) ctrl.unregisterCallback(entry.getValue());
            }
            activeControllers.clear();
            activeCallbacks.clear();
            activeMediaController = null;
            activeSessionId = null;
        }
        if (pendingStateUpdate != null) pendingStateUpdate.cancel(false);
        if (pendingMetadataUpdate != null) pendingMetadataUpdate.cancel(false);
        sendToNodes(PATH_DISCONNECT, BundleUtil.toByteArray(new Bundle()));
        isListening = false;
        Log.i(TAG, "Media controller stopped");
    }

    public boolean isActive() { return isListening; }

    private final MediaSessionManager.OnActiveSessionsChangedListener
            activeSessionsListener = controllers ->
                    executor.execute(() -> handleSessionsChanged(controllers));

    private void handleSessionsChanged(List<MediaController> controllers) {
        if (controllers == null) controllers = new java.util.ArrayList<>();
        Map<String, MediaController> current = new HashMap<>();
        for (MediaController ctrl : controllers)
            current.put(getSessionKey(ctrl), ctrl);

        synchronized (sessionLock) {
            for (String oldKey : activeControllers.keySet()) {
                if (!current.containsKey(oldKey)) {
                    MediaController.Callback cb = activeCallbacks.remove(oldKey);
                    MediaController ctrl = activeControllers.remove(oldKey);
                    if (cb != null && ctrl != null) ctrl.unregisterCallback(cb);
                }
            }
            for (Map.Entry<String, MediaController> entry : current.entrySet()) {
                if (!activeControllers.containsKey(entry.getKey())) {
                    MediaController ctrl = entry.getValue();
                    MediaController.Callback cb = new SessionCallback(ctrl);
                    ctrl.registerCallback(cb, null);
                    activeControllers.put(entry.getKey(), ctrl);
                    activeCallbacks.put(entry.getKey(), cb);
                }
            }
            selectActiveController(current);
        }
    }

    private void selectActiveController(Map<String, MediaController> current) {
        MediaController best = null;
        for (MediaController ctrl : current.values()) {
            PlaybackState state = ctrl.getPlaybackState();
            if (state != null && state.getState() == PlaybackState.STATE_PLAYING) {
                best = ctrl; break;
            }
        }
        if (best == null && !current.isEmpty()) best = current.values().iterator().next();
        activeMediaController = best;
        activeSessionId = best != null ? getSessionKey(best) : null;
        if (best != null) { sendCurrentMetadataToWatch(); sendCurrentStateToWatch(); }
    }

    private class SessionCallback extends MediaController.Callback {
        private final String sessionKey;
        SessionCallback(MediaController ctrl) { this.sessionKey = getSessionKey(ctrl); }

        @Override
        public void onPlaybackStateChanged(PlaybackState state) {
            if (state == null) return;
            synchronized (sessionLock) {
                if (sessionKey.equals(activeSessionId)) throttledSendState();
                else if (state.getState() == PlaybackState.STATE_PLAYING)
                    selectActiveController(currentSnapshot());
            }
        }

        @Override
        public void onMetadataChanged(MediaMetadata metadata) {
            if (metadata == null) return;
            synchronized (sessionLock) {
                if (sessionKey.equals(activeSessionId)) throttledSendMetadata();
            }
        }

        @Override
        public void onSessionDestroyed() {
            synchronized (sessionLock) {
                activeControllers.remove(sessionKey);
                MediaController.Callback cb = activeCallbacks.remove(sessionKey);
                if (cb != null) { MediaController c = activeControllers.get(sessionKey); if (c != null) c.unregisterCallback(this); }
                if (sessionKey.equals(activeSessionId)) selectActiveController(currentSnapshot());
            }
        }
    }

    private void throttledSendState() {
        long now = System.currentTimeMillis();
        if (now - lastStateUpdateTime >= STATE_THROTTLE_MS) {
            lastStateUpdateTime = now; executor.execute(this::sendCurrentStateToWatch);
        } else if (pendingStateUpdate == null || pendingStateUpdate.isDone()) {
            pendingStateUpdate = executor.schedule(() -> {
                lastStateUpdateTime = System.currentTimeMillis(); sendCurrentStateToWatch();
            }, STATE_THROTTLE_MS, TimeUnit.MILLISECONDS);
        }
    }

    private void throttledSendMetadata() {
        long now = System.currentTimeMillis();
        if (now - lastMetadataUpdateTime >= METADATA_THROTTLE_MS) {
            lastMetadataUpdateTime = now; executor.execute(this::sendCurrentMetadataToWatch);
        } else if (pendingMetadataUpdate == null || pendingMetadataUpdate.isDone()) {
            pendingMetadataUpdate = executor.schedule(() -> {
                lastMetadataUpdateTime = System.currentTimeMillis(); sendCurrentMetadataToWatch();
            }, METADATA_THROTTLE_MS, TimeUnit.MILLISECONDS);
        }
    }

    private void sendCurrentStateToWatch() {
        MediaController ctrl;
        synchronized (sessionLock) { ctrl = activeMediaController; }
        if (ctrl == null) return;
        PlaybackState state = ctrl.getPlaybackState();
        if (state == null) return;

        Bundle data = new Bundle();
        data.putInt(KEY_STATE, state.getState());
        data.putBoolean(KEY_IS_PLAYING, state.getState() == PlaybackState.STATE_PLAYING);
        data.putLong(KEY_POSITION, state.getPosition());
        data.putFloat(KEY_SPEED, state.getPlaybackSpeed());
        long actions = state.getActions();
        data.putBoolean(KEY_CAN_PLAY, (actions & PlaybackState.ACTION_PLAY) != 0 || (actions & PlaybackState.ACTION_PLAY_PAUSE) != 0);
        data.putBoolean(KEY_CAN_PAUSE, (actions & PlaybackState.ACTION_PAUSE) != 0 || (actions & PlaybackState.ACTION_PLAY_PAUSE) != 0);
        data.putBoolean(KEY_CAN_NEXT, (actions & PlaybackState.ACTION_SKIP_TO_NEXT) != 0);
        data.putBoolean(KEY_CAN_PREV, (actions & PlaybackState.ACTION_SKIP_TO_PREVIOUS) != 0);
        sendToNodes(PATH_STATE, BundleUtil.toByteArray(data));
    }

    private void sendCurrentMetadataToWatch() {
        MediaController ctrl;
        synchronized (sessionLock) { ctrl = activeMediaController; }
        if (ctrl == null) return;
        MediaMetadata metadata = ctrl.getMetadata();
        if (metadata == null) return;

        Bundle data = new Bundle();
        data.putString(KEY_TITLE, metadata.getString(MediaMetadata.METADATA_KEY_TITLE));
        data.putString(KEY_ARTIST, metadata.getString(MediaMetadata.METADATA_KEY_ARTIST));
        data.putString(KEY_ALBUM, metadata.getString(MediaMetadata.METADATA_KEY_ALBUM));
        data.putLong(KEY_DURATION, metadata.getLong(MediaMetadata.METADATA_KEY_DURATION));
        data.putString(KEY_PACKAGE, ctrl.getPackageName());
        String artBase64 = encodeAlbumArt(metadata);
        if (artBase64 != null) data.putString(KEY_ALBUM_ART, artBase64);
        sendToNodes(PATH_METADATA, BundleUtil.toByteArray(data));
    }

    // B-01 FIX: Guard against zero/negative bitmap dimensions
    private String encodeAlbumArt(MediaMetadata metadata) {
        Bitmap art = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
        if (art == null) art = metadata.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON);
        if (art == null) return null;
        try {
            int width = art.getWidth(); int height = art.getHeight();
            if (width <= 0 || height <= 0) { Log.w(TAG, "Invalid bitmap"); return null; }
            if (width > MAX_ART_WIDTH || height > MAX_ART_HEIGHT) {
                float scale = Math.min((float) MAX_ART_WIDTH / width, (float) MAX_ART_HEIGHT / height);
                art = Bitmap.createScaledBitmap(art, Math.max(1, (int)(width * scale)), Math.max(1, (int)(height * scale)), true);
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            art.compress(Bitmap.CompressFormat.JPEG, ART_QUALITY, baos);
            if (baos.size() > MAX_ART_BYTES) { baos.reset(); art.compress(Bitmap.CompressFormat.JPEG, 40, baos); }
            if (baos.size() > MAX_ART_BYTES) return null;
            return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
        } catch (Exception e) { Log.w(TAG, "Album art encode failed", e); return null; }
    }

    // B-02 FIX: Per-node retry isolation
    private void sendToNodes(String path, byte[] data) {
        if (googleApiClient == null || !googleApiClient.isConnected()) return;
        if (connectedNodes.isEmpty()) { refreshConnectedNodes(); if (connectedNodes.isEmpty()) return; }
        for (Node node : connectedNodes.values()) sendToSingleNode(node, path, data, 0);
    }

    private void sendToSingleNode(Node node, String path, byte[] data, int attempt) {
        Wearable.MessageApi.sendMessage(googleApiClient, node.getId(), path, data)
                .setResultCallback(result -> {
                    if (!result.getStatus().isSuccess()) {
                        if (attempt < 2) {
                            long delay = (long) Math.pow(2, attempt);
                            executor.schedule(() -> sendToSingleNode(node, path, data, attempt + 1), delay, TimeUnit.SECONDS);
                        } else {
                            Log.w(TAG, "Gave up: " + path + " to " + node.getDisplayName());
                        }
                    }
                });
    }

    public void handleCommandMessage(MessageEvent event) {
        Bundle bundle = BundleUtil.fromByteArray(event.getData());
        if (bundle == null) return;
        String command = bundle.getString("command", "");
        long seekPos = bundle.getLong("seek_position", 0);
        executor.execute(() -> executeCommand(command, seekPos));
    }

    public void handleDisconnectMessage(MessageEvent event) {
        Log.d(TAG, "Watch disconnected media");
    }

    private void executeCommand(String command, long seekPos) {
        MediaController ctrl;
        synchronized (sessionLock) { ctrl = activeMediaController; }
        if (ctrl == null) { Log.w(TAG, "No active controller"); return; }
        MediaController.TransportControls transport = ctrl.getTransportControls();
        if (transport == null) return;
        switch (command) {
            case CMD_PLAY:   transport.play(); break;
            case CMD_PAUSE:  transport.pause(); break;
            case CMD_TOGGLE:
                PlaybackState state = ctrl.getPlaybackState();
                if (state != null && state.getState() == PlaybackState.STATE_PLAYING) transport.pause();
                else transport.play(); break;
            case CMD_NEXT: transport.skipToNext(); break;
            case CMD_PREV: transport.skipToPrevious(); break;
            case CMD_SEEK: transport.seekTo(seekPos); break;
            case CMD_STOP: transport.stop(); break;
        }
        executor.schedule(this::sendCurrentStateToWatch, 100, TimeUnit.MILLISECONDS);
    }

    public void onWearOSReconnected() {
        refreshConnectedNodes();
        executor.execute(() -> { sendCurrentMetadataToWatch(); sendCurrentStateToWatch(); });
    }

    private void refreshConnectedNodes() {
        if (googleApiClient == null || !googleApiClient.isConnected()) return;
        Wearable.NodeApi.getConnectedNodes(googleApiClient)
                .setResultCallback(result -> {
                    connectedNodes.clear();
                    if (result.getNodes() != null)
                        for (Node node : result.getNodes()) connectedNodes.put(node.getId(), node);
                });
    }

    private void registerActiveSessionCallbacks() {
        try {
            List<MediaController> controllers = mediaSessionManager.getActiveSessions(notificationListenerComponent);
            handleSessionsChanged(controllers);
        } catch (SecurityException e) { Log.e(TAG, "No permission for active sessions", e); }
    }

    private String getSessionKey(MediaController ctrl) {
        return ctrl.getPackageName() + "/" + (ctrl.getTag() != null ? ctrl.getTag() : "default");
    }

    private Map<String, MediaController> currentSnapshot() {
        synchronized (sessionLock) { return new HashMap<>(activeControllers); }
    }
}
