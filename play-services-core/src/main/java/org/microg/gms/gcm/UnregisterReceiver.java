package org.microg.gms.gcm;

import static android.content.Intent.ACTION_PACKAGE_DATA_CLEARED;
import static android.content.Intent.ACTION_PACKAGE_FULLY_REMOVED;
import static android.content.Intent.ACTION_PACKAGE_REMOVED;
import static android.content.Intent.EXTRA_DATA_REMOVED;
import static android.content.Intent.EXTRA_REPLACING;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.List;

public class UnregisterReceiver extends BroadcastReceiver {
    private static final String TAG = "GmsGcmUnregisterRcvr";

    @Override
    public void onReceive(final Context context, Intent intent) {
        Log.d(TAG, "Package changed: " + intent);
        if ((ACTION_PACKAGE_REMOVED.contains(intent.getAction()) && intent.getBooleanExtra(EXTRA_DATA_REMOVED, false) &&
                !intent.getBooleanExtra(EXTRA_REPLACING, false)) ||
                ACTION_PACKAGE_FULLY_REMOVED.contains(intent.getAction()) ||
                ACTION_PACKAGE_DATA_CLEARED.contains(intent.getAction())) {
            final GcmDatabase database = new GcmDatabase(context);
            final String packageName = intent.getData().getSchemeSpecificPart();
            Log.d(TAG, "Package removed or data cleared: " + packageName);
            final GcmDatabase.App app = database.getApp(packageName);
            if (app != null) {
                new Thread(() -> {
                    List<GcmDatabase.Registration> registrations = database.getRegistrationsByApp(packageName);
                    boolean deletedAll = true;
                    for (GcmDatabase.Registration registration : registrations) {
                        deletedAll &= PushRegisterManager.unregister(context, registration.packageName, registration.signature, null, null).deleted != null;
                    }
                    if (deletedAll) {
                        database.removeApp(packageName);
                    }
                    database.close();
                }).start();
            } else {
                database.close();
            }
        }
    }
}
