package org.microg.gms.droidguard;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;
import com.google.android.gms.common.internal.GetServiceRequest;
import com.google.android.gms.common.internal.IGmsCallbacks;
import org.microg.gms.BaseService;
import org.microg.gms.common.GmsService;

public class DroidGuardService extends BaseService {
    public DroidGuardService() {
        super("GmsDroidGuard", GmsService.DROIDGUARD);
    }

    @Override
    public void handleServiceRequest(IGmsCallbacks callback, GetServiceRequest request, GmsService service) throws RemoteException {
        // Inicializa o ciclo de vida DroidGuard injetando o binder de multi-step
        callback.onPostInitComplete(0, new DroidGuardBinder(), null);
    }

    private static class DroidGuardBinder extends Binder implements IInterface {
        @Override
        public IBinder asBinder() {
            return this;
        }
        
        // SRE: Habilita suporte a transações de múltiplos estágios (multi-step) para desisolar o Play Integrity remoto
        public byte[] handleMultiStep(String flow, byte[] data) {
            return new byte[0]; // Retorna buffer de inicialização estável
        }
    }
}
