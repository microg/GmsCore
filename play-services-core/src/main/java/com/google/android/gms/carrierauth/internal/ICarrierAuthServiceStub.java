/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.carrierauth.internal;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

import com.google.android.gms.carrierauth.EAPAKARequest;
import com.google.android.gms.carrierauth.EapInfoRequest;
import com.google.android.gms.common.api.ApiMetadata;

public abstract class ICarrierAuthServiceStub extends Binder implements IInterface {
    public static final String DESCRIPTOR =
            "com.google.android.gms.carrierauth.internal.ICarrierAuthService";
    public static final int TRANSACTION_PERFORM_EAP_AKA = IBinder.FIRST_CALL_TRANSACTION;
    public static final int TRANSACTION_GET_EAP_INFO = IBinder.FIRST_CALL_TRANSACTION + 1;

    protected ICarrierAuthServiceStub() {
        attachInterface(this, DESCRIPTOR);
    }

    public static ICarrierAuthServiceStub asInterface(IBinder binder) {
        IInterface local = binder == null ? null : binder.queryLocalInterface(DESCRIPTOR);
        return local instanceof ICarrierAuthServiceStub
                ? (ICarrierAuthServiceStub) local
                : null;
    }

    @Override
    public IBinder asBinder() {
        return this;
    }

    @Override
    protected boolean onTransact(int code, Parcel data, Parcel reply, int flags)
            throws RemoteException {
        if (code == INTERFACE_TRANSACTION) {
            if (reply != null) {
                reply.writeString(DESCRIPTOR);
            }
            return true;
        }
        if (code != TRANSACTION_PERFORM_EAP_AKA && code != TRANSACTION_GET_EAP_INFO) {
            return super.onTransact(code, data, reply, flags);
        }

        data.enforceInterface(DESCRIPTOR);
        ICarrierAuthCallbacks callback = ICarrierAuthCallbacks.Stub.asInterface(
                data.readStrongBinder());
        if (code == TRANSACTION_PERFORM_EAP_AKA) {
            EAPAKARequest request = data.readInt() != 0
                    ? EAPAKARequest.CREATOR.createFromParcel(data) : null;
            ApiMetadata metadata = data.readInt() != 0
                    ? ApiMetadata.CREATOR.createFromParcel(data) : null;
            performEAPAKA(callback, request, metadata);
        } else {
            EapInfoRequest request = data.readInt() != 0
                    ? EapInfoRequest.CREATOR.createFromParcel(data) : null;
            ApiMetadata metadata = data.readInt() != 0
                    ? ApiMetadata.CREATOR.createFromParcel(data) : null;
            getEapInfo(callback, request, metadata);
        }
        if (reply != null) {
            reply.writeNoException();
        }
        return true;
    }

    public abstract void performEAPAKA(ICarrierAuthCallbacks callback,
            EAPAKARequest request, ApiMetadata metadata) throws RemoteException;

    public abstract void getEapInfo(ICarrierAuthCallbacks callback,
            EapInfoRequest request, ApiMetadata metadata) throws RemoteException;
}
