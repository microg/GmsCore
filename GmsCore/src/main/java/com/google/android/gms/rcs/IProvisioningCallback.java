package com.google.android.gms.rcs;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IProvisioningCallback extends IInterface {
    void onProvisioningStatus(int status) throws RemoteException;

    abstract class Stub extends Binder implements IProvisioningCallback {
        public Stub() {
            this.attachInterface(this, DESCRIPTOR);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            if (code == INTERFACE_TRANSACTION) {
                reply.writeString(DESCRIPTOR);
                return true;
            } else if (code == TRANSACTION_onProvisioningStatus) {
                data.enforceInterface(DESCRIPTOR);
                int _arg0 = data.readInt();
                this.onProvisioningStatus(_arg0);
                reply.writeNoException();
                return true;
            }
            return super.onTransact(code, data, reply, flags);
        }

        static final String DESCRIPTOR = "com.google.android.gms.rcs.IProvisioningCallback";
        static final int TRANSACTION_onProvisioningStatus = IBinder.FIRST_CALL_TRANSACTION + 0;
    }

    class Default implements IProvisioningCallback {
        @Override
        public void onProvisioningStatus(int status) throws RemoteException {
        }

        @Override
        public IBinder asBinder() {
            return null;
        }
    }
}