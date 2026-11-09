package org.microg.gms.rcs;

import android.util.Log;

public class RcsCoreProtocolManager {
    private static final String TAG = "RcsCoreProtocolManager";
    private boolean isImsConnected = false;

    public void initializeImsSession() {
        Log.d(TAG, "Initializing RCS IMS session connection state machine...");
        this.isImsConnected = true;
    }

    public boolean getImsConnectionState() {
        return isImsConnected;
    }
}
