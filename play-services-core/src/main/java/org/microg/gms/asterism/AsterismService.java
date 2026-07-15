package org.microg.gms.asterism;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public class AsterismService extends Service {
    private static final String TAG = "GmsAsterismSvc";

    private final IBinder binder = new com.google.android.gms.asterism.internal.IAsterismService.Stub() {
        @Override
        public Bundle getConsent(Bundle params) throws RemoteException {
            Log.d(TAG, "getConsent chamado. Retornando termos de RCS aceitos de forma padrão.");
            Bundle response = new Bundle();
            response.putInt("consent_status", 1); // 1 = CONSENTED
            return response;
        }

        @Override
        public Bundle setConsent(Bundle params) throws RemoteException {
            Log.d(TAG, "setConsent chamado. Gravando aceitação no banco.");
            Bundle response = new Bundle();
            response.putBoolean("success", true);
            return response;
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind recebido para Asterism: " + intent.getAction());
        return binder;
    }
}
