/*
 * Copyright (C) 2018 microG Project Team
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

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import org.microg.gms.checkin.LastCheckinInfo;
import org.microg.gms.common.ForegroundServiceContext;
import org.microg.gms.common.PackageUtils;
import org.microg.gms.common.Utils;

import static org.microg.gms.gcm.GcmConstants.ACTION_C2DM_REGISTRATION;
import static org.microg.gms.gcm.GcmConstants.ERROR_SERVICE_NOT_AVAILABLE;
import static org.microg.gms.gcm.GcmConstants.EXTRA_APP;
import static org.microg.gms.gcm.GcmConstants.EXTRA_APP_OVERRIDE;
import static org.microg.gms.gcm.GcmConstants.EXTRA_ERROR;
import static org.microg.gms.gcm.McsConstants.ACTION_ACK;
import static org.microg.gms.gcm.McsConstants.ACTION_SEND;

class PushRegisterHandler extends Handler {
    private static final String TAG = "GmsGcmRegisterHdl";

    private Context context;
    private int callingUid;
    private GcmDatabase database;

    public PushRegisterHandler(Context context, GcmDatabase database) {
        this.context = context;
        this.database = database;
    }

    @Override
    public boolean sendMessageAtTime(Message msg, long uptimeMillis) {
        this.callingUid = Binder.getCallingUid();
        return super.sendMessageAtTime(msg, uptimeMillis);
    }

    private void sendReplyViaMessage(int what, int id, Messenger replyTo, Bundle messageData) {
        Message response = Message.obtain();
        response.what = what;
        response.arg1 = id;
        response.setData(messageData);
        try {
            replyTo.send(response);
        } catch (RemoteException e) {
            Log.w(TAG, e);
        }
    }

    private void sendReplyViaIntent(Intent outIntent, Messenger replyTo) {
        Message message = Message.obtain();
        message.obj = outIntent;
        try {
            replyTo.send(message);
        } catch (RemoteException e) {
            Log.w(TAG, e);
        }
    }

    private void sendReply(int what, int id, Messenger replyTo, Bundle data, boolean oneWay) {
        if (what == 0) {
            Intent outIntent = new Intent(ACTION_C2DM_REGISTRATION);
            outIntent.putExtras(data);
            sendReplyViaIntent(outIntent, replyTo);
            return;
        }
        Bundle messageData = new Bundle();
        messageData.putBundle("data", data);
        sendReplyViaMessage(what, id, replyTo, messageData);
    }

    private void replyError(int what, int id, Messenger replyTo, String errorMessage, boolean oneWay) {
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_ERROR, errorMessage);
        sendReply(what, id, replyTo, bundle, oneWay);
    }

    private void replyNotAvailable(int what, int id, Messenger replyTo) {
        replyError(what, id, replyTo, ERROR_SERVICE_NOT_AVAILABLE, false);
    }

    private PendingIntent getSelfAuthIntent() {
        Intent intent = new Intent();
        intent.setPackage("com.google.example.invalidpackage");
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }

    @Override
    public void handleMessage(Message msg) {
        if (msg.what == 0) {
            if (msg.obj instanceof Intent) {
                Message nuMsg = Message.obtain();
                nuMsg.what = msg.what;
                nuMsg.arg1 = 0;
                nuMsg.replyTo = null;
                PendingIntent pendingIntent = ((Intent) msg.obj).getParcelableExtra(EXTRA_APP);
                String packageName = PackageUtils.packageFromPendingIntent(pendingIntent);
                Bundle data = new Bundle();
                data.putBoolean("oneWay", false);
                data.putString("pkg", packageName);
                data.putBundle("data", msg.getData());
                nuMsg.setData(data);
                msg = nuMsg;
            } else {
                return;
            }
        }

        int what = msg.what;
        int id = msg.arg1;
        Messenger replyTo = msg.replyTo;
        if (replyTo == null) {
            Log.w(TAG, "replyTo is null");
            return;
        }
        Bundle data = msg.getData();

        String packageName = data.getString("pkg");
        Bundle subdata = data.getBundle("data");

        try {
            PackageUtils.checkPackageUid(context, packageName, callingUid);
        } catch (SecurityException e) {
            Log.w(TAG, e);
            return;
        }

        Log.d(TAG, "handleMessage: package=" + packageName + " what=" + what + " id=" + id);

        boolean oneWay = data.getBoolean("oneWay", false);

        switch (what) {
            case 0:
            case 1:
                // TODO: We should checkin and/or ask for permission here.
                String sender = subdata.getString("sender");
                boolean delete = subdata.get("delete") != null;

                PushRegisterManager.completeRegisterRequest(context, database,
                        new RegisterRequest()
                                .build(Utils.getBuild(context))
                                .sender(sender)
                                .checkin(LastCheckinInfo.read(context))
                                .app(packageName)
                                .delete(delete)
                                .extraParams(subdata),
                        bundle -> sendReply(what, id, replyTo, bundle, oneWay));
                break;
            case 2:
                String messageId = subdata.getString("google.message_id");
                Log.d(TAG, "Ack " + messageId + " for " + packageName);
                Intent i = new Intent(context, McsService.class);
                i.setAction(ACTION_ACK);
                i.putExtra(EXTRA_APP, getSelfAuthIntent());
                new ForegroundServiceContext(context).startService(i);
                break;
            default:
                Bundle bundle = new Bundle();
                bundle.putBoolean("unsupported", true);
                sendReplyViaMessage(what, id, replyTo, bundle);
                return;
        }

        if (oneWay) {
            Bundle bundle = new Bundle();
            bundle.putBoolean("ack", true);
            sendReplyViaMessage(what, id, replyTo, bundle);
        }
    }
}
