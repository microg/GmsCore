package org.microg.gms.constellation;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public class ConstellationService extends Service {
    private static final String TAG = "GmsConstellationSvc";

    private final IBinder binder = new com.google.android.gms.constellation.internal.IConstellationService.Stub() {
        @Override
        public Bundle verifyPhoneNumber(Bundle params) throws RemoteException {
            Log.d(TAG, "verifyPhoneNumber iniciado. Simulando sucesso de GPNV (Google Phone Number Verification).");
            Bundle response = new Bundle();
            response.putInt("verification_status", 0); // 0 = VERIFIED_SUCCESS
            return response;
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind para Constellation recebido.");
        return binder;
    }
}
