/*
 * Copyright 2013-2015 microG Project Team
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
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
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

import java.io.Closeable;
import java.net.Socket;
import java.util.Collections;
import java.util.List;

import javax.net.ssl.SSLContext;

import static android.app.AlarmManager.ELAPSED_REALTIME_WAKEUP;
import static android.os.Build.VERSION.SDK_INT;
import static org.microg.gms.gcm.GcmConstants.ACTION_C2DM_RECEIVE;
import static org.microg.gms.gcm.GcmConstants.EXTRA_COLLAPSE_KEY;
import static org.microg.gms.gcm.GcmConstants.EXTRA_FROM;
import static org.microg.gms.gcm.McsConstants.ACTION_CONNECT;
import static org.microg.gms.gcm.McsConstants.ACTION_HEARTBEAT;
import static org.microg.gms.gcm.McsConstants.ACTION_RECONNECT;
import static org.microg.gms.gcm.McsConstants.EXTRA_REASON;
import static org.microg.gms.gcm.McsConstants.MCS_CLOSE_TAG;
import static org.microg.gms.gcm.McsConstants.MCS_DATA_MESSAGE_STANZA_TAG;
import static org.microg.gms.gcm.McsConstants.MCS_HEARTBEAT_ACK_TAG;
import static org.microg.gms.gcm.McsConstants.MCS_HEARTBEAT_PING_TAG;
import static org.microg.gms.gcm.McsConstants.MCS_LOGIN_REQUEST_TAG;
import static org.microg.gms.gcm.McsConstants.MCS_LOGIN_RESPONSE_TAG;
import static org.microg.gms.gcm.McsConstants.MSG_CONNECT;
import static org.microg.gms.gcm.McsConstants.MSG_HEARTBEAT;
import static org.microg.gms.gcm.McsConstants.MSG_INPUT;
import static org.microg.gms.gcm.McsConstants.MSG_INPUT_ERROR;
import static org.microg.gms.gcm.McsConstants.MSG_OUTPUT;
import static org.microg.gms.gcm.McsConstants.MSG_OUTPUT_DONE;
import static org.microg.gms.gcm.McsConstants.MSG_OUTPUT_ERROR;
import static org.microg.gms.gcm.McsConstants.MSG_OUTPUT_READY;
import static org.microg.gms.gcm.McsConstants.MSG_TEARDOWN;

public class McsService extends Service implements Handler.Callback {
    private static final String TAG = "GmsGcmMcsSvc";

    public static final String SELF_CATEGORY = "com.google.android.gsf.gtalkservice";
    public static final String IDLE_NOTIFICATION = "IdleNotification";
    public static final String FROM_FIELD = "gcm@android.com";

    public static final String SERVICE_HOST = "mtalk.google.com";
    public static final int SERVICE_PORT = 5228;

    private static final int WAKELOCK_TIMEOUT = 5000;

    private static long lastHeartbeatAckElapsedRealtime = -1;

    private static Socket sslSocket;
    private static McsInputStream inputStream;
    private static McsOutputStream outputStream;

    private PendingIntent heartbeatIntent;

    private static HandlerThread handlerThread;
    private static Handler rootHandler;

    private GcmData gcmStorage = new GcmData(this);

    private AlarmManager alarmManager;
    private PowerManager powerManager;
    private static PowerManager.WakeLock wakeLock;

    private static long currentDelay = 0;

    private Intent connectIntent;

    private class HandlerThread extends Thread {

        public HandlerThread() {
            setName("McsHandler");
        }

        @Override
        public void run() {
            Looper.prepare();
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "mcs");
            wakeLock.setReferenceCounted(false);
            synchronized (McsService.class) {
                rootHandler = new Handler(Looper.myLooper(), McsService.this);
                if (connectIntent != null) {
                    rootHandler.sendMessage(rootHandler.obtainMessage(MSG_CONNECT, connectIntent));
                    WakefulBroadcastReceiver.completeWakefulIntent(connectIntent);
                }
            }
            Looper.loop();
        }
    }

    private static void logd(String msg) {
        if (GcmPrefs.get(null).isGcmLogEnabled()) Log.d(TAG, msg);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        heartbeatIntent = PendingIntent.getService(this, 0, new Intent(ACTION_HEARTBEAT, null, this, McsService.class), 0);
        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        synchronized (McsService.class) {
            if (handlerThread == null) {
                handlerThread = new HandlerThread();
                handlerThread.start();
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public synchronized static boolean isConnected() {
        if (inputStream == null || !inputStream.isAlive() || outputStream == null || !outputStream.isAlive()) {
            logd("Connection is not enabled or dead.");
            return false;
        }
        // consider connection to be dead if we did not receive an ack within twice the heartbeat interval
        if (SystemClock.elapsedRealtime() - lastHeartbeatAckElapsedRealtime > 2 * GcmPrefs.get(null).getHeartbeatMs()) {
            logd("No heartbeat for " + (SystemClock.elapsedRealtime() - lastHeartbeatAckElapsedRealtime) / 1000 + " seconds, connection assumed to be dead after " + 2 * GcmPrefs.get(null).getHeartbeatMs() / 1000 + " seconds");
            return false;
        }
        return true;
    }

    public static void scheduleReconnect(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        long delay = getCurrentDelay();
        logd("Scheduling reconnect in " + delay / 1000 + " seconds...");
        alarmManager.set(ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + delay,
                PendingIntent.getBroadcast(context, 1, new Intent(ACTION_RECONNECT, null, context, TriggerReceiver.class), 0));
    }

    public void scheduleHeartbeat(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        logd("Scheduling heartbeat in " + GcmPrefs.get(this).getHeartbeatMs() / 1000 + " seconds...");

        int heartbeatMs = GcmPrefs.get(this).getHeartbeatMs();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            alarmManager.set(ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + heartbeatMs, heartbeatIntent);
        } else {
            // with KitKat, the alarms become inexact by default, but with the newly available setWindow we can get inexact alarms with guarantees.
            // Schedule the alarm to fire within the interval [heartbeatMs/2, heartbeatMs]
            alarmManager.setWindow(ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + heartbeatMs / 2, heartbeatMs / 2,
                    heartbeatIntent);
        }

    }

    public synchronized static long getCurrentDelay() {
        long delay = currentDelay == 0 ? 5000 : currentDelay;
        if (currentDelay < 60000) currentDelay += 10000;
        if (currentDelay >= 60000 && currentDelay < 600000) currentDelay += 60000;
        return delay;
    }

    public synchronized static void resetCurrentDelay() {
        currentDelay = 0;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        synchronized (McsService.class) {
            if (rootHandler != null) {
                if (intent == null) return START_REDELIVER_INTENT;
                wakeLock.acquire(WAKELOCK_TIMEOUT);
                Object reason = intent.hasExtra(EXTRA_REASON) ? intent.getExtras().get(EXTRA_REASON) : intent;
                if (ACTION_CONNECT.equals(intent.getAction())) {
                    rootHandler.sendMessage(rootHandler.obtainMessage(MSG_CONNECT, reason));
                } else if (ACTION_HEARTBEAT.equals(intent.getAction())) {
                    rootHandler.sendMessage(rootHandler.obtainMessage(MSG_HEARTBEAT, reason));
                }
                WakefulBroadcastReceiver.completeWakefulIntent(intent);
            } else if (connectIntent == null) {
                connectIntent = intent;
            } else {
                WakefulBroadcastReceiver.completeWakefulIntent(intent);
            }
        }
        return START_REDELIVER_INTENT;
    }

    private synchronized void connect() {
        try {
            tryClose(inputStream);
            tryClose(outputStream);
            logd("Starting MCS connection...");
            Socket socket = new Socket(SERVICE_HOST, SERVICE_PORT);
            logd("Connected to " + SERVICE_HOST + ":" + SERVICE_PORT);
            sslSocket = SSLContext.getDefault().getSocketFactory().createSocket(socket, SERVICE_HOST, SERVICE_PORT, true);
            logd("Activated SSL with " + SERVICE_HOST + ":" + SERVICE_PORT);
            inputStream = new McsInputStream(sslSocket.getInputStream(), rootHandler);
            outputStream = new McsOutputStream(sslSocket.getOutputStream(), rootHandler);
            inputStream.start();
            outputStream.start();

            lastHeartbeatAckElapsedRealtime = SystemClock.elapsedRealtime();
            scheduleHeartbeat(this);
        } catch (Exception e) {
            Log.w(TAG, "Exception while connecting!", e);
            rootHandler.sendMessage(rootHandler.obtainMessage(MSG_TEARDOWN, e));
        }
    }

    private void handleClose(Close close) {
        throw new RuntimeException("Server requested close!");
    }

    private void handleLoginResponse(LoginResponse loginResponse) {
        if (loginResponse.error == null) {
            GcmPrefs.get(this).clearLastPersistedId();
            logd("Logged in");
            wakeLock.release();
        } else {
            throw new RuntimeException("Could not login: " + loginResponse.error);
        }
    }

    private void handleCloudMessage(DataMessageStanza message) {
        if (message.persistent_id != null) {
            GcmPrefs.get(this).extendLastPersistedId(message.persistent_id);
        }
        if (SELF_CATEGORY.equals(message.category)) {
            handleSelfMessage(message);
        } else {
            handleAppMessage(message);
        }
    }

    private void handleHeartbeatPing(HeartbeatPing ping) {
        HeartbeatAck.Builder ack = new HeartbeatAck.Builder().status(ping.status);
        if (inputStream.newStreamIdAvailable()) {
            ack.last_stream_id_received(inputStream.getStreamId());
        }
        send(MCS_HEARTBEAT_ACK_TAG, ack.build());
    }

    private void handleHeartbeatAck(HeartbeatAck ack) {
        lastHeartbeatAckElapsedRealtime = SystemClock.elapsedRealtime();
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
                .received_persistent_id(GcmPrefs.get(this).getLastPersistedIds())
                .build();
    }

    private void handleAppMessage(DataMessageStanza msg) {
        Intent intent = new Intent();
        intent.setAction(ACTION_C2DM_RECEIVE);
        intent.setPackage(msg.category);
        intent.putExtra(EXTRA_FROM, msg.from);
        if (msg.token != null) intent.putExtra(EXTRA_COLLAPSE_KEY, msg.token);
        for (AppData appData : msg.app_data) {
            intent.putExtra(appData.key, appData.value);
        }
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        // FIXME: #75
        List<ResolveInfo> infos = getPackageManager().queryBroadcastReceivers(intent, PackageManager.GET_RESOLVED_FILTER);
        for (ResolveInfo resolveInfo : infos) {
            logd("Target: " + resolveInfo);
            Intent targetIntent = new Intent(intent);
            targetIntent.setComponent(new ComponentName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name));
            sendOrderedBroadcast(targetIntent, msg.category + ".permission.C2D_MESSAGE");
        }
        if (infos.isEmpty())
            logd("No target for message, wut?");

        gcmStorage.incrementAppMessageCount(msg.category, 1);
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
                send(MCS_DATA_MESSAGE_STANZA_TAG, msgResponse.build());
            }
        }
    }

    private void send(int type, Message message) {
        rootHandler.sendMessage(rootHandler.obtainMessage(MSG_OUTPUT, type, 0, message));
    }

    private void sendOutputStream(int what, int arg, Object obj) {
        McsOutputStream os = outputStream;
        if (os != null && os.isAlive()) {
            Handler outputHandler = os.getHandler();
            if (outputHandler != null)
                outputHandler.sendMessage(outputHandler.obtainMessage(what, arg, 0, obj));
        }
    }

    @Override
    public boolean handleMessage(android.os.Message msg) {
        switch (msg.what) {
            case MSG_INPUT:
                handleInput(msg.arg1, (Message) msg.obj);
                return true;
            case MSG_OUTPUT:
                sendOutputStream(MSG_OUTPUT, msg.arg1, msg.obj);
                return true;
            case MSG_INPUT_ERROR:
            case MSG_OUTPUT_ERROR:
                logd("I/O error: " + msg.obj);
                rootHandler.sendMessage(rootHandler.obtainMessage(MSG_TEARDOWN, msg.obj));
                return true;
            case MSG_TEARDOWN:
                logd("Teardown initiated, reason: " + msg.obj);
                handleTeardown(msg);
                return true;
            case MSG_CONNECT:
                logd("Connect initiated, reason: " + msg.obj);
                if (!isConnected()) {
                    connect();
                }
                return true;
            case MSG_HEARTBEAT:
                logd("Heartbeat initiated, reason: " + msg.obj);
                if (isConnected()) {
                    HeartbeatPing.Builder ping = new HeartbeatPing.Builder();
                    if (inputStream.newStreamIdAvailable()) {
                        ping.last_stream_id_received(inputStream.getStreamId());
                    }
                    send(MCS_HEARTBEAT_PING_TAG, ping.build());
                    scheduleHeartbeat(this);
                } else {
                    logd("Ignoring heartbeat, not connected!");
                    scheduleReconnect(this);
                }
                return true;
            case MSG_OUTPUT_READY:
                logd("Sending login request...");
                send(MCS_LOGIN_REQUEST_TAG, buildLoginRequest());
                return true;
            case MSG_OUTPUT_DONE:
                handleOutputDone(msg);
                return true;
        }
        Log.w(TAG, "Unknown message: " + msg);
        return false;
    }

    private void handleOutputDone(android.os.Message msg) {
        switch (msg.arg1) {
            case MCS_HEARTBEAT_PING_TAG:
                wakeLock.release();
                break;
            default:
        }
    }

    private void handleInput(int type, Message message) {
        try {
            switch (type) {
                case MCS_DATA_MESSAGE_STANZA_TAG:
                    handleCloudMessage((DataMessageStanza) message);
                    break;
                case MCS_HEARTBEAT_PING_TAG:
                    handleHeartbeatPing((HeartbeatPing) message);
                    break;
                case MCS_HEARTBEAT_ACK_TAG:
                    handleHeartbeatAck((HeartbeatAck) message);
                    break;
                case MCS_CLOSE_TAG:
                    handleClose((Close) message);
                    break;
                case MCS_LOGIN_RESPONSE_TAG:
                    handleLoginResponse((LoginResponse) message);
                    break;
                default:
                    Log.w(TAG, "Unknown message: " + message);
            }
            resetCurrentDelay();
        } catch (Exception e) {
            rootHandler.sendMessage(rootHandler.obtainMessage(MSG_TEARDOWN, e));
        }
    }

    private void tryClose(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception ignored) {
            }
        }
    }

    private void handleTeardown(android.os.Message msg) {
        tryClose(inputStream);
        tryClose(outputStream);
        try {
            sslSocket.close();
        } catch (Exception ignored) {
        }

        scheduleReconnect(this);

        alarmManager.cancel(heartbeatIntent);
        if (wakeLock != null) {
            try {
                wakeLock.release();
            } catch (Exception ignored) {
            }
        }
    }
}
