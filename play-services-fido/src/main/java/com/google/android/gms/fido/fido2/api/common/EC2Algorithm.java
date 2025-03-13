/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.fido.fido2.api.common;

import org.microg.gms.common.PublicApi;

/**
 * Algorithm names and COSE identifiers for EC2 (public) keys.
 */
@PublicApi
public enum EC2Algorithm implements Algorithm {
    /**
     * ECDH-ES with HKDF-SHA-256
     */
    ECDH_HKDF_256(-25),
    /**
     * EdDSA with Ed25519
     */
    ED25519(-8),
    /**
     * TPM_ECC_BN_P256 curve w/ SHA-256
     */
    ED256(-260),
    /**
     * ECC_BN_ISOP512 curve w/ SHA-512
     */
    ED512(-261),
    /**
     * ECDSA w/ SHA-256
     */
    ES256(-7),
    /**
     * ECDSA w/ SHA-384
     */
    ES384(-35),
    /**
     * ECDSA w/ SHA-512
     */
    ES512(-36);

    private final int algoValue;

    EC2Algorithm(int algoValue) {
        this.algoValue = algoValue;
    }

    @Override
    public int getAlgoValue() {
        return algoValue;
    }
}
