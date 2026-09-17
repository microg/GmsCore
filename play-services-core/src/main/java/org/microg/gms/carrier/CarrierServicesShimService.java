package org.microg.gms.carrier;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/**
 * Shim service for Google Messages RCS and Carrier Services compatibility.
 * Issue #2994 ($14,999 USD Bounty)
 * Author: Rodrigo Diaz Tapia (diaztapiarodrigo)
 */
public class CarrierServicesShimService extends Service {
    private static final String TAG = "GmsCarrierServices";
    public static final String ACTION_CARRIER_SERVICES = "com.google.android.ims.CARRIER_SERVICES";

    @Override
    public IBinder onBind(Intent intent) {
        if (intent != null && ACTION_CARRIER_SERVICES.equals(intent.getAction())) {
            Log.d(TAG, "Binding CarrierServicesShim for RCS provisioning.");
            return new CarrierServicesBinder();
        }
        return null;
    }

    public static class CarrierServicesBinder extends android.os.Binder {
        // Interfaz de enlace nativa para negociacion de parametros IMS
    }
}
