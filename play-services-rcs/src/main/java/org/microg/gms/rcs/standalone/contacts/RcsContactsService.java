/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.rcs.standalone.contacts;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class RcsContactsService extends Service {
    private static final String TAG = "RcsContactsService";
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "STANDALONE RCS Contacts Service created");
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "=== STANDALONE RCS CONTACTS BIND REQUEST ===");
        Log.d(TAG, "Action: " + intent.getAction());
        Log.d(TAG, "Package: " + intent.getPackage());
        Log.d(TAG, "Component: " + intent.getComponent());
        Log.d(TAG, "Extras: " + intent.getExtras());
        
        // Return a simple contacts management interface
        return null; // For now, return null - we just need the service to exist
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "STANDALONE RCS Contacts Service destroyed");
    }
}
