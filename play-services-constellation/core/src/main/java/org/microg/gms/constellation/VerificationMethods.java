/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation;

/**
 * Verification method and verification status codes as seen by the calling app
 * (Google Messages) over the Constellation binder interface.
 *
 * <p>Method values come from the Constellation server protocol: MO_SMS=1, MT_SMS=2,
 * CARRIER_ID=3, IMSI_LOOKUP=5, REGISTERED_SMS=7, FLASH_CALL=8, TS43=11 on the wire.
 * Over the binder interface TS43 is reported as 9. Status values: UNKNOWN/NONE=0,
 * VERIFIED=1, FAILED=2, THROTTLED=3, SKIPPED=4, NOT_REQUIRED=5, PENDING=6,
 * PHONE_NUMBER_ENTRY_REQUIRED=7, INELIGIBLE=8, DENIED=9, NOT_IN_SERVICE=10.
 *
 * <p>These integers are part of the contract with the caller and must not change.
 * Pure Java on purpose, so it can be unit tested without Android.
 */
public final class VerificationMethods {
    private VerificationMethods() {
    }

    // Verification methods (client-facing values)
    public static final int METHOD_UNKNOWN = 0;
    public static final int METHOD_MO_SMS = 1;
    public static final int METHOD_MT_SMS = 2;
    public static final int METHOD_CARRIER_ID = 3;
    public static final int METHOD_IMSI_LOOKUP = 5;
    public static final int METHOD_REGISTERED_SMS = 7;
    public static final int METHOD_FLASH_CALL = 8;
    public static final int METHOD_TS43 = 9;

    // Verification methods (server protocol values)
    public static final int SERVER_METHOD_UNKNOWN = 0;
    public static final int SERVER_METHOD_MO_SMS = 1;
    public static final int SERVER_METHOD_MT_SMS = 2;
    public static final int SERVER_METHOD_CARRIER_ID = 3;
    public static final int SERVER_METHOD_IMSI_LOOKUP = 5;
    public static final int SERVER_METHOD_REGISTERED_SMS = 7;
    public static final int SERVER_METHOD_FLASH_CALL = 8;
    public static final int SERVER_METHOD_TS43 = 11;

    // Verification statuses (client-facing values)
    public static final int STATUS_UNKNOWN = 0;
    public static final int STATUS_VERIFIED = 1;
    public static final int STATUS_FAILED = 2;
    public static final int STATUS_THROTTLED = 3;
    public static final int STATUS_SKIPPED = 4;
    public static final int STATUS_NOT_REQUIRED = 5;
    public static final int STATUS_PENDING = 6;
    public static final int STATUS_PHONE_NUMBER_ENTRY_REQUIRED = 7;
    public static final int STATUS_INELIGIBLE = 8;
    public static final int STATUS_DENIED = 9;
    public static final int STATUS_NOT_IN_SERVICE = 10;

    /**
     * Maps a server-protocol verification method to the value the calling app expects.
     * TS43 is 11 on the wire but 9 over the binder interface; every other method keeps
     * its value. Unknown values map to {@link #METHOD_UNKNOWN}.
     */
    public static int toClientMethod(int serverMethod) {
        switch (serverMethod) {
            case SERVER_METHOD_TS43:
                return METHOD_TS43;
            case SERVER_METHOD_MO_SMS:
                return METHOD_MO_SMS;
            case SERVER_METHOD_MT_SMS:
                return METHOD_MT_SMS;
            case SERVER_METHOD_CARRIER_ID:
                return METHOD_CARRIER_ID;
            case SERVER_METHOD_IMSI_LOOKUP:
                return METHOD_IMSI_LOOKUP;
            case SERVER_METHOD_REGISTERED_SMS:
                return METHOD_REGISTERED_SMS;
            case SERVER_METHOD_FLASH_CALL:
                return METHOD_FLASH_CALL;
            default:
                return METHOD_UNKNOWN;
        }
    }

    /** Returns true if this build can attempt the given client-facing method locally. */
    public static boolean isSupportedLocally(int clientMethod) {
        return clientMethod == METHOD_MO_SMS || clientMethod == METHOD_MT_SMS;
    }
}
