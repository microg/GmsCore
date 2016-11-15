package org.microg.gms.gcm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import static android.content.Intent.ACTION_PACKAGE_REMOVED;
import static android.content.Intent.EXTRA_DATA_REMOVED;

public class UnregisterReceiver extends BroadcastReceiver {
    private static final String TAG = "GmsGcmUnregisterRcvr";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Package removed: " + intent);
        if (ACTION_PACKAGE_REMOVED.contains(intent.getAction()) && intent.getBooleanExtra(EXTRA_DATA_REMOVED, false)) {
            Log.d(TAG, "Package removed: " + intent.getData());
        }
    }
}
