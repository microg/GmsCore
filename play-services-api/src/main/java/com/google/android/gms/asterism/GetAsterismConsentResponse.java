/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.asterism;

import org.microg.safeparcel.AutoSafeParcelable;

public class GetAsterismConsentResponse extends AutoSafeParcelable {
    @Field(1)
    public boolean consented;
    @Field(2)
    public String consentToken;

    public static final Creator<GetAsterismConsentResponse> CREATOR = new AutoCreator<>(GetAsterismConsentResponse.class);
}
