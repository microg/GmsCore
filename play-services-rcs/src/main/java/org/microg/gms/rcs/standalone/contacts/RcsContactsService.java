/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.rcs.standalone.contacts;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class RcsContactsService extends Service {
    
    @Override
    public void onCreate() {
        super.onCreate();
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null; 
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
