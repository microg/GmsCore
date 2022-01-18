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

package org.microg.gms.checkin;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.util.Log;

import androidx.legacy.content.WakefulBroadcastReceiver;

import com.google.android.gms.R;
import com.google.android.gms.checkin.internal.ICheckinService;

import org.microg.gms.auth.AuthConstants;
import org.microg.gms.common.ForegroundServiceInfo;
import org.microg.gms.common.ForegroundServiceContext;
import org.microg.gms.gcm.McsService;
import org.microg.gms.people.PeopleManager;

@ForegroundServiceInfo(value = "Google device registration", res = R.string.service_name_checkin)
public class CheckinService extends IntentService {
    private static final String TAG = "GmsCheckinSvc";
    public static final long MAX_VALID_CHECKIN_AGE = 24 * 60 * 60 * 1000; // 12 hours
    public static final long REGULAR_CHECKIN_INTERVAL = 12 * 60 * 60 * 1000; // 12 hours
    public static final long BACKUP_CHECKIN_DELAY = 3 * 60 * 60 * 1000; // 3 hours
    public static final String BIND_ACTION = "com.google.android.gms.checkin.BIND_TO_SERVICE";
    public static final String EXTRA_FORCE_CHECKIN = "force";
    @Deprecated
    public static final String EXTRA_CALLBACK_INTENT = "callback";
    public static final String EXTRA_RESULT_RECEIVER = "receiver";
    public static final String EXTRA_NEW_CHECKIN_TIME = "checkin_time";

    private ICheckinService iface = new ICheckinService.Stub() {
        @Override
        public String getDeviceDataVersionInfo() throws RemoteException {
            return LastCheckinInfo.read(CheckinService.this).getDeviceDataVersionInfo();
        }

        @Override
        public long getLastCheckinSuccessTime() throws RemoteException {
            return LastCheckinInfo.read(CheckinService.this).getLastCheckin();
        }

        @Override
        public String getLastSimOperator() throws RemoteException {
            return null;
        }
    };

    public CheckinService() {
        super(TAG);
    }

    @SuppressWarnings("MissingPermission")
    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            ForegroundServiceContext.completeForegroundService(this, intent, TAG);
            if (CheckinPrefs.isEnabled(this)) {
                LastCheckinInfo info = CheckinManager.checkin(this, intent.getBooleanExtra(EXTRA_FORCE_CHECKIN, false));
                if (info != null) {
                    Log.d(TAG, "Checked in as " + Long.toHexString(info.getAndroidId()));
                    String accountType = AuthConstants.DEFAULT_ACCOUNT_TYPE;
                    for (Account account : AccountManager.get(this).getAccountsByType(accountType)) {
                        PeopleManager.loadUserInfo(this, account);
                    }
                    McsService.scheduleReconnect(this);
                    if (intent.hasExtra(EXTRA_CALLBACK_INTENT)) {
                        startService((Intent) intent.getParcelableExtra(EXTRA_CALLBACK_INTENT));
                    }
                    if (intent.hasExtra(EXTRA_RESULT_RECEIVER)) {
                        ResultReceiver receiver = intent.getParcelableExtra(EXTRA_RESULT_RECEIVER);
                        if (receiver != null) {
                            Bundle bundle = new Bundle();
                            bundle.putLong(EXTRA_NEW_CHECKIN_TIME, info.getLastCheckin());
                            receiver.send(Activity.RESULT_OK, bundle);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, e);
        } finally {
            if (intent != null) {
                WakefulBroadcastReceiver.completeWakefulIntent(intent);
            }
            schedule(this);
            stopSelf();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (BIND_ACTION.equals(intent.getAction())) {
            return iface.asBinder();
        } else {
            return super.onBind(intent);
        }
    }

    static void schedule(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = PendingIntent.getService(context, TriggerReceiver.class.getName().hashCode(), new Intent(context, TriggerReceiver.class), PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.set(AlarmManager.RTC, Math.max(LastCheckinInfo.read(context).getLastCheckin() + REGULAR_CHECKIN_INTERVAL, System.currentTimeMillis() + BACKUP_CHECKIN_DELAY), pendingIntent);
    }
}
