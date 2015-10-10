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
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

import org.microg.gms.checkin.LastCheckinInfo;
import org.microg.gms.common.PackageUtils;
import org.microg.gms.common.Utils;

import java.io.IOException;

public class PushRegisterService extends IntentService {
    private static final String TAG = "GmsGcmRegisterSvc";

    private static final String REMOVED = "%%REMOVED%%";
    private static final String ERROR = "%%ERROR%%";

    public PushRegisterService() {
        super(TAG);
        setIntentRedelivery(false);
    }

    private SharedPreferences getSharedPreferences() {
        return getSharedPreferences("gcm_registrations", MODE_PRIVATE);
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
        Log.d(TAG, "register[res]: " + intent.toString() + " extras=" + intent.getExtras());

        Intent outIntent = new Intent("com.google.android.c2dm.intent.REGISTRATION");
        String appSignature = PackageUtils.firstSignatureDigest(this, app);

        String regId = register(this, app, appSignature, sender, null, false).token;
        if (regId != null) {
            outIntent.putExtra("registration_id", regId);
            getSharedPreferences().edit().putString(app + ":" + appSignature, regId).apply();
        } else {
            outIntent.putExtra("error", "SERVICE_NOT_AVAILABLE");
            getSharedPreferences().edit().putString(app + ":" + appSignature, "-").apply();
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

        outIntent.setPackage(app);
        sendOrderedBroadcast(outIntent, null);
    }

    public static RegisterResponse register(Context context, String app, String appSignature, String sender, String info, boolean delete) {
        try {
            RegisterResponse response = new RegisterRequest()
                    .build(Utils.getBuild(context))
                    .sender(sender)
                    .info(info)
                    .checkin(LastCheckinInfo.read(context))
                    .app(app, appSignature, PackageUtils.versionCode(context, app))
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
        Log.d(TAG, "unregister[res]: " + intent.toString() + " extras=" + intent.getExtras());

        Intent outIntent = new Intent("com.google.android.c2dm.intent.REGISTRATION");
        String appSignature = PackageUtils.firstSignatureDigest(this, app);

        if (REMOVED.equals(getSharedPreferences().getString(app + ":" + appSignature, null))) {
            outIntent.putExtra("unregistered", app);
        } else {
            RegisterResponse response = register(this, app, appSignature, null, null, true);
            if (!app.equals(response.deleted)) {
                outIntent.putExtra("error", "SERVICE_NOT_AVAILABLE");
                getSharedPreferences().edit().putString(app + ":" + PackageUtils.firstSignatureDigest(this, app), ERROR).apply();

                long retry = 0;
                if (response.retryAfter != null && !response.retryAfter.contains(":")) {
                    outIntent.putExtra("Retry-After", Long.parseLong(response.retryAfter));
                }
            } else {
                outIntent.putExtra("unregistered", app);
                getSharedPreferences().edit().putString(app + ":" + PackageUtils.firstSignatureDigest(this, app), REMOVED).apply();
            }
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

        outIntent.setPackage(app);
        sendOrderedBroadcast(outIntent, null);
    }
}
