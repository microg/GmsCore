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

package org.microg.gms.checkin

import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET
import android.net.NetworkRequest
import android.os.Build.VERSION.SDK_INT
import android.util.Log
import androidx.core.app.PendingIntentCompat
import org.microg.gms.checkin.CheckinService.EXTRA_FORCE_CHECKIN
import org.microg.gms.checkin.CheckinService.REGULAR_CHECKIN_INTERVAL
import org.microg.gms.common.ForegroundServiceContext

class TriggerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        try {
            val force = "android.provider.Telephony.SECRET_CODE" == intent.action

            if (CheckinPreferences.isEnabled(context) || force) {
                if (LastCheckinInfo.read(context).lastCheckin > System.currentTimeMillis() - REGULAR_CHECKIN_INTERVAL && !force) {
                    CheckinService.schedule(context)
                    return
                }

                val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val networkInfo = cm.activeNetworkInfo
                
                if (networkInfo != null && networkInfo.isConnected || force) {
                    val subIntent = Intent(context, CheckinService::class.java).apply {
                        putExtra(EXTRA_FORCE_CHECKIN, force)
                    }
                    ForegroundServiceContext(context).startService(subIntent)
                } else if (SDK_INT >= 23) {
                    // no network, register a network callback to retry when we have internet
                    val networkRequest = NetworkRequest.Builder()
                        .addCapability(NET_CAPABILITY_INTERNET)
                        .build()
                    val i = Intent(context, TriggerReceiver::class.java)
                    val pendingIntent = PendingIntentCompat.getBroadcast(context, 0, i, FLAG_UPDATE_CURRENT, true)
                    cm.registerNetworkCallback(networkRequest, pendingIntent)
                }
            } else {
                Log.d(TAG, "Ignoring $intent: checkin is disabled")
            }
        } catch (e: Exception) {
            Log.w(TAG, e)
        }
    }

    companion object {
        private const val TAG = "GmsCheckinTrigger"
        private var registered = false
    }
}
