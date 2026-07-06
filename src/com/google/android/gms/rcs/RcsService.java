package com.google.android.gms.rcs;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.rcs.internal.IRcsService;
import com.google.android.gms.rcs.internal.IRcsProvisioningCallback;

public class RcsService extends Service {
    private static final String TAG = "RcsService";
    private IRcsService.Stub binder = new IRcsService.Stub() {
        @Override
        public void requestRcsProvisioning(String phoneNumber, IRcsProvisioningCallback callback) throws RemoteException {
            Log.d(TAG, "requestRcsProvisioning called for: " + phoneNumber);
            // Retrieve the actual phone number from SIM if available
            String actualPhoneNumber = getDevicePhoneNumber();
            if (actualPhoneNumber == null || !actualPhoneNumber.equals(phoneNumber)) {
                callback.onProvisioningFailed(Status.RESULT_INTERNAL_ERROR);
                return;
            }
            // Simulate successful provisioning; in production, send a verification SMS
            callback.onProvisioningSuccess(phoneNumber);
        }

        @Override
        public void queryCapabilities(IRcsProvisioningCallback callback) throws RemoteException {
            Log.d(TAG, "queryCapabilities");
            // Return that RCS is available
            callback.onCapabilitiesChanged(true);
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private String getDevicePhoneNumber() {
        TelephonyManager tm = getSystemService(TelephonyManager.class);
        if (tm != null) {
            String number = tm.getLine1Number();
            if (number != null && !number.isEmpty()) {
                return number;
            }
            // Try subscription manager
            SubscriptionManager sm = getSystemService(SubscriptionManager.class);
            if (sm != null) {
                for (int i = 0; i < sm.getActiveSubscriptionInfoCountMax(); i++) {
                    String num = sm.getActiveSubscriptionInfo(i).getNumber();
                    if (num != null && !num.isEmpty()) return num;
                }
            }
        }
        return null;
    }
}
