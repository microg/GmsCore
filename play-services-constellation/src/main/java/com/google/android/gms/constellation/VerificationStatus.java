/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.constellation;

import com.google.android.gms.constellation.GetPnvCapabilitiesResponse.VerificationCapability;

/**
 * Whether a phone-number verification method is available for a SIM, as reported by
 * {@code getPnvCapabilities}. The integer values are part of the wire contract with the
 * calling app (Google Messages) and must not change.
 */
public enum VerificationStatus {
    SUPPORTED(1),
    UNSUPPORTED_CARRIER(2),
    UNSUPPORTED_API_VERSION(3),
    UNSUPPORTED_SIM_NOT_READY(4);

    public final int value;

    VerificationStatus(int value) {
        this.value = value;
    }

    public static VerificationStatus fromInt(int value) {
        switch (value) {
            case 2:
                return UNSUPPORTED_CARRIER;
            case 3:
                return UNSUPPORTED_API_VERSION;
            case 4:
                return UNSUPPORTED_SIM_NOT_READY;
            default:
                return SUPPORTED;
        }
    }

    public static VerificationCapability verificationCapability(int verificationMethod, VerificationStatus status) {
        return new VerificationCapability(verificationMethod, status.value);
    }
}
