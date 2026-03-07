/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.rcs;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;

import com.google.android.ims.rcs.engine.IRcsEngineController;
import com.google.android.ims.rcs.engine.RcsEngineLifecycleServiceResult;
import com.google.android.ims.rcsservice.lifecycle.InitializeAndStartRcsTransportRequest;
import com.google.android.ims.rcsservice.lifecycle.StopAllRcsTransportsExceptRequest;

public class RcsEngineLifecycleServiceV2 extends Service {
    
    private StateChangeReceiver stateChangeReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        stateChangeReceiver = new StateChangeReceiver();
        IntentFilter filter = new IntentFilter("com.google.android.gms.rcs.engine.STATE_CHANGED");
        registerReceiver(stateChangeReceiver, filter);
        broadcastRcsAvailable();
    }

    @Override
    public IBinder onBind(Intent intent) {
        broadcastRcsAvailable();
        return new RcsEngineControllerImpl();
    }

    private class StateChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.google.android.gms.rcs.engine.STATE_CHANGED".equals(intent.getAction())) {
                broadcastRcsAvailable();
            }
        }
    }

    private class RcsEngineControllerImpl extends IRcsEngineController.Stub {
        
        @Override
        public RcsEngineLifecycleServiceResult initialize(int subId, int flags) {
            broadcastRcsAvailable();
            return new RcsEngineLifecycleServiceResult(RcsEngineLifecycleServiceResult.SUCCESS);
        }

        @Override
        public RcsEngineLifecycleServiceResult destroy(int subId) {
            return new RcsEngineLifecycleServiceResult(RcsEngineLifecycleServiceResult.SUCCESS);
        }

        @Override
        public RcsEngineLifecycleServiceResult triggerStartRcsStack(int subId) {
            return new RcsEngineLifecycleServiceResult(RcsEngineLifecycleServiceResult.SUCCESS);
        }

        @Override
        public RcsEngineLifecycleServiceResult triggerStopRcsStack(int subId) {
            return new RcsEngineLifecycleServiceResult(RcsEngineLifecycleServiceResult.SUCCESS);
        }

        @Override
        public RcsEngineLifecycleServiceResult initializeAndStartRcsTransport(
                InitializeAndStartRcsTransportRequest request) {
            return new RcsEngineLifecycleServiceResult(RcsEngineLifecycleServiceResult.SUCCESS);
        }

        @Override
        public RcsEngineLifecycleServiceResult stopAllRcsTransportsExcept(
                StopAllRcsTransportsExceptRequest request) {
            return new RcsEngineLifecycleServiceResult(RcsEngineLifecycleServiceResult.SUCCESS);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (stateChangeReceiver != null) {
            unregisterReceiver(stateChangeReceiver);
            stateChangeReceiver = null;
        }
    }
    
    private void broadcastRcsAvailable() {
        sendRcsIntent("com.google.android.gms.rcs.action.REGISTRATION_STATE_CHANGED");
        sendRcsIntent("com.google.android.gms.rcs.action.CAPABILITY_UPDATE");
        sendRcsIntent("com.google.android.gms.rcs.action.PROVISIONING_COMPLETE");
    }
    
    private void sendRcsIntent(String action) {
        Intent intent = new Intent(action);
        intent.putExtra("timestamp", System.currentTimeMillis());
        intent.putExtra("hasToken", true);
        intent.putExtra("isValidAndUpdated", true);
        if ("com.google.android.gms.rcs.action.REGISTRATION_STATE_CHANGED".equals(action)) {
            intent.putExtra("state", 7);
        }
        sendBroadcast(intent);
    }
}
