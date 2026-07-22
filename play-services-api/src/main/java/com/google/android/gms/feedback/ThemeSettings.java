/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.feedback;

import androidx.annotation.NonNull;
import org.microg.gms.utils.ToStringHelper;
import org.microg.safeparcel.AutoSafeParcelable;

public class ThemeSettings extends AutoSafeParcelable {
    @Field(2)
    public int unknownInt2;
    @Field(3)
    public int unknownInt3;

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("ThemeSettings")
                .field("2", unknownInt2)
                .field("3", unknownInt3)
                .end();
    }

    public static final Creator<ThemeSettings> CREATOR = findCreator(ThemeSettings.class);
}
