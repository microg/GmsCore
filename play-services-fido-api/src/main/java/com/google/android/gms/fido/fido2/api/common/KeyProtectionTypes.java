/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.fido.fido2.api.common;

/**
 * The method used by the authenticator to protect the FIDO registration private key material. Available values are
 * defined in Section 3.2 Key Protection Types.
 */
public final class KeyProtectionTypes {
    /**
     * This flag must be set if the authenticator uses software-based key management. Exclusive in authenticator
     * metadata with KEY_PROTECTION_HARDWARE, KEY_PROTECTION_TEE, KEY_PROTECTION_SECURE_ELEMENT.
     */
    public static final short KEY_PROTECTION_SOFTWARE = 1;
    /**
     * This flag should be set if the authenticator uses hardware-based key management. Exclusive in authenticator
     * metadata with KEY_PROTECTION_SOFTWARE.
     */
    public static final short KEY_PROTECTION_HARDWARE = 2;
    /**
     * This flag should be set if the authenticator uses the Trusted Execution Environment for key management. In
     * authenticator metadata, this flag should be set in conjunction with KEY_PROTECTION_HARDWARE. Mutually exclusive
     * in authenticator metadata with KEY_PROTECTION_SOFTWARE, KEY_PROTECTION_SECURE_ELEMENT.
     */
    public static final short KEY_PROTECTION_TEE = 4;
    /**
     * This flag should be set if the authenticator uses a Secure Element for key management. In authenticator metadata,
     * this flag should be set in conjunction with KEY_PROTECTION_HARDWARE. Mutually exclusive in authenticator metadata
     * with KEY_PROTECTION_TEE, KEY_PROTECTION_SOFTWARE.
     */
    public static final short KEY_PROTECTION_SECURE_ELEMENT = 8;
    /**
     * This flag must be set if the authenticator does not store (wrapped) UAuth keys at the client, but relies on a
     * server-provided key handle. This flag must be set in conjunction with one of the other KEY_PROTECTION flags to
     * indicate how the local key handle wrapping key and operations are protected. Servers may unset this flag in
     * authenticator policy if they are not prepared to store and return key handles, for example, if they have a
     * requirement to respond indistinguishably to authentication attempts against userIDs that do and do not exist.
     * Refer to for more details.
     */
    public static final short KEY_PROTECTION_REMOTE_HANDLE = 16;
}
