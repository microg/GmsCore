/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.googlehelp;

import org.microg.safeparcel.AutoSafeParcelable;

public class SupportRequestHelp extends AutoSafeParcelable {
    @Field(1)
    public GoogleHelp googleHelp;
    @Field(4)
    public String phoneNumber;

    public static final Creator<SupportRequestHelp> CREATOR = findCreator(SupportRequestHelp.class);
}
