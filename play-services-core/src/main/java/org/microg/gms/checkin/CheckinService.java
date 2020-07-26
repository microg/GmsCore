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
import android.app.IntentService;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.legacy.content.WakefulBroadcastReceiver;

import com.google.android.gms.checkin.internal.ICheckinService;

import org.microg.gms.auth.AuthConstants;
import org.microg.gms.common.ForegroundServiceContext;
import org.microg.gms.gcm.McsService;
import org.microg.gms.people.PeopleManager;

public class CheckinService extends IntentService {
    private static final String TAG = "GmsCheckinSvc";
    public static final String BIND_ACTION = "com.google.android.gms.checkin.BIND_TO_SERVICE";
    public static final String EXTRA_FORCE_CHECKIN = "force";
    public static final String EXTRA_CALLBACK_INTENT = "callback";

    private ICheckinService iface = new ICheckinService.Stub() {
        @Override
        public String getDeviceDataVersionInfo() throws RemoteException {
            return LastCheckinInfo.read(CheckinService.this).deviceDataVersionInfo;
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
            if (CheckinPrefs.get(this).isEnabled()) {
                LastCheckinInfo info = CheckinManager.checkin(this, intent.getBooleanExtra(EXTRA_FORCE_CHECKIN, false));
                if (info != null) {
                    Log.d(TAG, "Checked in as " + Long.toHexString(info.androidId));
                    String accountType = AuthConstants.DEFAULT_ACCOUNT_TYPE;
                    for (Account account : AccountManager.get(this).getAccountsByType(accountType)) {
                        PeopleManager.loadUserInfo(this, account);
                    }
                    McsService.scheduleReconnect(this);
                    if (intent.hasExtra(EXTRA_CALLBACK_INTENT)) {
                        startService((Intent) intent.getParcelableExtra(EXTRA_CALLBACK_INTENT));
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, e);
        } finally {
            WakefulBroadcastReceiver.completeWakefulIntent(intent);
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
}
