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

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

public class TriggerReceiver extends WakefulBroadcastReceiver {
    private static final String TAG = "GmsGcmTrigger";
    private static final String PREF_ENABLE_GCM = "gcm_enable_mcs_service";

    @Override
    public void onReceive(Context context, Intent intent) {
        boolean force = "android.provider.Telephony.SECRET_CODE".equals(intent.getAction());

        if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREF_ENABLE_GCM, false) || force) {
            if (!McsService.isConnected() || force) {

                ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo networkInfo = cm.getActiveNetworkInfo();
                if (networkInfo != null && networkInfo.isConnected() || force) {
                    startWakefulService(context, new Intent(McsService.ACTION_CONNECT, null, context, McsService.class));
                } else {
                    Log.d(TAG, "Ignoring " + intent + ": network is offline");
                }
            } else {
                Log.d(TAG, "Ignoring " + intent + ": service is running");
            }
        } else {
            Log.d(TAG, "Ignoring " + intent + ": gcm is disabled");
        }
    }
}
