/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.asterism;

import android.content.Context;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;

import org.microg.gms.constellation.ConstellationConstants;
import org.microg.gms.constellation.GoogleConstellationClient;

/**
 * Binder shim for the Asterism consent APIs used by Messages.
 */
public class AsterismServiceImpl extends Binder {
    private static final String TAG = "GmsAsterismSvcImpl";
    private static final String DESCRIPTOR = "com.google.android.gms.asterism.internal.IAsterismApiService";
    private static final String CB_DESCRIPTOR = "com.google.android.gms.asterism.internal.IAsterismCallbacks";

    private static final int TX_GET_CONSENT = 1;
    private static final int TX_SET_CONSENT = 2;
    private static final int TX_IS_PNVR_DEVICE = 3;

    private static final int CB_ON_CONSENT_FETCHED = 1;
    private static final int CB_ON_CONSENT_REGISTERED = 2;
    private static final int CB_ON_IS_PNVR_DEVICE = 3;

    private final Context context;

    public AsterismServiceImpl(Context context) {
        this.context = context;
        Log.i(TAG, "AsterismServiceImpl created");
    }

    @Override
    protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        if (code == INTERFACE_TRANSACTION) {
            reply.writeString(DESCRIPTOR);
            return true;
        }
        data.enforceInterface(DESCRIPTOR);

        switch (code) {
            case TX_GET_CONSENT:
                return handleGetConsent(data, reply);
            case TX_SET_CONSENT:
                return handleSetConsent(data, reply);
            case TX_IS_PNVR_DEVICE:
                return handleIsPnvrDevice(data, reply);
            default:
                Log.w(TAG, "Unknown tx code: " + code);
                return handleGenericSuccess(data, reply);
        }
    }

    /**
     * Return CONSENTED plus the Messages IID token used for ACS requests.
     */
    private boolean handleGetConsent(Parcel data, Parcel reply) {
        try {
            IBinder callback = data.readStrongBinder();
            data.readInt(); // request SafeParcel header or null marker
            Log.i(TAG, "getAsterismConsent called");

            String iidToken = null;
            try {
                kotlin.Pair<String, String> result = GoogleConstellationClient.getOrRegisterIidToken(
                    context, context.getPackageName(), ConstellationConstants.SENDER_MESSAGES_IID);
                iidToken = result.getFirst();
                Log.i(TAG, "IID token for sender " + ConstellationConstants.SENDER_MESSAGES_IID + ": " +
                    (iidToken != null ? iidToken.substring(0, Math.min(20, iidToken.length())) + "..." : "null"));
            } catch (Exception e) {
                Log.w(TAG, "Failed to get IID token: " + e.getMessage());
            }

            if (callback != null) {
                Parcel cb = Parcel.obtain();
                try {
                    cb.writeInterfaceToken(CB_DESCRIPTOR);
                    writeStatus(cb, 0);
                    writeGetConsentResponse(cb, 1 /* CONSENTED */, iidToken);
                    writeDefaultApiMetadata(cb);
                    callback.transact(CB_ON_CONSENT_FETCHED, cb, null, FLAG_ONEWAY);
                } finally {
                    cb.recycle();
                }
            }

            reply.writeNoException();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error in getAsterismConsent", e);
            reply.writeNoException();
            return true;
        }
    }

    /**
     * Acknowledge consent registration and return the Messages IID token.
     */
    private boolean handleSetConsent(Parcel data, Parcel reply) {
        try {
            IBinder callback = data.readStrongBinder();
            Log.i(TAG, "setAsterismConsent called");

            String iidToken = null;
            try {
                kotlin.Pair<String, String> result = GoogleConstellationClient.getOrRegisterIidToken(
                    context, context.getPackageName(), ConstellationConstants.SENDER_MESSAGES_IID);
                iidToken = result.getFirst();
            } catch (Exception e) {
                Log.w(TAG, "Failed to get IID token for setConsent response: " + e.getMessage());
            }

            if (callback != null) {
                Parcel cb = Parcel.obtain();
                try {
                    cb.writeInterfaceToken(CB_DESCRIPTOR);
                    writeStatus(cb, 0);
                    writeSetConsentResponse(cb, iidToken);
                    writeDefaultApiMetadata(cb);
                    callback.transact(CB_ON_CONSENT_REGISTERED, cb, null, FLAG_ONEWAY);
                } finally {
                    cb.recycle();
                }
            }

            reply.writeNoException();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error in setAsterismConsent", e);
            reply.writeNoException();
            return true;
        }
    }

    /**
     * Report that this device supports the PNVR Constellation path.
     */
    private boolean handleIsPnvrDevice(Parcel data, Parcel reply) {
        try {
            IBinder callback = data.readStrongBinder();
            Log.i(TAG, "getIsPnvrConstellationDevice called");

            if (callback != null) {
                Parcel cb = Parcel.obtain();
                try {
                    cb.writeInterfaceToken(CB_DESCRIPTOR);
                    writeStatus(cb, 0);
                    cb.writeInt(1); // true
                    writeDefaultApiMetadata(cb);
                    callback.transact(CB_ON_IS_PNVR_DEVICE, cb, null, FLAG_ONEWAY);
                } finally {
                    cb.recycle();
                }
            }

            reply.writeNoException();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error in getIsPnvrConstellationDevice", e);
            reply.writeNoException();
            return true;
        }
    }

    /**
     * Write a minimal SUCCESS Status parcelable.
     */
    private void writeStatus(Parcel dest, int statusCode) {
        int startPos = beginSafeParcelable(dest);
        writeSafeParcelField(dest, 1000, 1); // versionCode = 1
        writeSafeParcelField(dest, 1, statusCode); // statusCode
        endSafeParcelable(dest, startPos);
    }

    /**
     * Write GetAsterismConsentResponse.
     */
    private void writeGetConsentResponse(Parcel dest, int consentState, String iidToken) {
        dest.writeInt(1);
        int startPos = beginSafeParcelable(dest);
        writeSafeParcelField(dest, 1, 0); // requestCode = 0
        writeSafeParcelField(dest, 2, consentState); // 1 = CONSENTED
        if (iidToken != null) {
            writeSafeParcelStringField(dest, 3, iidToken); // iidToken
        }
        // field 4: gaiaToken - omit (null)
        writeSafeParcelField(dest, 5, 1); // consentVersion = 1
        endSafeParcelable(dest, startPos);
    }

    /**
     * Write SetAsterismConsentResponse SafeParcelable.
     * Fields: 1=requestCode(int), 2=iidToken(String), 3=gaiaToken(String)
     */
    private void writeSetConsentResponse(Parcel dest, String iidToken) {
        dest.writeInt(1); // non-null marker
        int startPos = beginSafeParcelable(dest);
        writeSafeParcelField(dest, 1, 0); // requestCode = 0
        if (iidToken != null) {
            writeSafeParcelStringField(dest, 2, iidToken); // iidToken
        }
        endSafeParcelable(dest, startPos);
    }

    /**
     * Write default ApiMetadata SafeParcelable (minimal).
     */
    private void writeDefaultApiMetadata(Parcel dest) {
        dest.writeInt(1); // non-null marker
        int startPos = beginSafeParcelable(dest);
        // ApiMetadata minimal: just versionCode
        writeSafeParcelField(dest, 1000, 1);
        endSafeParcelable(dest, startPos);
    }

    // === SafeParcel encoding helpers ===
    // SafeParcel format: [total_length:4] [field_header:4 field_data:N]... [end_marker:0]
    // field_header = (fieldId << 16) | dataSize, or (fieldId << 16) | 0xFFFF for variable-length

    private int beginSafeParcelable(Parcel dest) {
        // Write placeholder for total length
        int startPos = dest.dataPosition();
        dest.writeInt(0); // placeholder
        return startPos;
    }

    private void endSafeParcelable(Parcel dest, int startPos) {
        int endPos = dest.dataPosition();
        dest.setDataPosition(startPos);
        dest.writeInt(endPos - startPos - 4); // total length (excluding the length field itself)
        dest.setDataPosition(endPos);
    }

    private void writeSafeParcelField(Parcel dest, int fieldId, int value) {
        // Int field: header = (fieldId << 16) | 4 (size of int)
        dest.writeInt((fieldId << 16) | 4);
        dest.writeInt(value);
    }

    private void writeSafeParcelStringField(Parcel dest, int fieldId, String value) {
        // String: variable length, header = (fieldId << 16) | 0xFFFF
        dest.writeInt((fieldId << 16) | 0xFFFF);
        int lenPos = dest.dataPosition();
        dest.writeInt(0); // placeholder for data length
        int dataStart = dest.dataPosition();
        dest.writeString(value);
        int dataEnd = dest.dataPosition();
        dest.setDataPosition(lenPos);
        dest.writeInt(dataEnd - dataStart);
        dest.setDataPosition(dataEnd);
    }

    private boolean handleGenericSuccess(Parcel data, Parcel reply) {
        try {
            IBinder callback = null;
            try { callback = data.readStrongBinder(); } catch (Exception e) {}

            if (callback != null) {
                Parcel cb = Parcel.obtain();
                try {
                    cb.writeInterfaceToken(CB_DESCRIPTOR);
                    writeStatus(cb, 0);
                    callback.transact(CB_ON_CONSENT_FETCHED, cb, null, FLAG_ONEWAY);
                } finally {
                    cb.recycle();
                }
            }

            reply.writeNoException();
            return true;
        } catch (Exception e) {
            Log.w(TAG, "Error in generic handler", e);
            reply.writeNoException();
            return true;
        }
    }

    public IBinder asBinder() {
        return this;
    }
}
