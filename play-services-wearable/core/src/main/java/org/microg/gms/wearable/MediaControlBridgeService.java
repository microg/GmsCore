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

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.List;

/**
 * Bridges media playback state and controls between phone and WearOS devices.
 * Uses the MediaSessionManager to track active media sessions and syncs
 * metadata (title, artist, album art) and playback state to the wearable.
 * Also handles transport control commands received from the watch.
 */
public class MediaControlBridgeService extends WearableListenerService {
    private static final String TAG = "GmsWearMediaBridge";

    public static final String PATH_MEDIA_STATE = "/media/state";
    public static final String PATH_MEDIA_METADATA = "/media/metadata";
    public static final String PATH_MEDIA_COMMAND = "/media/command";
    public static final String PATH_MEDIA_CAPABILITY = "/media/capability";

    public static final String COMMAND_PLAY = "play";
    public static final String COMMAND_PAUSE = "pause";
    public static final String COMMAND_NEXT = "next";
    public static final String COMMAND_PREVIOUS = "previous";
    public static final String COMMAND_STOP = "stop";
    public static final String COMMAND_SEEK = "seek";

    private static final String KEY_TITLE = "title";
    private static final String KEY_ARTIST = "artist";
    private static final String KEY_ALBUM = "album";
    private static final String KEY_DURATION = "duration";
    private static final String KEY_POSITION = "position";
    private static final String KEY_PLAYBACK_STATE = "playbackState";
    private static final String KEY_IS_PLAYING = "isPlaying";
    private static final String KEY_COMMAND = "command";
    private static final String KEY_SEEK_POSITION = "seekPosition";

    private MediaSessionManager mediaSessionManager;
    private MediaController activeController;
    private GoogleApiClient googleApiClient;
    private Handler mainHandler;

    private final MediaController.Callback controllerCallback = new MediaController.Callback() {
        @Override
        public void onPlaybackStateChanged(PlaybackState state) {
            if (state != null) {
                syncPlaybackState(state);
            }
        }

        @Override
        public void onMetadataChanged(MediaMetadata metadata) {
            if (metadata != null) {
                syncMetadata(metadata);
            }
        }

        @Override
        public void onSessionDestroyed() {
            Log.d(TAG, "Active media session destroyed");
            clearMediaState();
            unregisterController();
        }
    };

    private final MediaSessionManager.OnActiveSessionsChangedListener sessionsChangedListener =
            new MediaSessionManager.OnActiveSessionsChangedListener() {
                @Override
                public void onActiveSessionsChanged(List<MediaController> controllers) {
                    if (controllers != null && !controllers.isEmpty()) {
                        // Use the first active media controller
                        MediaController controller = controllers.get(0);
                        registerController(controller);
                    } else {
                        Log.d(TAG, "No active media sessions");
                        clearMediaState();
                        unregisterController();
                    }
                }
            };

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "MediaControlBridgeService created");
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.mediaSessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
        connectGoogleApiClient();
        registerActiveSessionsListener();
    }

    private synchronized void connectGoogleApiClient() {
        if (googleApiClient == null || !googleApiClient.isConnected()) {
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Wearable.API)
                    .build();
            googleApiClient.connect();
        }
    }

    private void registerActiveSessionsListener() {
        ComponentName componentName = new ComponentName(this, getClass());
        mediaSessionManager.addOnActiveSessionsChangedListener(sessionsChangedListener, componentName);
        // Force initial check
        sessionsChangedListener.onActiveSessionsChanged(
                mediaSessionManager.getActiveSessions(componentName));
    }

    private synchronized void registerController(MediaController controller) {
        unregisterController();
        activeController = controller;
        if (activeController != null) {
            activeController.registerCallback(controllerCallback, mainHandler);
            // Sync current state immediately
            if (activeController.getPlaybackState() != null) {
                syncPlaybackState(activeController.getPlaybackState());
            }
            if (activeController.getMetadata() != null) {
                syncMetadata(activeController.getMetadata());
            }
        }
    }

    private void unregisterController() {
        if (activeController != null) {
            activeController.unregisterCallback(controllerCallback);
            activeController = null;
        }
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (PATH_MEDIA_COMMAND.equals(messageEvent.getPath())) {
            String command = new String(messageEvent.getData());
            handleMediaCommand(command, messageEvent.getData());
        }
    }

    private void handleMediaCommand(String command, byte[] data) {
        if (activeController == null) {
            Log.w(TAG, "No active media session to handle command: " + command);
            return;
        }
        Log.d(TAG, "Handling media command: " + command);
        switch (command) {
            case COMMAND_PLAY:
                activeController.getTransportControls().play();
                break;
            case COMMAND_PAUSE:
                activeController.getTransportControls().pause();
                break;
            case COMMAND_NEXT:
                activeController.getTransportControls().skipToNext();
                break;
            case COMMAND_PREVIOUS:
                activeController.getTransportControls().skipToPrevious();
                break;
            case COMMAND_STOP:
                activeController.getTransportControls().stop();
                break;
            case COMMAND_SEEK:
                // Parse seek position from data
                try {
                    String dataStr = new String(data);
                    long pos = Long.parseLong(dataStr);
                    activeController.getTransportControls().seekTo(pos);
                } catch (NumberFormatException e) {
                    Log.w(TAG, "Invalid seek position", e);
                }
                break;
        }
    }

    private void syncPlaybackState(PlaybackState state) {
        try {
            connectGoogleApiClient();
            if (!googleApiClient.isConnected()) return;

            PutDataMapRequest putRequest = PutDataMapRequest.create(PATH_MEDIA_STATE);
            DataMap dataMap = putRequest.getDataMap();
            dataMap.putInt(KEY_PLAYBACK_STATE, state.getState());
            dataMap.putBoolean(KEY_IS_PLAYING, state.getState() == PlaybackState.STATE_PLAYING);
            dataMap.putLong(KEY_POSITION, state.getPosition());

            Wearable.DataApi.putDataItem(googleApiClient, putRequest.asPutDataRequest()).await();
            Log.d(TAG, "Synced playback state: " + state.getState());
        } catch (Exception e) {
            Log.w(TAG, "Failed to sync playback state", e);
        }
    }

    private void syncMetadata(MediaMetadata metadata) {
        try {
            connectGoogleApiClient();
            if (!googleApiClient.isConnected()) return;

            PutDataMapRequest putRequest = PutDataMapRequest.create(PATH_MEDIA_METADATA);
            DataMap dataMap = putRequest.getDataMap();
            dataMap.putString(KEY_TITLE, metadata.getString(MediaMetadata.METADATA_KEY_TITLE));
            dataMap.putString(KEY_ARTIST, metadata.getString(MediaMetadata.METADATA_KEY_ARTIST));
            dataMap.putString(KEY_ALBUM, metadata.getString(MediaMetadata.METADATA_KEY_ALBUM));
            dataMap.putLong(KEY_DURATION, metadata.getLong(MediaMetadata.METADATA_KEY_DURATION));

            Wearable.DataApi.putDataItem(googleApiClient, putRequest.asPutDataRequest()).await();
            Log.d(TAG, "Synced media metadata: " + metadata.getString(MediaMetadata.METADATA_KEY_TITLE));
        } catch (Exception e) {
            Log.w(TAG, "Failed to sync media metadata", e);
        }
    }

    private void clearMediaState() {
        try {
            connectGoogleApiClient();
            if (!googleApiClient.isConnected()) return;

            PutDataMapRequest putRequest = PutDataMapRequest.create(PATH_MEDIA_STATE);
            putRequest.getDataMap().putBoolean(KEY_IS_PLAYING, false);
            Wearable.DataApi.putDataItem(googleApiClient, putRequest.asPutDataRequest()).await();
        } catch (Exception e) {
            Log.w(TAG, "Failed to clear media state", e);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "MediaControlBridgeService destroyed");
        unregisterController();
        if (mediaSessionManager != null) {
            try {
                mediaSessionManager.removeOnActiveSessionsChangedListener(sessionsChangedListener);
            } catch (Exception e) {
                Log.w(TAG, "Error removing sessions listener", e);
            }
        }
        if (googleApiClient != null && googleApiClient.isConnected()) {
            googleApiClient.disconnect();
        }
    }
}