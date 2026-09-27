/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.rcs;

import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;

/**
 * Minimal binder for {@code com.google.android.gms.rcs.START}.
 *
 * Full IRcsService AIDL is not yet reverse-engineered; Google Messages primarily
 * needs a successful service bind so Constellation/Asterism can drive provisioning.
 * Transactions are logged to aid further RE without failing the bind.
 */
public abstract class IRcsService extends Binder {
    private static final String TAG = "GmsRcsServiceBinder";

    public abstract static class Stub extends IRcsService {
        @Override
        protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            Log.d(TAG, "onTransact: code=" + code + ", flags=" + flags);
            return super.onTransact(code, data, reply, flags);
        }

        public IBinder asBinder() {
            return this;
        }
    }
}
