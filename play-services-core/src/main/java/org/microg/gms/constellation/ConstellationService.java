package org.microg.gms.constellation;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import org.microg.gms.BaseService;
import org.microg.gms.common.GmsService;

public class ConstellationService extends BaseService {
    public ConstellationService() {
        super("GmsConstellation", GmsService.CONSTELLATION);
    }

    @Override
    public IBinder onBind(android.content.Intent intent) {
        return new ConstellationBinder();
    }

    private static class ConstellationBinder extends Binder implements IInterface {
        @Override
        public IBinder asBinder() {
            return this;
        }
    }
}
