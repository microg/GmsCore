package org.microg.gms.rcs;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.rcs.IRcsService;

public class RcsProvisioningUtil {
    private static final String TAG = "RcsProvisioningUtil";

    public static boolean triggerProvisioning(Context context) {
        Intent intent = new Intent("com.google.android.gms.rcs.SERVICE");
        ResolveInfo info = context.getPackageManager().resolveService(intent, PackageManager.GET_RESOLVED_FILTER);
        if (info != null) {
            Intent serviceIntent = new Intent(intent);
            serviceIntent.setPackage(info.serviceInfo.packageName);
            context.startService(serviceIntent);
            return true;
        }
        Log.w(TAG, "No RCS service found");
        return false;
    }

    public static boolean isRcsEnabled(Context context) {
        Intent intent = new Intent("com.google.android.gms.rcs.SERVICE");
        ResolveInfo info = context.getPackageManager().resolveService(intent, 0);
        if (info != null) {
            try {
                IBinder binder = context.bindService(intent, null, Context.BIND_AUTO_CREATE);
                if (binder != null) {
                    IRcsService service = IRcsService.Stub.asInterface(binder);
                    if (service != null) {
                        return service.isRcsEnabled();
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to query RCS state", e);
            }
        }
        return false;
    }
}
