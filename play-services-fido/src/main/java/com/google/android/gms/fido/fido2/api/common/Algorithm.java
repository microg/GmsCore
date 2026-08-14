/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.fido.fido2.api.common;

/**
 * An interface for an algorithm used in public key encryption. All implementations must conform to the guidelines
 * regarding algorithm registrations in RFC8152.
 */
public interface Algorithm {
    /**
     * Gets the COSE value for the algorithm used in the encryption of the credential.
     */
    int getAlgoValue();
}
