package com.google.android.gms.rcs;

import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;

public class RcsBinder extends Binder {
    private static final String TAG = "RcsBinder";
    private final RcsService service;

    public RcsBinder(RcsService service) {
        this.service = service;
    }

    @Override
    protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        Log.d(TAG, "onTransact code=" + code);
        // Simulate RCS registration success
        if (code == 1) { // Simulated RCS_REGISTER
            reply.writeInt(1); // success
            reply.writeString("+15551234567"); // simulated phone number
            reply.writeString("token_abc123"); // simulated token
            return true;
        }
        return super.onTransact(code, data, reply, flags);
    }
}