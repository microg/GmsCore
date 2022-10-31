/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.fido.fido2.api.common;

/**
 * The method used by the authenticator to protect the matcher that performs user verification. Available values are
 * defined in Section 3.3 Matcher Protection Types.
 */
public final class MatcherProtectionTypes {
    /**
     * This flag must be set if the authenticator's matcher is running in software. Exclusive in authenticator metadata
     * with MATCHER_PROTECTION_TEE, MATCHER_PROTECTION_ON_CHIP.
     */
    public static final short MATCHER_PROTECTION_SOFTWARE = 1;
    /**
     * This flag should be set if the authenticator's matcher is running inside the Trusted Execution Environment.
     * Mutually exclusive in authenticator metadata with MATCHER_PROTECTION_SOFTWARE, MATCHER_PROTECTION_ON_CHIP.
     */
    public static final short MATCHER_PROTECTION_TEE = 2;
    /**
     * This flag should be set if the authenticator's matcher is running on the chip. Mutually exclusive in
     * authenticator metadata with MATCHER_PROTECTION_TEE, MATCHER_PROTECTION_SOFTWARE.
     */
    public static final short MATCHER_PROTECTION_ON_CHIP = 4;
}
