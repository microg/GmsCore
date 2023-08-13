/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.credential.manager.invocationparams;

import androidx.annotation.NonNull;
import org.microg.safeparcel.AutoSafeParcelable;

public class CallerInfo extends AutoSafeParcelable {
    @Field(1)
    public String s1;
    @Field(2)
    public String s2;
    @Field(3)
    public String s3;
    @Field(4)
    public String s4;

    @NonNull
    @Override
    public String toString() {
        return "CallerInfo(" + s1 + "," + s2 + "," + s3 + "," + s4 + ")";
    }

    public static final Creator<CallerInfo> CREATOR = new AutoCreator<>(CallerInfo.class);
}
