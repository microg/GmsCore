/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.googlehelp;

import org.microg.safeparcel.AutoSafeParcelable;

public class InProductHelp extends AutoSafeParcelable {
    @Field(1)
    public GoogleHelp googleHelp;
    public static final Creator<InProductHelp> CREATOR = findCreator(InProductHelp.class);
}
