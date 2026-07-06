package com.google.android.gms.rcs;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IRcsService extends IInterface {
    boolean provisionPhoneNumber(String phoneNumber, IProvisioningCallback callback) throws RemoteException;
    boolean isRcsEnabled() throws RemoteException;
    boolean setRcsEnabled(boolean enabled) throws RemoteException;
    boolean provisionDevice(String carrierId, IProvisioningCallback callback) throws RemoteException;

    abstract class Stub extends Binder implements IRcsService {
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
                case TRANSACTION_provisionPhoneNumber: {
                    data.enforceInterface(DESCRIPTOR);
                    String _arg0 = data.readString();
                    IProvisioningCallback _arg1 = IProvisioningCallback.Stub.asInterface(data.readStrongBinder());
                    boolean _result = this.provisionPhoneNumber(_arg0, _arg1);
                    reply.writeNoException();
                    reply.writeInt(_result ? 1 : 0);
                    return true;
                }
                case TRANSACTION_isRcsEnabled: {
                    data.enforceInterface(DESCRIPTOR);
                    boolean _result = this.isRcsEnabled();
                    reply.writeNoException();
                    reply.writeInt(_result ? 1 : 0);
                    return true;
                }
                case TRANSACTION_setRcsEnabled: {
                    data.enforceInterface(DESCRIPTOR);
                    boolean _arg0 = data.readInt() != 0;
                    boolean _result = this.setRcsEnabled(_arg0);
                    reply.writeNoException();
                    reply.writeInt(_result ? 1 : 0);
                    return true;
                }
                case TRANSACTION_provisionDevice: {
                    data.enforceInterface(DESCRIPTOR);
                    String _arg0 = data.readString();
                    IProvisioningCallback _arg1 = IProvisioningCallback.Stub.asInterface(data.readStrongBinder());
                    boolean _result = this.provisionDevice(_arg0, _arg1);
                    reply.writeNoException();
                    reply.writeInt(_result ? 1 : 0);
                    return true;
                }
            }
            return super.onTransact(code, data, reply, flags);
        }

        static final String DESCRIPTOR = "com.google.android.gms.rcs.IRcsService";
        static final int TRANSACTION_provisionPhoneNumber = IBinder.FIRST_CALL_TRANSACTION + 0;
        static final int TRANSACTION_isRcsEnabled = IBinder.FIRST_CALL_TRANSACTION + 1;
        static final int TRANSACTION_setRcsEnabled = IBinder.FIRST_CALL_TRANSACTION + 2;
        static final int TRANSACTION_provisionDevice = IBinder.FIRST_CALL_TRANSACTION + 3;
    }

    class Default implements IRcsService {
        @Override
        public boolean provisionPhoneNumber(String phoneNumber, IProvisioningCallback callback) throws RemoteException {
            return false;
        }

        @Override
        public boolean isRcsEnabled() throws RemoteException {
            return false;
        }

        @Override
        public boolean setRcsEnabled(boolean enabled) throws RemoteException {
            return false;
        }

        @Override
        public boolean provisionDevice(String carrierId, IProvisioningCallback callback) throws RemoteException {
            return false;
        }

        @Override
        public IBinder asBinder() {
            return null;
        }
    }
}