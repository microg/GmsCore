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
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

import org.microg.gms.checkin.CheckinService;
import org.microg.gms.checkin.LastCheckinInfo;
import org.microg.gms.common.PackageUtils;
import org.microg.gms.common.Utils;

import java.io.IOException;

import static org.microg.gms.gcm.GcmConstants.ACTION_C2DM_REGISTER;
import static org.microg.gms.gcm.GcmConstants.ACTION_C2DM_REGISTRATION;
import static org.microg.gms.gcm.GcmConstants.ACTION_C2DM_UNREGISTER;
import static org.microg.gms.gcm.GcmConstants.ERROR_SERVICE_NOT_AVAILABLE;
import static org.microg.gms.gcm.GcmConstants.EXTRA_APP;
import static org.microg.gms.gcm.GcmConstants.EXTRA_DELETE;
import static org.microg.gms.gcm.GcmConstants.EXTRA_ERROR;
import static org.microg.gms.gcm.GcmConstants.EXTRA_MESSENGER;
import static org.microg.gms.gcm.GcmConstants.EXTRA_REGISTRATION_ID;
import static org.microg.gms.gcm.GcmConstants.EXTRA_RETRY_AFTER;
import static org.microg.gms.gcm.GcmConstants.EXTRA_SENDER;
import static org.microg.gms.gcm.GcmConstants.EXTRA_UNREGISTERED;

public class PushRegisterService extends IntentService {
    private static final String TAG = "GmsGcmRegisterSvc";
    private static final String EXTRA_SKIP_TRY_CHECKIN = "skip_checkin";

    private GcmData gcmStorage = new GcmData(this);

    public PushRegisterService() {
        super(TAG);
        setIntentRedelivery(false);
    }

    public static RegisterResponse register(Context context, String app, String appSignature, String sender, String info) {
        RegisterResponse response = register(context, app, appSignature, sender, info, false);
        String regId = response.token;
        if (regId != null) {
            (new GcmData(context)).app_registered(app, appSignature, regId);
        } else {
            (new GcmData(context)).app_registration_error(app, appSignature);
        }
        return response;
    }

    public static RegisterResponse unregister(Context context, String app, String appSignature, String sender, String info) {
        RegisterResponse response = register(context, app, appSignature, sender, info, true);
        if (!app.equals(response.deleted)) {
            (new GcmData(context)).app_unregistration_error(app, appSignature);
        } else {
            (new GcmData(context)).app_unregistered(app, appSignature);
        }
        return response;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "onHandleIntent: " + intent);
        if (LastCheckinInfo.read(this).lastCheckin > 0) {
            try {
                if (ACTION_C2DM_UNREGISTER.equals(intent.getAction()) ||
                        (ACTION_C2DM_REGISTER.equals(intent.getAction()) && "1".equals(intent.getStringExtra(EXTRA_DELETE)))) {
                    unregister(intent);
                } else if (ACTION_C2DM_REGISTER.equals(intent.getAction())) {
                    register(intent);
                }
            } catch (Exception e) {
                Log.w(TAG, e);
            }
        } else if (!intent.getBooleanExtra(EXTRA_SKIP_TRY_CHECKIN, false)) {
            Log.d(TAG, "No checkin yet, trying to checkin");
            intent.putExtra(EXTRA_SKIP_TRY_CHECKIN, true);
            Intent subIntent = new Intent(this, CheckinService.class);
            subIntent.putExtra(CheckinService.EXTRA_FORCE_CHECKIN, true);
            subIntent.putExtra(CheckinService.EXTRA_CALLBACK_INTENT, intent);
            startService(subIntent);
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
        PendingIntent pendingIntent = intent.getParcelableExtra(EXTRA_APP);
        String sender = intent.getStringExtra(EXTRA_SENDER);
        String app = packageFromPendingIntent(pendingIntent);
        Log.d(TAG, "register[req]: " + intent.toString() + " extras=" + intent.getExtras());

        Intent outIntent = new Intent(ACTION_C2DM_REGISTRATION);
        String appSignature = PackageUtils.firstSignatureDigest(this, app);

        String regId = register(this, app, appSignature, sender, null).token;
        if (regId != null) {
            outIntent.putExtra(EXTRA_REGISTRATION_ID, regId);
        } else {
            outIntent.putExtra(EXTRA_ERROR, ERROR_SERVICE_NOT_AVAILABLE);
        }

        Log.d(TAG, "register[res]: " + outIntent + " extras=" + outIntent.getExtras());
        try {
            if (intent.hasExtra(EXTRA_MESSENGER)) {
                Messenger messenger = intent.getParcelableExtra(EXTRA_MESSENGER);
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
        PendingIntent pendingIntent = intent.getParcelableExtra(EXTRA_APP);
        String app = packageFromPendingIntent(pendingIntent);
        Log.d(TAG, "unregister[req]: " + intent.toString() + " extras=" + intent.getExtras());

        Intent outIntent = new Intent(ACTION_C2DM_REGISTRATION);
        String appSignature = PackageUtils.firstSignatureDigest(this, app);

        if (!gcmStorage.getAppInfo(app, appSignature).isRemoved()) {
            outIntent.putExtra(EXTRA_UNREGISTERED, app);
        } else {
            RegisterResponse response = unregister(this, app, appSignature, null, null);
            if (!app.equals(response.deleted)) {
                outIntent.putExtra(EXTRA_ERROR, ERROR_SERVICE_NOT_AVAILABLE);

                if (response.retryAfter != null && !response.retryAfter.contains(":")) {
                    outIntent.putExtra(EXTRA_RETRY_AFTER, Long.parseLong(response.retryAfter));
                }
            } else {
                outIntent.putExtra(EXTRA_UNREGISTERED, app);
            }
        }

        Log.d(TAG, "unregister[res]: " + outIntent.toString() + " extras=" + outIntent.getExtras());
        try {
            if (intent.hasExtra(EXTRA_MESSENGER)) {
                Messenger messenger = intent.getParcelableExtra(EXTRA_MESSENGER);
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
