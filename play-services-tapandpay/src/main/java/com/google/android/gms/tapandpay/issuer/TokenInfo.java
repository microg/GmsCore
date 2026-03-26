/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.gms.tapandpay.issuer;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import com.google.android.gms.tapandpay.TapAndPay;
import org.microg.gms.utils.ToStringHelper;

@SafeParcelable.Class
public class TokenInfo extends AbstractSafeParcelable {
    @Field(1)
    @NonNull
    private final String issuerTokenId;
    @Field(2)
    @NonNull
    private final String issuerName;
    @Field(3)
    @NonNull
    private final String fpanLastFour;
    @Field(4)
    @NonNull
    private final String dpanLastFour;
    @Field(5)
    private final int tokenServiceProvider;
    @Field(6)
    private final int network;
    @Field(7)
    private final int tokenState;
    @Field(8)
    private final boolean isDefaultToken;
    @Field(9)
    @NonNull
    private final String portfolioName;

    @Constructor
    public TokenInfo(@NonNull @Param(1) String issuerTokenId, @NonNull @Param(2) String issuerName, @NonNull @Param(3) String fpanLastFour, @NonNull @Param(4) String dpanLastFour, @Param(5) @TapAndPay.TokenServiceProvider int tokenServiceProvider, @Param(6) @TapAndPay.CardNetwork int network, @Param(7) @TapAndPay.TokenState int tokenState, @Param(8) boolean isDefaultToken, @Param(9) @NonNull String portfolioName) {
        this.issuerTokenId = issuerTokenId;
        this.issuerName = issuerName;
        this.fpanLastFour = fpanLastFour;
        this.dpanLastFour = dpanLastFour;
        this.tokenServiceProvider = tokenServiceProvider;
        this.network = network;
        this.tokenState = tokenState;
        this.isDefaultToken = isDefaultToken;
        this.portfolioName = portfolioName;
    }

    @NonNull
    public String getDpanLastFour() {
        return dpanLastFour;
    }

    @NonNull
    public String getFpanLastFour() {
        return fpanLastFour;
    }

    public boolean getIsDefaultToken() {
        return isDefaultToken;
    }

    @NonNull
    public String getIssuerName() {
        return issuerName;
    }

    @NonNull
    public String getIssuerTokenId() {
        return issuerTokenId;
    }

    @TapAndPay.CardNetwork
    public int getNetwork() {
        return network;
    }

    @NonNull
    public String getPortfolioName() {
        return portfolioName;
    }

    @TapAndPay.TokenServiceProvider
    public int getTokenServiceProvider() {
        return tokenServiceProvider;
    }

    @TapAndPay.TokenState
    public int getTokenState() {
        return tokenState;
    }

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("TokenInfo").value(issuerTokenId)
                .field("issuerName", issuerName)
                .field("fpanLastFour", fpanLastFour)
                .field("dpanLastFour", dpanLastFour)
                .field("tokenServiceProvider", tokenServiceProvider)
                .field("network", network)
                .field("tokenState", tokenState)
                .field("isDefaultToken", isDefaultToken)
                .field("portfolioName", portfolioName)
                .end();
    }

    public static final SafeParcelableCreatorAndWriter<TokenInfo> CREATOR = findCreator(TokenInfo.class);

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }
}
