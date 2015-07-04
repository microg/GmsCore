/*
 * Copyright 2013-2015 µg Project Team
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

package org.microg.gms.gcm;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import com.squareup.wire.Message;

import org.microg.gms.checkin.LastCheckinInfo;
import org.microg.gms.gcm.mcs.AppData;
import org.microg.gms.gcm.mcs.Close;
import org.microg.gms.gcm.mcs.DataMessageStanza;
import org.microg.gms.gcm.mcs.HeartbeatAck;
import org.microg.gms.gcm.mcs.HeartbeatPing;
import org.microg.gms.gcm.mcs.LoginRequest;
import org.microg.gms.gcm.mcs.LoginResponse;
import org.microg.gms.gcm.mcs.Setting;

import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.SSLContext;

import static android.os.Build.VERSION.SDK_INT;

public class McsService extends IntentService {
    private static final String TAG = "GmsGcmMcsSvc";

    public static final String PREFERENCES_NAME = "mcs";
    public static final String PREF_LAST_PERSISTENT_ID = "last_persistent_id";

    public static final String SERVICE_HOST = "mtalk.google.com";
    public static final int SERVICE_PORT = 5228;

    public static final String SELF_CATEGORY = "com.google.android.gsf.gtalkservice";
    public static final String IDLE_NOTIFICATION = "IdleNotification";
    public static final String FROM_FIELD = "gcm@android.com";

    public static final int HEARTBEAT_MS = 60000;
    public static final int HEARTBEAT_ALLOWED_OFFSET_MS = 2000;
    private static final AtomicBoolean connecting = new AtomicBoolean(false);
    private static final AtomicBoolean pending = new AtomicBoolean(false);
    private static Thread connectionThread;
    private static Thread heartbeatThread;

    private Socket sslSocket;
    private McsInputStream inputStream;
    private McsOutputStream outputStream;
    private long lastMsgTime;

    public McsService() {
        super(TAG);
    }

    public static AtomicBoolean getPending() {
        return pending;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (!isConnected()) {
            connectionThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    connect();
                }
            });
            connectionThread.start();
        } else {
            Log.d(TAG, "MCS connection already started");
        }
        pending.set(false);
    }

    public static boolean isConnected() {
        return connecting.get() || (connectionThread != null && connectionThread.isAlive());
    }

    private void heartbeatLoop() {
        try {
            while (!Thread.interrupted()) {
                try {
                    long waitTime;
                    while ((waitTime = lastMsgTime + HEARTBEAT_MS - System.currentTimeMillis()) > HEARTBEAT_ALLOWED_OFFSET_MS) {
                        synchronized (heartbeatThread) {
                            Log.d(TAG, "Waiting for " + waitTime + "ms");
                            heartbeatThread.wait(waitTime);
                        }
                    }
                    HeartbeatPing.Builder ping = new HeartbeatPing.Builder();
                    if (inputStream.newStreamIdAvailable()) {
                        ping.last_stream_id_received(inputStream.getStreamId());
                    }
                    outputStream.write(ping.build());
                    lastMsgTime = System.currentTimeMillis();
                } catch (InterruptedException ie) {
                    Log.w(TAG, ie);
                    return;
                }
            }
        } catch (Exception e) {
            Log.w(TAG, e);
            connectionThread.interrupt();
        }
        if (heartbeatThread == Thread.currentThread()) {
            heartbeatThread = null;
        }
        Log.d(TAG, "Heartbeating stopped");
    }

    private void connect() {
        connecting.set(false);
        try {
            Log.d(TAG, "Starting MCS connection...");
            LastCheckinInfo info = LastCheckinInfo.read(this);
            Socket socket = new Socket(SERVICE_HOST, SERVICE_PORT);
            Log.d(TAG, "Connected to " + SERVICE_HOST + ":" + SERVICE_PORT);
            sslSocket = SSLContext.getDefault().getSocketFactory().createSocket(socket, "mtalk.google.com", 5228, true);
            Log.d(TAG, "Activated SSL with " + SERVICE_HOST + ":" + SERVICE_PORT);
            inputStream = new McsInputStream(sslSocket.getInputStream());
            outputStream = new McsOutputStream(sslSocket.getOutputStream());
            LoginRequest loginRequest = buildLoginRequest(info);
            Log.d(TAG, "Sending login request...");
            outputStream.write(loginRequest);
            while (!Thread.interrupted()) {
                Message o = inputStream.read();
                lastMsgTime = System.currentTimeMillis();
                if (o instanceof DataMessageStanza) {
                    handleMessage((DataMessageStanza) o);
                } else if (o instanceof HeartbeatPing) {
                    handleHearbeatPing((HeartbeatPing) o);
                } else if (o instanceof Close) {
                    handleClose((Close) o);
                } else if (o instanceof LoginResponse) {
                    handleLoginresponse((LoginResponse) o);
                }
            }
            sslSocket.close();
        } catch (Exception e) {
            Log.w(TAG, e);
            try {
                sslSocket.close();
            } catch (Exception ignored) {
            }
        }
        if (heartbeatThread != null) {
            heartbeatThread.interrupt();
            heartbeatThread = null;
        }
        Log.d(TAG, "Connection closed");
        sendBroadcast(new Intent("org.microg.gms.gcm.RECONNECT"), "org.microg.gms.STATUS_BROADCAST");
    }

    private void handleClose(Close close) throws IOException {
        throw new IOException("Server requested close!");
    }

    private void handleLoginresponse(LoginResponse loginResponse) throws IOException {
        getSharedPreferences().edit().putString(PREF_LAST_PERSISTENT_ID, "").apply();
        if (loginResponse.error == null) {
            Log.d(TAG, "Logged in");
        } else {
            throw new IOException("Could not login: " + loginResponse.error);
        }
        if (heartbeatThread == null) {
            heartbeatThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    heartbeatLoop();
                }
            });
            heartbeatThread.start();
        }
    }

    private void handleMessage(DataMessageStanza message) throws IOException {
        if (message.persistent_id != null) {
            String old = getSharedPreferences().getString(PREF_LAST_PERSISTENT_ID, "");
            if (!old.isEmpty()) {
                old += "|";
            }
            getSharedPreferences().edit()
                    .putString(PREF_LAST_PERSISTENT_ID, old + message.persistent_id).apply();
        }
        if (SELF_CATEGORY.equals(message.category)) {
            handleSelfMessage(message);
        } else {
            handleAppMessage(message);
        }
    }

    private void handleHearbeatPing(HeartbeatPing ping) throws IOException {
        HeartbeatAck.Builder ack = new HeartbeatAck.Builder().status(ping.status);
        if (inputStream.newStreamIdAvailable()) {
            ack.last_stream_id_received(inputStream.getStreamId());
        }
        outputStream.write(ack.build());
    }

    private LoginRequest buildLoginRequest(LastCheckinInfo info) {
        return new LoginRequest.Builder()
                .adaptive_heartbeat(false)
                .auth_service(LoginRequest.AuthService.ANDROID_ID)
                .auth_token(Long.toString(info.securityToken))
                .id("android-" + SDK_INT)
                .domain("mcs.android.com")
                .device_id("android-" + Long.toHexString(info.androidId))
                .network_type(1)
                .resource(Long.toString(info.androidId))
                .user(Long.toString(info.androidId))
                .use_rmq2(true)
                .setting(Collections.singletonList(new Setting("new_vc", "1")))
                .received_persistent_id(Arrays.asList(getSharedPreferences().getString(PREF_LAST_PERSISTENT_ID, "").split("\\|")))
                .build();
    }

    private void handleAppMessage(DataMessageStanza msg) {
        Intent intent = new Intent();
        intent.setAction("com.google.android.c2dm.intent.RECEIVE");
        intent.addCategory(msg.category);
        for (AppData appData : msg.app_data) {
            intent.putExtra(appData.key, appData.value);
        }
        sendOrderedBroadcast(intent, msg.category + ".permission.C2D_MESSAGE");
    }

    private void handleSelfMessage(DataMessageStanza msg) throws IOException {
        for (AppData appData : msg.app_data) {
            if (IDLE_NOTIFICATION.equals(appData.key)) {
                DataMessageStanza.Builder msgResponse = new DataMessageStanza.Builder()
                        .from(FROM_FIELD)
                        .sent(System.currentTimeMillis() / 1000)
                        .ttl(0)
                        .category(SELF_CATEGORY)
                        .app_data(Collections.singletonList(new AppData(IDLE_NOTIFICATION, "false")));
                if (inputStream.newStreamIdAvailable()) {
                    msgResponse.last_stream_id_received(inputStream.getStreamId());
                }
                outputStream.write(msgResponse.build());
            }
        }
    }

    private SharedPreferences getSharedPreferences() {
        return getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
    }
}
