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

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.v4.content.WakefulBroadcastReceiver;
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

import java.net.Socket;
import java.util.Arrays;
import java.util.Collections;

import javax.net.ssl.SSLContext;

import static android.os.Build.VERSION.SDK_INT;
import static org.microg.gms.gcm.Constants.MSG_CONNECT;
import static org.microg.gms.gcm.Constants.MSG_HEARTBEAT;
import static org.microg.gms.gcm.Constants.MSG_INPUT;
import static org.microg.gms.gcm.Constants.MSG_INPUT_ERROR;
import static org.microg.gms.gcm.Constants.MSG_OUTPUT;
import static org.microg.gms.gcm.Constants.MSG_OUTPUT_ERROR;
import static org.microg.gms.gcm.Constants.MSG_OUTPUT_READY;
import static org.microg.gms.gcm.Constants.MSG_TEARDOWN;

public class McsService extends IntentService implements Handler.Callback {
    private static final String TAG = "GmsGcmMcsSvc";

    public static String ACTION_CONNECT = "org.microg.gms.gcm.mcs.CONNECT";
    public static String ACTION_HEARTBEAT = "org.microg.gms.gcm.mcs.HEARTBEAT";

    public static final String PREFERENCES_NAME = "mcs";
    public static final String PREF_LAST_PERSISTENT_ID = "last_persistent_id";

    public static final String SELF_CATEGORY = "com.google.android.gsf.gtalkservice";
    public static final String IDLE_NOTIFICATION = "IdleNotification";
    public static final String FROM_FIELD = "gcm@android.com";

    public static final String SERVICE_HOST = "mtalk.google.com";
    public static final int SERVICE_PORT = 5228;

    public static final int HEARTBEAT_MS = 60000;

    private static Socket sslSocket;
    private static McsInputStream inputStream;
    private static McsOutputStream outputStream;

    private PendingIntent heartbeatIntent;

    private static MainThread mainThread;
    private static Handler mainHandler;

    private AlarmManager alarmManager;
    private PowerManager powerManager;
    private static PowerManager.WakeLock wakeLock;

    private static int delay = 0;

    private Intent connectIntent;

    public McsService() {
        super(TAG);
    }

    private class MainThread extends Thread {
        @Override
        public void run() {
            Looper.prepare();
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "mcs");
            wakeLock.setReferenceCounted(false);
            synchronized (McsService.class) {
                mainHandler = new Handler(Looper.myLooper(), McsService.this);
                if (connectIntent != null) {
                    mainHandler.dispatchMessage(mainHandler.obtainMessage(MSG_CONNECT, connectIntent));
                    WakefulBroadcastReceiver.completeWakefulIntent(connectIntent);
                }
            }
            Looper.loop();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        heartbeatIntent = PendingIntent.getService(this, 0, new Intent(ACTION_HEARTBEAT, null, this, McsService.class), 0);
        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        synchronized (McsService.class) {
            if (mainThread == null) {
                mainThread = new MainThread();
                mainThread.start();
            }
        }
    }

    public synchronized static boolean isConnected() {
        return inputStream != null && inputStream.isAlive() && outputStream != null && outputStream.isAlive();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        synchronized (McsService.class) {
            if (mainHandler != null) {
                wakeLock.acquire();
                if (ACTION_CONNECT.equals(intent.getAction())) {
                    mainHandler.dispatchMessage(mainHandler.obtainMessage(MSG_CONNECT, intent));
                } else if (ACTION_HEARTBEAT.equals(intent.getAction())) {
                    mainHandler.dispatchMessage(mainHandler.obtainMessage(MSG_HEARTBEAT, intent));
                }
                WakefulBroadcastReceiver.completeWakefulIntent(intent);
            } else if (connectIntent == null) {
                connectIntent = intent;
            } else {
                WakefulBroadcastReceiver.completeWakefulIntent(intent);
            }
        }
    }

    private synchronized void connect() {
        if (delay < 60000) delay += 5000;
        try {
            Log.d(TAG, "Starting MCS connection...");
            Socket socket = new Socket(SERVICE_HOST, SERVICE_PORT);
            Log.d(TAG, "Connected to " + SERVICE_HOST + ":" + SERVICE_PORT);
            sslSocket = SSLContext.getDefault().getSocketFactory().createSocket(socket, SERVICE_HOST, SERVICE_PORT, true);
            Log.d(TAG, "Activated SSL with " + SERVICE_HOST + ":" + SERVICE_PORT);
            inputStream = new McsInputStream(sslSocket.getInputStream(), mainHandler);
            outputStream = new McsOutputStream(sslSocket.getOutputStream(), mainHandler);
            inputStream.start();
            outputStream.start();

            alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime(), HEARTBEAT_MS, heartbeatIntent);
        } catch (Exception e) {
            Log.w(TAG, "Exception while connecting!", e);
            mainHandler.dispatchMessage(mainHandler.obtainMessage(MSG_TEARDOWN, e));
        }
    }

    private void handleClose(Close close) {
        throw new RuntimeException("Server requested close!");
    }

    private void handleLoginResponse(LoginResponse loginResponse) {
        if (loginResponse.error == null) {
            getSharedPreferences().edit().putString(PREF_LAST_PERSISTENT_ID, "").apply();
            Log.d(TAG, "Logged in");
            wakeLock.release();
        } else {
            throw new RuntimeException("Could not login: " + loginResponse.error);
        }
    }

    private void handleCloudMessage(DataMessageStanza message) {
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

    private void handleHearbeatPing(HeartbeatPing ping) {
        HeartbeatAck.Builder ack = new HeartbeatAck.Builder().status(ping.status);
        if (inputStream.newStreamIdAvailable()) {
            ack.last_stream_id_received(inputStream.getStreamId());
        }
        send(ack.build());
    }

    private void handleHeartbeatAck(HeartbeatAck ack) {
        wakeLock.release();
    }

    private LoginRequest buildLoginRequest() {
        LastCheckinInfo info = LastCheckinInfo.read(this);
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

    private void handleSelfMessage(DataMessageStanza msg) {
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
                send(msgResponse.build());
            }
        }
    }

    private SharedPreferences getSharedPreferences() {
        return getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    private void send(Message message) {
        mainHandler.dispatchMessage(mainHandler.obtainMessage(MSG_OUTPUT, message));
    }

    private void sendOutputStream(int what, Object obj) {
        McsOutputStream os = outputStream;
        if (os != null) {
            Handler outputHandler = os.getHandler();
            if (outputHandler != null)
                outputHandler.dispatchMessage(outputHandler.obtainMessage(what, obj));
        }
    }

    @Override
    public boolean handleMessage(android.os.Message msg) {
        switch (msg.what) {
            case MSG_INPUT:
                Log.d(TAG, "Incoming message: " + msg.obj);
                handleInput((Message) msg.obj);
                return true;
            case MSG_OUTPUT:
                Log.d(TAG, "Outgoing message: " + msg.obj);
                sendOutputStream(MSG_OUTPUT, msg.obj);
                return true;
            case MSG_INPUT_ERROR:
            case MSG_OUTPUT_ERROR:
                Log.d(TAG, "I/O error: " + msg.obj);
                mainHandler.dispatchMessage(mainHandler.obtainMessage(MSG_TEARDOWN, msg.obj));
                return true;
            case MSG_TEARDOWN:
                Log.d(TAG, "Teardown initiated, reason: " + msg.obj);
                handleTeardown(msg);
                return true;
            case MSG_CONNECT:
                Log.d(TAG, "Connect initiated, reason: " + msg.obj);
                if (!isConnected()) {
                    connect();
                }
                return true;
            case MSG_HEARTBEAT:
                Log.d(TAG, "Heartbeat initiated, reason: " + msg.obj);
                if (isConnected()) {
                    HeartbeatPing.Builder ping = new HeartbeatPing.Builder();
                    if (inputStream.newStreamIdAvailable()) {
                        ping.last_stream_id_received(inputStream.getStreamId());
                    }
                    send(ping.build());
                } else {
                    Log.d(TAG, "Ignoring heartbeat, not connected!");
                }
                return true;
            case MSG_OUTPUT_READY:
                Log.d(TAG, "Sending login request...");
                send(buildLoginRequest());
                return true;
        }
        Log.w(TAG, "Unknown message: " + msg);
        return false;
    }

    private void handleInput(Message message) {
        try {
            if (message instanceof DataMessageStanza) {
                handleCloudMessage((DataMessageStanza) message);
            } else if (message instanceof HeartbeatPing) {
                handleHearbeatPing((HeartbeatPing) message);
            } else if (message instanceof Close) {
                handleClose((Close) message);
            } else if (message instanceof LoginResponse) {
                handleLoginResponse((LoginResponse) message);
            } else if (message instanceof HeartbeatAck) {
                handleHeartbeatAck((HeartbeatAck) message);
            } else {
                Log.w(TAG, "Unknown message: " + message);
            }
            delay = 0;
        } catch (Exception e) {
            mainHandler.dispatchMessage(mainHandler.obtainMessage(MSG_TEARDOWN, e));
        }
    }

    private void handleTeardown(android.os.Message msg) {
        sendOutputStream(MSG_TEARDOWN, msg.obj);
        if (inputStream != null) {
            inputStream.close();
            inputStream = null;
        }
        try {
            sslSocket.close();
        } catch (Exception ignored) {
        }
        if (delay == 0) {
            sendBroadcast(new Intent("org.microg.gms.gcm.RECONNECT"), "org.microg.gms.STATUS_BROADCAST");
        } else {
            alarmManager.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + delay, PendingIntent.getBroadcast(this, 1, new Intent(this, TriggerReceiver.class), 0));
        }
        alarmManager.cancel(heartbeatIntent);
        if (wakeLock != null) {
            wakeLock.release();
        }
    }
}
