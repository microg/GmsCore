/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.rcs.standalone;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class RcsStatusBroadcaster extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // Broadcast RCS availability
        Intent rcsIntent = new Intent("com.google.android.gms.rcs.action.REGISTRATION_STATE_CHANGED");
        rcsIntent.putExtra("state", 7);
        rcsIntent.putExtra("timestamp", System.currentTimeMillis());
        rcsIntent.putExtra("hasToken", true);
        rcsIntent.putExtra("isValidAndUpdated", true);
        context.sendBroadcast(rcsIntent);
        
        // Broadcast capability update
        Intent capabilityIntent = new Intent("com.google.android.gms.rcs.action.CAPABILITY_UPDATE");
        capabilityIntent.putExtra("timestamp", System.currentTimeMillis());
        capabilityIntent.putExtra("hasToken", true);
        capabilityIntent.putExtra("isValidAndUpdated", true);
        context.sendBroadcast(capabilityIntent);
        
        // Broadcast provisioning complete
        Intent provisioningIntent = new Intent("com.google.android.gms.rcs.action.PROVISIONING_COMPLETE");
        provisioningIntent.putExtra("timestamp", System.currentTimeMillis());
        provisioningIntent.putExtra("hasToken", true);
        provisioningIntent.putExtra("isValidAndUpdated", true);
        context.sendBroadcast(provisioningIntent);
    }
}
