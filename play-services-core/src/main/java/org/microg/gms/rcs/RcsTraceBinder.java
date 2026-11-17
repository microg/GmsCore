/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.rcs;

import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.util.Log;

/**
 * Diagnostic binder for the RCS-related GMS services.
 *
 * <p>Google Messages binds these services and then calls methods on the returned binder.
 * microG does not implement those methods yet, so this binder only observes: it records
 * every transaction's metadata (code, announced interface descriptor, parcel size) into
 * an {@link RcsTrace} and then returns {@code false}, which the Binder framework turns
 * into an unknown-transaction error for the caller. Nothing in the parcel payload is ever
 * logged or stored.
 *
 * <p>One exception is {@link IBinder#INTERFACE_TRANSACTION}: the descriptor the caller
 * announced on a previous call is echoed back, so that the caller's {@code asInterface}
 * check can pass and it proceeds to issue real method calls that we can then observe.
 * This is a diagnostic shim, not an implementation of any RCS interface.
 */
public class RcsTraceBinder extends Binder {
    private static final String TAG = "GmsRcsTraceBinder";

    private final RcsTrace trace;
    private final String service;
    private final String callerPackage;

    /** Last interface descriptor announced by the caller, if any. */
    private volatile String lastDescriptor;

    public RcsTraceBinder(RcsTrace trace, String service, String callerPackage) {
        this.trace = trace;
        this.service = service;
        this.callerPackage = callerPackage;
    }

    @Override
    protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) {
        if (code == INTERFACE_TRANSACTION) {
            if (lastDescriptor != null) {
                reply.writeString(lastDescriptor);
                return true;
            }
            return false;
        }

        String descriptor = "<unreadable>";
        int dataSize = 0;
        try {
            dataSize = data.dataSize();
            data.setDataPosition(0);
            String token = data.readString();
            if (token != null) {
                descriptor = token;
                lastDescriptor = token;
            }
        } catch (Exception e) {
            // The parcel does not start with a string; there is nothing safe to record.
            Log.d(TAG, "could not read interface token for " + service + " code " + code);
        }

        trace.record(service, callerPackage, code, descriptor, dataSize);
        Log.d(TAG, "transaction: service=" + service + " caller=" + callerPackage
                + " code=" + code + " descriptor=" + descriptor + " size=" + dataSize);
        return false;
    }
}
