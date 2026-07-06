package org.microg.gms.rcs;

import android.content.Context;
import android.telephony.TelephonyManager;
import android.util.Log;

import org.microg.gms.common.Utils;

/**
 * Helper class to provide carrier information for RCS provisioning.
 */
public class RcsProvisioningHelper {

    private static final String TAG = "MicroG-RcsHelper";

    public static String getMsisdn(Context context) {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        // Attempt to get the line number (may not be available on all carriers)
        String msisdn = tm.getLine1Number();
        if (msisdn == null || msisdn.isEmpty()) {
            msisdn = "0000000000"; // fallback
        }
        Log.d(TAG, "Using MSISDN: " + msisdn);
        return msisdn;
    }

    public static String getImsi(Context context) {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String imsi = tm.getSubscriberId();
        if (imsi == null || imsi.isEmpty()) {
            imsi = "310150123456789"; // fallback \n        }
        Log.d(TAG, "Using IMSI: " + imsi);
        return imsi;
    }

    public static boolean isSimReady(Context context) {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        return tm.getSimState() == TelephonyManager.SIM_STATE_READY;
    }
}
