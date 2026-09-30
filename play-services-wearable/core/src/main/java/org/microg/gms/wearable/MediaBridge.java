/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
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

import androidx.annotation.RequiresApi;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.lang.ref.WeakReference;
import java.util.List;

/**
 * Bridges media playback state between the Android phone and connected Wear OS peers.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Listens for active media-session changes via {@link MediaSessionManager}.
 *   <li>Attaches a {@link MediaController.Callback} to the first active session to receive
 *       metadata and playback-state updates.
 *   <li>Sends media-state updates to all connected watch nodes via
 *       {@link WearableImpl#sendMessage} on path {@value #MEDIA_PATH}.
 *   <li>Handles media-control commands arriving from the watch on path
 *       {@value #MEDIA_COMMAND_PATH}: play, pause, next, previous, volume up/down.
 * </ul>
 *
 * <h3>Media-state payload format (path {@value #MEDIA_PATH})</h3>
 * <pre>
 *   byte  state    0 = paused / stopped, 1 = playing
 *   UTF   title    (empty string when unavailable)
 *   UTF   artist   (empty string when unavailable)
 *   UTF   album    (empty string when unavailable)
 *   long  position current playback position in milliseconds (−1 when unknown)
 *   long  duration track duration in milliseconds (−1 when unknown)
 * </pre>
 *
 * <h3>Command payload format (path {@value #MEDIA_COMMAND_PATH})</h3>
 * <pre>
 *   byte  command  1 = play, 2 = pause, 3 = next, 4 = previous,
 *                  5 = volume up, 6 = volume down
 * </pre>
 *
 * <p>Requires API 21+ ({@link Build.VERSION_CODES#LOLLIPOP}) for
 * {@link MediaSessionManager} and {@link MediaController}.
 */
public class MediaBridge {

    private static final String TAG = "GmsWearMediaBridge";

    /** Path used to push media-state updates to connected wearable peers. */
    public static final String MEDIA_PATH = "/wearable/media";

    /** Path on which the wearable peer sends media-control commands. */
    public static final String MEDIA_COMMAND_PATH = "/wearable/media/command";

    /** Component name of the in-process {@link android.service.notification.NotificationListenerService}. */
    private static final String NOTIFICATION_LISTENER_CLASS =
            "org.microg.gms.wearable.notification.WearableNotificationService";
    private static final byte STATE_PAUSED  = 0;
    private static final byte STATE_PLAYING = 1;

    // Command constants (received from the watch)
    private static final byte CMD_PLAY        = 1;
    private static final byte CMD_PAUSE       = 2;
    private static final byte CMD_NEXT        = 3;
    private static final byte CMD_PREVIOUS    = 4;
    private static final byte CMD_VOLUME_UP   = 5;
    private static final byte CMD_VOLUME_DOWN = 6;

    private static MediaSessionManager.OnActiveSessionsChangedListener sSessionsChangedListener;
    private static MediaController sActiveController;
    private static MediaController.Callback sControllerCallback;

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    /**
     * Starts monitoring active media sessions and registering a playback callback
     * on the currently active controller.
     *
     * <p>Requires API 21+; silently exits on older versions.
     *
     * @param context  application context
     * @param wearable the running {@link WearableImpl} instance to forward updates to
     */
    public static synchronized void start(Context context, WearableImpl wearable) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            Log.d(TAG, "start: MediaSessionManager requires API 21+, skipping");
            return;
        }
        stop(context);

        MediaSessionManager msm =
                (MediaSessionManager) context.getSystemService(Context.MEDIA_SESSION_SERVICE);
        if (msm == null) {
            Log.w(TAG, "start: MediaSessionManager unavailable");
            return;
        }

        sSessionsChangedListener = new WearSessionsChangedListener(context, wearable);

        // The NotificationListenerService component name is required to call
        // getActiveSessions().  Using the WearableNotificationService which is
        // already running in this process.
        ComponentName notifListenerComponent = new ComponentName(
                context, NOTIFICATION_LISTENER_CLASS);
        try {
            msm.addOnActiveSessionsChangedListener(sSessionsChangedListener, notifListenerComponent);
            // Trigger an initial update for the currently active session.
            List<MediaController> sessions = msm.getActiveSessions(notifListenerComponent);
            sSessionsChangedListener.onActiveSessionsChanged(sessions);
            Log.d(TAG, "start: media session listener registered");
        } catch (SecurityException e) {
            Log.w(TAG, "start: no permission to list media sessions", e);
        }
    }

    /**
     * Stops monitoring media sessions and detaches any active playback callback.
     */
    public static synchronized void stop(Context context) {
        detachController();
        if (sSessionsChangedListener == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            MediaSessionManager msm =
                    (MediaSessionManager) context.getSystemService(Context.MEDIA_SESSION_SERVICE);
            if (msm != null) {
                try {
                    msm.removeOnActiveSessionsChangedListener(sSessionsChangedListener);
                } catch (Exception ignored) {
                }
            }
        }
        sSessionsChangedListener = null;
        Log.d(TAG, "stop: media session listener unregistered");
    }

    // -------------------------------------------------------------------------
    // Incoming command handling
    // -------------------------------------------------------------------------

    /**
     * Dispatches a media-control command received from the watch to the currently
     * active {@link MediaController}, or adjusts system volume for volume commands.
     *
     * @param context application context
     * @param data    raw payload bytes from the watch message
     */
    public static void handleCommand(Context context, byte[] data) {
        if (data == null || data.length == 0) {
            Log.w(TAG, "handleCommand: empty payload");
            return;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            Log.d(TAG, "handleCommand: requires API 21+, ignoring");
            return;
        }
        try {
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data));
            byte command = dis.readByte();
            Log.d(TAG, "handleCommand: command=" + command);

            MediaController.TransportControls controls =
                    sActiveController != null ? sActiveController.getTransportControls() : null;

            switch (command) {
                case CMD_PLAY:
                    if (controls != null) controls.play();
                    break;
                case CMD_PAUSE:
                    if (controls != null) controls.pause();
                    break;
                case CMD_NEXT:
                    if (controls != null) controls.skipToNext();
                    break;
                case CMD_PREVIOUS:
                    if (controls != null) controls.skipToPrevious();
                    break;
                case CMD_VOLUME_UP:
                    adjustVolume(context, AudioManager.ADJUST_RAISE);
                    break;
                case CMD_VOLUME_DOWN:
                    adjustVolume(context, AudioManager.ADJUST_LOWER);
                    break;
                default:
                    Log.w(TAG, "handleCommand: unknown command=" + command);
            }
        } catch (Exception e) {
            Log.e(TAG, "handleCommand: failed to parse payload", e);
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private static void adjustVolume(Context context, int direction) {
        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (am != null) {
            am.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, 0);
        }
    }

    private static synchronized void detachController() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return;
        if (sActiveController != null && sControllerCallback != null) {
            sActiveController.unregisterCallback(sControllerCallback);
        }
        sActiveController = null;
        sControllerCallback = null;
    }

    private static synchronized void attachController(
            MediaController controller, Context context, WearableImpl wearable) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return;
        detachController();
        if (controller == null) return;
        sActiveController = controller;
        sControllerCallback = new WearControllerCallback(context, wearable);
        controller.registerCallback(sControllerCallback);
        Log.d(TAG, "attachController: " + controller.getPackageName());
        // Send the current state immediately so the watch is up-to-date.
        sendCurrentState(context, wearable, controller);
    }

    private static void sendCurrentState(
            Context context, WearableImpl wearable, MediaController controller) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return;
        byte[] payload = encodeState(controller);
        if (payload == null) return;
        for (String nodeId : wearable.getAllConnectedNodes()) {
            int result = wearable.sendMessage(context.getPackageName(), nodeId, MEDIA_PATH, payload);
            if (result < 0) {
                Log.w(TAG, "sendCurrentState: sendMessage to " + nodeId + " failed");
            }
        }
    }

    // -------------------------------------------------------------------------
    // Encoding helpers
    // -------------------------------------------------------------------------

    /**
     * Encodes the current playback state of {@code controller} into a byte array
     * to send to the watch.
     *
     * @return encoded bytes or {@code null} on error / missing state
     */
    static byte[] encodeState(MediaController controller) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return null;
        try {
            PlaybackState ps = controller.getPlaybackState();
            MediaMetadata mm = controller.getMetadata();

            byte playingByte = STATE_PAUSED;
            long position = -1L;
            if (ps != null) {
                int psState = ps.getState();
                if (psState == PlaybackState.STATE_PLAYING
                        || psState == PlaybackState.STATE_BUFFERING
                        || psState == PlaybackState.STATE_FAST_FORWARDING
                        || psState == PlaybackState.STATE_REWINDING) {
                    playingByte = STATE_PLAYING;
                }
                position = ps.getPosition();
            }

            String title  = "";
            String artist = "";
            String album  = "";
            long duration = -1L;
            if (mm != null) {
                CharSequence t = mm.getText(MediaMetadata.METADATA_KEY_TITLE);
                CharSequence ar = mm.getText(MediaMetadata.METADATA_KEY_ARTIST);
                CharSequence al = mm.getText(MediaMetadata.METADATA_KEY_ALBUM);
                if (t  != null) title  = t.toString();
                if (ar != null) artist = ar.toString();
                if (al != null) album  = al.toString();
                duration = mm.getLong(MediaMetadata.METADATA_KEY_DURATION);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeByte(playingByte);
            dos.writeUTF(title);
            dos.writeUTF(artist);
            dos.writeUTF(album);
            dos.writeLong(position);
            dos.writeLong(duration);
            dos.flush();
            return baos.toByteArray();
        } catch (Exception e) {
            Log.e(TAG, "encodeState: failed", e);
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // Inner listener classes
    // -------------------------------------------------------------------------

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private static final class WearSessionsChangedListener
            implements MediaSessionManager.OnActiveSessionsChangedListener {
        private final Context context;
        private final WeakReference<WearableImpl> wearableRef;

        WearSessionsChangedListener(Context context, WearableImpl wearable) {
            this.context = context.getApplicationContext();
            this.wearableRef = new WeakReference<>(wearable);
        }

        @Override
        public void onActiveSessionsChanged(List<MediaController> controllers) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return;
            WearableImpl wearable = wearableRef.get();
            if (wearable == null) return;

            MediaController first = (controllers != null && !controllers.isEmpty())
                    ? controllers.get(0) : null;
            Log.d(TAG, "onActiveSessionsChanged: first="
                    + (first != null ? first.getPackageName() : "none"));
            attachController(first, context, wearable);
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private static final class WearControllerCallback extends MediaController.Callback {
        private final Context context;
        private final WeakReference<WearableImpl> wearableRef;

        WearControllerCallback(Context context, WearableImpl wearable) {
            this.context = context.getApplicationContext();
            this.wearableRef = new WeakReference<>(wearable);
        }

        @Override
        public void onPlaybackStateChanged(PlaybackState state) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return;
            dispatch();
        }

        @Override
        public void onMetadataChanged(MediaMetadata metadata) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return;
            dispatch();
        }

        @Override
        public void onSessionDestroyed() {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return;
            Log.d(TAG, "onSessionDestroyed");
            WearableImpl wearable = wearableRef.get();
            if (wearable == null) return;
            // Send a "paused / stopped" empty state to the watch.
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream dos = new DataOutputStream(baos);
                dos.writeByte(STATE_PAUSED);
                dos.writeUTF("");
                dos.writeUTF("");
                dos.writeUTF("");
                dos.writeLong(-1L);
                dos.writeLong(-1L);
                dos.flush();
                byte[] payload = baos.toByteArray();
                for (String nodeId : wearable.getAllConnectedNodes()) {
                    wearable.sendMessage(context.getPackageName(), nodeId, MEDIA_PATH, payload);
                }
            } catch (Exception e) {
                Log.e(TAG, "onSessionDestroyed: failed to encode stop state", e);
            }
            detachController();
        }

        private void dispatch() {
            MediaController controller = sActiveController;
            if (controller == null) return;
            WearableImpl wearable = wearableRef.get();
            if (wearable == null) return;
            sendCurrentState(context, wearable, controller);
        }
    }
}
