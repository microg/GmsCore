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

import com.google.android.gms.carrierauth.EAPAKAResponse;
import com.google.android.gms.carrierauth.EapInfoResponse;
import com.google.android.gms.common.api.ApiMetadata;
import com.google.android.gms.common.api.Status;

public interface ICarrierAuthCallbacks extends IInterface {
    String DESCRIPTOR = "com.google.android.gms.carrierauth.internal.ICarrierAuthCallbacks";
    int TRANSACTION_ON_EAP_AKA_RESPONSE = IBinder.FIRST_CALL_TRANSACTION;
    int TRANSACTION_ON_EAP_INFO_RESPONSE = IBinder.FIRST_CALL_TRANSACTION + 1;

    void onEAPAKAResponse(Status status, EAPAKAResponse response, ApiMetadata metadata)
            throws RemoteException;

    void onEapInfoResponse(Status status, EapInfoResponse response, ApiMetadata metadata)
            throws RemoteException;

    abstract class Stub extends Binder implements ICarrierAuthCallbacks {
        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static ICarrierAuthCallbacks asInterface(IBinder binder) {
            if (binder == null) {
                return null;
            }
            IInterface local = binder.queryLocalInterface(DESCRIPTOR);
            return local instanceof ICarrierAuthCallbacks
                    ? (ICarrierAuthCallbacks) local
                    : new Proxy(binder);
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
            if (code != TRANSACTION_ON_EAP_AKA_RESPONSE
                    && code != TRANSACTION_ON_EAP_INFO_RESPONSE) {
                return super.onTransact(code, data, reply, flags);
            }
            data.enforceInterface(DESCRIPTOR);
            Status status = data.readInt() != 0 ? Status.CREATOR.createFromParcel(data) : null;
            ApiMetadata metadata;
            if (code == TRANSACTION_ON_EAP_AKA_RESPONSE) {
                EAPAKAResponse response = data.readInt() != 0
                        ? EAPAKAResponse.CREATOR.createFromParcel(data) : null;
                metadata = data.readInt() != 0 ? ApiMetadata.CREATOR.createFromParcel(data) : null;
                onEAPAKAResponse(status, response, metadata);
            } else {
                EapInfoResponse response = data.readInt() != 0
                        ? EapInfoResponse.CREATOR.createFromParcel(data) : null;
                metadata = data.readInt() != 0 ? ApiMetadata.CREATOR.createFromParcel(data) : null;
                onEapInfoResponse(status, response, metadata);
            }
            return true;
        }

        private static final class Proxy implements ICarrierAuthCallbacks {
            private final IBinder remote;

            Proxy(IBinder remote) {
                this.remote = remote;
            }

            @Override
            public IBinder asBinder() {
                return remote;
            }

            @Override
            public void onEAPAKAResponse(Status status, EAPAKAResponse response,
                    ApiMetadata metadata) throws RemoteException {
                Parcel data = Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    writeParcelable(data, status);
                    writeParcelable(data, response);
                    writeParcelable(data, metadata);
                    remote.transact(TRANSACTION_ON_EAP_AKA_RESPONSE, data, null, IBinder.FLAG_ONEWAY);
                } finally {
                    data.recycle();
                }
            }

            @Override
            public void onEapInfoResponse(Status status, EapInfoResponse response,
                    ApiMetadata metadata) throws RemoteException {
                Parcel data = Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    writeParcelable(data, status);
                    writeParcelable(data, response);
                    writeParcelable(data, metadata);
                    remote.transact(TRANSACTION_ON_EAP_INFO_RESPONSE, data, null, IBinder.FLAG_ONEWAY);
                } finally {
                    data.recycle();
                }
            }

            private static void writeParcelable(Parcel data, android.os.Parcelable value) {
                data.writeInt(value == null ? 0 : 1);
                if (value != null) {
                    value.writeToParcel(data, 0);
                }
            }
        }
    }
}
