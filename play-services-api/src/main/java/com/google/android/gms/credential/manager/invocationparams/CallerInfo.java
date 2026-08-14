/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.credential.manager.invocationparams;

import androidx.annotation.NonNull;
import org.microg.safeparcel.AutoSafeParcelable;

public class CallerInfo extends AutoSafeParcelable {
    @Field(1)
    public String source;
    @Field(2)
    public String medium;
    @Field(3)
    public String campaign;
    @Field(4)
    public String content;

    @NonNull
    @Override
    public String toString() {
        return "CallerInfo(" + source + "," + medium + "," + campaign + "," + content + ")";
    }

    public static final Creator<CallerInfo> CREATOR = new AutoCreator<>(CallerInfo.class);
}
