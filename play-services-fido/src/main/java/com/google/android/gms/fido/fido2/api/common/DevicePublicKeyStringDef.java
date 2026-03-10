/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.fido.fido2.api.common;

import androidx.annotation.NonNull;

/**
 * Enum values to be passed into DevicePublicKeyExtension. This tells the authenticator what to return for the DPK extension object.
 * <p>
 * These values are placeholders until the final spec is determined. For now,
 * <ul>
 * <li>NONE is to not request a DPK.</li>
 * <li>DIRECT is to request a DPK.</li>
 * <li>INDIRECT has no function at this time.</li>
 * </ul>
 */
public @interface DevicePublicKeyStringDef {
    @NonNull
    String NONE = "none";
    @NonNull
    String INDIRECT = "indirect";
    @NonNull
    String DIRECT = "direct";
}
