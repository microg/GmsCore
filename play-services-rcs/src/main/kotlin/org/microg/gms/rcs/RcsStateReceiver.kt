/*
 * Copyright 2024-2026 microG Project Team
 * Licensed under Apache-2.0
 *
 * RcsStateReceiver - Broadcast receiver for system events
 * 
 * Listens for SIM state changes, boot completion, and network changes
 * to automatically manage RCS registration state.
 */

package org.microg.gms.rcs

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class RcsStateReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "RcsStateReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        
        Log.d(TAG, "Received broadcast: $action")
        
        when (action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                handleBootCompleted(context)
            }
            
            "android.intent.action.SIM_STATE_CHANGED" -> {
                handleSimStateChanged(context, intent)
            }
            
            "android.net.conn.CONNECTIVITY_CHANGE" -> {
                handleConnectivityChanged(context)
            }
        }
    }

    private fun handleBootCompleted(context: Context) {
        Log.d(TAG, "Boot completed, checking RCS state")
        
        val provisioningManager = RcsProvisioningManager(context)
        
        if (provisioningManager.isProvisioned()) {
            Log.d(TAG, "RCS was previously provisioned, registration will resume")
        }
    }

    private fun handleSimStateChanged(context: Context, intent: Intent) {
        val simState = intent.getStringExtra("ss")
        Log.d(TAG, "SIM state changed: $simState")
        
        when (simState) {
            "READY" -> {
                Log.d(TAG, "SIM card is ready")
            }
            
            "ABSENT" -> {
                Log.d(TAG, "SIM card removed")
            }
            
            "LOCKED" -> {
                Log.d(TAG, "SIM card is locked")
            }
        }
    }

    private fun handleConnectivityChanged(context: Context) {
        val isNetworkAvailable = NetworkHelper.isNetworkAvailable(context)
        Log.d(TAG, "Network connectivity changed: available=$isNetworkAvailable")
    }
}
