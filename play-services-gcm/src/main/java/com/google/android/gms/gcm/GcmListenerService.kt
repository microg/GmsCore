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
package com.google.android.gms.gcm

import android.app.PendingIntent
import android.app.PendingIntent.CanceledException
import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.microg.gms.common.PublicApi

import org.microg.gms.gcm.GcmConstants.ACTION_C2DM_RECEIVE;
import org.microg.gms.gcm.GcmConstants.ACTION_NOTIFICATION_OPEN;
import org.microg.gms.gcm.GcmConstants.EXTRA_ERROR;
import org.microg.gms.gcm.GcmConstants.EXTRA_FROM;
import org.microg.gms.gcm.GcmConstants.EXTRA_MESSAGE_ID;
import org.microg.gms.gcm.GcmConstants.EXTRA_MESSAGE_TYPE;
import org.microg.gms.gcm.GcmConstants.EXTRA_PENDING_INTENT
import org.microg.gms.gcm.GcmConstants.MESSAGE_TYPE_DELETED_MESSAGE
import org.microg.gms.gcm.GcmConstants.MESSAGE_TYPE_GCM
import org.microg.gms.gcm.GcmConstants.MESSAGE_TYPE_SEND_ERROR
import org.microg.gms.gcm.GcmConstants.MESSAGE_TYPE_SEND_EVENT


/**
 * Base class for communicating with Google Cloud Messaging.
 *
 *
 * It also provides functionality such as automatically displaying
 * [notifications when requested by app server](https://developer.android.com/google/gcm/server.html).
 *
 *
 * Override base class methods to handle any events required by the application.
 * Methods are invoked asynchronously.
 *
 *
 * Include the following in the manifest:
 * <pre>
 * <service android:name=".YourGcmListenerService" android:exported="false">
 * <intent-filter>
 * <action android:name="com.google.android.c2dm.intent.RECEIVE"></action>
</intent-filter> *
</service></pre> *
 */
@PublicApi
abstract class GcmListenerService : Service() {
    private val lock = Any()
    private var startId = 0
    private var counter = 0
    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    /**
     * Called when GCM server deletes pending messages due to exceeded
     * storage limits, for example, when the device cannot be reached
     * for an extended period of time.
     *
     *
     * It is recommended to retrieve any missing messages directly from the
     * app server.
     */
    fun onDeletedMessages() {
        // To be overwritten
    }

    /**
     * Called when a message is received.
     *
     * @param from describes message sender.
     * @param data message data as String key/value pairs.
     */
    fun onMessageReceived(from: String?, data: Bundle?) {
        // To be overwritten
    }

    /**
     * Called when an upstream message has been successfully sent to the
     * GCM connection server.
     *
     * @param msgId of the upstream message sent using
     * [com.google.android.gms.gcm.GoogleCloudMessaging.send].
     */
    fun onMessageSent(msgId: String?) {
        // To be overwritten
    }

    /**
     * Called when there was an error sending an upstream message.
     *
     * @param msgId of the upstream message sent using
     * [com.google.android.gms.gcm.GoogleCloudMessaging.send].
     * @param error description of the error.
     */
    fun onSendError(msgId: String?, error: String?) {
        // To be overwritten
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        synchronized(lock) {
            this.startId = startId
            counter++
        }
        return if (intent != null) {
            when (intent.action) {
                ACTION_NOTIFICATION_OPEN -> {
                    handlePendingNotification(intent)
                    finishCounter()
                    GcmReceiver.completeWakefulIntent(intent)
                }
                ACTION_C2DM_RECEIVE -> {
                    GlobalScope.launch(context = Dispatchers.IO) {
                        handleC2dmMessage(intent)
                    }
                }
                else -> {
                    Log.w(TAG, "Unknown intent action: " + intent.action)
                }
            }
            START_REDELIVER_INTENT
        } else {
            finishCounter()
            START_NOT_STICKY
        }
    }

    private fun handleC2dmMessage(intent: Intent) {
        try {
            val messageType = intent.getStringExtra(EXTRA_MESSAGE_TYPE)
            if (messageType == null || MESSAGE_TYPE_GCM == messageType) {
                val from = intent.getStringExtra(EXTRA_FROM)
                val data = intent.extras
                data!!.remove(EXTRA_MESSAGE_TYPE)
                data.remove("android.support.content.wakelockid") // WakefulBroadcastReceiver.EXTRA_WAKE_LOCK_ID
                data.remove(EXTRA_FROM)
                onMessageReceived(from, data)
            } else if (MESSAGE_TYPE_DELETED_MESSAGE == messageType) {
                onDeletedMessages()
            } else if (MESSAGE_TYPE_SEND_EVENT == messageType) {
                onMessageSent(intent.getStringExtra(EXTRA_MESSAGE_ID))
            } else if (MESSAGE_TYPE_SEND_ERROR == messageType) {
                onSendError(intent.getStringExtra(EXTRA_MESSAGE_ID), intent.getStringExtra(EXTRA_ERROR))
            } else {
                Log.w(TAG, "Unknown message type: $messageType")
            }
            finishCounter()
        } finally {
            GcmReceiver.completeWakefulIntent(intent)
        }
    }

    private fun handlePendingNotification(intent: Intent) {
        val pendingIntent = intent.getParcelableExtra<PendingIntent>(EXTRA_PENDING_INTENT)
        if (pendingIntent != null) {
            try {
                pendingIntent.send()
            } catch (e: CanceledException) {
                Log.w(TAG, "Notification cancelled", e)
            }
        } else {
            Log.w(TAG, "Notification was null")
        }
    }

    private fun finishCounter() {
        synchronized(lock) {
            counter--
            if (counter == 0) {
                stopSelfResult(startId)
            }
        }
    }

    companion object {
        private const val TAG = "GcmListenerService"
    }
}