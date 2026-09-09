package org.microg.gms.constellation;

import android.os.Binder;
import android.os.Bundle;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;
import com.google.android.gms.common.internal.GetServiceRequest;
import com.google.android.gms.common.internal.IGmsCallbacks;
import org.microg.gms.BaseService;
import org.microg.gms.common.GmsService;

public class ConstellationService extends BaseService {
    private static final String TAG = "GmsConstellation";

    public ConstellationService() {
        super("GmsConstellation", GmsService.CONSTELLATION);
    }

    @Override
    public void handleServiceRequest(IGmsCallbacks callback, GetServiceRequest request, GmsService service) throws RemoteException {
        Log.d(TAG, "handleServiceRequest: Interceptando Constellation Phone Verification");
        // Retorna o Binder que valida o status do número de telefone
        callback.onPostInitComplete(0, new ConstellationVerifyBinder(), null);
    }

    // Binder funcional que simula verificação de número concluída
    private static class ConstellationVerifyBinder extends Binder {
        @Override
        protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            if (reply != null) {
                reply.writeNoException();
                Bundle bundle = new Bundle();
                bundle.putString("status", "VERIFIED");
                bundle.putString("state", "READY");
                bundle.putBoolean("success", true);
                bundle.writeToParcel(reply, 0);
            }
            return true;
        }
    }
}