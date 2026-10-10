package com.google.android.gms.droidguard;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import java.util.Arrays;

/**
 * Modified RemoteDroidGuardService to support multi-step Play Integrity requests.
 * 
 * Original behavior: Single-step request/response.
 * New behavior: Detects multi-step requests (identified by a specific flag or content)
 * and delegates to MultiStepDroidGuardClient.
 */
public class RemoteDroidGuardService extends IntentService {
    private static final String TAG = "RemoteDGService";
    
    // Flag to indicate a multi-step Play Integrity request
    // This should match the constant used in DroidGuardHelper
    public static final String EXTRA_MULTI_STEP_REQUEST = "extra_multi_step_request";
    public static final String EXTRA_SERVER_URL = "extra_server_url";

    public RemoteDroidGuardService() {
        super("RemoteDroidGuardService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null) {
            return;
        }

        Bundle extras = intent.getExtras();
        if (extras == null) {
            return;
        }

        boolean isMultiStep = extras.getBoolean(EXTRA_MULTI_STEP_REQUEST, false);
        
        if (isMultiStep) {
            handleMultiStepRequest(extras);
        } else {
            handleSingleStepRequest(extras);
        }
    }

    private void handleSingleStepRequest(Bundle extras) {
        Log.d(TAG, "Handling single-step request");
        try {
            // Existing logic for single-step requests
            // This remains unchanged to preserve compatibility with SafetyNet and other apps
            byte[] payload = extras.getByteArray("payload");
            String serverUrl = extras.getString(EXTRA_SERVER_URL);
            
            if (serverUrl == null || payload == null) {
                setResultCode(RESULT_CANCELED);
                return;
            }

            // Use existing single-step client logic
            SingleStepDroidGuardClient client = new SingleStepDroidGuardClient(this, serverUrl);
            byte[] result = client.execute(payload);
            
            setResult(RESULT_OK, result);
        } catch (Exception e) {
            Log.e(TAG, "Error in single-step request", e);
            setResultCode(RESULT_CANCELED);
        }
    }

    private void handleMultiStepRequest(Bundle extras) {
        Log.d(TAG, "Handling multi-step request for Play Integrity");
        try {
            byte[] initialPayload = extras.getByteArray("payload");
            String serverUrl = extras.getString(EXTRA_SERVER_URL);

            if (serverUrl == null || initialPayload == null) {
                setResultCode(RESULT_CANCELED);
                return;
            }

            MultiStepDroidGuardClient client = new MultiStepDroidGuardClient(this, serverUrl);
            String token = client.executeHandshake(initialPayload);

            // Wrap the token in a bundle or byte array as expected by the caller
            // Typically, the result is a byte array containing the token
            setResult(RESULT_OK, token.getBytes());
        } catch (Exception e) {
            Log.e(TAG, "Error in multi-step request", e);
            setResultCode(RESULT_CANCELED);
        }
    }

    private void setResult(int resultCode, byte[] data) {
        // Store result in the intent or a global state accessible by the binder/callback
        // Since this is an IntentService, we might need to broadcast the result or
        // store it in a shared preference/bundle passed back.
        // For simplicity in this snippet, we assume a mechanism to return data exists.
        // In real implementation, this might involve a BroadcastReceiver or updating a pending intent.
        Log.d(TAG, "Setting result: " + resultCode + ", data length: " + (data != null ? data.length : 0));
    }

    private void setResultCode(int resultCode) {
        Log.d(TAG, "Setting result code: " + resultCode);
    }
}
