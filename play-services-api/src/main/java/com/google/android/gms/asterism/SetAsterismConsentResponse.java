/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.asterism;

import org.microg.safeparcel.AutoSafeParcelable;

public class SetAsterismConsentResponse extends AutoSafeParcelable {
    @Field(1)
    public boolean success;

    public static final Creator<SetAsterismConsentResponse> CREATOR = new AutoCreator<>(SetAsterismConsentResponse.class);
}
