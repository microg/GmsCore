/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.auth.aang;

import org.microg.gms.common.Hide;
import org.microg.safeparcel.AutoSafeParcelable;

@Hide
public class FetchAppRestrictionRequest extends AutoSafeParcelable {
    @Field(1)
    public GoogleAccount account;
    @Field(2)
    public String languageTag;

    public static final Creator<FetchAppRestrictionRequest> CREATOR = new AutoCreator<>(FetchAppRestrictionRequest.class);
}
