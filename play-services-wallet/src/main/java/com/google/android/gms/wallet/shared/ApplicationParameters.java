/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.wallet.shared;

import android.accounts.Account;
import android.os.Bundle;
import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import com.google.android.gms.wallet.firstparty.WalletCustomTheme;

@SafeParcelable.Class
public class ApplicationParameters extends AbstractSafeParcelable {
    @Field(2)
    public int environment;
    @Field(3)
    public Account buyerAccount;
    @Field(4)
    public Bundle extraParams;
    @Field(5)
    public boolean isPurchaseManagerFlow;
    @Field(6)
    public int themeMode;
    @Field(7)
    public WalletCustomTheme walletCustomTheme;
    @Field(8)
    final int uiVariant;
    @Field(9)
    double popupWidth;
    @Field(10)
    double popupHeight;
    @Field(11)
    final int forceFullScreen;
    @Field(12)
    final int integratorType;

    @Constructor
    public ApplicationParameters() {
        this.isPurchaseManagerFlow = false;
        this.environment = 1;
        this.themeMode = 1;
        this.uiVariant = 0;
        this.forceFullScreen = 0;
        this.integratorType = -1;
    }

    @Constructor
    public ApplicationParameters(@Param(8) int uiVariant, @Param(11) int forceFullScreen, @Param(12) int integratorType, @Param(10) double popupHeight, @Param(9) double popupWidth,
                                 @Param(7) WalletCustomTheme walletCustomTheme, @Param(6) int themeMode, @Param(5) boolean isPurchaseManagerFlow,
                                 @Param(4) Bundle extraParams, @Param(3) Account buyerAccount, @Param(2) int environment) {
        this.uiVariant = uiVariant;
        this.forceFullScreen = forceFullScreen;
        this.integratorType = integratorType;
        this.popupHeight = popupHeight;
        this.popupWidth = popupWidth;
        this.walletCustomTheme = walletCustomTheme;
        this.themeMode = themeMode;
        this.isPurchaseManagerFlow = isPurchaseManagerFlow;
        this.extraParams = extraParams;
        this.buyerAccount = buyerAccount;
        this.environment = environment;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<ApplicationParameters> CREATOR = findCreator(ApplicationParameters.class);
}
