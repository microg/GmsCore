/*
 * SPDX-FileCopyrightText: 2020 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.phenotype;

import org.microg.gms.common.Hide;
import org.microg.safeparcel.AutoSafeParcelable;

@Hide
public class GenericDimension extends AutoSafeParcelable {
    @Field(1)
    public int a;
    @Field(2)
    public int b;

    public static final Creator<GenericDimension> CREATOR = findCreator(GenericDimension.class);
}
