/*
 * Copyright (C) 2013-2017 microG Project Team
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

package org.microg.gms.cast;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.mediarouter.media.MediaControlIntent;
import androidx.mediarouter.media.MediaItemStatus;
import androidx.mediarouter.media.MediaRouteProvider;
import androidx.mediarouter.media.MediaRouter;
import androidx.mediarouter.media.MediaSessionStatus;

import com.google.android.gms.cast.CastMediaControlIntent;

import org.microg.gms.cast.channel.CastChannel;
import org.microg.gms.cast.channel.CastDeviceSession;
import org.microg.gms.cast.channel.ReceiverApplication;
import org.microg.gms.cast.channel.ReceiverStatus;

public class CastMediaRouteController extends MediaRouteProvider.RouteController {
    private static final String TAG = CastMediaRouteController.class.getSimpleName();

    private final CastMediaRouteProvider provider;
    private final String routeId;
    private final String host;
    private final int port;

    // Status replies to an older SET_VOLUME arrive after newer local changes; ignore them for this long.
    private static final long LOCAL_VOLUME_HOLD_MS = 1000;

    // App id of the Default Media Receiver, which implements the media namespace.
    private static final String DEFAULT_MEDIA_RECEIVER_APP_ID = "CC1AD845";
    private static final String MEDIA_NAMESPACE = "urn:x-cast:com.google.cast.media";
    // MediaRouter permits a null callback when the caller does not need a result.
    private static final MediaRouter.ControlRequestCallback NO_RESULT_CALLBACK = new MediaRouter.ControlRequestCallback() {};

    private volatile int volume;
    private volatile boolean volumeKnown;

    // Remote playback state for control requests. The mediaSessionId and last reported
    // status are learned from MEDIA_STATUS messages on the media namespace.
    private volatile long mediaSessionId;
    // MediaRouter sessions survive multiple LOADs; Cast assigns a new mediaSessionId to each item.
    private String remotePlaybackSessionId;
    private String remotePlaybackItemId;
    private Handler controlRequestHandler;
    private volatile int mediaPlaybackState = MediaItemStatus.PLAYBACK_STATE_PENDING;
    private volatile long mediaPositionMs;
    private volatile long mediaDurationMs = -1;
    // Randomize the initial request id as required by the Cast media protocol.
    private final long firstMediaRequestId = 1 + (UUID.randomUUID().getLeastSignificantBits() & 0x3fffffffL);
    private long nextMediaRequestId = firstMediaRequestId;
    private PendingPlay pendingPlay;
    private PendingSessionStart pendingSessionStart;
    private boolean stopApplicationWhenSessionEnds;
    private String remotePlaybackApplicationId = DEFAULT_MEDIA_RECEIVER_APP_ID;
    private int reconnectAttempts;
    private static final int MAX_RECONNECT_ATTEMPTS = 5;
    private final Map<Long, PendingControl> pendingControls = new HashMap<Long, PendingControl>();

    private static class PendingPlay {
        final String contentId;
        final String contentType;
        final long positionMs;
        final String sessionId;
        final boolean enqueue;
        final MediaRouter.ControlRequestCallback callback;

        PendingPlay(String contentId, String contentType, long positionMs, String sessionId,
                MediaRouter.ControlRequestCallback callback) {
            this(contentId, contentType, positionMs, sessionId, false, callback);
        }

        PendingPlay(String contentId, String contentType, long positionMs, String sessionId,
                boolean enqueue, MediaRouter.ControlRequestCallback callback) {
            this.contentId = contentId;
            this.contentType = contentType;
            this.positionMs = positionMs;
            this.sessionId = sessionId;
            this.enqueue = enqueue;
            this.callback = callback;
        }
    }

    private static class PendingSessionStart {
        final String sessionId;
        final String applicationId;
        final MediaRouter.ControlRequestCallback callback;

        PendingSessionStart(String sessionId, String applicationId, MediaRouter.ControlRequestCallback callback) {
            this.sessionId = sessionId;
            this.applicationId = applicationId;
            this.callback = callback;
        }
    }

    static class PendingControl {
        final String sessionId;
        final String itemId;
        final boolean requiresItemStatus;
        final boolean stopsPlayback;
        final long receiverMediaSessionId;
        final MediaRouter.ControlRequestCallback callback;
        Runnable timeout;

        PendingControl(String sessionId, String itemId, boolean requiresItemStatus, boolean stopsPlayback,
                long receiverMediaSessionId, MediaRouter.ControlRequestCallback callback) {
            this.sessionId = sessionId;
            this.itemId = itemId;
            this.requiresItemStatus = requiresItemStatus;
            this.stopsPlayback = stopsPlayback;
            this.receiverMediaSessionId = receiverMediaSessionId;
            this.callback = callback;
        }
    }
    private volatile int requestedVolume = -1;
    private volatile long requestedVolumeAt;
    private volatile boolean deviceMuted;
    // The control connection of the selected route, used to follow and change the device volume.
    // Guarded by this; callbacks of a replaced session are ignored. Route state changes are posted
    // to the provider while holding the lock, so they arrive in the order the session changed.
    private CastDeviceSession session;
    private boolean sessionConnected;
    private boolean selected;
    private boolean released;

    public CastMediaRouteController(CastMediaRouteProvider provider, String routeId, String address, int port, int volume) {
        super();

        this.provider = provider;
        this.routeId = routeId;
        this.host = address;
        this.port = port > 0 ? port : CastChannel.DEFAULT_PORT;
        this.volume = volume;
    }

    @Override
    public boolean onControlRequest(Intent intent, MediaRouter.ControlRequestCallback callback) {
        String action = intent.getAction();
        if (action == null || !intent.hasCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)) {
            return false;
        }
        if (callback == null) callback = NO_RESULT_CALLBACK;
        switch (action) {
            case MediaControlIntent.ACTION_PLAY:
                return onPlayRequest(intent, callback);
            case MediaControlIntent.ACTION_ENQUEUE:
                return onEnqueueRequest(intent, callback);
            case MediaControlIntent.ACTION_REMOVE:
                return onRemoveRequest(intent, callback);
            case MediaControlIntent.ACTION_START_SESSION:
                return onStartSessionRequest(intent, callback);
            case MediaControlIntent.ACTION_GET_SESSION_STATUS:
                return onGetSessionStatusRequest(intent, callback);
            case MediaControlIntent.ACTION_END_SESSION:
                return onEndSessionRequest(intent, callback);
            case MediaControlIntent.ACTION_PAUSE:
                return sendMediaCommand("PAUSE", intent, callback);
            case MediaControlIntent.ACTION_RESUME:
                return sendMediaCommand("PLAY", intent, callback);
            case MediaControlIntent.ACTION_STOP:
                return sendMediaCommand("STOP", intent, callback);
            case MediaControlIntent.ACTION_SEEK:
                if (!intent.hasExtra(MediaControlIntent.EXTRA_ITEM_CONTENT_POSITION)) {
                    callback.onError("Seek request without " + MediaControlIntent.EXTRA_ITEM_CONTENT_POSITION, null);
                    return true;
                }
                return sendMediaCommand("SEEK", intent, callback);
            case MediaControlIntent.ACTION_GET_STATUS:
                return sendMediaCommand("GET_STATUS", intent, callback);
            default:
                Log.d(TAG, "unimplemented control request " + action + " on " + this.routeId);
                return false;
        }
    }

    private boolean onPlayRequest(Intent intent, MediaRouter.ControlRequestCallback callback) {
        Uri data = intent.getData();
        if (data == null || data.toString().isEmpty()) {
            callback.onError("Play request without content Uri", null);
            return true;
        }
        String sessionId = intent.getStringExtra(MediaControlIntent.EXTRA_SESSION_ID);
        synchronized (this) {
            if (sessionId != null && !sessionId.equals(remotePlaybackSessionId)) {
                callback.onError("Unknown remote playback session", null);
                return true;
            }
        }
        if (sessionId == null) sessionId = UUID.randomUUID().toString();
        CastDeviceSession target = usableSession();
        if (target == null) {
            callback.onError("No connection to " + routeId, null);
            return true;
        }
        PendingPlay previous;
        synchronized (this) {
            previous = pendingPlay;
            pendingPlay = new PendingPlay(data.toString(), intent.getType(),
                    intent.getLongExtra(MediaControlIntent.EXTRA_ITEM_CONTENT_POSITION, 0),
                    sessionId, callback);
        }
        if (previous != null) previous.callback.onError("Replaced by a newer play request", null);
        // The media namespace is served by the receiver application, so make sure the default
        // media receiver is running before the LOAD command is sent from onApplicationConnected.
        target.launchApplication(DEFAULT_MEDIA_RECEIVER_APP_ID, false, null);
        return true;
    }

    private boolean onEnqueueRequest(Intent intent, MediaRouter.ControlRequestCallback callback) {
        Uri data = intent.getData();
        if (data == null || data.toString().isEmpty()) {
            callback.onError("Enqueue request without content Uri", null);
            return true;
        }
        String sessionId = intent.getStringExtra(MediaControlIntent.EXTRA_SESSION_ID);
        synchronized (this) {
            if (sessionId != null && remotePlaybackSessionId != null && !sessionId.equals(remotePlaybackSessionId)) {
                callback.onError("Unknown remote playback session", null);
                return true;
            }
        }
        CastDeviceSession target = usableSession();
        if (target == null) {
            callback.onError("No connection to " + routeId, null);
            return true;
        }
        long receiverMediaSessionId;
        synchronized (this) {
            if (sessionId != null && remotePlaybackSessionId != null && !sessionId.equals(remotePlaybackSessionId)) {
                callback.onError("Unknown remote playback session", null);
                return true;
            }
            if (sessionId == null) {
                sessionId = remotePlaybackSessionId != null ? remotePlaybackSessionId : UUID.randomUUID().toString();
            }
            remotePlaybackSessionId = sessionId;
            receiverMediaSessionId = mediaSessionId;
        }
        if (receiverMediaSessionId <= 0) {
            PendingPlay previous;
            synchronized (this) {
                previous = pendingPlay;
                pendingPlay = new PendingPlay(data.toString(), intent.getType(),
                        intent.getLongExtra(MediaControlIntent.EXTRA_ITEM_CONTENT_POSITION, 0),
                        sessionId, true, callback);
            }
            if (previous != null) previous.callback.onError("Replaced by a newer enqueue request", null);
            target.launchApplication(DEFAULT_MEDIA_RECEIVER_APP_ID, false, null);
            return true;
        }
        long requestId;
        synchronized (this) {
            requestId = nextMediaRequestId++;
        }
        addPendingControl(requestId, new PendingControl(sessionId, null, true, false, receiverMediaSessionId, callback));
        try {
            sendToMediaChannel(target, buildQueueInsertCommand(data.toString(), intent.getType(),
                    receiverMediaSessionId, requestId), requestId);
        } catch (JSONException e) {
            failPendingControl(requestId, e.getMessage());
        }
        return true;
    }

    private boolean onRemoveRequest(Intent intent, MediaRouter.ControlRequestCallback callback) {
        String sessionId = intent.getStringExtra(MediaControlIntent.EXTRA_SESSION_ID);
        String itemId = intent.getStringExtra(MediaControlIntent.EXTRA_ITEM_ID);
        long receiverMediaSessionId;
        int queueItemId;
        synchronized (this) {
            if (sessionId == null || !sessionId.equals(remotePlaybackSessionId)) {
                callback.onError("Unknown remote playback session", null);
                return true;
            }
            if (itemId == null) {
                callback.onError("Unknown remote playback item", null);
                return true;
            }
            receiverMediaSessionId = mediaSessionId;
            try {
                queueItemId = Integer.parseInt(itemId);
            } catch (NumberFormatException e) {
                callback.onError("Remote playback item is not a queue item", null);
                return true;
            }
        }
        if (receiverMediaSessionId <= 0) {
            callback.onError("No active media session on " + routeId, null);
            return true;
        }
        CastDeviceSession target = usableSession();
        if (target == null) {
            callback.onError("No connection to " + routeId, null);
            return true;
        }
        long requestId;
        synchronized (this) {
            requestId = nextMediaRequestId++;
        }
        addPendingControl(requestId, new PendingControl(sessionId, itemId, false, false, receiverMediaSessionId, callback));
        try {
            sendToMediaChannel(target, buildQueueRemoveCommand(receiverMediaSessionId, queueItemId, requestId), requestId);
        } catch (JSONException e) {
            failPendingControl(requestId, e.getMessage());
        }
        return true;
    }

    private boolean onStartSessionRequest(Intent intent, MediaRouter.ControlRequestCallback callback) {
        String sessionId = intent.getStringExtra(MediaControlIntent.EXTRA_SESSION_ID);
        String applicationId = intent.getStringExtra(CastMediaControlIntent.EXTRA_CAST_APPLICATION_ID);
        if (applicationId == null || applicationId.isEmpty()) applicationId = DEFAULT_MEDIA_RECEIVER_APP_ID;
        boolean stopWhenEnds = intent.getBooleanExtra(
                CastMediaControlIntent.EXTRA_CAST_STOP_APPLICATION_WHEN_SESSION_ENDS, false);
        synchronized (this) {
            if (sessionId != null && remotePlaybackSessionId != null && !sessionId.equals(remotePlaybackSessionId)) {
                callback.onError("Unknown remote playback session", null);
                return true;
            }
        }
        CastDeviceSession target = usableSession();
        if (target == null) {
            callback.onError("No connection to " + routeId, temporaryDisconnectExtras());
            return true;
        }
        PendingSessionStart previous;
        synchronized (this) {
            // A connection may have been opened while another request changed the logical session.
            // Recheck before publishing any start-session state.
            if (sessionId != null && remotePlaybackSessionId != null && !sessionId.equals(remotePlaybackSessionId)) {
                callback.onError("Unknown remote playback session", null);
                return true;
            }
            if (sessionId == null) {
                sessionId = remotePlaybackSessionId != null ? remotePlaybackSessionId : UUID.randomUUID().toString();
            }
            remotePlaybackSessionId = sessionId;
            remotePlaybackApplicationId = applicationId;
            stopApplicationWhenSessionEnds = stopWhenEnds;
            // A device transport is not a launched receiver application. Complete this request
            // only when onApplicationConnected confirms the requested application below.
            previous = pendingSessionStart;
            pendingSessionStart = new PendingSessionStart(sessionId, applicationId, callback);
        }
        if (previous != null) previous.callback.onError("Replaced by a newer start-session request", null);
        boolean relaunch = intent.getBooleanExtra(CastMediaControlIntent.EXTRA_CAST_RELAUNCH_APPLICATION, true);
        target.launchApplication(applicationId, relaunch, intent.getStringExtra(CastMediaControlIntent.EXTRA_CAST_LANGUAGE_CODE));
        return true;
    }

    private boolean onGetSessionStatusRequest(Intent intent, MediaRouter.ControlRequestCallback callback) {
        String sessionId = intent.getStringExtra(MediaControlIntent.EXTRA_SESSION_ID);
        synchronized (this) {
            if (sessionId == null || !sessionId.equals(remotePlaybackSessionId)) {
                callback.onError("Unknown remote playback session", null);
                return true;
            }
            callback.onResult(sessionStatusBundle(sessionId, MediaSessionStatus.SESSION_STATE_ACTIVE));
        }
        return true;
    }

    private boolean onEndSessionRequest(Intent intent, MediaRouter.ControlRequestCallback callback) {
        String sessionId = intent.getStringExtra(MediaControlIntent.EXTRA_SESSION_ID);
        boolean stopApp;
        String endedId;
        long receiverMediaSessionId;
        synchronized (this) {
            if (sessionId == null || !sessionId.equals(remotePlaybackSessionId)) {
                callback.onError("Unknown remote playback session", null);
                return true;
            }
            endedId = remotePlaybackSessionId;
            stopApp = stopApplicationWhenSessionEnds;
            receiverMediaSessionId = mediaSessionId;
            remotePlaybackSessionId = null;
            remotePlaybackItemId = null;
            mediaSessionId = 0;
            mediaPlaybackState = MediaItemStatus.PLAYBACK_STATE_FINISHED;
            mediaPositionMs = 0;
            mediaDurationMs = -1;
        }
        CastDeviceSession target = usableSession();
        if (receiverMediaSessionId > 0 && target != null) {
            long requestId;
            synchronized (this) {
                requestId = nextMediaRequestId++;
            }
            try {
                sendToMediaChannel(target, buildMediaCommand("STOP", requestId, receiverMediaSessionId, 0), requestId);
            } catch (JSONException e) {
                Log.d(TAG, "Failed to stop media while ending session", e);
            }
        }
        if (stopApp && target != null) {
            target.stopApplication(null);
        }
        callback.onResult(sessionStatusBundle(endedId, MediaSessionStatus.SESSION_STATE_ENDED));
        return true;
    }

    private Bundle sessionStatusBundle(String sessionId, int sessionState) {
        Bundle result = new Bundle();
        result.putString(MediaControlIntent.EXTRA_SESSION_ID, sessionId);
        result.putBundle(MediaControlIntent.EXTRA_SESSION_STATUS, new MediaSessionStatus.Builder(sessionState)
                .setQueuePaused(mediaPlaybackState == MediaItemStatus.PLAYBACK_STATE_PAUSED)
                .build().asBundle());
        return result;
    }

    static Bundle temporaryDisconnectExtras() {
        Bundle extras = new Bundle();
        extras.putInt(CastMediaControlIntent.EXTRA_ERROR_CODE, CastMediaControlIntent.ERROR_CODE_TEMPORARILY_DISCONNECTED);
        return extras;
    }

    private boolean sendMediaCommand(String type, Intent intent, MediaRouter.ControlRequestCallback callback) {
        boolean requiresItemStatus = "SEEK".equals(type) || "GET_STATUS".equals(type);
        String sessionId = intent.getStringExtra(MediaControlIntent.EXTRA_SESSION_ID);
        String itemId = intent.getStringExtra(MediaControlIntent.EXTRA_ITEM_ID);
        long receiverMediaSessionId;
        synchronized (this) {
            if (sessionId == null || !sessionId.equals(remotePlaybackSessionId)) {
                callback.onError("Unknown remote playback session", null);
                return true;
            }
            if (requiresItemStatus && (itemId == null || !itemId.equals(remotePlaybackItemId))) {
                callback.onError("Unknown remote playback item", null);
                return true;
            }
            receiverMediaSessionId = mediaSessionId;
        }
        if (receiverMediaSessionId <= 0) {
            callback.onError("No active media session on " + routeId, null);
            return true;
        }
        CastDeviceSession target = usableSession();
        if (target == null) {
            callback.onError("No connection to " + routeId, null);
            return true;
        }
        long requestId;
        synchronized (this) {
            requestId = nextMediaRequestId++;
        }
        addPendingControl(requestId, new PendingControl(sessionId, itemId, requiresItemStatus, "STOP".equals(type),
                receiverMediaSessionId, callback));
        String message;
        try {
            message = buildMediaCommand(type, requestId, receiverMediaSessionId,
                    intent.getLongExtra(MediaControlIntent.EXTRA_ITEM_CONTENT_POSITION, 0));
        } catch (JSONException e) {
            failPendingControl(requestId, e.getMessage());
            return true;
        }
        sendToMediaChannel(target, message, requestId);
        return true;
    }

    private synchronized void addPendingControl(long requestId, PendingControl pending) {
        if (controlRequestHandler == null) controlRequestHandler = new Handler(Looper.getMainLooper());
        pending.timeout = () -> failPendingControl(requestId, "Media request timed out");
        pendingControls.put(requestId, pending);
        controlRequestHandler.postDelayed(pending.timeout, CastDeviceSession.REQUEST_TIMEOUT_MILLIS);
    }

    private synchronized PendingControl takePendingControl(long requestId) {
        PendingControl pending = pendingControls.remove(requestId);
        if (pending != null && pending.timeout != null) controlRequestHandler.removeCallbacks(pending.timeout);
        return pending;
    }

    private void sendToMediaChannel(CastDeviceSession target, String message, long requestId) {
        target.registerNamespace(MEDIA_NAMESPACE);
        target.sendMessage(MEDIA_NAMESPACE, message, requestId);
    }

    /**
     * Session usable for media commands. Unlike onSetVolume, only this route's own connection is
     * used: send success, send failure and media namespace replies are delivered to the callbacks
     * of the session that sent them, so a session another client opened through the device
     * controller cannot acknowledge or answer our requests. When the route is still selected and
     * no connection exists, open a fresh control connection like onSetVolume does.
     */
    private CastDeviceSession usableSession() {
        CastDeviceSession target;
        CastDeviceSession started = null;
        synchronized (this) {
            target = session;
            if (target == null && selected && !released) target = started = startSessionLocked();
        }
        if (started != null) started.connect();
        return target;
    }

    static String buildMediaCommand(String type, long requestId, long mediaSessionId, long positionMs)
            throws JSONException {
        JSONObject command = new JSONObject();
        command.put("type", type);
        command.put("requestId", requestId);
        if (mediaSessionId > 0) command.put("mediaSessionId", mediaSessionId);
        if ("SEEK".equals(type)) command.put("currentTime", positionMs / 1000.0);
        return command.toString();
    }

    static String buildLoadCommand(String contentId, String contentType, long positionMs, long requestId)
            throws JSONException {
        JSONObject media = new JSONObject();
        media.put("contentId", contentId);
        media.put("streamType", "BUFFERED");
        if (contentType != null) media.put("contentType", contentType);
        JSONObject command = new JSONObject();
        command.put("type", "LOAD");
        command.put("requestId", requestId);
        command.put("media", media);
        command.put("autoplay", true);
        command.put("currentTime", positionMs / 1000.0);
        return command.toString();
    }

    static String buildLoadCommand(PendingPlay play, long requestId) throws JSONException {
        if (play.enqueue) {
            return buildQueueLoadCommand(play.contentId, play.contentType, play.positionMs, requestId);
        }
        return buildLoadCommand(play.contentId, play.contentType, play.positionMs, requestId);
    }

    static JSONObject buildQueueItem(String contentId, String contentType, boolean autoplay) throws JSONException {
        JSONObject media = new JSONObject();
        media.put("contentId", contentId);
        media.put("streamType", "BUFFERED");
        if (contentType != null) media.put("contentType", contentType);
        JSONObject item = new JSONObject();
        item.put("media", media);
        item.put("autoplay", autoplay);
        return item;
    }

    static String buildQueueLoadCommand(String contentId, String contentType, long positionMs, long requestId)
            throws JSONException {
        JSONObject command = new JSONObject();
        command.put("type", "QUEUE_LOAD");
        command.put("requestId", requestId);
        command.put("items", new JSONArray().put(buildQueueItem(contentId, contentType, true)));
        command.put("startIndex", 0);
        command.put("repeatMode", "REPEAT_OFF");
        command.put("currentTime", positionMs / 1000.0);
        return command.toString();
    }

    static String buildQueueInsertCommand(String contentId, String contentType, long mediaSessionId, long requestId)
            throws JSONException {
        JSONObject command = new JSONObject();
        command.put("type", "QUEUE_INSERT");
        command.put("requestId", requestId);
        command.put("mediaSessionId", mediaSessionId);
        command.put("items", new JSONArray().put(buildQueueItem(contentId, contentType, false)));
        return command.toString();
    }

    static String buildQueueRemoveCommand(long mediaSessionId, int itemId, long requestId) throws JSONException {
        JSONObject command = new JSONObject();
        command.put("type", "QUEUE_REMOVE");
        command.put("requestId", requestId);
        command.put("mediaSessionId", mediaSessionId);
        command.put("itemIds", new JSONArray().put(itemId));
        return command.toString();
    }

    /** Snapshot of one media status entry, parsed separately for testability. */
    static class MediaStatusSnapshot {
        final long mediaSessionId;
        final int playbackState;
        final long positionMs;
        final long durationMs;
        final Integer queueItemId;

        MediaStatusSnapshot(long mediaSessionId, int playbackState, long positionMs, long durationMs) {
            this(mediaSessionId, playbackState, positionMs, durationMs, null);
        }

        MediaStatusSnapshot(long mediaSessionId, int playbackState, long positionMs, long durationMs,
                Integer queueItemId) {
            this.mediaSessionId = mediaSessionId;
            this.playbackState = playbackState;
            this.positionMs = positionMs;
            this.durationMs = durationMs;
            this.queueItemId = queueItemId;
        }
    }

    static MediaStatusSnapshot parseMediaStatus(JSONObject status) {
        long sessionId = status.optLong("mediaSessionId", 0);
        int playbackState = toItemPlaybackState(status.optString("playerState"), status.optString("idleReason"));
        long positionMs = (long) (status.optDouble("currentTime", 0) * 1000);
        long durationMs = -1;
        JSONObject media = status.optJSONObject("media");
        if (media != null) {
            double duration = media.optDouble("duration", -1);
            if (duration >= 0 && !Double.isInfinite(duration)) durationMs = (long) (duration * 1000);
        }
        Integer queueItemId = null;
        if (status.has("currentItemId")) {
            queueItemId = status.optInt("currentItemId");
        } else {
            JSONArray items = status.optJSONArray("items");
            JSONObject first = items != null ? items.optJSONObject(0) : null;
            if (first != null && first.has("itemId")) queueItemId = first.optInt("itemId");
        }
        return new MediaStatusSnapshot(sessionId, playbackState, positionMs, durationMs, queueItemId);
    }

    static int toItemPlaybackState(String playerState) {
        return toItemPlaybackState(playerState, null);
    }

    static int toItemPlaybackState(String playerState, String idleReason) {
        if ("IDLE".equals(playerState)) {
            if ("ERROR".equals(idleReason)) return MediaItemStatus.PLAYBACK_STATE_ERROR;
            // Cast spells CANCELLED with two Ls; AndroidX uses CANCELED.
            // A new LOAD interrupts and cancels the previous item, not finishes it.
            if ("CANCELLED".equals(idleReason) || "INTERRUPTED".equals(idleReason)) {
                return MediaItemStatus.PLAYBACK_STATE_CANCELED;
            }
        }
        if ("PLAYING".equals(playerState)) return MediaItemStatus.PLAYBACK_STATE_PLAYING;
        if ("PAUSED".equals(playerState)) return MediaItemStatus.PLAYBACK_STATE_PAUSED;
        if ("BUFFERING".equals(playerState)) return MediaItemStatus.PLAYBACK_STATE_BUFFERING;
        if ("IDLE".equals(playerState)) return MediaItemStatus.PLAYBACK_STATE_FINISHED;
        return MediaItemStatus.PLAYBACK_STATE_PENDING;
    }

    private void flushPendingPlay(ReceiverApplication application) {
        PendingPlay play;
        synchronized (this) {
            play = pendingPlay;
            if (play == null) return;
            if (!application.getNamespaces().contains(MEDIA_NAMESPACE)
                    && !DEFAULT_MEDIA_RECEIVER_APP_ID.equals(application.getAppId())) return;
            pendingPlay = null;
        }
        CastDeviceSession target = usableSession();
        if (target == null) {
            play.callback.onError("No connection to " + routeId, null);
            return;
        }
        long requestId;
        synchronized (this) {
            requestId = nextMediaRequestId++;
        }
        addPendingControl(requestId, new PendingControl(play.sessionId, null, true, false, 0, play.callback));
        try {
            sendToMediaChannel(target, buildLoadCommand(play, requestId), requestId);
        } catch (JSONException e) {
            failPendingControl(requestId, e.getMessage());
        }
    }

    private void completePendingSessionStart(ReceiverApplication application) {
        PendingSessionStart start;
        synchronized (this) {
            start = pendingSessionStart;
            if (start == null) return;
            if (!start.applicationId.equals(application.getAppId())) return;
            pendingSessionStart = null;
            remotePlaybackSessionId = start.sessionId;
        }
        start.callback.onResult(sessionStatusBundle(start.sessionId, MediaSessionStatus.SESSION_STATE_ACTIVE));
    }

    private void reconnectSelectedRoute() {
        CastDeviceSession started = null;
        synchronized (this) {
            if (!selected || released || session != null) return;
            started = startSessionLocked();
        }
        if (started != null) started.connect();
    }

    private void failPendingPlay(String error) {
        failPendingPlay(error, null);
    }

    private void failPendingPlay(String error, Bundle extras) {
        PendingPlay play;
        PendingSessionStart start;
        synchronized (this) {
            play = pendingPlay;
            pendingPlay = null;
            start = pendingSessionStart;
            pendingSessionStart = null;
        }
        if (play != null) play.callback.onError(error, extras);
        if (start != null) start.callback.onError(error, extras);
    }

    private void failPendingControl(long requestId, String error) {
        failPendingControl(requestId, error, null);
    }

    private void failPendingControl(long requestId, String error, Bundle extras) {
        PendingControl pending = takePendingControl(requestId);
        if (pending != null) pending.callback.onError(error, extras);
    }

    private void onMediaStatusMessage(JSONObject json) {
        long requestId = json.optLong("requestId", 0);
        PendingControl pending = requestId != 0 ? takePendingControl(requestId) : null;
        // Status from another sender also carries a nonzero request id. Ignore only
        // our own completed/expired requests, while retaining other senders' updates.
        if (pending == null && requestId >= firstMediaRequestId && requestId < nextMediaRequestId) return;
        if (pending != null && pending.receiverMediaSessionId > 0
                && pending.receiverMediaSessionId != mediaSessionId
                && !(pending.stopsPlayback && mediaSessionId == 0)) {
            pending.callback.onError("Remote playback item was replaced", null);
            return;
        }
        JSONArray statuses = json.optJSONArray("status");
        JSONObject status = statuses != null ? statuses.optJSONObject(0) : null;
        MediaStatusSnapshot snapshot = status != null ? parseMediaStatus(status) : null;
        // A matching request ID must not let another item's status replace current playback.
        if (pending != null && pending.receiverMediaSessionId > 0 && snapshot != null
                && snapshot.mediaSessionId > 0 && pending.receiverMediaSessionId != snapshot.mediaSessionId) {
            pending.callback.onError("Receiver returned no matching media item", null);
            return;
        }
        if (snapshot != null && snapshot.mediaSessionId > 0) {
            synchronized (this) {
                if (mediaSessionId != snapshot.mediaSessionId) {
                    remotePlaybackItemId = null;
                    mediaDurationMs = -1;
                }
                mediaSessionId = snapshot.mediaSessionId;
                mediaPlaybackState = snapshot.playbackState;
                mediaPositionMs = snapshot.positionMs;
                if (snapshot.durationMs != -1) mediaDurationMs = snapshot.durationMs;
            }
        } else if (statuses != null && statuses.length() == 0) {
            synchronized (this) {
                mediaSessionId = 0;
                remotePlaybackItemId = null;
                mediaPlaybackState = MediaItemStatus.PLAYBACK_STATE_FINISHED;
                mediaPositionMs = 0;
                mediaDurationMs = -1;
            }
        }
        if (pending == null) return;
        if (statuses == null || (statuses.length() == 0 ? !pending.stopsPlayback
                : snapshot == null || snapshot.mediaSessionId <= 0)) {
            pending.callback.onError("Receiver returned no matching media item", null);
            return;
        }
        if (pending.stopsPlayback) {
            // STOP invalidates the receiver session even when its final reply includes an IDLE entry.
            synchronized (this) {
                mediaSessionId = 0;
                remotePlaybackItemId = null;
                mediaPlaybackState = MediaItemStatus.PLAYBACK_STATE_FINISHED;
                mediaPositionMs = 0;
                mediaDurationMs = -1;
            }
        }
        Bundle result = new Bundle();
        result.putString(MediaControlIntent.EXTRA_SESSION_ID, pending.sessionId);
        if (pending.requiresItemStatus) {
            String itemId = pending.itemId != null ? pending.itemId : String.valueOf(snapshot.mediaSessionId);
            if (pending.itemId == null) {
                synchronized (this) {
                    remotePlaybackSessionId = pending.sessionId;
                    remotePlaybackItemId = itemId;
                }
            }
            if (snapshot.queueItemId != null && pending.itemId == null) {
                itemId = String.valueOf(snapshot.queueItemId);
                synchronized (this) {
                    remotePlaybackItemId = itemId;
                }
            }
            result.putString(MediaControlIntent.EXTRA_ITEM_ID, itemId);
            result.putBundle(MediaControlIntent.EXTRA_ITEM_STATUS, new MediaItemStatus.Builder(snapshot.playbackState)
                    .setContentPosition(snapshot.positionMs).setContentDuration(mediaDurationMs).build().asBundle());
        }
        result.putBundle(MediaControlIntent.EXTRA_SESSION_STATUS, new MediaSessionStatus.Builder(
                MediaSessionStatus.SESSION_STATE_ACTIVE)
                .setQueuePaused(snapshot != null && snapshot.playbackState == MediaItemStatus.PLAYBACK_STATE_PAUSED)
                .build().asBundle());
        pending.callback.onResult(result);
    }

    @Override
    public void onSelect() {
        CastDeviceSession newSession;
        synchronized (this) {
            if (released) return;
            selected = true;
            if (session != null) return;
            newSession = startSessionLocked();
        }
        newSession.connect();
    }

    private CastDeviceSession startSessionLocked() {
        SessionCallbacks callbacks = new SessionCallbacks();
        CastDeviceSession newSession = new CastDeviceSession(host, port, callbacks);
        callbacks.session = newSession;
        session = newSession;
        sessionConnected = false;
        provider.onRouteStateChanged(CastMediaRouteController.this, routeId, MediaRouter.RouteInfo.CONNECTION_STATE_CONNECTING, -1);
        return newSession;
    }

    @Override
    public void onUnselect() {
        onUnselect(MediaRouter.UNSELECT_REASON_UNKNOWN);
    }

    @Override
    public void onUnselect(int reason) {
        closeSession();
    }

    @Override
    public void onRelease() {
        synchronized (this) {
            if (released) return;
            released = true;
        }
        closeSession();
        provider.onRouteControllerReleased(CastMediaRouteController.this, routeId);
    }

    private void closeSession() {
        CastDeviceSession oldSession;
        synchronized (this) {
            selected = false;
            oldSession = session;
            session = null;
            sessionConnected = false;
            reconnectAttempts = 0;
            mediaSessionId = 0;
            remotePlaybackSessionId = null;
            remotePlaybackItemId = null;
            if (oldSession != null) {
                provider.onRouteStateChanged(CastMediaRouteController.this, routeId, MediaRouter.RouteInfo.CONNECTION_STATE_DISCONNECTED, -1);
            }
        }
        if (oldSession != null) oldSession.disconnect();
        failPendingPlay("Route was unselected");
        List<Long> pending;
        synchronized (this) {
            pending = new ArrayList<Long>(pendingControls.keySet());
        }
        for (long requestId : pending) failPendingControl(requestId, "Route was unselected");
    }

    @Override
    public void onSetVolume(int volume) {
        int newVolume = Math.max(0, Math.min(CastMediaRouteProvider.VOLUME_MAX, volume));
        this.volume = newVolume;
        requestedVolume = newVolume;
        requestedVolumeAt = SystemClock.elapsedRealtime();
        provider.onRouteStateChanged(CastMediaRouteController.this, routeId, -1, newVolume);

        // Volume is a device setting, so any open connection to the receiver can change it. Prefer
        // the route's own connection, then one an app opened through the device controller.
        CastDeviceSession target;
        CastDeviceSession started = null;
        synchronized (this) {
            target = sessionConnected ? session : null;
        }
        if (target == null) target = CastChannelRegistry.get(routeId);
        if (target == null) {
            synchronized (this) {
                // Still connecting: the request is sent once the connection is up. If the connection
                // dropped while the route stays selected, open it again.
                target = session;
                if (target == null && selected && !released) target = started = startSessionLocked();
            }
        }
        if (started != null) started.connect();
        if (target != null) {
            target.setVolume((double) newVolume / CastMediaRouteProvider.VOLUME_MAX);
            // A level change on a muted receiver would otherwise stay inaudible.
            if (deviceMuted && newVolume > 0) target.setMute(false);
        } else {
            Log.d(TAG, "No connection to " + routeId + " to set volume");
        }
    }

    @Override
    public void onUpdateVolume(int delta) {
        if (!volumeKnown) {
            // A step relative to the placeholder volume would jump the device to an arbitrary level.
            Log.d(TAG, "Volume of " + routeId + " not known yet, ignoring step");
            return;
        }
        onSetVolume(volume + delta);
    }

    private class SessionCallbacks implements CastDeviceSession.Callbacks {
        CastDeviceSession session;

        private boolean isCurrent() {
            synchronized (CastMediaRouteController.this) {
                return CastMediaRouteController.this.session == session;
            }
        }

        private void onSessionEnded(int statusCode) {
            boolean staySelected;
            synchronized (CastMediaRouteController.this) {
                if (CastMediaRouteController.this.session != session) return;
                CastMediaRouteController.this.session = null;
                sessionConnected = false;
                staySelected = selected && !released && reconnectAttempts < MAX_RECONNECT_ATTEMPTS;
                if (!staySelected) {
                    mediaSessionId = 0;
                    remotePlaybackSessionId = null;
                    remotePlaybackItemId = null;
                    reconnectAttempts = 0;
                    provider.onRouteStateChanged(CastMediaRouteController.this, routeId, MediaRouter.RouteInfo.CONNECTION_STATE_DISCONNECTED, -1);
                } else {
                    reconnectAttempts++;
                    provider.onRouteStateChanged(CastMediaRouteController.this, routeId, MediaRouter.RouteInfo.CONNECTION_STATE_CONNECTING, -1);
                }
            }
            Log.d(TAG, "Connection to " + routeId + " ended: " + statusCode);
            Bundle extras = temporaryDisconnectExtras();
            failPendingPlay("Connection to " + routeId + " ended: " + statusCode, extras);
            List<Long> pending;
            synchronized (CastMediaRouteController.this) {
                pending = new ArrayList<Long>(pendingControls.keySet());
            }
            for (long requestId : pending) {
                failPendingControl(requestId, "Connection to " + routeId + " ended: " + statusCode, extras);
            }
            session.disconnect();
            if (staySelected) reconnectSelectedRoute();
        }

        @Override
        public void onConnected() {
            synchronized (CastMediaRouteController.this) {
                if (CastMediaRouteController.this.session != session) return;
                sessionConnected = true;
                reconnectAttempts = 0;
                provider.onRouteStateChanged(CastMediaRouteController.this, routeId, MediaRouter.RouteInfo.CONNECTION_STATE_CONNECTED, -1);
            }
        }

        @Override
        public void onConnectionFailed(int statusCode) {
            onSessionEnded(statusCode);
        }

        @Override
        public void onDisconnected(int statusCode) {
            onSessionEnded(statusCode);
        }

        @Override
        public void onDeviceStatusChanged(@NonNull ReceiverStatus status) {
            if (!isCurrent()) return;
            deviceMuted = status.getMuted();
            if (!status.getHasVolumeLevel()) return;
            // Follow volume changes made on the device or by other senders.
            int deviceVolume = (int) Math.round(status.getVolumeLevel() * CastMediaRouteProvider.VOLUME_MAX);
            volumeKnown = true;
            if (deviceVolume != requestedVolume && SystemClock.elapsedRealtime() - requestedVolumeAt < LOCAL_VOLUME_HOLD_MS) return;
            volume = deviceVolume;
            provider.onRouteStateChanged(CastMediaRouteController.this, routeId, -1, deviceVolume);
        }

        @Override
        public void onApplicationConnected(@NonNull ReceiverApplication application, boolean wasLaunched) {
            if (!isCurrent()) return;
            completePendingSessionStart(application);
            flushPendingPlay(application);
        }

        @Override
        public void onApplicationConnectionFailed(int statusCode) {
            if (isCurrent()) failPendingPlay("Application launch failed: " + statusCode);
        }

        @Override
        public void onApplicationStatusChanged(String statusText) {
        }

        @Override
        public void onApplicationDisconnected(int statusCode) {
            List<PendingControl> pending;
            synchronized (CastMediaRouteController.this) {
                if (CastMediaRouteController.this.session != session) return;
                mediaSessionId = 0;
                remotePlaybackItemId = null;
                mediaPlaybackState = MediaItemStatus.PLAYBACK_STATE_CANCELED;
                mediaPositionMs = 0;
                mediaDurationMs = -1;
                pending = new ArrayList<PendingControl>(pendingControls.values());
                pendingControls.clear();
                for (PendingControl control : pending) {
                    if (control.timeout != null) controlRequestHandler.removeCallbacks(control.timeout);
                }
            }
            // A new receiver launch can close the previous application before its own reply.
            // Preserve pending PLAY/START_SESSION and the logical session for that transition.
            for (PendingControl control : pending) {
                control.callback.onError("Receiver application disconnected: " + statusCode, null);
            }
        }

        @Override
        public void onStopApplicationResult(int statusCode) {
        }

        @Override
        public void onLeaveApplicationResult(int statusCode) {
        }

        @Override
        public void onTextMessage(@NonNull String namespace, @NonNull String message) {
            if (!isCurrent() || !MEDIA_NAMESPACE.equals(namespace)) return;
            try {
                JSONObject json = new JSONObject(message);
                String type = json.optString("type");
                if ("MEDIA_STATUS".equals(type)) {
                    onMediaStatusMessage(json);
                } else if ("INVALID_REQUEST".equals(type) || "INVALID_PLAYER_STATE".equals(type)
                        || "INVALID_MEDIA_SESSION_ID".equals(type) || "LOAD_FAILED".equals(type)
                        || "LOAD_CANCELLED".equals(type)) {
                    failPendingControl(json.optLong("requestId", 0), type);
                }
            } catch (JSONException e) {
                Log.d(TAG, "Bad media message from " + routeId, e);
            }
        }

        @Override
        public void onBinaryMessage(@NonNull String namespace, @NonNull byte[] data) {
        }

        @Override
        public void onSendMessageSuccess(@NonNull String namespace, long requestId) {
            // A socket write does not acknowledge playback. MEDIA_STATUS or the receiver's
            // error response completes the request; the timeout bounds a missing reply.
        }

        @Override
        public void onSendMessageFailure(@NonNull String namespace, long requestId, int statusCode) {
            if (!MEDIA_NAMESPACE.equals(namespace)) return;
            failPendingControl(requestId, "Send failed: " + statusCode);
        }
    }
}
