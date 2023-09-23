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
 * Algorithm names and COSE identifiers for RSA (public) keys.
 */
@PublicApi
public enum RSAAlgorithm implements Algorithm {
    /**
     * RSASSA-PKCS1-v1_5 w/ SHA-256
     */
    RS256(-257),
    /**
     * RSASSA-PKCS1-v1_5 w/ SHA-384
     */
    RS384(-258),
    /**
     * RSASSA-PKCS1-v1_5 w/ SHA-512
     */
    RS512(-259),
    /**
     * The legacy value for "RSASSA-PKCS1-v1_5 w/ SHA-1"
     *
     * @deprecated please use {@link #RS1} instead.
     */
    @Deprecated
    LEGACY_RS1(-262),
    /**
     * RSASSA-PSS w/ SHA-256
     */
    PS256(-37),
    /**
     * RSASSA-PSS w/ SHA-384
     */
    PS384(-38),
    /**
     * RSASSA-PSS w/ SHA-512
     */
    PS512(-39),
    /**
     * RSASSA-PKCS1-v1_5 w/ SHA-1
     */
    RS1(-65535);

    private final int algoValue;

    RSAAlgorithm(int algoValue) {
        this.algoValue = algoValue;
    }

    @Override
    public int getAlgoValue() {
        return algoValue;
    }
}
