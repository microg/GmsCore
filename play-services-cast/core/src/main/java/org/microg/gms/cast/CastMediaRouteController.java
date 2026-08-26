/**
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

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import androidx.mediarouter.media.MediaControlIntent;
import androidx.mediarouter.media.MediaRouteProvider;
import androidx.mediarouter.media.MediaRouter;

import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.common.images.WebImage;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.io.IOException;
import java.lang.Thread;
import java.lang.Runnable;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import su.litvak.chromecast.api.v2.ChromeCast;
import su.litvak.chromecast.api.v2.ChromeCasts;
import su.litvak.chromecast.api.v2.Status;
import su.litvak.chromecast.api.v2.Application;
import su.litvak.chromecast.api.v2.Media;
import su.litvak.chromecast.api.v2.ChromeCastsListener;

public class CastMediaRouteController extends MediaRouteProvider.RouteController {
    private static final String TAG = CastMediaRouteController.class.getSimpleName();

    private static final String DEFAULT_RECEIVER_APP_ID = "CC1AD845";
    private static final String DEFAULT_MEDIA_NAMESPACE = "urn:x-cast:com.google.cast.media";

    private CastMediaRouteProvider provider;
    private String routeId;
    private ChromeCast chromecast;
    private boolean connected = false;

    public CastMediaRouteController(CastMediaRouteProvider provider, String routeId, String address) {
        super();

        this.provider = provider;
        this.routeId = routeId;
        this.chromecast = new ChromeCast(address);
    }

    @Override
    public void onSelect() {
        Log.d(TAG, "onSelect: " + this.routeId);
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    chromecast.connect();
                    Status status = chromecast.getStatus();
                    connected = true;
                    Log.d(TAG, "Connected to Chromecast device, volume=" + status.volume.level
                             + ", muted=" + status.volume.muted);
                    return true;
                } catch (IOException e) {
                    Log.w(TAG, "Failed to connect to Chromecast: " + e.getMessage());
                    connected = false;
                    return false;
                }
            }

            @Override
            protected void onPostExecute(Boolean success) {
                if (success) {
                    Log.d(TAG, "Cast device selected and connected successfully");
                } else {
                    Log.e(TAG, "Failed to connect to selected cast device");
                }
            }
        }.execute();
    }

    @Override
    public void onUnselect() {
        onUnselect(0);
    }

    @Override
    public void onUnselect(int reason) {
        Log.d(TAG, "onUnselect: " + this.routeId + " reason=" + reason);
        disconnectFromDevice();
    }

    @Override
    public void onRelease() {
        Log.d(TAG, "onRelease: " + this.routeId);
        disconnectFromDevice();
    }

    private void disconnectFromDevice() {
        if (!connected) return;
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    chromecast.disconnect();
                } catch (IOException e) {
                    Log.w(TAG, "Error during disconnect: " + e.getMessage());
                }
                connected = false;
                return null;
            }
        }.execute();
    }

    @Override
    public boolean onControlRequest(Intent intent, MediaRouter.ControlRequestCallback callback) {
        Log.d(TAG, "onControlRequest: " + intent.getAction() + " for " + this.routeId);

        if (!connected) {
            Log.w(TAG, "Control request received but not connected to device");
            return false;
        }

        String action = intent.getAction();
        if (action == null) {
            return false;
        }

        switch (action) {
            case MediaControlIntent.ACTION_START_SESSION:
                return handleStartSession(intent, callback);
            case MediaControlIntent.ACTION_PLAY:
                return handlePlay(intent, callback);
            case MediaControlIntent.ACTION_PAUSE:
                return handlePause(callback);
            case MediaControlIntent.ACTION_RESUME:
                return handleResume(callback);
            case MediaControlIntent.ACTION_SEEK:
                return handleSeek(intent, callback);
            case MediaControlIntent.ACTION_STOP:
                return handleStop(callback);
            case MediaControlIntent.ACTION_GET_STATUS:
                return handleGetStatus(callback);
            case MediaControlIntent.ACTION_END_SESSION:
                return handleEndSession(callback);
            case CastMediaControlIntent.ACTION_SYNC_STATUS:
                return handleSyncStatus(callback);
            default:
                Log.d(TAG, "Unknown control request action: " + action);
                return false;
        }
    }

    private boolean handleStartSession(final Intent intent,
                                        final MediaRouter.ControlRequestCallback callback) {
        new AsyncTask<Void, Void, Boolean>() {
            String sessionId;

            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    String appId = intent.getStringExtra(CastMediaControlIntent.EXTRA_CAST_APPLICATION_ID);
                    if (appId == null || appId.isEmpty()) {
                        appId = DEFAULT_RECEIVER_APP_ID;
                    }
                    Application app = chromecast.launchApp(appId);
                    if (app != null) {
                        sessionId = app.sessionId;
                        Log.d(TAG, "Session started: " + sessionId + " app=" + app.displayName);
                        return true;
                    }
                } catch (IOException e) {
                    Log.w(TAG, "Failed to start session: " + e.getMessage());
                }
                return false;
            }

            @Override
            protected void onPostExecute(Boolean success) {
                if (callback != null) {
                    Bundle result = new Bundle();
                    if (success && sessionId != null) {
                        result.putString(MediaControlIntent.EXTRA_SESSION_ID, sessionId);
                        result.putString(MediaControlIntent.EXTRA_SESSION_STATUS_OBJECT, sessionId);
                        callback.onResult(result);
                    } else {
                        callback.onError("Failed to start session", null);
                    }
                }
            }
        }.execute();
        return true;
    }

    private boolean handlePlay(final Intent intent,
                            final MediaRouter.ControlRequestCallback callback) {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    Uri data = intent.getData();
                    if (data == null) {
                        Log.w(TAG, "Play request without data URI");
                        return false;
                    }
                    String contentId = data.toString();
                    String mimeType = intent.getType();
                    if (mimeType == null) {
                        mimeType = "video/mp4";
                    }

                    Media media = new Media.Builder(contentId, mimeType).build();
                    Application app = chromecast.getRunningApp();
                    if (app == null) {
                        app = chromecast.launchApp(DEFAULT_RECEIVER_APP_ID);
                    }
                    if (app != null) {
                        chromecast.loadMedia(contentId, mimeType, app.sessionId);
                        Log.d(TAG, "Media load requested: " + contentId);
                        return true;
                    }
                } catch (IOException e) {
                    Log.w(TAG, "Failed to play media: " + e.getMessage());
                }
                return false;
            }

            @Override
            protected void onPostExecute(Boolean success) {
                if (callback != null) {
                    if (success) {
                        callback.onResult(new Bundle());
                    } else {
                        callback.onError("Failed to play media", null);
                    }
                }
            }
        }.execute();
        return true;
    }

    private boolean handlePause(final MediaRouter.ControlRequestCallback callback) {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    chromecast.pause();
                    Log.d(TAG, "Media paused");
                    return true;
                } catch (IOException e) {
                    Log.w(TAG, "Failed to pause: " + e.getMessage());
                    return false;
                }
            }

            @Override
            protected void onPostExecute(Boolean success) {
                if (callback != null) {
                    if (success) {
                        callback.onResult(new Bundle());
                    } else {
                        callback.onError("Failed to pause", null);
                    }
                }
            }
        }.execute();
        return true;
    }

    private boolean handleResume(final MediaRouter.ControlRequestCallback callback) {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    chromecast.play();
                    Log.d(TAG, "Media resumed");
                    return true;
                } catch (IOException e) {
                    Log.w(TAG, "Failed to resume: " + e.getMessage());
                    return false;
                }
            }

            @Override
            protected void onPostExecute(Boolean success) {
                if (callback != null) {
                    if (success) {
                        callback.onResult(new Bundle());
                    } else {
                        callback.onError("Failed to resume", null);
                    }
                }
            }
        }.execute();
        return true;
    }

    private boolean handleSeek(final Intent intent,
                            final MediaRouter.ControlRequestCallback callback) {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    long position = intent.getLongExtra(MediaControlIntent.EXTRA_ITEM_POSITION, 0);
                    chromecast.seek(position);
                    Log.d(TAG, "Seek to: " + position);
                    return true;
                } catch (IOException e) {
                    Log.w(TAG, "Failed to seek: " + e.getMessage());
                    return false;
                }
            }

            @Override
            protected void onPostExecute(Boolean success) {
                if (callback != null) {
                    if (success) {
                        callback.onResult(new Bundle());
                    } else {
                        callback.onError("Failed to seek", null);
                    }
                }
            }
        }.execute();
        return true;
    }

    private boolean handleStop(final MediaRouter.ControlRequestCallback callback) {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    chromecast.stop();
                    Log.d(TAG, "Media stopped");
                    return true;
                } catch (IOException e) {
                    Log.w(TAG, "Failed to stop: " + e.getMessage());
                    return false;
                }
            }

            @Override
            protected void onPostExecute(Boolean success) {
                if (callback != null) {
                    if (success) {
                        callback.onResult(new Bundle());
                    } else {
                        callback.onError("Failed to stop", null);
                    }
                }
            }
        }.execute();
        return true;
    }

    private boolean handleGetStatus(final MediaRouter.ControlRequestCallback callback) {
        new AsyncTask<Void, Void, Bundle>() {
            @Override
            protected Bundle doInBackground(Void... params) {
                Bundle result = new Bundle();
                try {
                    Status status = chromecast.getStatus();
                    Application app = status.getRunningApp();
                    result.putString(MediaControlIntent.EXTRA_SESSION_STATUS_OBJECT,
                            app != null ? app.sessionId : null);
                    return result;
                } catch (IOException e) {
                    Log.w(TAG, "Failed to get status: " + e.getMessage());
                    return null;
                }
            }

            @Override
            protected void onPostExecute(Bundle result) {
                if (callback != null) {
                    if (result != null) {
                        callback.onResult(result);
                    } else {
                        callback.onError("Failed to get status", null);
                    }
                }
            }
        }.execute();
        return true;
    }

    private boolean handleEndSession(final MediaRouter.ControlRequestCallback callback) {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    Application app = chromecast.getRunningApp();
                    if (app != null && app.sessionId != null) {
                        chromecast.stopSession(app.sessionId);
                    }
                    return true;
                } catch (IOException e) {
                    Log.w(TAG, "Failed to end session: " + e.getMessage());
                    return false;
                }
            }

            @Override
            protected void onPostExecute(Boolean success) {
                if (callback != null) {
                    if (success) {
                        callback.onResult(new Bundle());
                    } else {
                        callback.onError("Failed to end session", null);
                    }
                }
            }
        }.execute();
        return true;
    }

    private boolean handleSyncStatus(final MediaRouter.ControlRequestCallback callback) {
        return handleGetStatus(callback);
    }

    @Override
    public void onSetVolume(int volume) {
        Log.d(TAG, "onSetVolume: " + volume);
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    chromecast.setVolume(volume / 20.0);
                } catch (IOException e) {
                    Log.w(TAG, "Failed to set volume: " + e.getMessage());
                }
                return null;
            }
        }.execute();
    }

    @Override
    public void onUpdateVolume(int delta) {
        Log.d(TAG, "onUpdateVolume: delta=" + delta);
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    Status status = chromecast.getStatus();
                    double newVolume = Math.max(0.0, Math.min(1.0,
                            status.volume.level + (delta / 20.0)));
                    chromecast.setVolume(newVolume);
                } catch (IOException e) {
                    Log.w(TAG, "Failed to update volume: " + e.getMessage());
                }
                return null;
            }
        }.execute();
    }
}