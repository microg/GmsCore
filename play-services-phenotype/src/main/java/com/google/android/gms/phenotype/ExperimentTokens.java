/*
 * SPDX-FileCopyrightText: 2020 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.phenotype;

import org.microg.safeparcel.AutoSafeParcelable;

public class ExperimentTokens extends AutoSafeParcelable {
    @Field(2)
    public String field2;
    @Field(3)
    public byte[] direct;
    @Field(4)
    public byte[][] gaia;
    @Field(5)
    public byte[][] pseudo;
    @Field(6)
    public byte[][] always;
    @Field(7)
    public byte[][] other;
    @Field(8)
    public int[] weak;
    @Field(9)
    public byte[][] directs;
    @Field(10)
    public int[] genericDimensions;

    public static final Creator<ExperimentTokens> CREATOR = findCreator(ExperimentTokens.class);
}
