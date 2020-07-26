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

package com.google.android.gms.iid;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import androidx.legacy.content.WakefulBroadcastReceiver;

import static org.microg.gms.gcm.GcmConstants.ACTION_C2DM_REGISTRATION;
import static org.microg.gms.gcm.GcmConstants.ACTION_INSTANCE_ID;
import static org.microg.gms.gcm.GcmConstants.EXTRA_FROM;
import static org.microg.gms.gcm.GcmConstants.EXTRA_GSF_INTENT;

/**
 * Base class to handle Instance ID service notifications on token
 * refresh.
 * <p/>
 * Any app using Instance ID or GCM must include a class extending
 * InstanceIDListenerService and implement {@link com.google.android.gms.iid.InstanceIDListenerService#onTokenRefresh()}.
 * <p/>
 * Include the following in the manifest:
 * <pre>
 * <service android:name=".YourInstanceIDListenerService" android:exported="false">
 *     <intent-filter>
 *         <action android:name="com.google.android.gms.iid.InstanceID"/>
 *     </intent-filter>
 * </service></pre>
 * Do not export this service. Instead, keep it private to prevent other apps
 * accessing your service.
 */
public class InstanceIDListenerService extends Service {

    private BroadcastReceiver registrationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            handleIntent(intent);
            stop();
        }
    };
    private MessengerCompat messengerCompat = new MessengerCompat(new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            handleIntent((Intent) msg.obj);
        }
    });

    private int counter = 0;
    private int startId = -1;

    private void handleIntent(Intent intent) {
        // TODO
    }

    public IBinder onBind(Intent intent) {
        if (intent != null && ACTION_INSTANCE_ID.equals(intent.getAction())) {
            return messengerCompat.getBinder();
        }
        return null;
    }

    public void onCreate() {
        IntentFilter filter = new IntentFilter(ACTION_C2DM_REGISTRATION);
        filter.addCategory(getPackageName());
        registerReceiver(registrationReceiver, filter);
    }

    public void onDestroy() {
        unregisterReceiver(registrationReceiver);
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        synchronized (this) {
            this.counter++;
            if (startId > this.startId) this.startId = startId;
        }
        try {
            if (intent != null) {
                if (ACTION_INSTANCE_ID.equals(intent.getAction()) && intent.hasExtra(EXTRA_GSF_INTENT)) {
                    startService((Intent) intent.getParcelableExtra(EXTRA_GSF_INTENT));
                    return START_STICKY;
                }

                handleIntent(intent);

                if (intent.hasExtra(EXTRA_FROM))
                    WakefulBroadcastReceiver.completeWakefulIntent(intent);
            }
        } finally {
            stop();
        }
        return START_NOT_STICKY;
    }

    /**
     * Called when the system determines that the tokens need to be refreshed. The application
     * should call getToken() and send the tokens to all application servers.
     * <p/>
     * This will not be called very frequently, it is needed for key rotation and to handle special
     * cases.
     * <p/>
     * The system will throttle the refresh event across all devices to avoid overloading
     * application servers with token updates.
     */
    public void onTokenRefresh() {
        // To be overwritten
    }

    private void stop() {
        synchronized (this) {
            counter--;
            if (counter <= 0) {
                stopSelf(startId);
            }
        }
    }

}
