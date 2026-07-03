/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.asterism;

import org.microg.safeparcel.AutoSafeParcelable;

public class GetAsterismConsentRequest extends AutoSafeParcelable {
    @Field(1)
    public String[] gcmCapablePackageNames;

    public static final Creator<GetAsterismConsentRequest> CREATOR = new AutoCreator<>(GetAsterismConsentRequest.class);
}
