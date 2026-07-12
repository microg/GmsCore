package org.microg.gms.wearable;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Stub Wearable service for WearOS pairing support in MicroG.
 */
public class WearableService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
