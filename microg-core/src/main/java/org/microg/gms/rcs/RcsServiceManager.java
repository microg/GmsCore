package org.microg.gms.rcs;

import android.content.Context;
import android.os.IBinder;
import org.microg.gms.common.Service;

@Service("com.google.android.gms.rcs.service.START")
public class RcsServiceManager {

    public static IBinder bindService(Context context) {
        // Return the binder from the RcsService
        return new RcsService().onBind(null);
    }
}