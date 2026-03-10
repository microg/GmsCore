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
 * The authentication method/factor used by the authenticator to verify the user.
 */
@PublicApi
public final class UserVerificationMethods {
    /**
     * This flag must be set if the authenticator is able to confirm user presence in any fashion. If this flag and no
     * other is set for user verification, the guarantee is only that the authenticator cannot be operated without some
     * human intervention, not necessarily that the sensing of "presence" provides any level of user verification (e.g.
     * a device that requires a button press to activate).
     */
    public static final int USER_VERIFY_PRESENCE = 1;
    /**
     * This flag must be set if the authenticator uses any type of measurement of a fingerprint for user verification.
     */
    public static final int USER_VERIFY_FINGERPRINT = 2;
    /**
     * This flag must be set if the authenticator uses a local-only passcode (i.e. a passcode not known by the server)
     * for user verification.
     */
    public static final int USER_VERIFY_PASSCODE = 4;
    /**
     * This flag must be set if the authenticator uses a voiceprint (also known as speaker recognition) for user
     * verification.
     */
    public static final int USER_VERIFY_VOICEPRINT = 8;
    /**
     * This flag must be set if the authenticator uses any manner of face recognition to verify the user.
     */
    public static final int USER_VERIFY_FACEPRINT = 16;
    /**
     * This flag must be set if the authenticator uses any form of location sensor or measurement for user verification.
     */
    public static final int USER_VERIFY_LOCATION = 32;
    /**
     * This flag must be set if the authenticator uses any form of eye biometrics for user verification.
     */
    public static final int USER_VERIFY_EYEPRINT = 64;
    /**
     * This flag must be set if the authenticator uses a drawn pattern for user verification.
     */
    public static final int USER_VERIFY_PATTERN = 128;
    /**
     * This flag must be set if the authenticator uses any measurement of a full hand (including palm-print, hand
     * geometry or vein geometry) for user verification.
     */
    public static final int USER_VERIFY_HANDPRINT = 256;
    /**
     * This flag must be set if the authenticator will respond without any user interaction (e.g. Silent Authenticator).
     */
    public static final int USER_VERIFY_NONE = 512;
    /**
     * If an authenticator sets multiple flags for user verification types, it may also set this flag to indicate that
     * all verification methods will be enforced (e.g. faceprint AND voiceprint). If flags for multiple user
     * verification methods are set and this flag is not set, verification with only one is necessary (e.g. fingerprint
     * OR passcode).
     */
    public static final int USER_VERIFY_ALL = 1024;

    private UserVerificationMethods() {
    }
}
