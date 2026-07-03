/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.asterism;

import org.microg.safeparcel.AutoSafeParcelable;

public class SetAsterismConsentRequest extends AutoSafeParcelable {
    @Field(1)
    public String[] gcmCapablePackageNames;
    @Field(2)
    public boolean consented;

    public static final Creator<SetAsterismConsentRequest> CREATOR = new AutoCreator<>(SetAsterismConsentRequest.class);
}
