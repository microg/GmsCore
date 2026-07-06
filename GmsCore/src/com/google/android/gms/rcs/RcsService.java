package com.google.android.gms.rcs;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.common.rcs.IRcsService;

/**
 * Main RCS service that provides core RCS functionality to Google Messages.
 * This handles message sending, receiving, and session management.
 * Currently a stub that returns success for all operations.
 */
public class RcsService extends Service {

    private static final String TAG = "RcsService";

    private final IRcsService.Stub binder = new IRcsService.Stub() {
        @Override
        public boolean isRcsEnabled() {
            Log.d(TAG, "isRcsEnabled called, returning true");
            return true;
        }

        @Override
        public void sendMessage(String destination, String text, com.google.android.gms.common.rcs.IRcsMessageCallback callback) {
            Log.i(TAG, "Sending RCS message to " + destination);
            // In production, implement actual RCS message sending over the carrier network
            // For now, simulate success
            try {
                callback.onMessageSent("msg_" + System.currentTimeMillis());
            } catch (Exception e) {
                Log.e(TAG, "Callback error", e);
            }
        }

        @Override
        public void startSession(String sessionId) {
            Log.d(TAG, "Session started: " + sessionId);
        }

        @Override
        public void stopSession(String sessionId) {
            Log.d(TAG, "Session stopped: " + sessionId);
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "RcsService created");
    }
}