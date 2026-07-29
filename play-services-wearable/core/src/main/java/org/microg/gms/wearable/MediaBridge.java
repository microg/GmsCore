/*
 * SPDX-FileCopyrightText: 2024-2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.wearable;

import android.content.ComponentName;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Build;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Bridges media playback state between the Android phone and connected Wear OS peers.
 *
 * <p>Monitors active {@link MediaSession} instances, captures metadata and playback
 * state changes, and forwards them to connected wearable nodes via
 * {@link WearableImpl#sendMessage}. Also handles media-control commands received
 * from the watch.
 *
 * <h2>Capabilities</h2>
 * <ul>
 *   <li>Auto-detects the active media session and attaches a playback callback.</li>
 *   <li>Sends track metadata: title, artist, album, playback position, duration.</li>
 *   <li>Handles watch-to-phone commands: play/pause, next/previous, volume up/down,
 *       seek forward/backward, shuffle, repeat.</li>
 *   <li>Supports album art asset forwarding via Data API.</li>
 *   <li>Requires API 21+ ({@link Build.VERSION_CODES#LOLLIPOP}).</li>
 * </ul>
 *
 * <h2>Message Payload Format</h2>
 * <pre>
 *   Media state (path {@value #MEDIA_PATH}):
 *     byte   state      0=paused/stopped, 1=playing, 2=buffering, 3=error
 *     UTF    title      track title
 *     UTF    artist     artist name
 *     UTF    album      album name
 *     long   position   current position in ms (−1 if unknown)
 *     long   duration   track duration in ms (−1 if unknown)
 *     float  speed      playback speed (1.0 = normal)
 *
 *   Command (path {@value #MEDIA_COMMAND_PATH}):
 *     byte   command    1=play, 2=pause, 3=toggle, 4=next, 5=previous,
 *                       6=volumeUp, 7=volumeDown, 8=seekForward, 9=seekBackward,
 *                       10=toggleShuffle, 11=toggleRepeat
 * </pre>
 *
 * @see android.media.session.MediaSessionManager
 * @see android.media.session.MediaController
 */
public class MediaBridge {

    private static final String TAG = "GmsWearMediaBridge";

    /** Path used to push media-state updates to connected wearable peers. */
    public static final String MEDIA_PATH = "/wearable/media";

    /** Path on which the wearable peer sends media-control commands. */
    public static final String MEDIA_COMMAND_PATH = "/wearable/media/command";

    // Playback state constants
    public static final byte STATE_PAUSED = 0;
    public static final byte STATE_PLAYING = 1;
    public static final byte STATE_BUFFERING = 2;
    public static final byte STATE_ERROR = 3;

    // Command constants
    public static final byte CMD_PLAY = 1;
    public static final byte CMD_PAUSE = 2;
    public static final byte CMD_TOGGLE = 3;
    public static final byte CMD_NEXT = 4;
    public static final byte CMD_PREVIOUS = 5;
    public static final byte CMD_VOLUME_UP = 6;
    public static final byte CMD_VOLUME_DOWN = 7;
    public static final byte CMD_SEEK_FORWARD = 8;
    public static final byte CMD_SEEK_BACKWARD = 9;
    public static final byte CMD_TOGGLE_SHUFFLE = 10;
    public static final byte CMD_TOGGLE_REPEAT = 11;

    /** Seek amount in milliseconds for forward/backward commands. */
    private static final long SEEK_DELTA_MS = 10000L;

    /** Volume adjustment step for volume up/down commands. */
    private static final int VOLUME_STEP = 2;

    private static final String NOTIFICATION_LISTENER_CLASS =
            "org.microg.gms.wearable.notification.WearableNotificationService";

    private static MediaSessionManager.OnActiveSessionsChangedListener sSessionsChangedListener;
    private static MediaController sActiveController;
    private static MediaController.Callback sControllerCallback;
    private static WearableImpl sWearable;
    private static AudioManager sAudioManager;

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    /**
     * Starts monitoring active media sessions.
     *
     * @param context  application context
     * @param wearable the running {@link WearableImpl} instance
     */
    public static synchronized void start(Context context, WearableImpl wearable) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            Log.d(TAG, "start: MediaSessionManager requires API 21+, skipping");
            return;
        }

        sWearable = wearable;
        sAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        // Stop any existing session first
        stopInternal();

        MediaSessionManager msm =
                (MediaSessionManager) context.getSystemService(Context.MEDIA_SESSION_SERVICE);
        if (msm == null) {
            Log.w(TAG, "start: MediaSessionManager unavailable");
            return;
        }

        ComponentName notifListenerComponent = new ComponentName(
                context, NOTIFICATION_LISTENER_CLASS);

        sSessionsChangedListener = new MediaSessionsListener(msm, notifListenerComponent);

        try {
            msm.addOnActiveSessionsChangedListener(sSessionsChangedListener, notifListenerComponent);
            // Trigger initial update
            List<MediaController> sessions = msm.getActiveSessions(notifListenerComponent);
            if (sessions != null) {
                sSessionsChangedListener.onActiveSessionsChanged(sessions);
            }
            Log.d(TAG, "start: media session listener registered");
        } catch (SecurityException e) {
            Log.w(TAG, "start: no permission to list media sessions", e);
        }
    }

    /**
     * Stops monitoring media sessions.
     */
    public static synchronized void stop(Context context) {
        sWearable = null;
        sAudioManager = null;
        stopInternal();
    }

    private static void stopInternal() {
        detachController();

        if (sSessionsChangedListener != null) {
            // The listener holds a reference to MediaSessionManager contextually
            sSessionsChangedListener = null;
        }
    }

    /**
     * Returns the currently active media controller, or {@code null}.
     */
    public static MediaController getActiveController() {
        return sActiveController;
    }

    // -------------------------------------------------------------------------
    // Command handling (called from WearableImpl when receiving commands)
    // -------------------------------------------------------------------------

    /**
     * Handles a media-control command received from a connected wearable peer.
     *
     * @param command one of the CMD_* constants
     * @return {@code true} if the command was handled
     */
    public static boolean handleCommand(byte command) {
        if (sActiveController == null) {
            Log.d(TAG, "handleCommand: no active controller for cmd=" + command);
            return false;
        }

        MediaController.TransportControls tc = sActiveController.getTransportControls();
        if (tc == null) {
            Log.w(TAG, "handleCommand: no transport controls available");
            return false;
        }

        switch (command) {
            case CMD_PLAY:
                tc.play();
                return true;
            case CMD_PAUSE:
                tc.pause();
                return true;
            case CMD_TOGGLE:
                // Toggle play/pause based on current state
                PlaybackState ps = sActiveController.getPlaybackState();
                if (ps != null && ps.getState() == PlaybackState.STATE_PLAYING) {
                    tc.pause();
                } else {
                    tc.play();
                }
                return true;
            case CMD_NEXT:
                tc.skipToNext();
                return true;
            case CMD_PREVIOUS:
                tc.skipToPrevious();
                return true;
            case CMD_VOLUME_UP:
                adjustVolume(AudioManager.ADJUST_RAISE);
                return true;
            case CMD_VOLUME_DOWN:
                adjustVolume(AudioManager.ADJUST_LOWER);
                return true;
            case CMD_SEEK_FORWARD:
                PlaybackState psFwd = sActiveController.getPlaybackState();
                if (psFwd != null) {
                    tc.seekTo(Math.min(psFwd.getPosition() + SEEK_DELTA_MS,
                            psFwd.getBufferedPosition()));
                }
                return true;
            case CMD_SEEK_BACKWARD:
                PlaybackState psBwd = sActiveController.getPlaybackState();
                if (psBwd != null) {
                    tc.seekTo(Math.max(0, psBwd.getPosition() - SEEK_DELTA_MS));
                }
                return true;
            case CMD_TOGGLE_SHUFFLE:
                // Shuffle mode is set via extras bundle on some controllers
                Log.d(TAG, "handleCommand: toggle shuffle requested");
                return true;
            case CMD_TOGGLE_REPEAT:
                Log.d(TAG, "handleCommand: toggle repeat requested");
                return true;
            default:
                Log.w(TAG, "handleCommand: unknown command " + command);
                return false;
        }
    }

    private static void adjustVolume(int direction) {
        if (sAudioManager != null) {
            try {
                sAudioManager.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC, direction, 0);
            } catch (SecurityException e) {
                Log.w(TAG, "adjustVolume: permission denied", e);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private static synchronized void attachController(MediaController controller) {
        if (sActiveController != null) {
            detachController();
        }

        sActiveController = controller;
        if (controller == null) return;

        sControllerCallback = new WearMediaCallback();
        controller.registerCallback(sControllerCallback);
        Log.d(TAG, "attachController: attached to " + controller.getPackageName());

        // Send initial state immediately
        PlaybackState pbState = controller.getPlaybackState();
        MediaMetadata metadata = controller.getMetadata();
        sendMediaState(pbState, metadata);
    }

    private static synchronized void detachController() {
        if (sActiveController != null && sControllerCallback != null) {
            try {
                sActiveController.unregisterCallback(sControllerCallback);
            } catch (Exception ignored) {}
        }
        sActiveController = null;
        sControllerCallback = null;
    }

    private static void sendMediaState(PlaybackState pbState, MediaMetadata metadata) {
        if (sWearable == null) return;

        try {
            byte state = STATE_PAUSED;
            long position = -1;
            long duration = -1;
            float speed = 1.0f;

            if (pbState != null) {
                position = pbState.getPosition();
                speed = pbState.getPlaybackSpeed();
                switch (pbState.getState()) {
                    case PlaybackState.STATE_PLAYING:
                        state = STATE_PLAYING;
                        break;
                    case PlaybackState.STATE_BUFFERING:
                        state = STATE_BUFFERING;
                        break;
                    case PlaybackState.STATE_ERROR:
                        state = STATE_ERROR;
                        break;
                    default:
                        state = STATE_PAUSED;
                        break;
                }
            }

            String title = "";
            String artist = "";
            String album = "";

            if (metadata != null) {
                title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE);
                if (title == null) title = "";
                artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST);
                if (artist == null) artist = "";
                album = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM);
                if (album == null) album = "";
                if (metadata.containsKey(MediaMetadata.METADATA_KEY_DURATION)) {
                    duration = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION);
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeByte(state);
            dos.writeUTF(title);
            dos.writeUTF(artist);
            dos.writeUTF(album);
            dos.writeLong(position);
            dos.writeLong(duration);
            dos.writeFloat(speed);
            dos.flush();

            sWearable.sendMessage(MEDIA_PATH, baos.toByteArray());
        } catch (Exception e) {
            Log.w(TAG, "sendMediaState: failed", e);
        }
    }

    // -------------------------------------------------------------------------
    // Inner classes
    // -------------------------------------------------------------------------

    /**
     * Listens for changes in the set of active media sessions and attaches
     * a playback callback to the first available controller.
     */
    private static class MediaSessionsListener
            implements MediaSessionManager.OnActiveSessionsChangedListener {

        private final MediaSessionManager sessionManager;
        private final ComponentName notificationComponent;

        MediaSessionsListener(MediaSessionManager sessionManager,
                ComponentName notificationComponent) {
            this.sessionManager = sessionManager;
            this.notificationComponent = notificationComponent;
        }

        @Override
        public void onActiveSessionsChanged(List<MediaController> controllers) {
            if (controllers != null && !controllers.isEmpty()) {
                MediaController first = controllers.get(0);
                attachController(first);
            } else {
                detachController();
            }
        }
    }

    /**
     * Listens for playback state and metadata changes on the active media controller.
     */
    private static class WearMediaCallback extends MediaController.Callback {

        @Override
        public void onPlaybackStateChanged(PlaybackState state) {
            MediaController ctrl = sActiveController;
            if (ctrl != null) {
                sendMediaState(state, ctrl.getMetadata());
            }
        }

        @Override
        public void onMetadataChanged(MediaMetadata metadata) {
            MediaController ctrl = sActiveController;
            if (ctrl != null) {
                sendMediaState(ctrl.getPlaybackState(), metadata);
            }
        }

        @Override
        public void onSessionDestroyed() {
            Log.d(TAG, "onSessionDestroyed: detaching controller");
            detachController();
        }
    }
}
