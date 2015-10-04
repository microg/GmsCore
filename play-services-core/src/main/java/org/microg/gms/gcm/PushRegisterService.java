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

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

import org.microg.gms.checkin.LastCheckinInfo;
import org.microg.gms.common.PackageUtils;
import org.microg.gms.common.Utils;

import java.io.IOException;

public class PushRegisterService extends IntentService {
    private static final String TAG = "GmsGcmRegisterSvc";

    public PushRegisterService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "onHandleIntent: " + intent);
        try {
            if (intent != null && intent.getAction() != null) {
                if (intent.getAction().equalsIgnoreCase("com.google.android.c2dm.intent.REGISTER")) {
                    register(intent);
                } else if (intent.getAction().equalsIgnoreCase("com.google.android.c2dm.intent.UNREGISTER")) {
                    unregister(intent);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, e);
        }
    }

    @SuppressWarnings("deprecation")
    private String packageFromPendingIntent(PendingIntent pi) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return pi.getTargetPackage();
        } else {
            return pi.getCreatorPackage();
        }
    }

    private void register(Intent intent) {
        PendingIntent pendingIntent = intent.getParcelableExtra("app");
        String sender = intent.getStringExtra("sender");
        String app = packageFromPendingIntent(pendingIntent);
        Bundle extras = intent.getExtras();
        extras.keySet();
        Log.d(TAG, "register[req]: " + extras);
        Intent outIntent = new Intent("com.google.android.c2dm.intent.REGISTRATION");
        outIntent.setPackage(app);
        String regId = register(this, app, sender, null, false).token;
        if (regId != null) {
            outIntent.putExtra("registration_id", regId);
        } else {
            outIntent.putExtra("error", "SERVICE_NOT_AVAILABLE");
        }
        Log.d(TAG, "register[res]: " + outIntent + " extras=" + outIntent.getExtras());
        try {
            if (intent.hasExtra("google.messenger")) {
                Messenger messenger = intent.getParcelableExtra("google.messenger");
                Message message = Message.obtain();
                message.obj = outIntent;
                messenger.send(message);
                return;
            }
        } catch (Exception e) {
            Log.w(TAG, e);
        }
        sendOrderedBroadcast(outIntent, null);
    }

    public static RegisterResponse register(Context context, String app, String sender, String info, boolean delete) {
        try {
            RegisterResponse response = new RegisterRequest()
                    .build(Utils.getBuild(context))
                    .sender(sender)
                    .info(info)
                    .checkin(LastCheckinInfo.read(context))
                    .app(app, PackageUtils.firstSignatureDigest(context, app), PackageUtils.versionCode(context, app))
                    .delete(delete)
                    .getResponse();
            Log.d(TAG, "received response: " + response);
            return response;
        } catch (IOException e) {
            Log.w(TAG, e);
        }

        return new RegisterResponse();
    }

    private void unregister(Intent intent) {
        PendingIntent pendingIntent = intent.getParcelableExtra("app");
        String app = packageFromPendingIntent(pendingIntent);

        Intent outIntent = new Intent("com.google.android.c2dm.intent.REGISTRATION");
        outIntent.setPackage(app);

        RegisterResponse response = register(this, app, null, null, true);
        if (!app.equals(response.deleted)) {
            outIntent.putExtra("error", "SERVICE_NOT_AVAILABLE");

            long retry = 0;
            if (response.retryAfter != null && !response.retryAfter.contains(":")) {
                retry = Long.parseLong(response.retryAfter);
            }

            if (retry > 0) {
                outIntent.putExtra("Retry-After", retry);
            }
        } else {
            outIntent.putExtra("unregistered", app);
        }

        Log.d(TAG, "unregister[res]: " + outIntent.toString() + " extras=" + outIntent.getExtras());
        try {
            if (intent.hasExtra("google.messenger")) {
                Messenger messenger = intent.getParcelableExtra("google.messenger");
                Message message = Message.obtain();
                message.obj = outIntent;
                messenger.send(message);
                return;
            }
        } catch (Exception e) {
            Log.w(TAG, e);
        }
        sendOrderedBroadcast(outIntent, null);
    }
}
