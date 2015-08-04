/*
 * Copyright 2013-2015 µg Project Team
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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.SystemClock;
import android.preference.PreferenceManager;

public class TriggerReceiver extends BroadcastReceiver {
    private static final String PREF_ENABLE_GCM = "gcm_enable_mcs_service";
    private static final long pendingDelay = 1000;

    @Override
    public void onReceive(Context context, Intent intent) {
        boolean force = "android.provider.Telephony.SECRET_CODE".equals(intent.getAction());

        if (!McsService.isConnected() || force) {
            if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREF_ENABLE_GCM, false) || force) {

                ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo networkInfo = cm.getActiveNetworkInfo();
                if (networkInfo != null && networkInfo.isConnected() || force) {
                    AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                    PendingIntent pendingIntent = PendingIntent.getService(context, 0, new Intent(McsService.ACTION_CONNECT, null, context, McsService.class), 0);
                    alarmManager.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + pendingDelay, pendingIntent);
                }
            }
        }
    }
}
