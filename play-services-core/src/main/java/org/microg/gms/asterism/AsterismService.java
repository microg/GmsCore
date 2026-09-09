package org.microg.gms.asterism;

import android.os.Binder;
import android.os.Bundle;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;
import com.google.android.gms.common.internal.GetServiceRequest;
import com.google.android.gms.common.internal.IGmsCallbacks;
import org.microg.gms.BaseService;
import org.microg.gms.common.GmsService;

public class AsterismService extends BaseService {
    private static final String TAG = "GmsAsterism";

    public AsterismService() {
        super("GmsAsterism", GmsService.ASTERISM);
    }

    @Override
    public void handleServiceRequest(IGmsCallbacks callback, GetServiceRequest request, GmsService service) throws RemoteException {
        Log.d(TAG, "handleServiceRequest: Interceptando Asterism RCS Handshake");
        // Retorna o Binder que aprova o consentimento do RCS
        callback.onPostInitComplete(0, new AsterismConsentBinder(), null);
    }

    // Binder funcional que responde positivamente ao Google Messages
    private static class AsterismConsentBinder extends Binder {
        @Override
        protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            if (reply != null) {
                reply.writeNoException();
                Bundle bundle = new Bundle();
                bundle.putInt("consent_status", 1); // 1 = ACCEPTED
                bundle.putBoolean("rcs_eligible", true);
                bundle.putString("terms_version", "v1");
                bundle.writeToParcel(reply, 0);
            }
            return true;
        }
    }
}