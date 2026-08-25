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

package com.google.android.gms.gcm;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import org.microg.gms.common.PublicApi;

import static org.microg.gms.gcm.GcmConstants.ACTION_C2DM_RECEIVE;
import static org.microg.gms.gcm.GcmConstants.ACTION_NOTIFICATION_OPEN;
import static org.microg.gms.gcm.GcmConstants.EXTRA_ERROR;
import static org.microg.gms.gcm.GcmConstants.EXTRA_FROM;
import static org.microg.gms.gcm.GcmConstants.EXTRA_MESSAGE_ID;
import static org.microg.gms.gcm.GcmConstants.EXTRA_MESSAGE_TYPE;
import static org.microg.gms.gcm.GcmConstants.EXTRA_PENDING_INTENT;
import static org.microg.gms.gcm.GcmConstants.MESSAGE_TYPE_DELETED_MESSAGE;
import static org.microg.gms.gcm.GcmConstants.MESSAGE_TYPE_GCM;
import static org.microg.gms.gcm.GcmConstants.MESSAGE_TYPE_SEND_ERROR;
import static org.microg.gms.gcm.GcmConstants.MESSAGE_TYPE_SEND_EVENT;

/**
 * Base class for communicating with Google Cloud Messaging.
 * <p/>
 * It also provides functionality such as automatically displaying
 * <a href="https://developer.android.com/google/gcm/server.html">notifications when requested by app server</a>.
 * <p/>
 * Override base class methods to handle any events required by the application.
 * Methods are invoked asynchronously.
 * <p/>
 * Include the following in the manifest:
 * <pre>
 * <service
 *     android:name=".YourGcmListenerService"
 *     android:exported="false" >
 *     <intent-filter>
 *         <action android:name="com.google.android.c2dm.intent.RECEIVE" />
 *     </intent-filter>
 * </service></pre>
 */
@PublicApi
public abstract class GcmListenerService extends Service {
    private static final String TAG = "GcmListenerService";

    private final Object lock = new Object();
    private int startId;
    private int counter = 0;

    public final IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Called when GCM server deletes pending messages due to exceeded
     * storage limits, for example, when the device cannot be reached
     * for an extended period of time.
     * <p/>
     * It is recommended to retrieve any missing messages directly from the
     * app server.
     */
    public void onDeletedMessages() {
        // To be overwritten
    }

    /**
     * Called when a message is received.
     *
     * @param from describes message sender.
     * @param data message data as String key/value pairs.
     */
    public void onMessageReceived(String from, Bundle data) {
        // To be overwritten
    }

    /**
     * Called when an upstream message has been successfully sent to the
     * GCM connection server.
     *
     * @param msgId of the upstream message sent using
     *              {@link com.google.android.gms.gcm.GoogleCloudMessaging#send(java.lang.String, java.lang.String, android.os.Bundle)}.
     */
    public void onMessageSent(String msgId) {
        // To be overwritten
    }

    /**
     * Called when there was an error sending an upstream message.
     *
     * @param msgId of the upstream message sent using
     *              {@link com.google.android.gms.gcm.GoogleCloudMessaging#send(java.lang.String, java.lang.String, android.os.Bundle)}.
     * @param error description of the error.
     */
    public void onSendError(String msgId, String error) {
        // To be overwritten
    }

    public final int onStartCommand(final Intent intent, int flags, int startId) {
        synchronized (lock) {
            this.startId = startId;
            this.counter++;
        }

        if (intent != null) {
            if (ACTION_NOTIFICATION_OPEN.equals(intent.getAction())) {
                handlePendingNotification(intent);
                finishCounter();
                GcmReceiver.completeWakefulIntent(intent);
            } else if (ACTION_C2DM_RECEIVE.equals(intent.getAction())) {
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        handleC2dmMessage(intent);
                        return null;
                    }
                }.execute();
            } else {
                Log.w(TAG, "Unknown intent action: " + intent.getAction());

            }

            return START_REDELIVER_INTENT;
        } else {
            finishCounter();
            return START_NOT_STICKY;
        }
    }

    private void handleC2dmMessage(Intent intent) {
        try {
            String messageType = intent.getStringExtra(EXTRA_MESSAGE_TYPE);
            if (messageType == null || MESSAGE_TYPE_GCM.equals(messageType)) {
                String from = intent.getStringExtra(EXTRA_FROM);
                Bundle data = intent.getExtras();
                data.remove(EXTRA_MESSAGE_TYPE);
                data.remove("android.support.content.wakelockid"); // WakefulBroadcastReceiver.EXTRA_WAKE_LOCK_ID
                data.remove(EXTRA_FROM);
                onMessageReceived(from, data);
            } else if (MESSAGE_TYPE_DELETED_MESSAGE.equals(messageType)) {
                onDeletedMessages();
            } else if (MESSAGE_TYPE_SEND_EVENT.equals(messageType)) {
                onMessageSent(intent.getStringExtra(EXTRA_MESSAGE_ID));
            } else if (MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
                onSendError(intent.getStringExtra(EXTRA_MESSAGE_ID), intent.getStringExtra(EXTRA_ERROR));
            } else {
                Log.w(TAG, "Unknown message type: " + messageType);
            }
            finishCounter();
        } finally {
            GcmReceiver.completeWakefulIntent(intent);
        }
    }

    private void handlePendingNotification(Intent intent) {
        PendingIntent pendingIntent = intent.getParcelableExtra(EXTRA_PENDING_INTENT);
        if (pendingIntent != null) {
            try {
                pendingIntent.send();
            } catch (PendingIntent.CanceledException e) {
                Log.w(TAG, "Notification cancelled", e);
            }
        } else {
            Log.w(TAG, "Notification was null");
        }
    }

    private void finishCounter() {
        synchronized (lock) {
            this.counter--;
            if (counter == 0) {
                stopSelfResult(startId);
            }
        }
    }

}
