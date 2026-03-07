/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.rcs;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class RcsStatusBroadcaster extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        sendRcsIntent(context, "com.google.android.gms.rcs.action.REGISTRATION_STATE_CHANGED");
        sendRcsIntent(context, "com.google.android.gms.rcs.action.CAPABILITY_UPDATE");
        sendRcsIntent(context, "com.google.android.gms.rcs.action.PROVISIONING_COMPLETE");
    }
    
    private void sendRcsIntent(Context context, String action) {
        Intent rcsIntent = new Intent(action);
        rcsIntent.putExtra("timestamp", System.currentTimeMillis());
        rcsIntent.putExtra("hasToken", true);
        rcsIntent.putExtra("isValidAndUpdated", true);
        if ("com.google.android.gms.rcs.action.REGISTRATION_STATE_CHANGED".equals(action)) {
            rcsIntent.putExtra("state", 7);
        }
        context.sendBroadcast(rcsIntent);
    }
}
