package com.google.android.gms.carrier;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface ICarrierService extends IInterface {
    boolean provisionCarrier(String carrierId, ICarrierProvisioningCallback callback) throws RemoteException;
    boolean isCarrierProvisioned() throws RemoteException;

    abstract class Stub extends Binder implements ICarrierService {
        public Stub() {
            this.attachInterface(this, DESCRIPTOR);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            switch (code) {
                case INTERFACE_TRANSACTION: {
                    reply.writeString(DESCRIPTOR);
                    return true;
                }
                case TRANSACTION_provisionCarrier: {
                    data.enforceInterface(DESCRIPTOR);
                    String _arg0 = data.readString();
                    ICarrierProvisioningCallback _arg1 = ICarrierProvisioningCallback.Stub.asInterface(data.readStrongBinder());
                    boolean _result = this.provisionCarrier(_arg0, _arg1);
                    reply.writeNoException();
                    reply.writeInt(_result ? 1 : 0);
                    return true;
                }
                case TRANSACTION_isCarrierProvisioned: {
                    data.enforceInterface(DESCRIPTOR);
                    boolean _result = this.isCarrierProvisioned();
                    reply.writeNoException();
                    reply.writeInt(_result ? 1 : 0);
                    return true;
                }
            }
            return super.onTransact(code, data, reply, flags);
        }

        static final String DESCRIPTOR = "com.google.android.gms.carrier.ICarrierService";
        static final int TRANSACTION_provisionCarrier = IBinder.FIRST_CALL_TRANSACTION + 0;
        static final int TRANSACTION_isCarrierProvisioned = IBinder.FIRST_CALL_TRANSACTION + 1;
    }

    class Default implements ICarrierService {
        @Override
        public boolean provisionCarrier(String carrierId, ICarrierProvisioningCallback callback) throws RemoteException {
            return false;
        }

        @Override
        public boolean isCarrierProvisioned() throws RemoteException {
            return false;
        }

        @Override
        public IBinder asBinder() {
            return null;
        }
    }
}