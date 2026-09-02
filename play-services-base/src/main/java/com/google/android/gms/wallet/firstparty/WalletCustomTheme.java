/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.wallet.firstparty;

import android.os.Bundle;
import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

@SafeParcelable.Class
public class WalletCustomTheme extends AbstractSafeParcelable {
    @Field(2)
    int mainThemeStyle;
    @Field(5)
    int startTransitionStyle;
    @Field(6)
    int endTransitionStyle;
    @Field(3)
    Bundle extraThemeParams;
    @Field(4)
    final String themeDescription;

    @Constructor
    public WalletCustomTheme() {
        this.mainThemeStyle = 0;
        this.startTransitionStyle = 0;
        this.endTransitionStyle = 0;
        this.extraThemeParams = new Bundle();
        this.themeDescription = "";
    }

    @Constructor
    public WalletCustomTheme(@Param(2) int mainThemeStyle, @Param(5) int startTransitionStyle, @Param(6) int endTransitionStyle,
                             @Param(3) Bundle extraThemeParams, @Param(4) String themeDescription) {
        this.mainThemeStyle = mainThemeStyle;
        this.startTransitionStyle = startTransitionStyle;
        this.endTransitionStyle = endTransitionStyle;
        this.extraThemeParams = extraThemeParams;
        this.themeDescription = themeDescription;
    }

    @Override
    public String toString() {
        return "WalletCustomTheme{" +
                "mainThemeStyle=" + mainThemeStyle +
                ", startTransitionStyle=" + startTransitionStyle +
                ", endTransitionStyle=" + endTransitionStyle +
                ", extraThemeParams=" + extraThemeParams +
                ", themeDescription='" + themeDescription + '\'' +
                '}';
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<WalletCustomTheme> CREATOR = findCreator(WalletCustomTheme.class);
}
