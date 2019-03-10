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

package org.microg.gms.gcm;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.annotation.Nullable;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

import org.microg.gms.checkin.CheckinService;
import org.microg.gms.checkin.LastCheckinInfo;
import org.microg.gms.common.PackageUtils;
import org.microg.gms.common.Utils;
import org.microg.gms.ui.AskPushPermission;

import static org.microg.gms.gcm.GcmConstants.ACTION_C2DM_REGISTER;
import static org.microg.gms.gcm.GcmConstants.ACTION_C2DM_REGISTRATION;
import static org.microg.gms.gcm.GcmConstants.ACTION_C2DM_UNREGISTER;
import static org.microg.gms.gcm.GcmConstants.ERROR_SERVICE_NOT_AVAILABLE;
import static org.microg.gms.gcm.GcmConstants.EXTRA_APP;
import static org.microg.gms.gcm.GcmConstants.EXTRA_DELETE;
import static org.microg.gms.gcm.GcmConstants.EXTRA_ERROR;
import static org.microg.gms.gcm.GcmConstants.EXTRA_KID;
import static org.microg.gms.gcm.GcmConstants.EXTRA_MESSENGER;
import static org.microg.gms.gcm.GcmConstants.EXTRA_PENDING_INTENT;
import static org.microg.gms.gcm.GcmConstants.EXTRA_SENDER;

public class PushRegisterService extends IntentService {
    private static final String TAG = "GmsGcmRegisterSvc";
    private static final String EXTRA_SKIP_TRY_CHECKIN = "skip_checkin";

    private GcmDatabase database;
    private static boolean requestPending = false;

    public PushRegisterService() {
        super(TAG);
        setIntentRedelivery(false);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        database = new GcmDatabase(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        database.close();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        WakefulBroadcastReceiver.completeWakefulIntent(intent);
        Log.d(TAG, "onHandleIntent: " + intent);

        String requestId = null;
        if (intent.hasExtra(EXTRA_KID) && intent.getStringExtra(EXTRA_KID).startsWith("|")) {
            String[] kid = intent.getStringExtra(EXTRA_KID).split("\\|");
            if (kid.length >= 3 && "ID".equals(kid[1])) {
                requestId = kid[2];
            }
        }

        if (LastCheckinInfo.read(this).lastCheckin > 0) {
            try {
                if (ACTION_C2DM_UNREGISTER.equals(intent.getAction()) ||
                        (ACTION_C2DM_REGISTER.equals(intent.getAction()) && "1".equals(intent.getStringExtra(EXTRA_DELETE)))) {
                    unregister(intent, requestId);
                } else if (ACTION_C2DM_REGISTER.equals(intent.getAction())) {
                    register(intent, requestId);
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

    private void register(final Intent intent, String requestId) {
        PendingIntent pendingIntent = intent.getParcelableExtra(EXTRA_APP);
        final String packageName = PackageUtils.packageFromPendingIntent(pendingIntent);

        GcmDatabase.App app = database.getApp(packageName);
        if (app == null && GcmPrefs.get(this).isConfirmNewApps()) {
            try {
                getPackageManager().getApplicationInfo(packageName, 0); // Check package exists
                Intent i = new Intent(this, AskPushPermission.class);
                i.putExtra(EXTRA_PENDING_INTENT, intent);
                i.putExtra(EXTRA_APP, packageName);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
            } catch (PackageManager.NameNotFoundException e) {
                replyNotAvailable(this, intent, packageName, requestId);
            }
        } else {
            registerAndReply(this, database, intent, packageName, requestId);
        }
    }

    public static void replyNotAvailable(Context context, Intent intent, String packageName, String requestId) {
        Intent outIntent = new Intent(ACTION_C2DM_REGISTRATION);
        outIntent.putExtra(EXTRA_ERROR, PushRegisterManager.attachRequestId(ERROR_SERVICE_NOT_AVAILABLE, requestId));
        sendReply(context, intent, packageName, outIntent);
    }

    public static void registerAndReply(Context context, GcmDatabase database, Intent intent, String packageName, String requestId) {
        Log.d(TAG, "register[req]: " + intent.toString() + " extras=" + intent.getExtras());
        PushRegisterManager.completeRegisterRequest(context, database,
                new RegisterRequest()
                        .build(Utils.getBuild(context))
                        .sender(intent.getStringExtra(EXTRA_SENDER))
                        .checkin(LastCheckinInfo.read(context))
                        .app(packageName),
                bundle -> {
                    Intent outIntent = new Intent(ACTION_C2DM_REGISTRATION);
                    outIntent.putExtras(bundle);
                    Log.d(TAG, "register[res]: " + outIntent.toString() + " extras=" + outIntent.getExtras());
                    sendReply(context, intent, packageName, outIntent);
                });
    }

    private static void sendReply(Context context, Intent intent, String packageName, Intent outIntent) {
        try {
            if (intent != null && intent.hasExtra(EXTRA_MESSENGER)) {
                Messenger messenger = intent.getParcelableExtra(EXTRA_MESSENGER);
                Message message = Message.obtain();
                message.obj = outIntent;
                messenger.send(message);
                return;
            }
        } catch (Exception e) {
            Log.w(TAG, e);
        }

        outIntent.setPackage(packageName);
        context.sendOrderedBroadcast(outIntent, null);
    }

    private void unregister(Intent intent, String requestId) {
        PendingIntent pendingIntent = intent.getParcelableExtra(EXTRA_APP);
        String packageName = PackageUtils.packageFromPendingIntent(pendingIntent);
        Log.d(TAG, "unregister[req]: " + intent.toString() + " extras=" + intent.getExtras());

        PushRegisterManager.completeRegisterRequest(this, database,
                new RegisterRequest()
                        .build(Utils.getBuild(this))
                        .sender(intent.getStringExtra(EXTRA_SENDER))
                        .checkin(LastCheckinInfo.read(this))
                        .app(packageName),
                bundle -> {
                    Intent outIntent = new Intent(ACTION_C2DM_REGISTRATION);
                    outIntent.putExtras(bundle);
                    Log.d(TAG, "unregister[res]: " + outIntent.toString() + " extras=" + outIntent.getExtras());
                    sendReply(this, intent, packageName, outIntent);
                });
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind: " + intent.toString());
        if (ACTION_C2DM_REGISTER.equals(intent.getAction())) {
            Messenger messenger = new Messenger(new PushRegisterHandler(this, database));
            return messenger.getBinder();
        }
        return super.onBind(intent);
    }

}
