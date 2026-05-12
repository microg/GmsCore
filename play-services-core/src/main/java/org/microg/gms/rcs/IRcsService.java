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

/** RCS Service binder stub (svc 189). */
public abstract class IRcsService extends Binder {
    private static final String TAG = "GmsRcsServiceBinder";

    public static abstract class Stub extends IRcsService {
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
