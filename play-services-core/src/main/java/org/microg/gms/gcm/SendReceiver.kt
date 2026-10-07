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

package org.microg.gms.gcm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import org.microg.gms.common.ForegroundServiceContext
import org.microg.gms.gcm.McsConstants.ACTION_SEND

class SendReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val extras = intent.extras ?: return
        Log.d("GmsMcsSendRcvr", "original extras: $extras")
        
        // Use an iterator to safely remove items while iterating
        val keysToRemove = extras.keySet().filter { it.startsWith("GOOG.") || it.startsWith("GOOGLE.") }
        for (key in keysToRemove) {
            extras.remove(key)
        }

        val i = Intent(context, McsService::class.java).apply {
            action = ACTION_SEND
            putExtras(extras)
        }
        ForegroundServiceContext(context).startService(i)
    }
}
