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

package org.microg.gms.checkin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class TriggerReceiver extends BroadcastReceiver {
    private static final String TAG = "GmsCheckinTrigger";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Trigger checkin: " + intent);
        Intent subIntent = new Intent(context, CheckinService.class);
        if ("android.provider.Telephony.SECRET_CODE".equals(intent.getAction())) {
            subIntent.putExtra("force", true);
        }
        context.startService(subIntent);
    }
}
